package com.kxxr.sharide.db

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val messageType: String = "text",
    val timestamp: Timestamp? = null,
    val location: GeoPoint? = null,
    val isRead: Boolean = false,
    val imageUrl: String? = null,
    @get:Exclude var firestoreId: String = ""
)
