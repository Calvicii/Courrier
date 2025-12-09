package ca.kebs.courrier.helpers

import ca.kebs.courrier.data.MessageDTO
import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Flags.Flag
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun splitFrom(from: String): Pair<String, String> {
    return from.split("<")[0] to from.split("<")[1].removeSuffix(">")
}

fun getTrashFolder(store: Store): Folder? {
    val gmailTrash = listOf("[Gmail]/Trash", "[Google Mail]/Trash").firstNotNullOfOrNull { name ->
        try {
            store.getFolder(name).takeIf { it.exists() }
        } catch (_: Exception) {
            null
        }
    }
    if (gmailTrash != null) return gmailTrash

    val commonNames = listOf("Trash", "Deleted Items", "Bin", "Papierkorb", "Corbeille")
    for (name in commonNames) {
        try {
            val f = store.getFolder(name)
            if (f.exists()) return f
        } catch (_: Exception) {
        }
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
    } catch (_: Exception) {
    }

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

            folder.copyMessages(arrayOf(message), targetFolder)
            message.setFlag(Flag.DELETED, true)
            folder.expunge()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
}

fun extractContent(message: Message): String {
    return when (val content = message.content) {
        is String -> content
        is Multipart -> {
            (0 until content.count)
                .map { content.getBodyPart(it) }
                .firstOrNull { it.isMimeType("text/html") }
                ?.content as? String
                ?: (0 until content.count)
                    .map { content.getBodyPart(it) }
                    .firstOrNull { it.isMimeType("text/plain") }
                    ?.content as? String
                ?: ""
        }

        else -> ""
    }
}

suspend fun toMessageDTO(message: Message): MessageDTO = withContext(Dispatchers.IO) {
    MessageDTO(
        subject = message.subject,
        from = message.from[0].toString(),
        to = message.allRecipients.toList().map { it.toString() },
        receivedDate = message.receivedDate,
        content = extractContent(message),
        ref = message,
    )
}
