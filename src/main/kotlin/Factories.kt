package ca.kebs.courrier

import ca.kebs.courrier.models.InboxRow
import ca.kebs.courrier.models.MailRow
import org.gnome.gtk.Box
import org.gnome.gtk.Image
import org.gnome.gtk.Label
import org.gnome.gtk.ListItem
import org.gnome.gtk.Orientation
import org.gnome.gtk.SignalListItemFactory

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

fun setupEmailsFactories(): SignalListItemFactory {
    val emailsFactory = SignalListItemFactory()
    emailsFactory.onSetup {
        val listItem = it as ListItem
        val box = Box(Orientation.VERTICAL, 8)
        val from = Label()
        val subject = Label()
        val receivedDate = Label()
        box.append(from)
        box.append(subject)
        box.append(receivedDate)
        listItem.child = box
    }
    emailsFactory.onBind {
        val listItem = it as ListItem
        val item = listItem.item as MailRow
        val box = listItem.child as Box
        val from = box.firstChild as Label
        val subject = from.nextSibling as Label
        val receivedDate = box.lastChild as Label
        from.text = item.from
        subject.text = item.subject
        receivedDate.text = item.receivedDate
    }
    return emailsFactory
}