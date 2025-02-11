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
import com.kxxr.sharide.R

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
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
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
