package com.kxxr.sharide

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.sharide.logic.NetworkViewModel
import com.kxxr.sharide.screen.AppNavHost
import com.kxxr.sharide.screen.IntroScreen
import com.kxxr.sharide.screen.NoInternetScreen
import com.kxxr.sharide.ui.theme.SHARideTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

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
    // Get the NetworkViewModel instance using hiltViewModel
    val networkViewModel: NetworkViewModel = hiltViewModel()

    // Observe network state
    val isConnected by networkViewModel.isConnected.collectAsState(initial = true)

    MaterialTheme {
        if (isConnected) {
            // Pass FirebaseAuth and FirebaseFirestore
            AppNavHost(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), networkViewModel)
        } else {
            // Show a no-internet connection screen
            NoInternetScreen(onRetry = { /* Retry logic, if needed */ })
        }
    }
}




@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SHARideTheme {
        Greeting("Android")
    }
}