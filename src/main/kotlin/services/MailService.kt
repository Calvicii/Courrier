package ca.kebs.courrier.services

import ca.kebs.courrier.data.Mail
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * Handles various email services such as auth, reading and sending.
 */
class MailService(
    private val host: String,
) {
    private var store: Store? = null
    private var session: Session? = null

    suspend fun login(username: String, token: String) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", host)
                put("mail.imaps.port", "993")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
                put("mail.imaps.auth.mechanisms", "XOAUTH2")
            }

            session = Session.getInstance(props, null)
            store = session!!.getStore("imaps").apply {
                connect(host, username, token)
            }
        }
    }

    suspend fun getAllFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val connectedStore = store ?: throw IllegalStateException("store not set")
        val defaultFolder = connectedStore.defaultFolder

        fun collectFolders(folder: Folder): List<Folder> {
            val result = mutableListOf<Folder>()
            if (folder.type and Folder.HOLDS_MESSAGES != 0) {
                result.add(folder)
            }
            if (folder.type and Folder.HOLDS_FOLDERS != 0) {
                for (child in folder.list()) {
                    result.addAll(collectFolders(child))
                }
            }
            return result
        }

        collectFolders(defaultFolder)
    }

    suspend fun getAllMails(folder: Folder): List<Mail> = withContext(Dispatchers.IO) {
        if (!folder.isOpen) {
            folder.open(Folder.READ_ONLY)
        }
        folder.messages.map { message ->
            Mail(
                subject = message.subject,
                from = message.from[0].toString(),
                to = message.allRecipients.toList().map { it.toString() },
                receivedDate = message.receivedDate,
                content = extractContent(message),
                ref = message,
            )
        }
    }

    private fun extractContent(message: Message): String {
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
}