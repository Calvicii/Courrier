package ca.kebs.courrier.data

import jakarta.mail.Message
import java.util.Date

data class MessageDTO(
    val subject: String,
    val from: String,
    val to: List<String>,
    val receivedDate: Date,
    val content: String,
    val ref: Message,
)
