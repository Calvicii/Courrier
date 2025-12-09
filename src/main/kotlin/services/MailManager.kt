package ca.kebs.courrier.services

import goa.GoaObject
import io.github.jwharm.javagi.base.Out
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnome.gio.Cancellable
import java.util.Properties

object MailManager {
    private var store: Store? = null

    var CurrentAccount: GoaObject? = null
        private set

    var CurrentFolder: Folder? = null
        private set

    var CurrentMessage: Message? = null
        private set

    suspend fun switchAccount(account: GoaObject) {
        withContext(Dispatchers.IO) {
            store?.close()
            store = null
            CurrentAccount = null

            val goaMail = account.mail
            val oauth2 = account.oauth2Based
            val outToken = Out<String>()
            val outExpires = Out<Int>()

            val success = oauth2.callGetAccessTokenSync(outToken, outExpires, Cancellable())

            if (!success) {
                throw RuntimeException("Could not get access token from GOA")
            }

            val accessToken = outToken.get().toString()
            val username = goaMail.imapUserName

            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", goaMail.imapHost)
                put("mail.imaps.port", "993")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
                put("mail.imaps.auth.mechanisms", "XOAUTH2")
            }

            val session = Session.getInstance(props, null)
            store = session.getStore("imaps").apply {
                connect(goaMail.imapHost, username, accessToken)
            }

            CurrentAccount = account
            switchFolder(null)
            switchMessage(null)
        }
    }

    suspend fun switchFolder(folder: Folder?) {
        withContext(Dispatchers.IO) {
            CurrentFolder = folder
        }
    }

    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val defaultFolder = store?.defaultFolder ?: throw IllegalStateException("Store not set")

        try {
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

            return@withContext collectFolders(defaultFolder)
        } catch (error: Exception) {
            println("Error fetching folders: ${error.message}")
            emptyList()
        }
    }

    suspend fun switchMessage(message: Message?) {
        withContext(Dispatchers.IO) {
            CurrentMessage = message
        }
    }

    suspend fun getMessages(): List<Message> = withContext(Dispatchers.IO) {
        val targetFolder = CurrentFolder ?: return@withContext emptyList()

        try {
            if (!targetFolder.isOpen) {
                targetFolder.open(Folder.READ_ONLY)
            }

            val messages = targetFolder.messages
            val messageList = messages.toList()

            messageList
        } catch (error: Exception) {
            println("Error fetching messages: ${error.message}")
            emptyList()
        }
    }
}