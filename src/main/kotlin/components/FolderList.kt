package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import io.github.compose4gtk.gtk.ImageSource
import io.github.compose4gtk.gtk.components.HorizontalBox
import io.github.compose4gtk.gtk.components.Image
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.ListView
import io.github.compose4gtk.gtk.components.ScrolledWindow
import io.github.compose4gtk.gtk.components.rememberSingleSelectionModel
import io.github.compose4gtk.modifier.Modifier
import io.github.compose4gtk.modifier.cssClasses
import io.github.compose4gtk.modifier.expandVertically
import jakarta.mail.Folder
import org.gnome.gobject.GObject
import org.gnome.pango.EllipsizeMode

@Composable
fun FolderList(
    folders: List<Folder>,
    onFolderChange: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedFolders = remember { mutableStateListOf<FolderListViewItem>() }

    LaunchedEffect(folders.size) {
        formattedFolders.clear()
        formattedFolders.addAll(folders.map { FolderListViewItem(it.name, it) })
    }

    val model = rememberSingleSelectionModel(formattedFolders.toList())
    model.canUnselect = true
    model.autoselect = false

    LaunchedEffect(model) {
        model.onSelectionChanged { _, _ ->
            onFolderChange(model.selectedItem.ref)
        }
    }

    ScrolledWindow(
        modifier = Modifier.expandVertically(),
    ) {
        ListView(
            model = model,
            modifier = modifier.cssClasses("navigation-sidebar"),
        ) {
            HorizontalBox(spacing = 8) {
                var iconName = "mail-unread-symbolic"
                when (it.name) {
                    "INBOX" -> iconName = "inbox-symbolic"
                    "All Mail" -> iconName = "mail-archive-symbolic"
                    "Drafts" -> iconName = "pencil-symbolic"
                    "Important" -> iconName = "exclamation-mark-symbolic"
                    "Sent Mail" -> iconName = "outbox-symbolic"
                    "Spam" -> iconName = "junk-symbolic"
                    "Starred" -> iconName = "star-large-symbolic"
                    "Trash" -> iconName = "user-trash-symbolic"
                }

                Image(ImageSource.Icon(iconName))
                Label(
                    text = if (it.name == "INBOX") "Inbox" else it.name,
                    ellipsize = EllipsizeMode.END,
                )
            }
        }
    }
}

private data class FolderListViewItem(val name: String, val ref: Folder) : GObject()
