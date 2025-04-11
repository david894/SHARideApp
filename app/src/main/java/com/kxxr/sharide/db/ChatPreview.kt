package com.kxxr.sharide.db

import com.google.firebase.Timestamp

data class ChatPreview(
    val chatId: String,
    val passengerName: String,
    val passengerImage: String,
    val lastMessage: String,
    val lastMessageTimestamp: Timestamp?
)