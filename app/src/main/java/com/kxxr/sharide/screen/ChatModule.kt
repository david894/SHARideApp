package com.kxxr.sharide.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

fun sendMessage(firestore: FirebaseFirestore, chatId: String, senderId: String, messageText: String) {
    val message = hashMapOf(
        "senderId" to senderId,
        "text" to messageText,
        "timestamp" to FieldValue.serverTimestamp()
    )

    firestore.collection("chats").document(chatId)
        .collection("messages")
        .add(message)
}

fun listenForMessages(firestore: FirebaseFirestore, chatId: String, onNewMessages: (List<DocumentSnapshot>) -> Unit) {
    firestore.collection("chats").document(chatId)
        .collection("messages")
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                onNewMessages(it.documents)
            }
        }
}

@Composable
fun ChatScreen(firestore: FirebaseFirestore, chatId: String, senderId: String) {
    var messages by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        listenForMessages(firestore, chatId) { newMessages ->
            messages = newMessages
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                val text = message.getString("text") ?: ""
                val isSender = message.getString("senderId") == senderId
                ChatBubble(text, isSender)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                sendMessage(firestore, chatId, senderId, messageText)
                messageText = ""
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isSender: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isSender) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            modifier = Modifier
                .background(if (isSender) Color.Blue else Color.Gray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            color = Color.White
        )
    }
}
