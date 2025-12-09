package ca.kebs.courrier.models

import jakarta.mail.Message
import org.gnome.gobject.GObject

data class MailRow(val subject: String, val from: String, val receivedDate: String, val message: Message) : GObject()
