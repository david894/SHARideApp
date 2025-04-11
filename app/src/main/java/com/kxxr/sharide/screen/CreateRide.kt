@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun CreateRideScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var date by remember { mutableStateOf("Select Date") }
    var time by remember { mutableStateOf("Select Time") }
    var location by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var stop by remember { mutableStateOf("") }
    var locationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var stopLatLng by remember { mutableStateOf<LatLng?>(null) }
    var capacity by remember { mutableStateOf(1) }
    var rideId by remember { mutableStateOf("") }

    ObserveSelectedLocations(navController, lifecycleOwner,
        { loc, latLng ->
            location = loc
            locationLatLng = latLng
        },
        { stopName, latLng ->
            stop = stopName
            stopLatLng = latLng
        },
        { dest, latLng ->
            destination = dest
            destinationLatLng = latLng
        },
    )

    Scaffold(
        topBar = { CreateRideTopBar(navController) },
        bottomBar = { BottomNavBar("create_ride", navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CreateRideDetailsCard(
                navController,
                context,
                date, { date = it },
                time, { time = it },
                location,
                stop,
                destination, { destination = it },
                capacity, { capacity = it },
            )

            Spacer(modifier = Modifier.height(20.dp))

            ConfirmRideButton(
                navController = navController,
                firestore = firestore,
                date = date,
                time = time,
                location = location,
                stop = stop,
                destination = destination,
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
    onStopSelected: (String, LatLng) -> Unit,
    onDestinationSelected: (String, LatLng) -> Unit
) {
    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_location")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                onLocationSelected(selectedAddress, selectedLatLng)
            }
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Pair<String, LatLng>>("selected_stop")
            ?.observe(lifecycleOwner) { (selectedAddress, selectedLatLng) ->
                onStopSelected(selectedAddress, selectedLatLng)
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD))
    )
}

@Composable
fun CreateRideDetailsCard(
    navController: NavController,
    context: Context,
    date: String, onDateChange: (String) -> Unit,
    time: String, onTimeChange: (String) -> Unit,
    location: String,
    stop: String,
    destination: String, onDestinationChange: (String) -> Unit,
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
            LocationFields(navController, location, stop, destination, onDestinationChange)
            DateTimePicker(context, date, onDateChange, time, onTimeChange)
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
                IconButton(onClick = {
                    showDatePicker(context, onDateChange) {
                        onTimeChange("Now") // Reset time when date is changed
                    }
                }) {
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
                IconButton(onClick = { showTimePicker(context, date, onTimeChange) }) {
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
    stop: String?,
    destination: String,
    onDestinationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Current Location Field
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

        // Stop Location Field
        Text("Add Stop(Optional)", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = stop ?: " ", // Show "Add Stop" if stop is null
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { navController.navigate("search_stop") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Select Stop Location")
                }
            }
        )

        // Destination Field
        Text("Select Destination", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = destination,
            onValueChange = onDestinationChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { navController.navigate("search_destination") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Select Destination")
                }
            }
        )
    }
}

@Composable
fun ConfirmRideButton(
    navController: NavController,
    firestore: FirebaseFirestore,
    date: String,
    time: String,
    location: String,
    stop: String,
    destination: String,
    capacity: Int,
    userId: String,
    onRideIdChange: (String) -> Unit
) {
    val context = LocalContext.current

    val isValidRide = remember(date, time, stop, location, destination) {
        date != "Select Date" &&
                time != "Now" &&
                location.isNotBlank() &&
                destination.isNotBlank()
    }
    Button(
        onClick = {
            if (location.trim() == destination.trim() || location.trim() == stop?.trim() || destination.trim() == stop?.trim()) {
                Toast.makeText(
                    context,
                    "Location, Stop, and Destination must be different!",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }
            if (!isValidRide) {
                Toast.makeText(
                    context,
                    "Please fill in all required fields before creating a ride.",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }
            CoroutineScope(Dispatchers.Main).launch {
                val canSearch = withContext(Dispatchers.IO) {
                    canUserCreateAgainInOneHour(firestore, userId, date, time)
                }

                if (!canSearch) {
                    Toast.makeText(
                        context,
                        "You can only create one ride every hour.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch // Stops here, doesn't continue
                }
                val rideRef = firestore.collection("rides").document() // Auto-generate ride ID
                val rideId = rideRef.id

                // Initialize passengerId list with "null" based on capacity
                val passengerIds = List(capacity) { "null" }
                // Initialize rideStatus to "process"
                val rideStatus = "process"

                val rideData = mapOf(
                    "rideId" to rideId,
                    "driverId" to userId, // Store userId as driverId
                    "date" to date,
                    "time" to time,
                    "location" to location,
                    "stop" to stop,
                    "destination" to destination,
                    "capacity" to capacity,
                    "passengerIds" to passengerIds, // Store passenger IDs
                    "rideStatus" to rideStatus,
                    "timestamp" to FieldValue.serverTimestamp() // Add server timestamp
                )

                rideRef.set(rideData).addOnSuccessListener {
                    onRideIdChange(rideId) // Update rideId in UI state
                    navController.navigate("home") // Navigate to MatchingScreen
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Confirm Ride", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private suspend fun canUserCreateAgainInOneHour(
    firestore: FirebaseFirestore,
    userId: String,
    currentDate: String,
    currentTime: String
): Boolean {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val nowTime = sdf.parse(currentTime)

    val snapshot = firestore.collection("rides")
        .whereEqualTo("driverId", userId)
        .whereEqualTo("date", currentDate)
        .get()
        .await()

    for (document in snapshot.documents) {
        val timeStr = document.getString("time") ?: continue
        val pastTime = sdf.parse(timeStr) ?: continue

        val diffInMillis = nowTime.time - pastTime.time
        val diffInMinutes = diffInMillis / (60 * 1000)

        if (diffInMinutes in -59..59) {
            return false // Less than 1 hour apart
        }
    }
    return true
}

@Composable
fun CapacitySelector(
    capacity: Int,
    onCapacityChanged: (Int) -> Unit
) {
    var maxCapacity by remember { mutableStateOf(3) }

    // Fetch max capacity based on vehicle info
    fetchUserVehicleMaxCapacity { max ->
        maxCapacity = max
        if (capacity > max) onCapacityChanged(max)
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

@Composable
fun fetchUserVehicleMaxCapacity(
    firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    onResult: (Int) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val userId = firebaseAuth.currentUser?.uid ?: return

    LaunchedEffect(Unit) {
        db.collection("Vehicle")
            .whereEqualTo("UserID",userId)
            .get()
            .addOnSuccessListener { document ->
                val document = document.documents.firstOrNull()
                val carType = document?.getString("CarType") ?: "5-Seater"
                val maxCapacity = if (carType == "7-Seater") 5 else 3
                onResult(maxCapacity)
            }
            .addOnFailureListener {
                // fallback if failed
                onResult(3)
            }
    }
}

fun showTimePicker(context: Context, selectedDate: String, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    // Get today's date for comparison
    val todayDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
            }

            // If the selected date is today, only allow future times
            if (selectedDate == todayDate && selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(context, "Please select a future time.", Toast.LENGTH_SHORT).show()
            } else {
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            }
        },
        if (selectedDate == todayDate) currentHour else 0, // Default hour (0 for future dates)
        if (selectedDate == todayDate) currentMinute else 0, // Default minute (0 for future dates)
        false
    )

    timePickerDialog.show()
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit, onTimeReset: () -> Unit) {
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

            val formattedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
            onDateSelected(formattedDate)

            // Reset time when date is changed
            onTimeReset()
        },
        year,
        month,
        day
    )
    // Set minimum date to today (restrict past dates)
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()
    datePickerDialog.show()
}
