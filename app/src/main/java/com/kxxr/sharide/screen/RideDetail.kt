@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.kxxr.sharide.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun RideDetailScreen(navController: NavController, firestore: FirebaseFirestore,index: Int, rideId: String) {
    val db = Firebase.firestore
    val context = LocalContext.current

    val rideState by produceState<RideDetail?>(initialValue = null, rideId) {
        db.collection("rides").document(rideId).get()
            .addOnSuccessListener { document ->
                value = document.toObject(RideDetail::class.java)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching ride", exception)
            }
    }

    Scaffold(
        topBar = { RideDetailTopBar(navController, index) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when {
                rideState == null -> {
                    Text("Loading ride details...", fontSize = 16.sp, color = Color.Gray)
                }
                rideState != null -> {
                    rideState?.let { ride ->
                        val pickupLatLng = getLatLngFromName(ride.location)
                        val stopLatLng = getLatLngFromName(ride.stop)
                        val destinationLatLng = getLatLngFromName(ride.destination)

                        if (pickupLatLng != null && destinationLatLng != null) {
                            RideMap(
                                context = LocalContext.current,
                                pickup = pickupLatLng,
                                stop = stopLatLng,
                                destination = destinationLatLng
                            )
                        } else {
                            Text("Error: Invalid location data", color = Color.Red)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        RouteDetails(pickupLocation = ride.location, stop = ride.stop, destination = ride.destination)

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        RideTimeSection(ride.time)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Column {
                            // Ride Passenger Section
                            Text(
                                text = "Ride Passenger",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            RidePassengerListSection(rideId = rideId, rideTime = ride.time)

                            Spacer(modifier = Modifier.height(16.dp))

                            // ✅ Remove "null" values from passenger list
                            val currentPassengers = ride.passengerIds?.filter { it != "null" } ?: emptyList()
                            val rideCapacity = ride.capacity ?: 0

                            if (currentPassengers.size < rideCapacity) {
                                Text(
                                    text = "Pending Passenger",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )


                            PendingPassengerListSection(rideId = rideId, rideTime = ride.time, navController = navController)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            // ✅ Calculate time left until the ride starts
                            val timeLeftMillis = convertToTimestamp(ride.date, ride.time) - System.currentTimeMillis()
                            val oneHourMillis = 60 * 60 * 1000

                           if (timeLeftMillis <= oneHourMillis) {
                               ConfirmRideButton(context = context, firestore = firestore,rideId = rideId,pickupLatLng, stopLatLng,destinationLatLng)
                           } else {
                              CancelRideButton(firestore = db, navController = navController, rideId = rideId, context = context)
                           }
                        }
                    }
                }
                else -> {
                    Text("Error loading ride details. Please try again.", color = Color.Red)
                }
            }
        }
    }
}


fun getLatLngFromName(locationName: String?): LatLng? {
    return predefinedLocations.find { it.first == locationName }?.second
}


@Composable
fun RideMap(
    context: Context,
    pickup: LatLng,
    stop: LatLng?,
    destination: LatLng
) {
    val mapState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickup, 14f)
    }

    var route by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Fetch route when map loads
    LaunchedEffect(pickup, stop, destination) {
        val result = getRoute(context, pickup, stop, destination)
        if (result != null) {
            route = result

            // Adjust camera to show the entire route
            val boundsBuilder = LatLngBounds.Builder()
            result.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()

            mapState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    GoogleMap(
        modifier = Modifier
            .height(350.dp)  // Reduce map height
            .fillMaxWidth(),
        cameraPositionState = mapState
    ) {
        // Pickup Marker
        Marker(
            state = MarkerState(position = pickup),
            title = "Pickup",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        )

        // Stop Marker (if exists)
        stop?.let {
            Marker(
                state = MarkerState(position = it),
                title = "Stop",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            )
        }

        // Destination Marker
        Marker(
            state = MarkerState(position = destination),
            title = "Destination",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        // Draw Route (RED & THICKER)
        if (route.isNotEmpty()) {
            Polyline(
                points = route,
                color = Color.Red,  // Change to red
                width = 12f          // Make thicker
            )
        }
    }
}


suspend fun getRoute(
    context: Context,
    origin: LatLng,
    stop: LatLng?,
    destination: LatLng
): List<LatLng>? {
    val apiKey = context.getString(R.string.maps_api_key)

    // Correctly format waypoints
    val waypoints = stop?.let { "&waypoints=${it.latitude},${it.longitude}" } ?: ""

    val url = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "$waypoints&key=$apiKey"

    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("DirectionsAPI", "API Response: $response") // Debug log

            val json = JSONObject(response)

            if (json.has("error_message")) {
                Log.e("DirectionsAPI", "Error: ${json.getString("error_message")}")
                return@withContext null
            }

            val status = json.getString("status")
            if (status != "OK") {
                Log.e("DirectionsAPI", "Directions API Status: $status")
                return@withContext null
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.e("DirectionsAPI", "No routes found")
                return@withContext null
            }

            val polyline = routes.getJSONObject(0)
                .getJSONObject("overview_polyline")
                .getString("points")

            return@withContext decodePolyline(polyline)
        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Error fetching route", e)
        }
        null
    }
}

fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1F shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1F shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
    }
    return poly
}

@Composable
fun RouteDetails(pickupLocation: String, stop: String?, destination: String) {
    Row {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(24.dp))
            if (stop != null) {
                Box(modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(Color.Blue))
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(24.dp))
            }
            Box(modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(Color.Blue))
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(pickupLocation, fontSize = 16.sp)
            stop?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Stop: $it", fontSize = 16.sp, color = Color.Yellow)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(destination, fontSize = 16.sp)
        }
    }
}

// **Update TopBar to Show Ride Index**
@Composable
fun RideDetailTopBar(navController: NavController, index: Int) {
    TopAppBar(
        title = { Text("Ride $index", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}


// **Update RideTimeSection to Show Firebase Time**
@Composable
fun RideTimeSection(time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.Blue)
        Spacer(modifier = Modifier.width(4.dp))
        Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RidePassengerListSection(rideId: String,rideTime: String) {
    val db = Firebase.firestore
    val passengerListState = produceState<List<Passenger>>(initialValue = emptyList(), rideId) {
        val passengers = mutableListOf<Passenger>()

        db.collection("rides").document(rideId).get()
            .addOnSuccessListener { rideSnapshot ->
                val passengerIds = rideSnapshot.get("passengerIds") as? List<String> ?: emptyList()

                if (passengerIds.isEmpty()) {
                    value = emptyList() // No confirmed passengers
                    return@addOnSuccessListener
                }

                passengerIds.forEach { passengerId ->
                    db.collection("users")
                        .whereEqualTo("firebaseUserId", passengerId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            userSnapshot.documents.firstOrNull()?.let { userDoc ->
                                val name = userDoc.getString("name") ?: "Unknown"
                                val imageUrl = userDoc.getString("profileImageUrl") ?: ""
                                val passengerId = userDoc.getString("firebaseUserId") ?: ""
                                passengers.add(Passenger(passengerId, name, 4.5, 2, imageUrl, rideTime))

                                // Update state only when all data is fetched
                                value = passengers
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching ride passengers", exception)
            }
    }

    Column {
        if (passengerListState.value.isEmpty()) {
            Text("No confirmed passengers", fontSize = 16.sp, color = Color.Gray)
        } else {
            passengerListState.value.forEach { passenger ->
                ConfirmPassengerCard(passenger)
            }
        }
    }
}

@Composable
fun PendingPassengerListSection(rideId: String, rideTime: String, navController: NavController) {
    val db = Firebase.firestore
    val passengerListState = produceState<List<Passenger>>(initialValue = emptyList(), rideId) {
        val passengers = mutableListOf<Passenger>()

        db.collection("requests")
            .whereEqualTo("rideId", rideId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { requestSnapshot ->
                val passengerIds = requestSnapshot.documents.mapNotNull { it.getString("passengerId") }

                if (passengerIds.isEmpty()) {
                    Log.d("Firebase", "No pending passengers for rideId: $rideId")
                    value = emptyList()
                    return@addOnSuccessListener
                }

                val remainingFetches = AtomicInteger(passengerIds.size)

                passengerIds.forEach { passengerId ->
                    db.collection("users")
                        .whereEqualTo("firebaseUserId", passengerId) // Match `firebaseUserId`
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            userSnapshot.documents.firstOrNull()?.let { userDoc ->
                                val name = userDoc.getString("name") ?: "Unknown"
                                val imageUrl = userDoc.getString("profileImageUrl") ?: ""
                                val passengerId = userDoc.getString("firebaseUserId") ?: ""
                                passengers.add(Passenger(passengerId, name, 4.5, 2, imageUrl, rideTime))
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error fetching user details for $passengerId", e)
                        }
                        .addOnCompleteListener {
                            if (remainingFetches.decrementAndGet() == 0) {
                                Log.d("Firebase", "All passengers fetched: $passengers")
                                value = passengers.toList()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error fetching requests for rideId: $rideId", e)
                value = emptyList()
            }
    }

    Column {
        if (passengerListState.value.isEmpty()) {
            Text("No pending passengers", fontSize = 16.sp, color = Color.Gray)
        } else {
            passengerListState.value.forEach { passenger ->
                PendingPassengerCard(passenger, rideId, navController) // Pass rideId
            }
        }
    }
}

@Composable
fun ConfirmRideButton(
    context: Context,
    firestore: FirebaseFirestore,
    rideId: String,
    pickup: LatLng?,
    stop: LatLng?,
    destination: LatLng?
) {
    Button(
        onClick = {
            if (pickup != null && destination != null) {
                // First, find the document where rideId matches
                firestore.collection("requests")
                    .whereEqualTo("rideId", rideId) // Find document with matching rideId field
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val documentId = querySnapshot.documents[0].id // Get the document ID
                            Log.d("FirestoreDebug", "Found document ID: $documentId")

                            // Now update the correct document
                            firestore.collection("requests").document(documentId)
                                .update("status", "startBoarding")
                                .addOnSuccessListener {
                                    Log.d("FirestoreDebug", "Successfully updated status")
                                    openGoogleMapsNavigation(context, pickup, stop, destination)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirestoreError", "Failed to update ride status: ${e.message}")
                                    Toast.makeText(context, "Failed to update ride status", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "No matching ride found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Failed to find ride: ${e.message}")
                        Toast.makeText(context, "Error finding ride: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Invalid location data", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
    ) {
        Text("Confirm Ride & Start Navigation")
    }
}



// Cancel Ride Button
@Composable
fun CancelRideButton(firestore: FirebaseFirestore, navController: NavController, rideId: String, context: Context) {
    Button(
        onClick = {
            cancelRide(firestore, navController, rideId, context)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
    ) {
        Text("Cancel Ride", color = Color.White)
    }
}

// Function to update the "requests" status to "cancel"
fun cancelRide(firestore: FirebaseFirestore, navController: NavController , rideId: String, context: Context) {
    firestore.collection("requests")
        .whereEqualTo("rideId", rideId)
        .get()
        .addOnSuccessListener { documents ->
            val batch = firestore.batch()
            for (document in documents) {
                val docRef = firestore.collection("requests").document(document.id)
                batch.update(docRef, "status", "cancel")
            }
            // Delete the ride from "rides" collection
            val rideRef = firestore.collection("rides").document(rideId)
            batch.delete(rideRef)

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(context, "Ride canceled successfully!", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") { // 👈 Navigate to Home Page
                        popUpTo("home") { inclusive = true } // 👈 Clear back stack
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to cancel ride.", Toast.LENGTH_SHORT).show()
                }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error fetching ride requests.", Toast.LENGTH_SHORT).show()
        }
}



@Composable
fun PendingPassengerCard(passenger: Passenger, rideId: String, navController: NavController) { // Receive rideId
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = passenger.imageRes.ifEmpty { R.drawable.profile_ico },
                contentDescription = "Passenger Image",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = passenger.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${passenger.rating} (${passenger.reviews})", fontSize = 14.sp, color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("RM 1", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(passenger.time, fontSize = 14.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Button(
                        onClick = {
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RectangleShape
                    ) {
                        Text("Accept", color = Color.White)
                    }

                    Button(
                        onClick = {
                            rejectPassenger(passenger.passengerId, rideId){
                                navController.popBackStack() // Navigate back after accepting
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RectangleShape
                    ) {
                        Text("Reject", color = Color.White)
                    }
                }
            }
        }
    }
    // Show confirmation dialog
    if (showDialog) {
        AcceptConfirmationDialog(
            passenger = passenger,
            rideId = rideId,
            onDismiss = { showDialog = false },
            onConfirm = {
                acceptPassenger(passenger.passengerId, rideId) {
                    navController.popBackStack() // Navigate back after accepting
                }
                showDialog = false
            }
        )
    }
}


fun acceptPassenger(passengerId: String, rideId: String, onSuccess: () -> Unit) {
    val db = Firebase.firestore
    val rideRef = db.collection("rides").document(rideId)
    val requestQuery = db.collection("requests")
        .whereEqualTo("rideId", rideId)
        .whereEqualTo("passengerId", passengerId)

    db.runTransaction { transaction ->
        val rideSnapshot = transaction.get(rideRef)
        val passengerIds = rideSnapshot.get("passengerIds") as? MutableList<String> ?: mutableListOf()
        val capacity = rideSnapshot.getLong("capacity")?.toInt() ?: 0

        passengerIds.remove("null")

        if (passengerIds.size < capacity) {
            passengerIds.add(passengerId)
            transaction.update(rideRef, "passengerIds", passengerIds)
        } else {
            return@runTransaction
        }
    }.addOnSuccessListener {
        requestQuery.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                db.collection("requests").document(document.id)
                    .update("status", "successful")
            }
            onSuccess() // This will trigger navigation after success
        }
    }
}


@Composable
fun AcceptConfirmationDialog(
    passenger: Passenger,
    rideId: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Acceptance") },
        text = { Text("Are you sure you want to accept ${passenger.name} for this ride?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}


fun rejectPassenger(passengerId: String, rideId: String, onSuccess: () -> Unit) {
    val db = Firebase.firestore
    val requestQuery = db.collection("requests")
        .whereEqualTo("rideId", rideId)
        .whereEqualTo("passengerId", passengerId)

    requestQuery.get().addOnSuccessListener { querySnapshot ->
        for (document in querySnapshot.documents) {
            db.collection("requests").document(document.id)
                .update("status", "rejected")
        }
        onSuccess() // Navigate back after rejecting
    }
}




@Composable
fun ConfirmPassengerCard(passenger: Passenger) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RectangleShape, // Ensures the card has sharp corners
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD)), // Blue background
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Icon at the Top
            AsyncImage(
                model = passenger.imageRes.ifEmpty { R.drawable.profile_ico },
                contentDescription = "Passenger Image",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Passenger Name
            Text(
                text = passenger.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Rating Row (Star Icon + Rating Text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${passenger.rating} (${passenger.reviews})", fontSize = 14.sp, color = Color.White)
            }

            // Price Row (Money Icon + Price)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("RM 1", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Time Row (Clock Icon + Time)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text(passenger.time, fontSize = 14.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Button (Filling the entire bottom of the card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Ensures button fully occupies the bottom space
            ) {
                Button(
                    onClick = { /* Handle view */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    modifier = Modifier
                        .fillMaxSize(), // Ensures button takes full space
                    shape = RectangleShape // Makes sure button is rectangular
                ) {
                    Text("View", color = Color.White)
                }
            }
        }
    }
}





data class Passenger(
    val passengerId: String,
    val name: String,
    val rating: Double,
    val reviews: Int,
    val imageRes: String,
    val time: String
)





data class RideDetail(
    val rideId: String = "",
    val driverId: String = "",
    val location: String = "",
    val stop: String = "",
    val destination: String = "",
    val time: String = "",
    val passengerIds: List<String> = emptyList(),
    val capacity: Int = 0,
    val date: String = "",
)
