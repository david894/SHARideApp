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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    var isPermissionRequested by remember { mutableStateOf(false) }

    when {
        // If permission is granted, show the map
        locationPermissionState.status.isGranted -> {
            ShowGoogleMap(navController)
        }

        // If permission is permanently denied, show error screen
        locationPermissionState.status.shouldShowRationale.not() && isPermissionRequested -> {
            PermissionErrorScreen(context) // Redirect to settings
        }

        // If permission is not granted, request it (Only trigger once)
        else -> {
            LaunchedEffect(Unit) {
                if (!isPermissionRequested) {
                    isPermissionRequested = true // Ensure it's only triggered once
                    locationPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}

@SuppressLint("MissingPermission", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ShowGoogleMap(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    var showDialog by remember { mutableStateOf(false) }

    // Function to request location updates
    fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000 // 5 seconds interval
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation!!, 15f)
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        )
    }

    // Request location updates when the Composable is first created
    LaunchedEffect(Unit) {
        showDialog = true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(userLocation!!, 15f)
                showDialog = false
            } else {
                requestLocationUpdates()
                showDialog = false
            }
        }
    }

    // **Scaffold Layout for BottomAppBar Placement**
    Scaffold(
        bottomBar = { BottomNavBar("home",navController) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true, // Enable Zoom Controls
                    zoomGesturesEnabled = true, // Enable pinch-to-zoom
                    myLocationButtonEnabled = true // Show My Location Button
                )
            ) {
                userLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You are here",
                        snippet = "Your current location"
                    )
                }
            }

        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun PermissionErrorScreen(
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Location Access Required",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This app requires location access to function properly. Please enable location permissions in settings.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent) // Open App Settings
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Go to Settings", color = Color.White)
        }

    }
}
