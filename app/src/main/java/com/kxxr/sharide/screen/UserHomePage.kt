
package com.kxxr.sharide.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// Main home screen(driver and passenger
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


// Map screen and settle location permissions
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(navController: NavController, firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var isPermissionRequested by remember { mutableStateOf(false) }
    var isDriver by remember { mutableStateOf(false) }
    when {
        // If permission is granted, show the map
        locationPermissionState.status.isGranted -> {
            ShowUserScreen(navController, firebaseAuth, firestore,isDriver, onRoleChange = { isDriver = it })
        }
        // If permission was denied, show error screen
        isPermissionRequested && !locationPermissionState.status.isGranted -> {
            PermissionErrorScreen(context)
        }
        else -> {
            // Request permission on first launch
            LaunchedEffect(Unit) {
                if (!isPermissionRequested) {
                    isPermissionRequested = true
                    locationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}


// Displays driver screen with map,driver location,reminder list, create ride...
@Composable
fun ShowUserScreen(navController: NavController?, firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore, isDriver: Boolean, onRoleChange: (Boolean) -> Unit) {
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

    // Scaffold Layout for Bottom Navigation
    Scaffold(
        bottomBar = { BottomNavBar("home", navController) } // Bottom Navigation Bar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Prevents overlapping
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
                RideReminder(firebaseAuth,firestore)
                Button(
                    onClick = { navController.navigate("create_ride") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
                ) {
                    Text(text = "Create Ride", color = Color.White, fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = {  navController.navigate("search_ride") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
                ) {
                    Text(text = "Search Ride", color = Color.White, fontSize = 18.sp)
                }
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

        // Switch with custom color (moved to the right)
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

@Composable
fun RideReminder(firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore) {
    val rides = remember { mutableStateListOf<Ride>() }
    val userId = firebaseAuth.currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            firestore.collection("rides")
                .whereEqualTo("driverId", userId) // Filter only the user's rides
                .get()
                .addOnSuccessListener { documents ->
                    val currentTime = System.currentTimeMillis()
                    val rideList = documents.mapNotNull { doc ->
                        val date = doc.getString("date") ?: return@mapNotNull null
                        val time = doc.getString("time") ?: return@mapNotNull null
                        val rideTimestamp = convertToTimestamp(date, time)
                        val timeLeftMillis = rideTimestamp - currentTime
                        val timeLeftText = formatTimeLeft(timeLeftMillis)
                        val status = if (timeLeftMillis > 0) timeLeftText else "Completed"

                        Ride(
                            id = doc.id,
                            status = status,
                            timeLeftMillis = timeLeftMillis
                        )
                    }.sortedByDescending { it.timeLeftMillis } // Sort rides by time left

                    rides.clear()
                    rides.addAll(rideList)
                }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Reminder", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))

        if (rides.isEmpty()) {
            Text(text = "No rides available", fontSize = 16.sp, color = Color.Gray)
        } else {
            rides.forEachIndexed { index, ride ->
                RideItem(
                    title = "Ride ${index + 1}",
                    status = ride.status,
                    statusColor = getStatusColor(ride.status)
                )
            }
        }
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

fun formatTimeLeft(timeLeftMillis: Long): String {
    return when {
        timeLeftMillis > 3600000 -> "${timeLeftMillis / 3600000} hr ${timeLeftMillis % 3600000 / 60000} min left"
        timeLeftMillis > 60000 -> "${timeLeftMillis / 60000} min left"
        else -> "Completed"
    }
}

fun getStatusColor(status: String): Color {
    return when {
        status.contains("hr") || status.contains("min") -> Color(0xFF0075FD) // Upcoming rides
        status == "Completed" -> Color(0xFF008000) // Green for completed
        else -> Color(0xFFFF4444) // Red for cancelled (if applicable)
    }
}


@Composable
fun RideItem(title: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.car_front),
            contentDescription = "Car Icon",
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



data class Ride(
    val id: String,
    val status: String,
    val timeLeftMillis: Long
)



// Error screen when location permission is denied
@Composable
fun PermissionErrorScreen(context: Context) {
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
