module goa {
    requires static java.compiler;
    requires static org.jetbrains.annotations;
    exports goa;
    requires transitive org.gnome.glib;
}
