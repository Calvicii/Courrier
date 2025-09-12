package ca.kebs.courrier.models

import org.gnome.gobject.GObject

data class MailRow(val subject: String, val from: String, val receivedDate: String) : GObject()
