package com.kxxr.logiclibrary.Network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kxxr.logiclibrary.R

//
//@Composable
//fun isConnectedToInternet(): Boolean {
//    val context = LocalContext.current
//    val connectivityManager = remember {
//        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    }
//
//    val networkStatus = remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        val network = connectivityManager.activeNetwork
//        val capabilities = connectivityManager.getNetworkCapabilities(network)
//        networkStatus.value = capabilities != null &&
//                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
//                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
//    }
//
//    return networkStatus.value
//}

