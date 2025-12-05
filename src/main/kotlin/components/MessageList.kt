package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import io.github.compose4gtk.adw.components.Avatar
import io.github.compose4gtk.gtk.components.HorizontalBox
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.ListView
import io.github.compose4gtk.gtk.components.ScrolledWindow
import io.github.compose4gtk.gtk.components.VerticalBox
import io.github.compose4gtk.gtk.components.rememberSingleSelectionModel
import io.github.compose4gtk.modifier.Modifier
import io.github.compose4gtk.modifier.alignment
import io.github.compose4gtk.modifier.cssClasses
import io.github.compose4gtk.modifier.expandVertically
import jakarta.mail.Message
import org.gnome.gobject.GObject
import org.gnome.gtk.Align
import org.gnome.pango.EllipsizeMode

@Composable
fun MessageList(
    messages: List<Message>,
    onMessageChange: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedMessages = remember { mutableStateListOf<MessageListViewItem>() }

    LaunchedEffect(messages.size) {
        formattedMessages.clear()
        formattedMessages.addAll(messages.map { MessageListViewItem(it) })
    }

    val model = rememberSingleSelectionModel(formattedMessages.toList())
    model.canUnselect = true
    model.autoselect = false

    LaunchedEffect(model) {
        model.onSelectionChanged { _, _ ->
            onMessageChange(model.selectedItem.ref)
        }
    }

    ScrolledWindow(
        modifier = Modifier.expandVertically(),
    ) {
        ListView(
            model = model,
            modifier = modifier.cssClasses("navigation-sidebar")
        ) {
            HorizontalBox(spacing = 8) {
                val from = it.ref.from[0].toString()
                val subject = it.ref.subject
                val date = it.ref.receivedDate.toString()

                Avatar(
                    image = null,
                    text = from,
                    showInitials = true,
                    size = 48,
                )
                VerticalBox(
                    modifier = Modifier.alignment(Align.START)
                ) {
                    Label(text = from, modifier = Modifier.alignment(Align.START), ellipsize = EllipsizeMode.END)
                    Label(text = subject, modifier = Modifier.alignment(Align.START), ellipsize = EllipsizeMode.END)
                    Label(
                        text = date,
                        modifier = Modifier.alignment(Align.START).cssClasses("dimmed"),
                        ellipsize = EllipsizeMode.END,
                    )
                }
            }
        }
    }
}

private data class MessageListViewItem(val ref: Message) : GObject()
