module goa {
    requires static java.compiler;
    requires static org.jetbrains.annotations;
    requires transitive org.gnome.gio;
    exports goa;
}
