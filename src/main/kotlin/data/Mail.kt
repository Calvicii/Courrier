package ca.kebs.courrier.data

import java.util.Date

data class Mail(val subject: String, val from: String, val to: List<String>, val receivedDate: Date, val content: String)
