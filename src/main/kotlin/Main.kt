import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import components.AccountDropDown
import components.ShowSidebarButton
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
import io.github.compose4gtk.modifier.margin
import io.github.compose4gtk.useGioResource
import org.gnome.adw.BreakpointCondition

fun main(args: Array<String>) {
    useGioResource("resources.gresource") {
        adwApplication(appId = "ca.kebs.Courrier", args) {
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

                var selectedAccount by remember { mutableStateOf<String?>("lavoiethomas17@gmail.com") }

                OverlaySplitView(
                    collapsed = collapsedOverlaySplitView || collapsedNavigationSplitView,
                    sidebar = {
                        VerticalBox(
                            modifier = Modifier.margin(8)
                        ) {
                            AccountDropDown(onSelectionChanges = { selectedAccount = it.name })
                            Label("Selected Account: $selectedAccount")
                        }
                    }
                ) {
                    NavigationSplitView(
                        state = navigationSplitViewState,
                        minSidebarWidth = 260.0,
                        sidebar = {
                            NavigationPage("Sidebar") {
                                ToolbarView(
                                    topBar = {
                                        HeaderBar(showTitle = false, startWidgets = {
                                            if (collapsedOverlaySplitView || collapsedNavigationSplitView) {
                                                ShowSidebarButton(
                                                    onClick = {
                                                        showSidebar()
                                                    }
                                                )
                                            }
                                        })
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
