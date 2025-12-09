package ca.kebs.courrier

import ca.kebs.courrier.helpers.splitFrom
import ca.kebs.courrier.models.InboxRow
import ca.kebs.courrier.models.MailRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.gnome.adw.Avatar
import org.gnome.gtk.Align
import org.gnome.gtk.Box
import org.gnome.gtk.GestureClick
import org.gnome.gtk.Image
import org.gnome.gtk.Label
import org.gnome.gtk.ListItem
import org.gnome.gtk.Orientation
import org.gnome.gtk.Popover
import org.gnome.gtk.SignalListItemFactory
import org.gnome.pango.EllipsizeMode

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun setupInboxesFactories(): SignalListItemFactory {
    val inboxFactory = SignalListItemFactory()
    inboxFactory.onSetup {
        val listItem = it as ListItem
        val box = Box(Orientation.HORIZONTAL, 8)
        val image = Image()
        val label = Label()
        box.append(image)
        box.append(label)
        listItem.child = box
    }
    inboxFactory.onBind {
        val listItem = it as ListItem
        val item = listItem.item as InboxRow
        val box = listItem.child as Box
        val image = box.firstChild as Image
        val label = box.lastChild as Label
        image.setFromIconName(item.iconName)
        label.text = item.name
    }
    return inboxFactory
}

fun setupMessagesFactories(): SignalListItemFactory {
    val messagesFactory = SignalListItemFactory()
    messagesFactory.onSetup {
        val listItem = it as ListItem
        val box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setSpacing(8)
            .setMarginTop(8)
            .setMarginBottom(8)
            .setCanTarget(true)
            .build()

        val fromBox = Box.builder()
            .setOrientation(Orientation.HORIZONTAL)
            .setSpacing(8)
            .build()
        val avatar = Avatar.builder()
            .setSize(32)
            .setShowInitials(true)
            .build()
        val from = Label.builder()
            .setHalign(Align.START)
            .setUseMarkup(true)
            .setEllipsize(EllipsizeMode.END)
            .build()
        val address = Label.builder()
            .setHalign(Align.START)
            .setCssClasses(arrayOf("dimmed"))
            .setEllipsize(EllipsizeMode.END)
            .build()

        val subject = Label.builder()
            .setHalign(Align.START)
            .setEllipsize(EllipsizeMode.END)
            .build()
        val receivedDate = Label.builder()
            .setHalign(Align.START)
            .setCssClasses(arrayOf("dimmed"))
            .setEllipsize(EllipsizeMode.END)
            .build()

        // Context menu
        val popover = Popover.builder()
            .setChild(Box(Orientation.VERTICAL, 8))
            .build()

        popover.parent = box

        val rightClickGesture = GestureClick.builder()
            .setButton(3)
            .build()

        rightClickGesture.onPressed { _, _, _ ->
            popover.popup()
        }

        box.addController(rightClickGesture)

        fromBox.append(avatar)
        fromBox.append(from)
        fromBox.append(address)
        box.append(fromBox)

        box.append(subject)
        box.append(receivedDate)
        listItem.child = box
    }
    messagesFactory.onBind {
        val listItem = it as ListItem
        val item = listItem.item as MailRow
        val box = listItem.child as Box
        val popover = box.firstChild as Popover

        val contextMenuBox = popover.firstChild.firstChild as Box
        val fromBox = popover.nextSibling as Box
        val avatar = fromBox.firstChild as Avatar
        val from = avatar.nextSibling as Label
        val address = fromBox.lastChild as Label

        val subject = fromBox.nextSibling as Label
        val receivedDate = box.lastChild as Label

        val sender = splitFrom(item.from)

        avatar.text = sender.first
        from.setMarkup("<b>${sender.first}</b>")
        address.text = sender.second
        subject.text = item.subject
        receivedDate.text = item.receivedDate

        // Context menu
//        val start = System.currentTimeMillis()
//        val store = item.message.folder.store
//        println(System.currentTimeMillis() - start)
//        val trashFolder = getTrashFolder(store)
//        println(System.currentTimeMillis() - start)
//
//        val moveButton = Button.builder()
//            .setLabel("Move to trash")
//            .setCssClasses(arrayOf("flat"))
//            .onClicked {
//                if (trashFolder != null) {
//                    popover.popdown()
//                    box.visible = false
//                    scope.launch {
//                        moveEmail(item.message, trashFolder)
//                    }
//                } else {
//                    println("Trash folder not found")
//                }
//
//            }
//            .build()
//
//        if (trashFolder != null) {
//            contextMenuBox.append(moveButton)
//        }
    }
    return messagesFactory
}