import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import components.AccountDropDown
import components.FolderList
import components.MessageList
import components.ShowSidebarButton
import goa.GoaObject
import io.github.compose4gtk.adw.adwApplication
import io.github.compose4gtk.adw.components.ApplicationWindow
import io.github.compose4gtk.adw.components.HeaderBar
import io.github.compose4gtk.adw.components.NavigationPage
import io.github.compose4gtk.adw.components.NavigationSplitView
import io.github.compose4gtk.adw.components.OverlaySplitView
import io.github.compose4gtk.adw.components.ToastOverlay
import io.github.compose4gtk.adw.components.ToolbarView
import io.github.compose4gtk.adw.components.rememberNavigationSplitViewState
import io.github.compose4gtk.gtk.components.Box
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.Spinner
import io.github.compose4gtk.gtk.components.VerticalBox
import io.github.compose4gtk.modifier.Modifier
import io.github.compose4gtk.modifier.alignment
import io.github.compose4gtk.modifier.expandHorizontally
import io.github.compose4gtk.modifier.sizeRequest
import io.github.compose4gtk.useGioResource
import jakarta.mail.Folder
import jakarta.mail.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.gnome.adw.BreakpointCondition
import org.gnome.adw.Toast
import org.gnome.adw.ToolbarStyle
import org.gnome.gtk.Align
import services.MailService

fun main(args: Array<String>) {
    useGioResource("resources.gresource") {
        adwApplication(appId = "ca.kebs.Courrier", args) {
            val scope = rememberCoroutineScope()
            var mailService by remember { mutableStateOf<MailService?>(null) }

            val splitViewBreakpointCondition = remember { BreakpointCondition.parse("max-width: 800sp") }
            val overlayBreakpointCondition = remember { BreakpointCondition.parse("max-width: 1000sp") }

            ApplicationWindow(
                title = "Courrier",
                onClose = ::exitApplication,
                defaultWidth = 1400,
                defaultHeight = 600,
            ) {
                val collapsedOverlaySplitView by rememberBreakpoint(
                    condition = overlayBreakpointCondition,
                )

                val navigationSplitViewState = rememberNavigationSplitViewState()
                val collapsedNavigationSplitView by rememberBreakpoint(
                    condition = splitViewBreakpointCondition,
                )

                LaunchedEffect(collapsedNavigationSplitView) {
                    navigationSplitViewState.collapsed = collapsedNavigationSplitView
                }

                var selectedAccount by remember { mutableStateOf<GoaObject?>(null) }
                var selectedFolder by remember { mutableStateOf<Folder?>(null) }
                var selectedMessage by remember { mutableStateOf<Message?>(null) }
                val folders = remember { mutableStateListOf<Folder>() }
                val messages = remember { mutableStateListOf<Message>() }

                var isLoadingFolders by remember { mutableStateOf(false) }
                var isLoadingMessages by remember { mutableStateOf(false) }

                var currentJob: Job? = null

                ToastOverlay {
                    // When selected account changes
                    LaunchedEffect(selectedAccount) {
                        isLoadingFolders = true
                        currentJob?.cancel()
                        currentJob = null

                        folders.clear()
                        messages.clear()

                        selectedAccount?.let {
                            scope.launch {
                                try {
                                    mailService = MailService.create(it)
                                    folders.addAll(mailService!!.getFolders())
                                } finally {
                                    isLoadingFolders = false
                                }
                            }
                        } ?: run { isLoadingFolders = false }
                    }

                    // When selected folder changes
                    LaunchedEffect(selectedFolder) {
                        isLoadingMessages = true
                        currentJob?.cancel()
                        currentJob = null

                        messages.clear()
                        val folder = selectedFolder
                        if (folder == null) {
                            isLoadingMessages = false
                            return@LaunchedEffect
                        }

                        currentJob = scope.launch {
                            try {
                                val fetched = mailService?.getMessages(folder) ?: emptyList()
                                // Check that user didn't change folder during load
                                if (selectedFolder == folder) {
                                    messages.addAll(fetched)
                                }
                            } catch (_: CancellationException) {
                                // Do nothing, action was canceled by user
                            } catch (_: Exception) {
                                addToast(Toast("Failed to load messages"))
                            } finally {
                                isLoadingMessages = false
                            }
                        }
                    }

                    OverlaySplitView(
                        collapsed = collapsedOverlaySplitView || collapsedNavigationSplitView,
                        sidebar = {
                            ToolbarView(
                                topBarStyle = ToolbarStyle.RAISED,
                                topBar = {
                                    HeaderBar(
                                        title = {
                                            AccountDropDown(
                                                modifier = Modifier.expandHorizontally(true),
                                                onSelectionChanges = {
                                                    selectedAccount = it.account
                                                },
                                            )
                                        }
                                    )
                                },
                            ) {
                                if (isLoadingFolders) {
                                    Box(modifier = Modifier.alignment(Align.CENTER)) {
                                        Spinner(modifier = Modifier.sizeRequest(48, 48), spinning = true)
                                    }
                                } else {
                                    FolderList(folders, onFolderChange = { selectedFolder = it })
                                }
                            }
                        }
                    ) {
                        NavigationSplitView(
                            state = navigationSplitViewState,
                            minSidebarWidth = 250.0,
                            maxSidebarWidth = 400.0,
                            sidebarWidthFraction = 0.4,
                            sidebar = {
                                NavigationPage("Sidebar") {
                                    ToolbarView(
                                        topBar = {
                                            HeaderBar(
                                                showTitle = true,
                                                title = {
                                                    var title = selectedFolder?.name ?: ""
                                                    if (title == "INBOX") title = "Inbox"
                                                    Label(text = title)
                                                },
                                                startWidgets = {
                                                    if (collapsedOverlaySplitView || collapsedNavigationSplitView) {
                                                        ShowSidebarButton(
                                                            onClick = {
                                                                showSidebar()
                                                            }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    ) {
                                        if (isLoadingMessages) {
                                            Box(modifier = Modifier.alignment(Align.CENTER)) {
                                                Spinner(
                                                    modifier = Modifier.sizeRequest(48, 48),
                                                    spinning = true
                                                )
                                            }
                                        } else {
                                            VerticalBox {
                                                MessageList(
                                                    messages = messages,
                                                    onMessageChange = { selectedMessage = it })
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            NavigationPage("Content") {
                                ToolbarView(
                                    topBar = {
                                        HeaderBar(showTitle = false)
                                    }
                                ) {
                                    VerticalBox {
                                        selectedMessage?.let {
                                            // WebView in future compose-4-gtk release
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
