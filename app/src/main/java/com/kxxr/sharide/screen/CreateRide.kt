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
import androidx.compose.foundation.shape.RoundedCornerShape
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





@Composable
fun CreateRideScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    // State variables
    var date by remember { mutableStateOf("Select Date") }
    var time by remember { mutableStateOf("Now") }
    var location by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var locationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var routePreference by remember { mutableStateOf("Selecting Preference") }
    var capacity by remember { mutableStateOf(1) }
    var rideId by remember { mutableStateOf("") }

    ObserveSelectedLocations(navController, lifecycleOwner, { loc, latLng ->
        location = loc
        locationLatLng = latLng
    }, { dest, latLng ->
        destination = dest
        destinationLatLng = latLng
    })

    Scaffold(
        topBar = { CreateRideTopBar(navController) },
        bottomBar = { BottomNavBar("create_ride", navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RideDetailsCard(
                navController,
                context,
                date, { date = it },
                time, { time = it },
                location,
                destination, { destination = it },
                routePreference, { routePreference = it },
                capacity, { capacity = it }
            )

            ConfirmRideButton(
                navController = navController,
                firestore = firestore,
                date = date,
                time = time,
                location = location,
                destination = destination,
                routePreference = routePreference,
                capacity = capacity,
                userId = userId,
                onRideIdChange = { rideId = it }
            )
        }
    }
}

@Composable
fun ObserveSelectedLocations(
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
fun CreateRideTopBar(navController: NavController) {
    TopAppBar(
        title = { Text("Create Ride", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { // Navigates back
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD)) // Blue background
    )
}


@Composable
fun RideDetailsCard(
    navController: NavController,
    context: Context,
    date: String, onDateChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit,
    location: String,
    destination: String, onDestinationChange: (String) -> Unit,
    routePreference: String, onRoutePreferenceChange: (String) -> Unit,
    capacity: Int, onCapacityChange: (Int) -> Unit
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
            DateTimePicker(context, date, onDateChange, time, onTimeChange)
            LocationFields(navController, location, destination, onDestinationChange)
            RoutePreferenceDropdown(routePreference, onRoutePreferenceChange)
            CapacitySelector(capacity, onCapacityChange)
        }
    }
}
@Composable
fun DateTimePicker(
    context: Context,
    date: String, onDateChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit
) {
    Column {
        Text("Select Date", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = date,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showDatePicker(context, onDateChange) }) {
                    Icon(Icons.Outlined.DateRange, contentDescription = "Pick Date", tint = Color(0xFF0075FD))
                }
            }
        )
    }

    Column {
        Text("Select Time", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = time,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showTimePicker(context, onTimeChange) }) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = "Pick Time", tint = Color(0xFF0075FD))
                }
            }
        )
    }
}

@Composable
fun LocationFields(
    navController: NavController,
    location: String,
    destination: String, onDestinationChange: (String) -> Unit
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
fun RoutePreferenceDropdown(routePreference: String, onRoutePreferenceChange: (String) -> Unit) {
    val routePreferences = listOf("Shortest Time", "Shortest Distance", "Highest Passenger Count")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Route Preference", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = routePreference,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Route Preference", tint = Color(0xFF0075FD))
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routePreferences.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onRoutePreferenceChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConfirmRideButton(
    navController: NavController,
    firestore: FirebaseFirestore,
    date: String,
    time: String,
    location: String,
    destination: String,
    routePreference: String,
    capacity: Int,
    userId: String,
    onRideIdChange: (String) -> Unit
) {
    Button(
        onClick = {
            val rideRef = firestore.collection("rides").document() // Auto-generate ride ID
            val rideId = rideRef.id

            val rideData = mapOf(
                "rideId" to rideId,
                "driverId" to userId, // Store userId as driverId
                "date" to date,
                "time" to time,
                "location" to location,
                "destination" to destination,
                "routePreference" to routePreference,
                "capacity" to capacity
            )

            rideRef.set(rideData).addOnSuccessListener {
                onRideIdChange(rideId) // Update rideId in UI state
                navController.navigate("matching_screen") // Navigate to MatchingScreen
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Confirm Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}



@Composable
fun CapacitySelector(capacity: Int, onCapacityChanged: (Int) -> Unit) {
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
                onClick = { if (capacity < 3) onCapacityChanged(capacity + 1) },
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

fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }

            if (selectedCalendar.timeInMillis > System.currentTimeMillis()) {
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            } else {
                Toast.makeText(context, "Please select a future time.", Toast.LENGTH_SHORT).show()
            }
        },
        hour,
        minute,
        false
    )
    timePickerDialog.show()
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }

            if (selectedCalendar.timeInMillis >= System.currentTimeMillis()) {
                val formattedDate =
                    String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                onDateSelected(formattedDate)
            } else {
                Toast.makeText(context, "Please select a future date.", Toast.LENGTH_SHORT).show()
            }
        },
        year,
        month,
        day
    )

    // Set minimum date to today (restrict past dates)
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()
    datePickerDialog.show()
}
