package ca.kebs.courrier

import ca.kebs.courrier.models.InboxRow
import ca.kebs.courrier.models.MailRow
import ca.kebs.courrier.services.MailService
import goa.Client
import goa.GoaObject
import io.github.jwharm.javagi.base.Out
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
import org.gnome.gio.ListStore
import org.gnome.gtk.ScrolledWindow
import org.gnome.gtk.SingleSelection
import org.gnome.gtk.Stack
import org.gnome.webkit.WebView
import java.text.SimpleDateFormat

@GtkTemplate(name = "MainWindow", ui = "/ca/kebs/main.ui")
class MainWindow : ApplicationWindow() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @GtkChild(name = "account_dropdown")
    lateinit var accountDropDown: DropDown

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

    @GtkChild(name = "content_scrolled_window")
    lateinit var contentScrolledWindow: ScrolledWindow

    @GtkChild(name = "content_status_page")
    lateinit var contentStatusPage: StatusPage

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
        emailsListView.factory = setupEmailsFactories()

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

    suspend fun loginMail(account: GoaObject): MailService {
        val mail = account.mail
        val service = MailService(mail.imapHost)

        val oauth2 = account.oauth2Based
        val outToken = Out<String>()
        val outExpires = Out<Int>()

        val success = oauth2.callGetAccessTokenSync(outToken, outExpires, Cancellable())

        if (!success) {
            throw RuntimeException("Could not get access token from GOA")
        }

        val accessToken = outToken.get().toString()
        val username = mail.imapUserName

        service.login(username, accessToken)

        return service
    }

    private fun loadAccount(account: GoaObject) {
        scope.launch {
            val mailService = loginMail(account)
            val folders = mailService.getAllFolders()

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
                for (inboxRow in inboxRows) {
                    listStore.append(inboxRow)
                }
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
                        loadMail(mailService, folder)
                    } else {
                        emailsNavigationPage.title = "Messages"
                        // TODO: Display a status page
                    }
                }

                inboxesStack.visibleChild = inboxesScrolledWindow
                false
            }
        }
    }

    private fun loadMail(service: MailService, folder: Folder) {
        scope.launch {
            val mails = service.getAllMails(folder)
            val sortedMails = mails.sortedByDescending { it.receivedDate }

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            val mailRows = sortedMails.map { mail ->
                MailRow(mail.subject, mail.from, dateFormatter.format(mail.receivedDate))
            }

            GLib.idleAdd(GLib.PRIORITY_DEFAULT) {
                val listStore = ListStore<MailRow>()
                for (mailRow in mailRows) {
                    listStore.append(mailRow)
                }
                val selection: SingleSelection<ListStore<MailRow>> = SingleSelection(listStore)

                selection.canUnselect = true
                selection.autoselect = false
                selection.selected = -1
                emailsListView.model = selection

                selection.onNotify("selected") { _ ->
                    val index = selection.selected
                    val mail = if (index >= 0) sortedMails[index] else null
                    if (mail != null) {
                        val webView = WebView()
                        webView.loadHtml(mail.content, "")
                        contentScrolledWindow.child = webView
                        contentStack.visibleChild = contentScrolledWindow
                    } else {
                        contentStack.visibleChild = contentStatusPage
                    }
                }

                emailsStack.visibleChild = emailsScrolledWindow
                false
            }
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