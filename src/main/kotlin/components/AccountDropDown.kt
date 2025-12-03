package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import goa.Client
import goa.GoaObject
import io.github.compose4gtk.gtk.components.DropDown
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.rememberSingleSelectionModel
import io.github.compose4gtk.modifier.Modifier
import org.gnome.gio.Cancellable
import org.gnome.gobject.GObject
import org.gnome.pango.EllipsizeMode
import services.AccountService

@Composable
fun AccountDropDown(
    onSelectionChanges: (DropDownItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accountService = remember { AccountService(Client.sync(Cancellable())) }
    val accounts = accountService.getMailAccounts()
    val formattedAccounts = remember { mutableListOf<DropDownItem>() }
    for (account in accounts) {
        formattedAccounts.add(DropDownItem(account.mail.imapUserName, account))
    }
    val model = rememberSingleSelectionModel(formattedAccounts)

    LaunchedEffect(Unit) {
        onSelectionChanges(formattedAccounts[0])
    }

    DropDown(
        model = model,
        item = { Label(text = it.name, ellipsize = EllipsizeMode.END) },
        selectedItem = { Label(text = it.name, ellipsize = EllipsizeMode.END) },
        modifier = modifier,
        onSelectionChanges = onSelectionChanges,
    )
}

data class DropDownItem(val name: String, val account: GoaObject) : GObject()
