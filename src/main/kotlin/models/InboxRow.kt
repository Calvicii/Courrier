package ca.kebs.courrier.models

import org.gnome.gobject.GObject

data class InboxRow(val iconName: String, val name: String, val tag: String) : GObject()
