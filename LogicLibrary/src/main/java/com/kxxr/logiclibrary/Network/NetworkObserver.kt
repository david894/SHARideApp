package com.kxxr.logiclibrary.Network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            coroutineScope.launch {
                delay(500) // Debounce quick transitions (e.g., LTE to Wi-Fi)
                if (hasInternetConnection()) {
                    _isConnected.emit(true)
                }
            }
        }

        override fun onLost(network: Network) {
            coroutineScope.launch {
                delay(500) // Allow time for quick reconnects
                if (!hasInternetConnection()) {
                    _isConnected.emit(false)
                }
            }
        }
    }

    fun startObserving() {
        // Ensure initial state is updated
        coroutineScope.launch {
            _isConnected.emit(hasInternetConnection())
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // Actual Internet check
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopObserving() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        coroutineScope.cancel() // Clean up coroutines
    }

    private fun hasInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
