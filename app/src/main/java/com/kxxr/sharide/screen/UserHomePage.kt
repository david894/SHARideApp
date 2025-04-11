
package com.kxxr.sharide.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import com.kxxr.sharide.R
import com.kxxr.sharide.db.Ride
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// Main home screen(driver and passenger)
@Composable
fun HomePage(navController: NavController, firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore) {


    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapScreen(navController, firebaseAuth, firestore)
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(navController: NavController, firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore) {
    val context = LocalContext.current

    // Multiple permissions: Location and Notification
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    )

    var isPermissionRequested by remember { mutableStateOf(false) }
    var isDriver by remember { mutableStateOf(getDriverPreference(context)) }

    val locationGranted = permissionsState.permissions.find { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }?.status?.isGranted == true
    val notificationGranted = permissionsState.permissions.find { it.permission == Manifest.permission.POST_NOTIFICATIONS }?.status?.isGranted == true

    when {
        // All permissions granted
        locationGranted && notificationGranted -> {
            ShowUserScreen(
                navController, firebaseAuth, firestore, isDriver,
                onRoleChange = {
                    isDriver = it
                    saveDriverPreference(context, it)
                }
            )
        }

        // Show error for specific missing permission
        isPermissionRequested -> {
            when {
                !locationGranted -> LocationPermissionErrorScreen(context)
                !notificationGranted -> NotificationPermissionErrorScreen(context)
            }
        }

        // First launch: request permissions
        else -> {
            LaunchedEffect(Unit) {
                isPermissionRequested = true
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}


fun saveDriverPreference(context: Context, isDriver: Boolean) {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean("isDriver", isDriver).apply()
}

fun getDriverPreference(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("isDriver", false) // Default to false (Passenger mode)
}


@Composable
fun ShowUserScreen(
    navController: NavController?,
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    isDriver: Boolean,
    onRoleChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val location = fusedLocationProviderClient.lastLocation.await()
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation!!, 15f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Exit if navController is null
    val safeNavController = navController ?: return

    Scaffold(
        bottomBar = { BottomNavBar("home", navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProfileHeader(firebaseAuth, firestore, isDriver, onRoleChange, navController)

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
                ) {
                    userLocation?.let { Marker(state = MarkerState(position = it), title = "You are here") }
                }
            }

            if (isDriver) {
                RideReminder(firebaseAuth, firestore, safeNavController)
            } else {
                SearchReminder(firebaseAuth, firestore, safeNavController)
            }

            Button(
                onClick = { navController.navigate(if (isDriver) "create_ride" else "search_ride") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
            ) {
                Text(text = if (isDriver) "Create Ride" else "Search Ride", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}


// Profile header with user info and icons
@Composable
fun ProfileHeader(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    isDriver: Boolean,
    onRoleChange: (Boolean) -> Unit,
    navController: NavController
) {
    val userName = fetchUserName(firebaseAuth, firestore)
    val profileBitmap = fetchProfileImage(firebaseAuth)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profileBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.profile_ico),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Hi $userName",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Car/Passenger icon
        val roleIcon = if (isDriver) R.drawable.car_front else R.drawable.profile_ico
        Image(
            painter = painterResource(id = roleIcon),
            contentDescription = if (isDriver) "Driver Mode" else "Passenger Mode",
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))


        Switch(
            checked = isDriver,
            onCheckedChange = { onRoleChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0075FD),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Notification icon
        IconButton(onClick = { navController.navigate("notification") }) {
            Icon(
                painter = painterResource(id = R.drawable.notification_ico),
                contentDescription = "Notifications",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

//@Composable
//fun ProfileHeader(
//    firebaseAuth: FirebaseAuth,
//    firestore: FirebaseFirestore,
//    isDriver: Boolean,
//    onRoleChange: (Boolean) -> Unit,
//    navController: NavController
//) {
//    val userName = fetchUserName(firebaseAuth, firestore)
//    val profileBitmap = fetchProfileImage(firebaseAuth)
//    val context = LocalContext.current
//    val currentUser = firebaseAuth.currentUser
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(56.dp)
//            .padding(horizontal = 16.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        profileBitmap?.let {
//            Image(
//                bitmap = it.asImageBitmap(),
//                contentDescription = "Profile Picture",
//                modifier = Modifier
//                    .size(40.dp)
//                    .clip(CircleShape)
//            )
//        } ?: Image(
//            painter = painterResource(id = R.drawable.profile_ico),
//            contentDescription = "Profile Picture",
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//        )
//
//        Spacer(modifier = Modifier.width(8.dp))
//
//        Text(
//            text = "Hi $userName",
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold
//        )
//
//        Spacer(modifier = Modifier.weight(1f))
//
//        // Car/Passenger icon
//        val roleIcon = if (isDriver) R.drawable.car_front else R.drawable.profile_ico
//        Image(
//            painter = painterResource(id = roleIcon),
//            contentDescription = if (isDriver) "Driver Mode" else "Passenger Mode",
//            modifier = Modifier.size(40.dp)
//        )
//
//        Spacer(modifier = Modifier.width(8.dp))
//
//        // Role Switch with Validation
//        Switch(
//            checked = isDriver,
//            onCheckedChange = { isChecked ->
//                if (isChecked) {
//                    currentUser?.let {
//                        firestore.collection("driver")
//                            .whereEqualTo("userId", it.uid)
//                            .get()
//                            .addOnSuccessListener { querySnapshot ->
//                                if (querySnapshot.isEmpty) {
//                                    navController.navigate("driverintro")
//                                } else {
//                                    onRoleChange(true)
//                                }
//                            }
//                            .addOnFailureListener {
//                                Toast.makeText(context, "Error checking driver status", Toast.LENGTH_SHORT).show()
//                            }
//                    }
//                } else {
//                    onRoleChange(false)
//                }
//            },
//            colors = SwitchDefaults.colors(
//                checkedThumbColor = Color.White,
//                checkedTrackColor = Color(0xFF0075FD),
//                uncheckedThumbColor = Color.White,
//                uncheckedTrackColor = Color.Gray
//            )
//        )
//
//        Spacer(modifier = Modifier.width(8.dp))
//
//        // Notification icon
//        IconButton(onClick = { navController.navigate("notification") }) {
//            Icon(
//                painter = painterResource(id = R.drawable.notification_ico),
//                contentDescription = "Notifications",
//                modifier = Modifier.size(40.dp)
//            )
//        }
//    }
//}



@Composable
fun RideReminder(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    navController: NavController
) {
    val rideItems = remember { mutableStateListOf<Ride>() }
    val userId = firebaseAuth.currentUser?.uid ?: ""

    // Fetch rides for drivers
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            firestore.collection("rides")
                .whereEqualTo("driverId", userId)
                .addSnapshotListener { documents, error ->
                    if (error != null || documents == null) return@addSnapshotListener

                    val currentTime = System.currentTimeMillis()
                    val itemList = mutableListOf<Ride>()

                    documents.forEach { doc ->
                        val date = doc.getString("date") ?: return@forEach
                        val time = doc.getString("time") ?: return@forEach
                        val rideId = doc.id
                        val timestamp = convertToTimestamp(date, time)
                        val timeLeftMillis = timestamp - currentTime
                        val rideIdStatus = doc.getString("rideStatus") ?: return@forEach

                        // Default status (assumes "Prepare" until requestStatus is retrieved)
                        var status = formatTimeLeft(timeLeftMillis, null,null)

                        firestore.collection("requests")
                            .whereEqualTo("rideId", rideId)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val requestStatus = querySnapshot.documents
                                    .firstOrNull()?.getString("status") ?: "Unknown"
                                val updatedStatus = formatTimeLeft(timeLeftMillis,rideIdStatus,requestStatus)

                                // Update rideItems inside listener
                                rideItems.removeIf { it.id == rideId }
                                rideItems.add(
                                    Ride(
                                        id = rideId,
                                        status = updatedStatus,
                                        timeLeftMillis = timeLeftMillis,
                                        date = date,
                                        time = time
                                    )
                                )
                                rideItems.sortByDescending { it.timeLeftMillis }
                            }
                            .addOnFailureListener {
                                // If no request document is found, default to "Unknown"
                                rideItems.removeIf { it.id == rideId }
                                rideItems.add(
                                    Ride(
                                        id = rideId,
                                        status = "Unknown",
                                        timeLeftMillis = timeLeftMillis,
                                        date = date,
                                        time = time
                                    )
                                )
                                rideItems.sortByDescending { it.timeLeftMillis }
                            }

                        // Add temporary Ride object (status will update asynchronously)
                        itemList.add(
                            Ride(
                                id = rideId,
                                status = status,
                                timeLeftMillis = timeLeftMillis,
                                date = date,
                                time = time
                            )
                        )
                    }

                    // Clear and update rideItems
                    rideItems.clear()
                    rideItems.addAll(itemList)
                }
        }
    }

    ReminderContent(
        title = "Ride Reminder",
        emptyText = "No rides available",
        items = rideItems,
        isDriver = true,
        navController = navController,
        firestore = firestore,
    )
}

@Composable
fun SearchReminder(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    navController: NavController
) {
    val searchItems = remember { mutableStateListOf<Ride>() }
    val userId = firebaseAuth.currentUser?.uid ?: ""

    // Fetch searches for passengers
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            firestore.collection("searchs")
                .whereEqualTo("passengerId", userId)
                .addSnapshotListener { documents, error ->
                    if (error != null || documents == null) return@addSnapshotListener

                    val currentTime = System.currentTimeMillis()
                    val itemList = mutableListOf<Ride>()

                    documents.forEach { doc ->
                        val date = doc.getString("date") ?: return@forEach
                        val time = doc.getString("time") ?: return@forEach
                        val searchId = doc.id
                        val timestamp = convertToTimestamp(date, time)
                        val timeLeftMillis = timestamp - currentTime
                        val rideIdStatus = "null"
                        // Default status (assumes "Unknown" until requestStatus is retrieved)
                        var status = formatTimeLeft(timeLeftMillis, null,null)

                        // Fetch status from "requests" collection using searchId
                        firestore.collection("requests")
                            .whereEqualTo("searchId", searchId) // Ensure this field exists in "requests"
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val requestStatus = querySnapshot.documents
                                    .firstOrNull()?.getString("status") ?: "Unknown"  // Take first matching request status
                                val updatedStatus = formatTimeLeft(timeLeftMillis,rideIdStatus, requestStatus)

                                // Update searchItems list with correct status
                                searchItems.removeIf { it.id == searchId }
                                searchItems.add(
                                    Ride(
                                        id = searchId,
                                        status = updatedStatus,
                                        timeLeftMillis = timeLeftMillis,
                                        date = date,
                                        time = time
                                    )
                                )
                                searchItems.sortByDescending { it.timeLeftMillis }
                            }
                            .addOnFailureListener {
                                // If no request is found, default to "Unknown"
                                searchItems.removeIf { it.id == searchId }
                                searchItems.add(
                                    Ride(
                                        id = searchId,
                                        status = "Unknown",
                                        timeLeftMillis = timeLeftMillis,
                                        date = date,
                                        time = time
                                    )
                                )
                                searchItems.sortByDescending { it.timeLeftMillis }
                            }

                        // Add temporary Ride object (status will update asynchronously)
                        itemList.add(
                            Ride(
                                id = searchId,
                                status = status,
                                timeLeftMillis = timeLeftMillis,
                                date = date,
                                time = time
                            )
                        )
                    }

                    // Clear and update searchItems
                    searchItems.clear()
                    searchItems.addAll(itemList)
                }
        }
    }

    ReminderContent(
        title = "Search Reminder",
        emptyText = "No active searches",
        items = searchItems,
        isDriver = false,
        navController = navController,
        firestore = firestore,
    )
}



@Composable
fun ReminderContent(
    title: String,
    emptyText: String,
    items: List<Ride>,
    isDriver: Boolean,
    navController: NavController,
    firestore: FirebaseFirestore,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))

        val filteredItems = items.filter { it.status.lowercase() != "canceled" }

        if (filteredItems.isEmpty()) {
            Text(text = emptyText, fontSize = 16.sp, color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(filteredItems) { item ->
                    val index = items.indexOf(item)
                    RideItem(
                        title = if (isDriver) "Ride ${index + 1}" else "Search ${index + 1}",
                        status = item.status,
                        statusColor = getStatusColor(item.status),
                        isDriver = isDriver,
                        onClick = {
                            if (isDriver) {
                                    firestore.collection("requests")
                                        .whereEqualTo("rideId", item.id)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            val rideStatus = if (documents.isEmpty) "Unknown"
                                            else documents.documents.first().getString("status") ?: "Unknown"




                                            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                                            val dateTimeString = "${item.date} ${item.time}"
                                            val rideTimeMillis = formatter.parse(dateTimeString)?.time ?: 0L

                                            val currentTimeMillis = System.currentTimeMillis()
                                            val timeDiff = rideTimeMillis - currentTimeMillis
                                            val gracePeriodMillis = -60 * 60 * 1000

                                            if (rideStatus !in listOf("pending", "Unknown","successful")) {
                                                navController.navigate("ride_detail/${index + 1}/${item.id}")
                                            } else if ((rideStatus == "pending" || rideStatus == "Unknown" || rideStatus == "successful") && timeDiff >= gracePeriodMillis) {
                                                navController.navigate("ride_detail/${index + 1}/${item.id}")
                                            }
                                            else {
                                                Toast.makeText(context, "Ride is already past. No action taken.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Log.e("Navigation", "Failed to fetch ride details")
                                        }


                            } else {
                                checkRequestStatus(
                                    firestore = firestore,
                                    searchId = item.id,
                                    time = item.time,
                                    date = item.date,
                                    navController = navController,
                                    index = index,
                                    context = context
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

fun checkRequestStatus(
    firestore: FirebaseFirestore,
    searchId: String,
    time: String,
    date: String,
    navController: NavController,
    index: Int,
    context: Context
) {
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    val dateTimeString = "$date $time"
    val rideTimeMillis = formatter.parse(dateTimeString)?.time ?: 0L

    val currentTimeMillis = System.currentTimeMillis()
    val timeDiff = rideTimeMillis - currentTimeMillis

    firestore.collection("requests")
        .whereEqualTo("searchId", searchId)
        .get()
        .addOnSuccessListener { documents ->
            val expiredStatuses = listOf("pending", "Unknown","successful")


            if (documents.isEmpty) {
                if (timeDiff > 0) {
                    navController.navigate("matching_screen")
                } else {
                    Toast.makeText(context, "Ride is already past. No action taken.", Toast.LENGTH_SHORT).show()
                }
                return@addOnSuccessListener
            }


            val status = documents.documents.first().getString("status") ?: "Unknown"
            val gracePeriodMillis = -60 * 60 * 1000

            if (timeDiff <  gracePeriodMillis && status in expiredStatuses) {
                Toast.makeText(context, "Ride is already past. No action taken.", Toast.LENGTH_SHORT).show()
            } else {
                when (status) {
                    "canceled" ->  navController.navigate("matching_screen")
                    "pending" -> navController.navigate("pending_ride_requested/$searchId")
                    "successful", "startBoarding","onBoarding","complete" -> navController.navigate("successful_ride_requested/${index + 1}/$searchId")
                }
            }
        }
        .addOnFailureListener {
            Log.e("Firestore", "Error getting request: ${it.message}")
            navController.navigate("matching_screen")
        }
}


fun convertToTimestamp(date: String, time: String): Long {
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    return try {
        val parsedDate = formatter.parse("$date $time")
        parsedDate?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun formatTimeLeft(timeLeftMillis: Long, rideIdStatus: String?, requestStatus: String?): String {
    val gracePeriodMillis = -120 * 60 * 1000

    return when {
        rideIdStatus == "complete" -> "Completed"
        // If more than 1 hour is left
        timeLeftMillis > 3600000 -> {
            val hours = timeLeftMillis / 3600000
            val minutes = (timeLeftMillis % 3600000) / 60000
            "$hours hr $minutes min left"
        }

        // If time is between 1 minute and 1 hour
        timeLeftMillis > 60000 -> when (requestStatus) {
            "startBoarding","onBoarding" -> "OnGoing"
            "complete" -> "Completed"
            else -> "${timeLeftMillis / 60000} min left"
        }

        // If time is late but within the 60-minute grace period
        timeLeftMillis in (gracePeriodMillis + 1)..0 -> when (requestStatus) {
            "Unknown" -> "Prepare"
            "pending", "successful" -> "Prepare"       // Let users prepare even if a bit late
            "startBoarding","onBoarding" -> "OnGoing"
            "complete" -> "Completed"
            else -> "Expired"
        }

        // If time has passed and is beyond the grace period (late by more than 30 minutes)
        timeLeftMillis <= gracePeriodMillis -> when (requestStatus) {
            "pending" -> "Expired"
            "successful" -> "Prepare"
            "startBoarding","onBoarding" -> "OnGoing"
            "complete" -> "Completed"
            else -> "Expired"
        }

        // Default fallback (should rarely be hit)
        else -> when (requestStatus) {
            "pending" -> "Expired"
            "complete" -> "Completed"
            else -> "Expired"
        }
    }
}




fun getStatusColor(status: String): Color {
    return when {
        status.contains("hr") || status.contains("min") -> Color(0xFF0075FD) // Blue for upcoming rides
        status == "Completed" -> Color(0xFF008000) // Green for completed
        status == "Expired" -> Color(0xFFFF4444) // Red for expired
        status == "Prepare" -> Color(0xFFFFA500) // Orange for preparing
        status == "OnGoing" -> Color(0xFF800080) // Purple for ongoing
        else -> Color(0xFFB22222) // Gray for unknown statuses
    }
}


@Composable
fun RideItem(title: String, status: String, statusColor: Color, isDriver: Boolean, onClick: () -> Unit) {
    val iconRes = if (isDriver) R.drawable.car_front else R.drawable.profile_ico

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "User Mode Icon",
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .background(statusColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = status, color = Color.White, fontSize = 14.sp)
        }
    }
}



// Error screen when location permission is denied
@Composable
fun LocationPermissionErrorScreen(context: Context) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Location Access Required", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Image(painter = painterResource(id = R.drawable.error), contentDescription = "Error Icon", modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Go to Settings", color = Color.White)
        }
    }
}


@Composable
fun NotificationPermissionErrorScreen(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Notification Access Required",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.error),
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please allow notification access to stay updated with important ride and chat updates.",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Open App Settings", color = Color.White)
        }
    }
}


@Composable
fun fetchUserName(firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore): String {
    var userName by remember { mutableStateOf("") }
    val currentUser = firebaseAuth.currentUser

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { userId ->
            firestore.collection("users")
                .whereEqualTo("firebaseUserId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val document = querySnapshot.documents.firstOrNull()
                    userName = document?.getString("name").orEmpty()
                }
                .addOnFailureListener {
                    userName = "Unknown"
                }
        }
    }
    return userName
}

@Composable
fun fetchProfileImage(firebaseAuth: FirebaseAuth): Bitmap? {
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val currentUser = firebaseAuth.currentUser

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { userId ->
            Firebase.firestore.collection("users")
                .whereEqualTo("firebaseUserId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val document = querySnapshot.documents.firstOrNull()
                    val profileImageUrl = document?.getString("profileImageUrl").orEmpty()

                    if (profileImageUrl.isNotEmpty()) {
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl)
                        storageReference.getBytes(1024 * 1024)
                            .addOnSuccessListener { bytes ->
                                profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                    }
                }
        }
    }
    return profileBitmap
}
