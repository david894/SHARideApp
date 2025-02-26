@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun SearchRideScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // State variables
    var date by remember { mutableStateOf("Select Date") }
    var time by remember { mutableStateOf("Select Time") }
    var location by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var locationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var capacity by remember { mutableStateOf(1) }
    var searchId by remember { mutableStateOf("") }
    var petPreference by remember { mutableStateOf("Select pet Preference") }
    var genderPreference by remember { mutableStateOf("Select gender Preference") }
    var vehicleType by remember { mutableStateOf("Vehicle Type") }
    ObserveSearchSelectedLocations(navController, lifecycleOwner, { loc, latLng ->
        location = loc
        locationLatLng = latLng
    }, { dest, latLng ->
        destination = dest
        destinationLatLng = latLng
    })

    // Reset capacity when vehicleType changes
    LaunchedEffect(vehicleType) {
        capacity = 1
    }

    Scaffold(
        topBar = { CreateSearchTopBar(navController) },
        // important
        bottomBar = { BottomNavBar("search_ride", navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Enables scrolling
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchRideDetailsCard(
                navController,
                context,
                date, { date = it },
                time, { time = it },
                location,
                destination, { destination = it },
                capacity, { capacity = it } ,
                onCapacityReset = { capacity = 1 },
                petPreference, { petPreference = it },
                genderPreference, { genderPreference = it },
                vehicleType, { vehicleType = it },
            )

            ConfirmSearchButton(
                navController = navController,
                firestore = firestore,
                date = date,
                time = time,
                location = location,
                destination = destination,
                petPreference = petPreference,
                genderPreference = petPreference,
                vehicleType = vehicleType,
                capacity = capacity,
                userId = userId,
                onSearchIdChange = { searchId = it }
            )
        }
    }
}

@Composable
fun ObserveSearchSelectedLocations(
    navController: NavController,
    lifecycleOwner: LifecycleOwner,
    onLocationSelected: (String, LatLng) -> Unit,
    onDestinationSelected: (String, LatLng) -> Unit
) {
    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_location")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                onLocationSelected(selectedAddress, selectedLatLng)
            }

        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_destination")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                onDestinationSelected(selectedAddress, selectedLatLng)
            }
    }
}


@Composable
fun CreateSearchTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("Search Ride", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { // Navigates back
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD)) // Blue background
    )
}


@Composable
fun SearchRideDetailsCard(
    navController: NavController,
    context: Context,
    date: String, onDateChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit,
    location: String,
    destination: String, onDestinationChange: (String) -> Unit,
    capacity: Int, onCapacityChange: (Int) -> Unit,
    onCapacityReset: () -> Unit,
    petPreference: String, onPetPreferenceChange: (String) -> Unit,
    genderPreference: String, onGenderPreferenceChange: (String) -> Unit,
    VehicleType:  String, onVehicleTypeChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SearchLocationFields(navController, location, destination, onDestinationChange)
            DateTimePicker(context, date, onDateChange, time, onTimeChange)
            petPreferenceDropdown(petPreference, onPetPreferenceChange)
            genderPreferenceDropdown(genderPreference, onGenderPreferenceChange)
            VehicleDropdown(VehicleType, onVehicleTypeChange)
            CapacitySelectorCarSeated(VehicleType, capacity, onCapacityChange, onCapacityReset)
        }
    }
}
@Composable
fun SearchLocationFields(
    navController: NavController,
    location: String,
    destination: String,
    onDestinationChange: (String) -> Unit
) {
    Column {
        Text("Current Location", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = location,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { navController.navigate("search_location") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Select Location")
                }
            }
        )
        Text("Select Destination", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = destination,
            onValueChange = onDestinationChange,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { navController.navigate("search_destination") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Select Destination")
                }
            }
        )
    }
}

@Composable
fun petPreferenceDropdown(petPreference: String, onPetPreferenceChange: (String) -> Unit) {
    val routePreferences = listOf("No", "Yes")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Pet Preference", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = petPreference,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Pet Preference", tint = Color(0xFF0075FD))
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routePreferences.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onPetPreferenceChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun genderPreferenceDropdown(genderPreference: String, onGenderPreferenceChange: (String) -> Unit) {
    val routePreferences = listOf("Male", "Female", "Both")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Driver Gender Preference", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = genderPreference,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Gender Preference", tint = Color(0xFF0075FD))
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routePreferences.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onGenderPreferenceChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VehicleDropdown(VehicleType: String, onVehicleTypeChange: (String) -> Unit) {
    val routePreferences = listOf("5-seated", "7-seated")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Vehicle Type", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = VehicleType,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Vehicle Type", tint = Color(0xFF0075FD))
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routePreferences.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onVehicleTypeChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConfirmSearchButton(
    navController: NavController,
    firestore: FirebaseFirestore,
    date: String,
    time: String,
    location: String,
    destination: String,
    petPreference: String,
    genderPreference: String,
    vehicleType: String,
    capacity: Int,
    userId: String,
    onSearchIdChange: (String) -> Unit
) {
    val context = LocalContext.current

    val isValidSearch = remember(date, time, location, destination, petPreference, genderPreference, vehicleType) {
        date != "Select Date" &&
                time != "Now" &&
                location.isNotBlank() &&
                destination.isNotBlank() &&
                petPreference != "Select pet Preference" &&
                genderPreference != "Select gender Preference"
    }

    Button(
        onClick = {
            if (!isValidSearch) {
                Toast.makeText(
                    context,
                    "Please fill in all required fields before searching for a ride.",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }

            if (location.trim() == destination.trim()) {
                Toast.makeText(
                    context,
                    "Location and Destination cannot be the same!",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }

            val searchRef = firestore.collection("searchs").document() // Auto-generate search ID
            val searchId = searchRef.id

            val searchData = mapOf(
                "searchId" to searchId,
                "passengerId" to userId, // Store userId as passengerId
                "date" to date,
                "time" to time,
                "location" to location,
                "destination" to destination,
                "petPreference" to petPreference,
                "genderPreference" to genderPreference,
                "vehicleType" to vehicleType,
                "capacity" to capacity,
                "timestamp" to FieldValue.serverTimestamp() // Add server timestamp
            )

            searchRef.set(searchData)
                .addOnSuccessListener {
                    onSearchIdChange(searchId) // Update searchId in UI state
                    navController.navigate("matching_screen") // Navigate to MatchingScreen
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to create search: ${e.message}", Toast.LENGTH_LONG).show()
                }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Confirm Search", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun CapacitySelectorCarSeated(
    vehicleType: String,
    capacity: Int,
    onCapacityChanged: (Int) -> Unit,
    onCapacityReset: () -> Unit
) {
    val maxCapacity = when (vehicleType) {
        "5-seated" -> 3
        "7-seated" -> 5
        else -> 1
    }

    LaunchedEffect(vehicleType) {
        onCapacityReset() // Reset capacity when vehicle type changes
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Capacity", fontWeight = FontWeight.SemiBold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (capacity > 1) onCapacityChanged(capacity - 1) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease Capacity",
                    tint = Color.White
                )
            }

            Text(
                text = capacity.toString(),
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 18.sp
            )

            IconButton(
                onClick = { if (capacity < maxCapacity) onCapacityChanged(capacity + 1) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase Capacity",
                    tint = Color.White
                )
            }
        }
    }
}






