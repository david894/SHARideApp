@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.sharide.R
import com.kxxr.sharide.db.NotificationDao
import com.kxxr.sharide.db.NotificationDatabase
import com.kxxr.sharide.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationScreen(
    navController: NavController,
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    context: Context // Pass context to access Room Database
) {
    val userId = firebaseAuth.currentUser?.uid ?: ""
    val notifications = remember { mutableStateListOf<NotificationEntity>() }
    val notificationDao = remember { NotificationDatabase.getDatabase(context).notificationDao() }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val localNotifications = withContext(Dispatchers.IO) {
                notificationDao.getAllNotifications()
            }
            notifications.clear()
            notifications.addAll(localNotifications.map {
                NotificationEntity(it.id,it.image, it.title, it.description, it.time,it.postTime)
            })

            observeNotifications(context,userId, firestore, notifications, notificationDao)
            observeUnreadMessages(context, userId,firestore,notifications,notificationDao )
            observeCancelRideStatus(context,userId,firestore,notifications,notificationDao)

        }
    }

    Scaffold(topBar = { NotificationTopBar(navController,notificationDao) }) { paddingValues ->
        NotificationList(notifications, paddingValues)
    }
}

fun observeNotifications(
    context: Context,
    currentUserId:String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationEntity>,
    notificationDao: NotificationDao
) {
    val isDriver = getDriverPreference(context)
    val collection = if (isDriver) "rides" else "searchs"
    val userField = if (isDriver) "driverId" else "passengerId"

    firestore.collection(collection)
        .whereEqualTo(userField, currentUserId)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener

            val currentTime = System.currentTimeMillis()
            val timeIntervals = listOf(6, 3, 1)

            val newNotifications = snapshots.documents.mapNotNull { doc ->
                createCombinedNotificationIfNeeded(doc, currentTime, timeIntervals, isDriver)
            }

            val activeNotifications = newNotifications.filterNot { it.time.contains("expired", ignoreCase = true) }

            val existingNotifications = notifications.toList().toMutableList()
            activeNotifications.forEach { newNotification ->
                if (existingNotifications.none { it.title == newNotification.title && it.description == newNotification.description }) {
                    existingNotifications.add(newNotification)
                    AndroidNotification.show(context, newNotification)

                }
            }

            notifications.clear()
            notifications.addAll(existingNotifications)

            GlobalScope.launch(Dispatchers.IO) {
                notificationDao.deleteExpiredNotifications()
                activeNotifications.forEach { notification ->
                    val exists = notificationDao.getNotification(notification.title, notification.description) != null
                    if (!exists) {
                        notificationDao.insertNotification(notification)
                    }
                }
            }
        }
}

private fun createCombinedNotificationIfNeeded(
    doc: DocumentSnapshot,
    currentTime: Long,
    timeIntervals: List<Int>,
    isDriver: Boolean
): NotificationEntity? {
    val date = doc.getString("date") ?: return null
    val time = doc.getString("time") ?: return null
    val location = doc.getString("location") ?: return null
    val destination = doc.getString("destination") ?: return null

    val timestamp = convertToTimestamp(date, time)
    val timeLeftMillis = timestamp - currentTime
    val hoursLeft = (timeLeftMillis / (1000 * 60 * 60)).toInt()
    val minutesLeft = ((timeLeftMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val postTime = getCurrentTime()

    val imageRes = if (isDriver) R.drawable.car_front else R.drawable.profile_ico
    val titleUpcoming = if (isDriver) "Upcoming Ride Reminder" else "Search Ride Reminder"
    val titleExpired = if (isDriver) "Ride Expired" else "Searched Ride Expired"
    val entityType = if (isDriver) "ride" else "searched ride"

    return when {
        hoursLeft in timeIntervals && minutesLeft >= 0 -> NotificationEntity(
            image = imageRes,
            title = titleUpcoming,
            description = "Your $entityType from $location to $destination is in $hoursLeft hour${if (hoursLeft > 1) "s" else ""}!",
            time = "$hoursLeft hours left",
            postTime = postTime
        )
        hoursLeft < 0 -> NotificationEntity(
            image = imageRes,
            title = titleExpired,
            description = "Your $entityType from $location to $destination has already passed.",
            time = "expired",
            postTime = postTime
        )
        else -> null
    }
}

@Composable
fun NotificationTopBar(navController: NavController, notificationDao: NotificationDao) {
    var showDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Notification", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Notifications",
                    tint = Color(0xFF1976D2)
                )
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.White)
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Clear Notifications") },
            text = { Text("Are you sure you want to delete all notifications?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    clearAllNotifications(notificationDao)
                }) {
                    Text("Yes", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

fun clearAllNotifications(notificationDao: NotificationDao) {
    CoroutineScope(Dispatchers.IO).launch {
        notificationDao.clearAllNotifications()
        Log.d("DB_CLEANUP", "All notifications deleted!")
    }
}

// Displays the list of notifications.
@Composable
fun NotificationList(notifications: List<NotificationEntity>, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        if (notifications.isEmpty()) {
            item {
                Text(text = "No ride notifications", fontSize = 16.sp, color = Color.Gray)
            }
        } else {
            items(notifications) { notification ->
                NotificationItem(
                    image = notification.image,
                    title = notification.title,
                    description = notification.description,
                    time = notification.time,
                   postTime = notification.postTime

                )
            }
        }
    }
}

@Composable
fun NotificationItem(image: Int, title: String, description: String, time: String, postTime: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = time, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = postTime, fontSize = 14.sp, color = Color.Gray)
                }
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = description, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

// Returns the current system time in HH:mm format
fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

fun observeUnreadMessages(
    context: Context,
    currentUserId: String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationEntity>,
    notificationDao: NotificationDao
) {
    firestore.collection("chats")
        .whereEqualTo("driverId", currentUserId)
        .get()
        .addOnSuccessListener { driverChats ->
            firestore.collection("chats")
                .whereEqualTo("passengerId", currentUserId)
                .get()
                .addOnSuccessListener { passengerChats ->

                    val allChats = driverChats.documents + passengerChats.documents

                    allChats.forEach { chatDoc ->
                        val chatId = chatDoc.id

                        firestore.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .whereEqualTo("isRead", false)
                            .whereNotEqualTo("senderId", currentUserId)
                            .addSnapshotListener { messagesSnap, msgError ->
                                if (msgError != null || messagesSnap == null) return@addSnapshotListener

                                val unreadMessages = messagesSnap.documents
                                val messagesBySender = unreadMessages.groupBy { it.getString("senderId") ?: "Unknown" }

                                messagesBySender.forEach { (senderId, senderMessages) ->
                                    val messageCount = senderMessages.size
                                    val latestMsg = senderMessages.maxByOrNull {
                                        it.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                                    }

                                    val latestText = latestMsg?.getString("messageText") ?: "New message"
                                    val latestTime = latestMsg?.getTimestamp("timestamp")?.toDate()
                                    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(latestTime ?: Date())

                                    // Fetch sender name from users collection
                                    firestore.collection("users")
                                        .whereEqualTo("firebaseUserId", senderId)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { userSnapshot ->
                                            val senderName = userSnapshot.documents.firstOrNull()?.getString("name") ?: "Someone"

                                            val notification = NotificationEntity(
                                                image = R.drawable.chat_icon,
                                                title = " Message from $senderName",
                                                description = "You have $messageCount new message${if (messageCount > 1) "s" else ""}: \"$latestText\"",
                                                time = "Unread Message",
                                                postTime = " $formattedTime"
                                            )

                                            val alreadyShown = notifications.any {
                                                it.postTime == notification.postTime &&
                                                        it.description == notification.description
                                            }

                                            if (!alreadyShown) {
                                                notifications.add(notification)
                                                AndroidNotification.show(context, notification)

                                                GlobalScope.launch(Dispatchers.IO) {
                                                    val exists = notificationDao.getNotification(notification.title, notification.description) != null
                                                    if (!exists) {
                                                        notificationDao.insertNotification(notification)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                    }
                }
        }
}

fun observeCancelRideStatus(
    context: Context,
    currentUserId: String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationEntity>,
    notificationDao: NotificationDao
) {
    val isDriver = getDriverPreference(context)

    val requestQuery = if (isDriver) {
        firestore.collection("requests")
            .whereEqualTo("driverId", currentUserId)
            .whereEqualTo("status", "canceled")
    } else {
        firestore.collection("requests")
            .whereEqualTo("passengerId", currentUserId)
            .whereEqualTo("status", "canceled")
    }

    requestQuery.addSnapshotListener { requestSnapshot, error ->
        if (error != null || requestSnapshot == null) return@addSnapshotListener


        requestSnapshot.documents.forEach { doc ->
            val rideId = if (isDriver) doc.getString("rideId") else doc.getString("searchId") ?: return@forEach
            val otherUserId = if (isDriver) doc.getString("passengerId") else doc.getString("driverId") ?: return@forEach

            firestore.collection("users")
                .whereEqualTo("firebaseUserId", otherUserId)
                .limit(1)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    val name = userSnapshot.documents.firstOrNull()?.getString("name") ?: "User"

                    val collection = if (isDriver) "rides" else "searchs"
                    if (rideId != null) {
                        firestore.collection(collection).document(rideId)
                            .get()
                            .addOnSuccessListener { rideDoc ->
                                val location = rideDoc.getString("location") ?: "Unknown"
                                val destination = rideDoc.getString("destination") ?: "Unknown"
                                val postTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                                val notification = NotificationEntity(
                                    image = R.drawable.ban,
                                    title = "Ride Cancel",
                                    description = "Your ride participant, $name from $location to $destination has canceled the ride.",
                                    time = "Ride Cancelled",
                                    postTime = postTime
                                )

                                val alreadyShown = notifications.any {
                                    it.description == notification.description && it.postTime == notification.postTime
                                }

                                if (!alreadyShown) {
                                    notifications.add(notification)
                                    AndroidNotification.show(context, notification)

                                    GlobalScope.launch(Dispatchers.IO) {
                                        val exists = notificationDao.getNotification(notification.title, notification.description) != null
                                        if (!exists) {
                                            notificationDao.insertNotification(notification)
                                        }
                                    }
                                }
                            }
                    }
                }
        }
    }
}
