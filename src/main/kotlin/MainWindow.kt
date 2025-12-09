package ca.kebs.courrier

import ca.kebs.courrier.data.MessageDTO
import ca.kebs.courrier.helpers.splitFrom
import ca.kebs.courrier.helpers.toMessageDTO
import ca.kebs.courrier.models.InboxRow
import ca.kebs.courrier.models.MailRow
import ca.kebs.courrier.services.MailManager
import goa.Client
import goa.GoaObject
import io.github.jwharm.javagi.gtk.annotations.GtkChild
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate
import jakarta.mail.Folder
import org.gnome.adw.ApplicationWindow
import org.gnome.adw.Clamp
import org.gnome.adw.HeaderBar
import org.gnome.adw.StatusPage
import org.gnome.gio.Cancellable
import org.gnome.glib.GLib
import org.gnome.gtk.Box
import org.gnome.gtk.Button
import org.gnome.gtk.DropDown
import org.gnome.gtk.ListView
import org.gnome.gtk.Orientation
import org.gnome.gtk.StringList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.gnome.adw.NavigationPage
import org.gnome.adw.NavigationSplitView
import org.gnome.gio.AppInfo
import org.gnome.gio.ListStore
import org.gnome.gtk.Frame
import org.gnome.gtk.Label
import org.gnome.gtk.ScrolledWindow
import org.gnome.gtk.SingleSelection
import org.gnome.gtk.Stack
import org.gnome.pango.EllipsizeMode
import org.gnome.webkit.NavigationPolicyDecision
import org.gnome.webkit.PolicyDecisionType
import org.gnome.webkit.WebView
import java.text.SimpleDateFormat

@GtkTemplate(name = "MainWindow", ui = "/ca/kebs/main.ui")
class MainWindow : ApplicationWindow() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @GtkChild(name = "account_dropdown")
    lateinit var accountDropDown: DropDown

    @GtkChild(name = "inner_split_view")
    lateinit var innerSplitView: NavigationSplitView

    // Inboxes widgets
    @GtkChild(name = "inboxes_list_view")
    lateinit var inboxesListView: ListView

    @GtkChild(name = "inboxes_stack")
    lateinit var inboxesStack: Stack

    @GtkChild(name = "inboxes_placeholder")
    lateinit var inboxesPlaceholder: Box

    @GtkChild(name = "inboxes_scrolled_window")
    lateinit var inboxesScrolledWindow: ScrolledWindow

    // Emails widgets
    @GtkChild(name = "emails_navigation_page")
    lateinit var emailsNavigationPage: NavigationPage

    @GtkChild(name = "emails_list_view")
    lateinit var emailsListView: ListView

    @GtkChild(name = "emails_stack")
    lateinit var emailsStack: Stack

    @GtkChild(name = "emails_placeholder")
    lateinit var emailsPlaceholder: Box

    @GtkChild(name = "emails_scrolled_window")
    lateinit var emailsScrolledWindow: ScrolledWindow

    // Content widgets
    @GtkChild(name = "content_stack")
    lateinit var contentStack: Stack

    @GtkChild(name = "content_box")
    lateinit var contentBox: Box

    @GtkChild(name = "content_frame")
    lateinit var contentFrame: Frame

    @GtkChild(name = "content_status_page")
    lateinit var contentStatusPage: StatusPage

    // Email's details
    @GtkChild(name = "details_sender_box")
    lateinit var detailsSenderBox: Box

    @GtkChild(name = "details_subject_box")
    lateinit var detailsSubjectBox: Box

    @GtkChild(name = "details_to_box")
    lateinit var detailsToBox: Box

    init {
        val goaClient = Client.sync(Cancellable())
        val goaAccounts = goaClient.getAccounts()

        val validAccounts = mutableListOf<GoaObject>()
        for (goaAccount in goaAccounts) {
            if (goaAccount.mail != null) {
                validAccounts.add(goaAccount)
            }
        }

        val accountsByEmail = mutableMapOf<String, GoaObject>()
        val accountStringList = StringList()

        for (account in validAccounts) {
            accountStringList.append(account.mail.emailAddress)
            accountsByEmail[account.mail.emailAddress] = account
        }

        accountDropDown.model = accountStringList
        accountDropDown.onNotify("selected") {
            val index = accountDropDown.selected
            if (index >= 0) {
                scope.launch {
                    loadAccount(validAccounts[0])
                }
            }
        }

        // Setup factories
        inboxesListView.factory = setupInboxesFactories()
        emailsListView.factory = setupMessagesFactories()

        inboxesStack.visibleChild = inboxesPlaceholder

        // Build and show a page when no mail accounts are found
        if (validAccounts.isEmpty()) {
            showEmptyEmailsWindow()
        } else {
            // Logs into the first account when the app starts
            scope.launch {
                loadAccount(validAccounts[0])
            }
        }
    }

    private fun loadAccount(account: GoaObject) {
        scope.launch {
            MailManager.switchAccount(account)
            val folders = MailManager.getFolders()

            val inboxRows = folders.map { folder ->
                val iconName = when (folder.name) {
                    "INBOX" -> "inbox-symbolic"
                    "Inbox" -> "inbox-symbolic"
                    "All Mail" -> "mail-archive-symbolic"
                    "Drafts" -> "pencil-symbolic"
                    "Important" -> "exclamation-mark-symbolic"
                    "Sent Mail" -> "outbox-symbolic"
                    "Spam" -> "junk-symbolic"
                    "Starred" -> "star-large-symbolic"
                    "Trash" -> "user-trash-symbolic"
                    else -> "mailbox-symbolic"
                }
                InboxRow(iconName, if (folder.name == "INBOX") "Inbox" else folder.name, folder.fullName)
            }

            GLib.idleAdd(GLib.PRIORITY_DEFAULT) {
                val listStore = ListStore<InboxRow>()
                listStore.addAll(inboxRows)
                val selection: SingleSelection<ListStore<InboxRow>> = SingleSelection(listStore)

                selection.canUnselect = true
                selection.autoselect = false
                selection.selected = -1
                inboxesListView.model = selection

                selection.onNotify("selected") { _ ->
                    val index = selection.selected
                    val inboxRow = if (index >= 0) inboxRows[index] else null
                    if (inboxRow != null) {
                        emailsNavigationPage.title = inboxRow.name
                        emailsStack.visibleChild = emailsPlaceholder
                        contentStack.visibleChild = contentStatusPage
                        val folder = folders[index]
                        loadMessages(folder)
                    } else {
                        emailsNavigationPage.title = "Messages"
                        // TODO: Display a status page
                    }
                    innerSplitView.showContent = false
                }

                inboxesStack.visibleChild = inboxesScrolledWindow
                false
            }
        }
    }

    private fun loadMessages(folder: Folder) {
        scope.launch {
            MailManager.switchFolder(folder)
            val messages: List<MessageDTO> = MailManager.getMessages().map { message -> toMessageDTO(message) }
            val sortedMessages = messages.sortedByDescending { it.receivedDate }

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            val mailRows = sortedMessages.map { message ->
                MailRow(
                    message.subject,
                    message.from,
                    dateFormatter.format(message.receivedDate),
                    message.ref
                )
            }

            GLib.idleAdd(GLib.PRIORITY_DEFAULT) {
                val listStore = ListStore<MailRow>()
                listStore.addAll(mailRows)
                val selection: SingleSelection<ListStore<MailRow>> = SingleSelection(listStore)

                selection.canUnselect = true
                selection.autoselect = false
                selection.selected = -1
                emailsListView.model = selection

                selection.onNotify("selected") {
                    val index = selection.selected
                    val message = if (index >= 0) sortedMessages[index] else null
                    if (message != null) {
                        resetEmailDetails()

                        var isInit = false
                        val webView = WebView()

                        // Redirect links to the default browser
                        webView.onDecidePolicy { decision, decisionType ->
                            if (decisionType == PolicyDecisionType.NAVIGATION_ACTION && isInit) {
                                val navigation = decision as NavigationPolicyDecision
                                val request = navigation.navigationAction.request
                                val uri = request.uri

                                AppInfo.launchDefaultForUri(uri, null)

                                decision.ignore()
                                true
                            } else {
                                isInit = true
                                false
                            }
                        }

                        // Disable web related context menu
                        webView.onContextMenu { _, hitTestResult ->
                            if (hitTestResult.contextIsSelection() ||
                                hitTestResult.contextIsLink() ||
                                hitTestResult.contextIsImage()
                            ) {
                                false
                            } else {
                                true
                            }
                        }

                        webView.loadHtml(message.content, null)
                        contentFrame.child = webView
                        contentStack.visibleChild = contentBox
                        innerSplitView.showContent = true

                        setupEmailDetails(message)
                    } else {
                        contentStack.visibleChild = contentStatusPage
                        innerSplitView.showContent = false
                    }
                }

                emailsStack.visibleChild = emailsScrolledWindow
                false
            }
        }
    }

    private fun setupEmailDetails(message: MessageDTO) {
        val to = message.to
        val sender = splitFrom(message.from)

        val senderBox = Box.builder().setSpacing(8).build()

        val from = Label.builder().setUseMarkup(true).setEllipsize(EllipsizeMode.END).setSelectable(true).build()
        from.setMarkup("<b>${sender.first}</b>")

        val address = Label.builder()
            .setLabel(sender.second)
            .setEllipsize(EllipsizeMode.END)
            .setCssClasses(arrayOf("dimmed"))
            .setSelectable(true)
            .build()

        senderBox.append(from)
        senderBox.append(address)
        detailsSenderBox.append(senderBox)

        val subject =
            Label.builder().setLabel(message.subject).setEllipsize(EllipsizeMode.END).setSelectable(true).build()
        detailsSubjectBox.append(subject)

        val recipientBox = Box.builder().setSpacing(8).build()

        for (address in to) {
            val label = Label.builder().setLabel(address).setEllipsize(EllipsizeMode.END).setSelectable(true).build()
            recipientBox.append(label)
        }

        detailsToBox.append(recipientBox)
    }

    private fun resetEmailDetails() {
        while (detailsSenderBox.firstChild != null) {
            detailsSenderBox.remove(detailsSenderBox.firstChild)
        }
        while (detailsSubjectBox.firstChild != null) {
            detailsSubjectBox.remove(detailsSubjectBox.firstChild)
        }
        while (detailsToBox.firstChild != null) {
            detailsToBox.remove(detailsToBox.firstChild)
        }
    }

    private fun showEmptyEmailsWindow() {
        val mainBox = Box.builder().setOrientation(Orientation.VERTICAL).build()

        // Add header bar
        val headerBar = HeaderBar.builder().setCssClasses(arrayOf("flat")).build()
        mainBox.append(headerBar)

        // Add status page
        val clamp =
            Clamp.builder().setOrientation(Orientation.HORIZONTAL).setVexpand(true).setMaximumSize(300).build()

        val button = Button.builder()
            .setLabel("Go to settings")
            .setCssClasses(arrayOf("pill", "suggested-action"))
            .onClicked { GLib.spawnCommandLineAsync("gnome-control-center online-accounts") }
            .build()

        val statusPage = StatusPage.builder()
            .setTitle("No Accounts Found")
            .setDescription("Go to Gnome's settings and add an online account that supports email")
            .setChild(button)
            .build()

        clamp.child = statusPage
        mainBox.append(clamp)
        this.content = mainBox
    }
}