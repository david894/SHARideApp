package com.kxxr.sharide.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kxxr.sharide.R
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun ChatListScreen(userId: String, navController: NavController) {

    val db = FirebaseFirestore.getInstance()
    val chatList = remember { mutableStateListOf<ChatPreview>() }
    val context = LocalContext.current
    val isDriver by remember { mutableStateOf(getDriverPreference(context)) }

    LaunchedEffect(userId, isDriver) {
        val fieldToFilter = if (isDriver) "driverId" else "passengerId"

        db.collection("chats")
            .whereEqualTo(fieldToFilter, userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                chatList.clear()

                for (doc in snapshots) {
                    val otherUserId = if (isDriver) doc.getString("passengerId") else doc.getString("driverId")
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("lastMessageTimestamp")

                    if (otherUserId != null) {
                        db.collection("users").document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("name") ?: "Unknown"
                                val image = userDoc.getString("imageRes") ?: ""
                                chatList.add(
                                    ChatPreview(
                                        chatId = doc.id,
                                        passengerName = name,
                                        passengerImage = image,
                                        lastMessage = lastMessage,
                                        lastMessageTimestamp = timestamp
                                    )
                                )
                            }
                    }
                }
            }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(chatList) { chat ->
            ChatPreviewCard(chat = chat) {
                navController.navigate("chat_screen/${chat.chatId}")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatPreviewCard(chat: ChatPreview, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = chat.passengerImage.ifEmpty { R.drawable.profile_ico },
                contentDescription = "Passenger Image",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(chat.passengerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(chat.lastMessage, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = chat.lastMessageTimestamp?.toDate()?.let {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                } ?: "",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

data class ChatPreview(
    val chatId: String,
    val passengerName: String,
    val passengerImage: String,
    val lastMessage: String,
    val lastMessageTimestamp: Timestamp?
)
