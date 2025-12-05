package services

import goa.GoaObject
import io.github.jwharm.javagi.base.Out
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnome.gio.Cancellable
import java.util.Properties

class MailService private constructor(
    private val account: GoaObject,
    private var store: Store? = null,
    private var session: Session? = null,
) {
    companion object {
        suspend fun create(account: GoaObject): MailService {
            val service = MailService(account, null, null)
            service.login(account)
            return service
        }
    }

    private suspend fun login(account: GoaObject) {
        val mail = account.mail

        val oauth2 = account.oauth2Based
        val outToken = Out<String>()
        val outExpires = Out<Int>()

        val success = oauth2.callGetAccessTokenSync(outToken, outExpires, Cancellable())

        if (!success) {
            throw RuntimeException("Could not get access token from GOA")
        }

        val accessToken = outToken.get().toString()
        val username = mail.imapUserName


        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", account.mail.imapHost)
                put("mail.imaps.port", "993")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
                put("mail.imaps.auth.mechanisms", "XOAUTH2")
            }

            session = Session.getInstance(props, null)
            store = session!!.getStore("imaps").apply {
                connect(account.mail.imapHost, username, accessToken)
            }
        }
    }

    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val connectedStore = store ?: throw IllegalStateException("Store not set")
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

        val collected = collectFolders(defaultFolder)
        return@withContext collected
    }

    suspend fun getMessages(folder: Folder): List<Message> = withContext(Dispatchers.IO) {
        if (!folder.exists()) return@withContext emptyList()

        if (!folder.isOpen) folder.open(Folder.READ_ONLY)

        val count = folder.messageCount
        if (count <= 0) return@withContext emptyList()

        val start = maxOf(1, count - 99)
        val messages = folder.getMessages(start, count)

        val fp = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
        }

        folder.fetch(messages, fp)

        return@withContext messages.toList()
    }
}