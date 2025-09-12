package ca.kebs.courrier

import org.gnome.adw.Application
import org.gnome.gio.Resource

fun main(args: Array<String>) {
    val resource = Resource.load("src/main/resources/ui.gresource")
    resource.resourcesRegister()

    val app = Application("ca.kebs.courrier")
    app.onActivate { activate(app) }
    app.run(args)
}

private fun activate(app: Application) {
    val window = MainWindow()
    window.application = app
    window.present()
}