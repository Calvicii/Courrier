package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.compose4gtk.gtk.components.DropDown
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.rememberSingleSelectionModel
import io.github.compose4gtk.modifier.Modifier
import org.gnome.gobject.GObject

@Composable
fun AccountDropDown(
    onSelectionChanges: (DropDownItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        remember { listOf(DropDownItem("lavoiethomas17@gmail.com"), DropDownItem("calvicii@proton.me")) }
    val model = rememberSingleSelectionModel(items)

    DropDown(
        model = model,
        item = { Label(it.name) },
        selectedItem = { Label(it.name) },
        modifier = modifier,
        onSelectionChanges = onSelectionChanges,
    )
}

data class DropDownItem(val name: String) : GObject()
