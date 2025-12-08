package ca.kebs.courrier.helpers

import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Flags.Flag
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun splitFrom(from: String): Pair<String, String> {
    return from.split("<")[0] to from.split("<")[1].removeSuffix(">")
}

fun getTrashFolder(store: Store): Folder? {
    val gmailTrash = listOf("[Gmail]/Trash", "[Google Mail]/Trash").firstNotNullOfOrNull { name ->
        try { store.getFolder(name).takeIf { it.exists() } } catch (_: Exception) { null }
    }
    if (gmailTrash != null) return gmailTrash

    val commonNames = listOf("Trash", "Deleted Items", "Bin", "Papierkorb", "Corbeille")
    for (name in commonNames) {
        try {
            val f = store.getFolder(name)
            if (f.exists()) return f
        } catch (_: Exception) { }
    }

    try {
        val root = store.defaultFolder
        if (root is IMAPFolder) {
            val folders = root.listSubscribed() ?: root.list()
            for (f in folders) {
                if (f is IMAPFolder) {
                    val attrs = f.attributes
                    if (attrs != null) {
                        for (attr in attrs) {
                            if (attr.equals("\\Trash", ignoreCase = true)) {
                                return f
                            }
                        }
                    }
                }
            }
        }
    } catch (_: Exception) { }

    return null
}

suspend fun moveEmail(message: Message, targetFolder: Folder) {
    withContext(Dispatchers.IO) {
        try {
            val folder = message.folder ?: return@withContext

            if (folder.mode == Folder.READ_ONLY) {
                folder.close(false)
                folder.open(Folder.READ_WRITE)
            }

            if (!targetFolder.exists()) {
                return@withContext
            }
            targetFolder.open(Folder.READ_WRITE)

            try {
                folder.copyMessages(arrayOf(message), targetFolder)
            } finally {
                message.flags.add(Flag.DELETED)
                targetFolder.close(true)
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
}
