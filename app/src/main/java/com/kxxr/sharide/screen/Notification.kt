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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

            observeRideNotifications(userId, firestore, notifications, notificationDao)
            observeSearchNotifications(userId, firestore, notifications, notificationDao) // Add this line
        }
    }

    Scaffold(topBar = { NotificationTopBar(navController,notificationDao) }) { paddingValues ->
        NotificationList(notifications, paddingValues)
    }
}


/**
 * Observes ride notifications in Firestore and updates the list.
 */
private fun observeRideNotifications(
    userId: String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationEntity>,
    notificationDao: NotificationDao
) {
    firestore.collection("rides")
        .whereEqualTo("driverId", userId)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener

            val currentTime = System.currentTimeMillis()
            val timeIntervals = listOf(6, 3, 1) // Hours before ride

            val newNotifications = snapshots.documents.mapNotNull { doc ->
                createNotificationIfNeeded(doc, currentTime, timeIntervals)
            }

            // Remove expired notifications (past rides)
            val activeNotifications = newNotifications.filterNot { it.time.contains("expired", ignoreCase = true) }

            // Merge: Add only new notifications that don't already exist
            val existingNotifications = notifications.toList().toMutableList()
            activeNotifications.forEach { newNotification ->
                if (existingNotifications.none { it.title == newNotification.title && it.description == newNotification.description }) {
                    existingNotifications.add(newNotification)
                }
            }

            notifications.clear()
            notifications.addAll(existingNotifications)

            // Update local database asynchronously
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                // Remove expired notifications
                notificationDao.deleteExpiredNotifications()

                // Insert only new notifications
                activeNotifications.forEach { notification ->
                    val exists = notificationDao.getNotification(notification.title, notification.description) != null
                    if (!exists) {
                        notificationDao.insertNotification(
                            NotificationEntity(
                                image = notification.image,
                                title = notification.title,
                                description = notification.description,
                                time = notification.time,
                                postTime = notification.postTime
                            )
                        )
                    }
                }
            }
        }
}


/**
 * Checks if a notification should be created for a ride and returns it if needed.
 */
private fun createNotificationIfNeeded(doc: DocumentSnapshot, currentTime: Long, timeIntervals: List<Int>): NotificationEntity? {
    val rideId = doc.id
    val date = doc.getString("date") ?: return null
    val time = doc.getString("time") ?: return null
    val location = doc.getString("location") ?: return null
    val destination = doc.getString("destination") ?: return null

    val rideTimestamp = convertToTimestamp(date, time)

    val timeLeftMillis = rideTimestamp - currentTime
    val hoursLeft = (timeLeftMillis / (1000 * 60 * 60)).toInt()
    val minutesLeft = ((timeLeftMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val postTime = getCurrentTime()
    return when {
        hoursLeft in timeIntervals && minutesLeft == 0 -> NotificationEntity(
            image = R.drawable.car_front,
            title = "Upcoming Ride Reminder",
            description = "Your ride from $location to $destination is in $hoursLeft hour${if (hoursLeft > 1) "s" else ""}!",
            time = "$hoursLeft hours left",
            postTime = postTime
        )
        hoursLeft < 0 -> NotificationEntity( // Mark ride as expired
            image = R.drawable.car_front,
            title = "Ride Expired",
            description = "Your ride from $location to $destination has already passed.",
            time = "expired",
            postTime = postTime
        )
        else -> null
    }
}

private fun observeSearchNotifications(
    userId: String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationEntity>,
    notificationDao: NotificationDao
) {
    firestore.collection("searchs")
        .whereEqualTo("passengerId", userId)
        .addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) return@addSnapshotListener

            val currentTime = System.currentTimeMillis()
            val timeIntervals = listOf(6, 3, 1) // Hours before search ride

            val newNotifications = snapshots.documents.mapNotNull { doc ->
                createSearchNotificationIfNeeded(doc, currentTime, timeIntervals)
            }

            // Remove expired notifications
            val activeNotifications = newNotifications.filterNot { it.time.contains("expired", ignoreCase = true) }

            // Merge new notifications without duplication
            val existingNotifications = notifications.toList().toMutableList()
            activeNotifications.forEach { newNotification ->
                if (existingNotifications.none { it.title == newNotification.title && it.description == newNotification.description }) {
                    existingNotifications.add(newNotification)
                }
            }

            notifications.clear()
            notifications.addAll(existingNotifications)

            // Update local database asynchronously
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                notificationDao.deleteExpiredNotifications() // Remove expired notifications

                activeNotifications.forEach { notification ->
                    val exists = notificationDao.getNotification(notification.title, notification.description) != null
                    if (!exists) {
                        notificationDao.insertNotification(
                            NotificationEntity(
                                image = notification.image,
                                title = notification.title,
                                description = notification.description,
                                time = notification.time,
                                postTime = notification.postTime
                            )
                        )
                    }
                }
            }
        }
}

private fun createSearchNotificationIfNeeded(
    doc: DocumentSnapshot,
    currentTime: Long,
    timeIntervals: List<Int>
): NotificationEntity? {
    val searchId = doc.id
    val date = doc.getString("date") ?: return null
    val time = doc.getString("time") ?: return null
    val location = doc.getString("location") ?: return null
    val destination = doc.getString("destination") ?: return null

    val searchTimestamp = convertToTimestamp(date, time)

    val timeLeftMillis = searchTimestamp - currentTime
    val hoursLeft = (timeLeftMillis / (1000 * 60 * 60)).toInt()
    val minutesLeft = ((timeLeftMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
// Save the notification creation time as a fixed timestamp
    val postTime = getCurrentTime() // Fixed timestamp when created
    return when {
        hoursLeft in timeIntervals && minutesLeft == 0 -> NotificationEntity(
            image = R.drawable.profile_ico, // Change image for search notification
            title = "Search Ride Reminder",
            description = "Your searched ride from $location to $destination is in $hoursLeft hour${if (hoursLeft > 1) "s" else ""}!",
            time = "$hoursLeft hours left",
            postTime = postTime
        )
        hoursLeft < 0 -> NotificationEntity( // Mark search as expired
            image = R.drawable.profile_ico,
            title = "Searched Ride Expired",
            description = "Your searched ride from $location to $destination has already passed.",
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
                    tint = Color(0xFF1976D2) // SHARide theme color
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



/**
 * Displays the list of notifications.
 */
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
                    horizontalArrangement = Arrangement.SpaceBetween // Align time to the right
                ) {
                    Text(text = time, fontWeight = FontWeight.Bold, fontSize = 16.sp) // "3 hours left"
                    Text(text = postTime, fontSize = 14.sp, color = Color.Gray) // Fixed timestamp
                }
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = description, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}


/**
 * Returns the current system time in HH:mm format
 */
fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}


