package ca.kebs.courrier

import ca.kebs.courrier.helpers.splitFrom
import ca.kebs.courrier.models.InboxRow
import ca.kebs.courrier.models.MailRow
import org.gnome.gtk.Align
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
        val box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setSpacing(8)
            .build()

        val fromBox = Box.builder()
            .setOrientation(Orientation.HORIZONTAL)
            .setSpacing(8)
            .setMarginStart(8)
            .setMarginTop(8)
            .build()
        val from = Label.builder()
            .setHalign(Align.START)
            .setUseMarkup(true)
            .build()
        val address = Label.builder()
            .setHalign(Align.START)
            .setCssClasses(arrayOf("dimmed"))
            .build()

        val subject = Label.builder()
            .setHalign(Align.START)
            .setMarginStart(8)
            .build()
        val receivedDate = Label.builder()
            .setHalign(Align.START)
            .setMarginStart(8)
            .setMarginBottom(8)
            .setCssClasses(arrayOf("dimmed"))
            .build()

        fromBox.append(from)
        fromBox.append(address)
        box.append(fromBox)

        box.append(subject)
        box.append(receivedDate)
        listItem.child = box
    }
    emailsFactory.onBind {
        val listItem = it as ListItem
        val item = listItem.item as MailRow
        val box = listItem.child as Box
        val fromBox = box.firstChild as Box
        val from = fromBox.firstChild as Label
        val address = fromBox.lastChild as Label
        val subject = fromBox.nextSibling as Label
        val receivedDate = box.lastChild as Label

        val sender = splitFrom(item.from)

        from.setMarkup("<b>${sender.first}</b>")
        address.text = sender.second
        subject.text = item.subject
        receivedDate.text = item.receivedDate
    }
    return emailsFactory
}