@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun NotificationScreen(
    navController: NavController,
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    context: Context // Pass context to access Room Database
) {
    val userId = firebaseAuth.currentUser?.uid ?: ""
    val notifications = remember { mutableStateListOf<NotificationData>() }
    val notificationDao = remember { NotificationDatabase.getDatabase(context).notificationDao() }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            // Load notifications from local database first
            val localNotifications = withContext(Dispatchers.IO) {
                notificationDao.getAllNotifications()
            }
            notifications.clear()
            notifications.addAll(localNotifications.map {
                NotificationData(it.image, it.title, it.description, it.time)
            })

            // Sync with Firestore
            observeRideNotifications(userId, firestore, notifications, notificationDao)
        }
    }

    Scaffold(topBar = { NotificationTopBar(navController) }) { paddingValues ->
        NotificationList(notifications, paddingValues)
    }
}

/**
 * Observes ride notifications in Firestore and updates the list.
 */
private fun observeRideNotifications(
    userId: String,
    firestore: FirebaseFirestore,
    notifications: MutableList<NotificationData>,
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

            // Get existing notifications from local database
            val existingNotifications = notifications.toList().toMutableList()

            // Merge: Add only new notifications that don't already exist
            newNotifications.forEach { newNotification ->
                if (existingNotifications.none { it.title == newNotification.title && it.description == newNotification.description }) {
                    existingNotifications.add(newNotification)
                }
            }

            notifications.clear()
            notifications.addAll(existingNotifications)

            // Update local database asynchronously
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                newNotifications.forEach { notification ->
                    val exists = notificationDao.getNotification(notification.title, notification.description) != null
                    if (!exists) {
                        notificationDao.insertNotification(
                            NotificationEntity(
                                image = notification.image,
                                title = notification.title,
                                description = notification.description,
                                time = notification.time
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
private fun createNotificationIfNeeded(doc: DocumentSnapshot, currentTime: Long, timeIntervals: List<Int>): NotificationData? {
    val rideId = doc.id
    val date = doc.getString("date") ?: return null
    val time = doc.getString("time") ?: return null
    val rideTimestamp = convertToTimestamp(date, time)

    val timeLeftMillis = rideTimestamp - currentTime
    val hoursLeft = (timeLeftMillis / (1000 * 60 * 60)).toInt()
    val minutesLeft = ((timeLeftMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

    return if (hoursLeft in timeIntervals && minutesLeft == 0) {
        NotificationData(
            image = R.drawable.car_front,
            title = "Upcoming Ride Reminder",
            description = "Your ride (ID: $rideId) is in $hoursLeft hours!",
            time = "$hoursLeft hours left"
        )
    } else {
        null
    }
}

/**
 * Top App Bar for the notification screen.
 */
@Composable
fun NotificationTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("Notification", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color.White)
    )
}

/**
 * Displays the list of notifications.
 */
@Composable
fun NotificationList(notifications: List<NotificationData>, paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        if (notifications.isEmpty()) {
            Text(text = "No ride notifications", fontSize = 16.sp, color = Color.Gray)
        } else {
            notifications.forEach { notification ->
                NotificationItem(
                    image = notification.image,
                    title = notification.title,
                    description = notification.description,
                    time = notification.time
                )
            }
        }
    }
}

@Composable
fun NotificationItem(image: Int, title: String, description: String, time: String) {
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
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = description, fontSize = 14.sp, color = Color.Gray)
            }
            Text(text = time, fontWeight = FontWeight.Bold, color = Color.Blue)
        }
    }
}

data class NotificationData(
    val image: Int,
    val title: String,
    val description: String,
    val time: String
)
