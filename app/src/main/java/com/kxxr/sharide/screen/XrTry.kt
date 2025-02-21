@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlin.math.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Calendar
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DateRange
import com.kxxr.sharide.R

// Setapak central location & radius constraint
val setapakCenter = LatLng(3.187308, 101.703697)
const val RADIUS_METERS = 6000.0 // 6km radius

fun isWithinSetapak(userLocation: LatLng?): Boolean {
    userLocation ?: return false
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        setapakCenter.latitude, setapakCenter.longitude,
        results
    )
    return results[0] <= RADIUS_METERS
}

// Main home screen(driver and passenger
@Composable
fun HomePage(navController: NavController) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapScreen(navController)
        }
    }
}

// Map screen and settle location permissions
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var isPermissionRequested by remember { mutableStateOf(false) }

    when {
        // If permission is granted, show the map
        locationPermissionState.status.isGranted -> {
            ShowDriverScreen(navController)
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
fun ShowDriverScreen(navController: NavController?) {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()

    // Get user's last known location
    LaunchedEffect(Unit) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation!!, 15f)
            }
        }
    }
    // will Exit if navController null
    val safeNavController = navController ?: return

    // Scaffold Layout for Bottom Navigation
    Scaffold(
        bottomBar = { BottomNavBar("home", navController) } // Bottom Navigation Bar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // to Prevents overlapping
        ) {
            // Profile Section
            ProfileHeader()

            // Google Map
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
                ) {
                    userLocation?.let {
                        Marker(state = MarkerState(position = it), title = "You are here")
                    }
                }
            }

            // Ride Reminder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Rounded Corners
                    .background(Color.White)
                    .border(1.dp, Color.Gray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(top = 16.dp)
            ) {
                RideReminder()
            }

            // Create Ride Button
            Button(
                onClick = {navController.navigate("create_ride") }, // Navigate to Create Ride Screen
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
            ) {
                Text(text = "Create Ride", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}


// Profile header with user info and icons
@Composable
fun ProfileHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)  // fixed height for heading consistency
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile_ico),
            contentDescription = "Profile Picture",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Hi John",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f)) // Push icons to the right
        Image(
            painter = painterResource(id = R.drawable.car_front),
            contentDescription = "Car",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.notification_ico),
            contentDescription = "Notifications",
            modifier = Modifier.size(40.dp)
        )
    }
}


// Need to update logic for reminder (Problem Xr)
@Composable
fun RideReminder() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp)) // Add space above the title
        Text(text = "Reminder", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        RideItem("Ride 3", "1 hr 30 min left", Color(0xFF0075FD))
        RideItem("Ride 2", "Completed", Color(0xFF008000))
        RideItem("Ride 1", "Cancelled", Color(0xFFFF4444))
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
        // Car Icon on the Left
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

        // Status Box on the Right
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

// save ride to firebase method
fun saveRideToFirebase(rideId: String, date: String, time: String, currentLocation: LatLng, destination: LatLng, routePreference: String, capacity: Int) {
    val db = Firebase.firestore
    val rideData = hashMapOf(
        "rideId" to rideId,
        "date" to date,  // Add date field
        "time" to time,
        "currentLocation" to hashMapOf(
            "latitude" to currentLocation.latitude,
            "longitude" to currentLocation.longitude
        ),
        "destination" to hashMapOf(
            "latitude" to destination.latitude,
            "longitude" to destination.longitude
        ),
        "routePreference" to routePreference,
        "capacity" to capacity
    )

    db.collection("rides").document(rideId)
        .set(rideData)
        .addOnSuccessListener { println("Ride successfully stored in Firebase!") }
        .addOnFailureListener { e -> println("Error storing ride: \${e.message}") }
}


@Composable
fun CreateRideScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var date by remember { mutableStateOf("Select Date") } // New state for date
    var time by remember { mutableStateOf("Now") }
    var location by remember { mutableStateOf("Fetching location...") }
    var destination by remember { mutableStateOf("") }
    var routePreference by remember { mutableStateOf("Selecting Preference") }
    var capacity by remember { mutableStateOf(1) }
    var rideId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Ride", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD))
            )
        },
        bottomBar = {
            BottomNavBar("create_ride", navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CARD CONTAINING ALL RIDE DETAILS
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // DATE PICKER
                    Column {
                        Text("Select Date", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = date,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker(context) { selectedDate -> date = selectedDate } }) {
                                    Icon(Icons.Outlined.DateRange, contentDescription = "Pick Date", tint = Color(0xFF0075FD))
                                }
                            }
                        )
                    }

                    // TIME PICKER
                    Column {
                        Text("Select Time", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = time,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showTimePicker(context) { selectedTime -> time = selectedTime } }) {
                                    Icon(Icons.Outlined.AccessTime, contentDescription = "Pick Time", tint = Color(0xFF0075FD))
                                }
                            }
                        )
                    }
                    // LOCATION & DESTINATION INPUT
                    Column {
                        Text("Current Location", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Fetching location...") },
                            colors = TextFieldDefaults.outlinedTextFieldColors()
                        )

                        Text("Select Destination", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter destination") },
                            colors = TextFieldDefaults.outlinedTextFieldColors()
                        )
                    }

                    // Define the available route preferences
                    val routePreferences = listOf("Shortest Time", "Shortest Distance", "Highest Passenger Count")

// State to manage dropdown visibility
                    var expanded by remember { mutableStateOf(false) }

                    Column {
                        Text("Route Preference", fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = routePreference,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {  // Toggle dropdown visibility
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Route Preference", tint = Color(0xFF0075FD))
                                }
                            }
                        )

                        // Dropdown Menu
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            routePreferences.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        routePreference = option  // Update selected value
                                        expanded = false  // Close the dropdown
                                    }
                                )
                            }
                        }
                    }

                    // CAPACITY SELECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Capacity", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (capacity > 1) capacity-- }, // Prevent capacity from going below 1
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove, // Changed from Close to Remove
                                    contentDescription = "Decrease Capacity",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = capacity.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                fontSize = 18.sp
                            )

                            IconButton(
                                onClick = { if (capacity < 3) capacity++ }, // Ensure max capacity is 3
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Capacity",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // CONFIRM RIDE BUTTON
                    Button(
                        onClick = {
                            val rideData = mapOf(
                                "rideId" to rideId,
                                "date" to date,
                                "time" to time,
                                "location" to location,
                                "destination" to destination,
                                "routePreference" to routePreference,
                                "capacity" to capacity
                            )
                            firestore.collection("rides").add(rideData).addOnSuccessListener { documentReference ->
                                rideId = documentReference.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }

            if (selectedCalendar.timeInMillis > System.currentTimeMillis()) {
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            } else {
                Toast.makeText(context, "Please select a future time.", Toast.LENGTH_SHORT).show()
            }
        },
        hour,
        minute,
        false
    )
    timePickerDialog.show()
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }

            if (selectedCalendar.timeInMillis >= System.currentTimeMillis()) {
                val formattedDate =
                    String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                onDateSelected(formattedDate)
            } else {
                Toast.makeText(context, "Please select a future date.", Toast.LENGTH_SHORT).show()
            }
        },
        year,
        month,
        day
    )

    // Set minimum date to today (restrict past dates)
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()
    datePickerDialog.show()
}