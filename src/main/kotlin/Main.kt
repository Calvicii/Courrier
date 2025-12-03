import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import components.AccountDropDown
import components.FolderList
import components.ShowSidebarButton
import goa.GoaObject
import io.github.compose4gtk.adw.adwApplication
import io.github.compose4gtk.adw.components.ApplicationWindow
import io.github.compose4gtk.adw.components.HeaderBar
import io.github.compose4gtk.adw.components.NavigationPage
import io.github.compose4gtk.adw.components.NavigationSplitView
import io.github.compose4gtk.adw.components.OverlaySplitView
import io.github.compose4gtk.adw.components.ToolbarView
import io.github.compose4gtk.adw.components.rememberNavigationSplitViewState
import io.github.compose4gtk.gtk.components.Button
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.VerticalBox
import io.github.compose4gtk.modifier.Modifier
import io.github.compose4gtk.modifier.expandHorizontally
import io.github.compose4gtk.useGioResource
import jakarta.mail.Folder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.gnome.adw.BreakpointCondition
import org.gnome.adw.ToolbarStyle
import services.MailService

fun main(args: Array<String>) {
    useGioResource("resources.gresource") {
        adwApplication(appId = "ca.kebs.Courrier", args) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var mailService: MailService?

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
                val folders = remember { mutableStateListOf<Folder>() }

                LaunchedEffect(selectedAccount) {
                    scope.launch {
                        if (selectedAccount != null) {
                            mailService = MailService.create(selectedAccount!!)
                            folders.clear()
                            folders.addAll(mailService.getFolders())
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
                            FolderList(folders, onFolderChange = { selectedFolder = it })
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
                                    VerticalBox {
                                        Label("Sidebar")
                                        Button(
                                            "$collapsedNavigationSplitView & $collapsedOverlaySplitView",
                                            onClick = { navigationSplitViewState.showContent() }
                                        )
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
                                    Label("Content")
                                    Button(
                                        "$collapsedNavigationSplitView & $collapsedOverlaySplitView",
                                        onClick = { navigationSplitViewState.hideContent() })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
