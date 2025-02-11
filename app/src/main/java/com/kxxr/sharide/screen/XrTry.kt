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

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            MyApp()
//        }
//    }
//}

@Composable
fun MyApp(navController: NavController) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapScreen(navController)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var isPermissionRequested by remember { mutableStateOf(false) }

    when {
        locationPermissionState.status.isGranted -> {
            ShowGoogleMap(navController)
        }
        isPermissionRequested && !locationPermissionState.status.isGranted -> {
            PermissionErrorScreen(context)
        }
        else -> {
            LaunchedEffect(Unit) {
                if (!isPermissionRequested) {
                    isPermissionRequested = true
                    locationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}

@Composable
fun ShowGoogleMap(navController: NavController?) {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation!!, 15f)
            }
        }
    }
    // Ensure navController is non-null
    val safeNavController = navController ?: return // will Exit if navController null

    // **Scaffold Layout for Bottom Navigation**
    Scaffold(
        bottomBar = { BottomNavBar("home", navController) } // Add Bottom Navigation Bar
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



            // Ride Reminder Section
            RideReminder()

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
            painter = painterResource(id = R.drawable.car_ico),
            contentDescription = "Car",
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.notification_ico),
            contentDescription = "Notifications",
            modifier = Modifier.size(32.dp)
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
        RideItem("Ride 3", "1 hr 30 min left", Color(0xFF0075FD)) // Blue
        RideItem("Ride 2", "Completed", Color(0xFF008000)) // Green
        RideItem("Ride 1", "Cancelled", Color(0xFFFF4444)) // Red
    }
}


@Composable
fun RideItem(rideName: String, status: String, statusColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = rideName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                text = status,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier
                    .background(statusColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}


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
