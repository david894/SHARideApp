package com.kxxr.sharmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.kxxr.logiclibrary.Network.NetworkViewModel
import com.kxxr.sharmin.ui.theme.SHARideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SHARminApp()
        }
    }
}

@Composable
fun SHARminApp() {
    // Get the application context
    val context = LocalContext.current
    // Get the NetworkViewModel instance using hiltViewModel
    val networkViewModel: NetworkViewModel = hiltViewModel()

    // Observe network state
    val isConnected by networkViewModel.isConnected.collectAsState(initial = true)


    MaterialTheme {
        if (isConnected) {
            // Pass FirebaseAuth and FirebaseFirestore
            //AppNavHost(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), networkViewModel,context)
        } else {
            // Show a no-internet connection screen
            //NoInternetScreen(onRetry = { /* Retry logic, if needed */ })
        }
    }
}
