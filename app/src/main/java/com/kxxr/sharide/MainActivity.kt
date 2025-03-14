package com.kxxr.sharide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Network.NetworkViewModel
import com.kxxr.sharide.screen.AppNavHost
import com.kxxr.sharide.screen.NoInternetScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAYFHXZLaqAKMiPvxHKw45xRrlm95Vng7U")
        }

        enableEdgeToEdge()
        setContent {
            ShareRideApp()
        }
    }
}

@Composable
fun ShareRideApp() {
    // Get the application context
    val context = LocalContext.current
    // Get the NetworkViewModel instance using hiltViewModel
   val networkViewModel: NetworkViewModel = hiltViewModel()

    // Observe network state
    val isConnected by networkViewModel.isConnected.collectAsState(initial = true)


    MaterialTheme {
        if (isConnected) {
            // Pass FirebaseAuth and FirebaseFirestore
            AppNavHost(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), networkViewModel,context)
        } else {
            // Show a no-internet connection screen
            NoInternetScreen(onRetry = { /* Retry logic, if needed */ })
        }
    }
}
