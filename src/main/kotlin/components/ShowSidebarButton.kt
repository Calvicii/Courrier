package components

import androidx.compose.runtime.Composable
import io.github.compose4gtk.gtk.ImageSource
import io.github.compose4gtk.gtk.components.IconButton

@Composable
fun ShowSidebarButton(onClick: () -> Unit) {
    IconButton(ImageSource.Icon("dock-left-symbolic"), onClick = onClick)
}