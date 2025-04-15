package com.kxxr.sharide

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Network.NetworkViewModel
import com.kxxr.sharide.screen.AppNavHost
import com.kxxr.sharide.screen.NoInternetScreen
import com.kxxr.sharide.viewmodel.NfcViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcViewModel: NfcViewModel
    private var isNfcSupported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get NFC adapter safely
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        isNfcSupported = nfcAdapter != null

        // Get ViewModel directly
        nfcViewModel = ViewModelProvider(this)[NfcViewModel::class.java]

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAYFHXZLaqAKMiPvxHKw45xRrlm95Vng7U")
        }

        enableEdgeToEdge()
        setContent {
            ShareRideApp(isNfcSupported)
        }

        // Only handle NFC if supported
        if (isNfcSupported) {
            intent?.let { handleNfcIntent(it) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        tag?.let {
            val result = nfcViewModel.readSector2Block0(it)
            nfcViewModel.setNfcValue(result)
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf<IntentFilter>()
        val techList = arrayOf(arrayOf(MifareClassic::class.java.name))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

}

@Composable
fun ShareRideApp(isNfcSupport: Boolean) {
    // Get the application context
    val context = LocalContext.current
    // Get the NetworkViewModel instance using hiltViewModel
   val networkViewModel: NetworkViewModel = hiltViewModel()
    val nfcViewModel: NfcViewModel = hiltViewModel()

    // Observe network state
    val isConnected by networkViewModel.isConnected.collectAsState(initial = true)
    val nfcValue by nfcViewModel.nfcValue

    MaterialTheme {
        if (isConnected) {
            // Pass FirebaseAuth and FirebaseFirestore
            AppNavHost(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance(), networkViewModel,context, isNfcSupport,nfcValue)

        } else {
            // Show a no-internet connection screen
            NoInternetScreen(onRetry = { /* Retry logic, if needed */ })
        }
    }
}


