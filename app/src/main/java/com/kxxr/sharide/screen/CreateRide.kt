@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.app.TimePickerDialog
import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.io.IOException
import java.util.Calendar
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.libraries.places.api.Places
import com.google.maps.android.compose.*


// Setapak central location & radius constraint
val setapakCenter = LatLng(3.187308, 101.703697)
const val RADIUS_METERS = 6000.0 // 6km radius

fun isWithinSetapak(userLocation: LatLng?): Boolean {
    userLocation ?: return false
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        setapakCenter.latitude, setapakCenter.longitude,
        results
    )
    return results[0] <= RADIUS_METERS
}

// save ride to firebase method
fun saveRideToFirebase(rideId: String, date: String, time: String, currentLocation: LatLng, destination: LatLng, routePreference: String, capacity: Int) {
    val db = Firebase.firestore
    val rideData = hashMapOf(
        "rideId" to rideId,
        "date" to date,  // Add date field
        "time" to time,
        "currentLocation" to hashMapOf(
            "latitude" to currentLocation.latitude,
            "longitude" to currentLocation.longitude
        ),
        "destination" to hashMapOf(
            "latitude" to destination.latitude,
            "longitude" to destination.longitude
        ),
        "routePreference" to routePreference,
        "capacity" to capacity
    )

    db.collection("rides").document(rideId)
        .set(rideData)
        .addOnSuccessListener { println("Ride successfully stored in Firebase!") }
        .addOnFailureListener { e -> println("Error storing ride: \${e.message}") }
}


@Composable
fun CreateRideScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var date by remember { mutableStateOf("Select Date") } // New state for date
    var time by remember { mutableStateOf("Now") }
    var location by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var locationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var routePreference by remember { mutableStateOf("Selecting Preference") }
    var capacity by remember { mutableStateOf(1) }
    var rideId by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current

    // 🔹 Observe selected location from SearchLocationScreen
    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_location")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                location = selectedAddress
                locationLatLng = selectedLatLng
            }

        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_destination")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                destination = selectedAddress
                destinationLatLng = selectedLatLng
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Ride", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD))
            )
        },
        bottomBar = {
            BottomNavBar("create_ride", navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CARD CONTAINING ALL RIDE DETAILS
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // DATE PICKER
                    Column {
                        Text("Select Date", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = date,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    showDatePicker(context) { selectedDate ->
                                        date = selectedDate
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.DateRange,
                                        contentDescription = "Pick Date",
                                        tint = Color(0xFF0075FD)
                                    )
                                }
                            }
                        )
                    }

                    // TIME PICKER
                    Column {
                        Text("Select Time", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = time,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    showTimePicker(context) { selectedTime ->
                                        time = selectedTime
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        contentDescription = "Pick Time",
                                        tint = Color(0xFF0075FD)
                                    )
                                }
                            }
                        )
                    }
                    // LOCATION & DESTINATION INPUT
                    Column {
                        Text("Current Location", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = location,
                            onValueChange = {}, // 🔹 User cannot manually input location
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Fetching location...") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(),
                            readOnly = true, // 🔹 Make it read-only
                            trailingIcon = {
                                IconButton(onClick = {
                                    navController.navigate("search_location") // 🔹 Open SearchLocationScreen
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Select Location"
                                    )
                                }
                            }
                        )

                        Text(
                            "Select Destination",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {
                                destination = it
                            }, // 🔹 User can input destination manually
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter destination") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    navController.navigate("search_destination") // 🔹 Open SearchLocationScreen for destination
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Select Destination"
                                    )
                                }
                            }
                        )
                    }


                    // Define the available route preferences
                    val routePreferences =
                        listOf("Shortest Time", "Shortest Distance", "Highest Passenger Count")

                    // State to manage dropdown visibility
                    var expanded by remember { mutableStateOf(false) }

                    Column {
                        Text("Route Preference", fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = routePreference,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    expanded = true
                                }) {  // Toggle dropdown visibility
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Route Preference",
                                        tint = Color(0xFF0075FD)
                                    )
                                }
                            }
                        )

                        // Dropdown Menu
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            routePreferences.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        routePreference = option  // Update selected value
                                        expanded = false  // Close the dropdown
                                    }
                                )
                            }
                        }
                    }

                    // CAPACITY SELECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Capacity", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (capacity > 1) capacity-- }, // Prevent capacity from going below 1
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Blue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove, // Changed from Close to Remove
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
                                onClick = { if (capacity < 3) capacity++ }, // Ensure max capacity is 3
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

                    // CONFIRM RIDE BUTTON
                    Button(
                        onClick = {
                            val rideData = mapOf(
                                "rideId" to rideId,
                                "date" to date,
                                "time" to time,
                                "location" to location,
                                "destination" to destination,
                                "routePreference" to routePreference,
                                "capacity" to capacity
                            )
                            firestore.collection("rides").add(rideData)
                                .addOnSuccessListener { documentReference ->
                                    rideId = documentReference.id
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Confirm Ride",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
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
