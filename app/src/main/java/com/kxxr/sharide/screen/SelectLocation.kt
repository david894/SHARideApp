@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState


@Composable
fun SearchLocationScreen(navController: NavController, isSelectingDestination: Boolean) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(3.187308, 101.703697), 10f) // Default: Setapak
    }

    val placesClient = remember { Places.createClient(context) }

    fun fetchAutocompleteSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                suggestions = response.autocompletePredictions
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    var isExpanded by remember { mutableStateOf(false) } // Controls LazyColumn visibility

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0075FD) // Blue background
                    ),
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location Icon",
                                tint = Color.White,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            TextField(
                                value = searchQuery,
                                onValueChange = { newQuery ->
                                    searchQuery = newQuery
                                    fetchAutocompleteSuggestions(newQuery)
                                    isExpanded = true // Expand when user types
                                },
                                placeholder = { Text("Search location...", color = Color.Gray) },
                                modifier = Modifier
                                    .weight(1f) // Make it fill remaining space
                                    .background(Color.White, shape = RoundedCornerShape(topStart = 12.dp)), // No rounded corners
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = Color.Black,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier
                                    .size(56.dp) // Fixed size to match height
                                    .background(Color.White, shape = RoundedCornerShape(topEnd = 12.dp))
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand/Collapse",
                                    tint = Color(0xFF0075FD)
                                )
                            }
                        }
                    },
                    modifier = Modifier.height(80.dp) // Standard top bar height
                )

                // 🔽 LazyColumn (Only shows when expanded)
                if (isExpanded && suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp)
                    ) {
                        items(suggestions) { prediction ->
                            Text(
                                text = prediction.getPrimaryText(null).toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = prediction.getPrimaryText(null).toString()
                                        val placeId = prediction.placeId

                                        val placeRequest = FetchPlaceRequest.builder(
                                            placeId, listOf(Place.Field.LAT_LNG)
                                        ).build()
                                        placesClient.fetchPlace(placeRequest)
                                            .addOnSuccessListener { response ->
                                                selectedLocation = response.place.latLng
                                                cameraPositionState.move(
                                                    CameraUpdateFactory.newLatLngZoom(response.place.latLng!!, 15f)
                                                )
                                                isExpanded = false // 🔹 Close after selection
                                            }
                                            .addOnFailureListener { it.printStackTrace() }
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
                onMapClick = { latLng ->
                    selectedLocation = latLng
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            ) {
                selectedLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Selected Location"
                    )
                }
            }

            Button(
                onClick = {
                    selectedLocation?.let { latLng ->
                        val address = getAddressFromLatLng(context, latLng)
                        val key = if (isSelectingDestination) "selected_destination" else "selected_location"

                        // 🔹 Ensure the data is correctly stored in the savedStateHandle
                        navController.previousBackStackEntry?.savedStateHandle?.set(key, address to latLng)

                        // 🔹 Navigate back to the previous screen
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
            ) {
                Text("Confirm Location")
            }
        }
    }
}





fun getLatLngFromAddress(context: Context, address: String): LatLng? {
    return try {
        val geocoder = Geocoder(context, java.util.Locale.getDefault())
        val results = geocoder.getFromLocationName(address, 1)
        results?.firstOrNull()?.let {
            LatLng(it.latitude, it.longitude)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    return try {
        val geocoder = Geocoder(context, java.util.Locale.getDefault())
        val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        results?.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown Location"
    }
}
