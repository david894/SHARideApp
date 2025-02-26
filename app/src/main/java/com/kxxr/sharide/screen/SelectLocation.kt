package com.kxxr.sharide.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.*

@Composable
fun SearchLocationScreen(navController: NavController, locationType: Int) {
    var searchQuery by remember { mutableStateOf("") }
    var filteredSuggestions by remember { mutableStateOf(predefinedLocations) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(predefinedLocations.first().second, 15f)
    }
    var isExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SearchBar(
                searchQuery, isExpanded, filteredSuggestions,
                onQueryChange = { newQuery ->
                    searchQuery = newQuery
                    filteredSuggestions = predefinedLocations.filter {
                        it.first.contains(newQuery, ignoreCase = true)
                    }
                    isExpanded = false
                },
                onExpandToggle = { isExpanded = !isExpanded },
                onSelectSuggestion = { name, latLng ->
                    searchQuery = name
                    selectedLocation = latLng
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    isExpanded = false
                }
                ,navController
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            MapView(
                cameraPositionState, selectedLocation,
                onMapClick = { latLng ->
                    val nearest = predefinedLocations.minByOrNull { location ->
                        SphericalUtil.computeDistanceBetween(latLng, location.second)
                    }
                    if (nearest != null && SphericalUtil.computeDistanceBetween(latLng, nearest.second) < 50) {
                        selectedLocation = nearest.second
                        searchQuery = nearest.first
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(nearest.second, 15f))
                    } else {
                        Toast.makeText(context, "Please select a predefined location", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            ConfirmLocationButton(navController, context, selectedLocation, locationType)
        }
    }
}

// Predefined locations list
val predefinedLocations = listOf(
    "TAR UMT Main Entrance" to LatLng(3.1869, 101.6958),
    "Block A" to LatLng(3.1875, 101.6973),
    "Library" to LatLng(3.1882, 101.6990),
    "Canteen" to LatLng(3.1890, 101.7005)
)

@Composable
fun SearchBar(
    searchQuery: String,
    isExpanded: Boolean,
    suggestions: List<Pair<String, LatLng>>,
    onQueryChange: (String) -> Unit,
    onExpandToggle: () -> Unit,
    onSelectSuggestion: (String, LatLng) -> Unit,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Spacer(modifier = Modifier.height(50.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button instead of Location Icon
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back Button",
                    tint = Color(0xFF0075FD),
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                )
            }
            TextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search location...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
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
            IconButton(onClick = onExpandToggle) {
                Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand/Collapse", tint = Color(0xFF0075FD))
            }
        }
        if (isExpanded && suggestions.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                items(suggestions) { (name, latLng) ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(name, latLng) }
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MapView(cameraPositionState: CameraPositionState, selectedLocation: LatLng?, onMapClick: (LatLng) -> Unit) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true),
        onMapClick = onMapClick
    ) {
        selectedLocation?.let {
            Marker(state = MarkerState(position = it), title = "Selected Location")
        }
    }
}

@Composable
fun ConfirmLocationButton(navController: NavController, context: Context, selectedLocation: LatLng?, locationType: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = {
                selectedLocation?.let { latLng ->
                    val matchedLocation = predefinedLocations.find { it.second == latLng }
                    if (matchedLocation != null) {
                        val key = when (locationType) {
                            0 -> "selected_location"
                            1 -> "selected_stop"
                            2 -> "selected_destination"
                            else -> return@Button
                        }
                        navController.previousBackStackEntry?.savedStateHandle?.set(key, matchedLocation.first to latLng)
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Invalid location. Please select from the list.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD))
        ) {
            Text("Confirm Location")
        }
    }
}
