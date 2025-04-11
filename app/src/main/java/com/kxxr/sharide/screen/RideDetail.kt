@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.sharide.R
import com.kxxr.sharide.db.Passenger
import com.kxxr.sharide.db.RideDetail
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
    val status = remember { mutableStateOf(false) }

    LaunchedEffect(rideId) {
        firestore.collection("requests")
            .whereEqualTo("rideId", rideId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val hasBoardingStatus = querySnapshot.documents.any { doc ->
                    val statusValue = doc.getString("status")
                    statusValue == "startBoarding" || statusValue == "onBoarding"
                }
                status.value = hasBoardingStatus
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch request status: ${e.message}")
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
                        RideTimeSection(ride.time, ride.driverId ,navController)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        EmergencyCallButton()
                        Spacer(modifier = Modifier.height(16.dp))

                        Column {
                            // Ride Passenger Section
                            Text(
                                text = "Ride Passenger",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            RidePassengerListSection(rideId = rideId, rideTime = ride.time, driverId = ride.driverId, navController = navController)

                            Spacer(modifier = Modifier.height(16.dp))

                            //  Remove "null" values from passenger list
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
                            // Calculate time left until the ride starts
                            val timeLeftMillis = convertToTimestamp(ride.date, ride.time) - System.currentTimeMillis()
                            val oneHourMillis = 60 * 60 * 1000
                            val tenMinMillis = -30 * 60 * 1000
                            if (ride.rideStatus == "complete"){
                                // not display any button
                            }else if (status.value) {
                                CompleteRideButton(firestore = firestore, rideId = rideId, navController = navController,context = context)
                            }else if (timeLeftMillis in tenMinMillis..oneHourMillis) {
                                StartNavigationButton(
                                    context = context,
                                    firestore = firestore,
                                    rideId = rideId,
                                    pickup = pickupLatLng,
                                    stop = stopLatLng,
                                    destination = destinationLatLng
                                )
                            } else if (timeLeftMillis > oneHourMillis) {
                                CancelRideButton(
                                    firestore = db,
                                    navController = navController,
                                    rideId = rideId,
                                    context = context
                                )
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

        // Draw Route
        if (route.isNotEmpty()) {
            Polyline(
                points = route,
                color = Color.Red,
                width = 12f
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
    val baseUrl = context.getString(R.string.ride_detail_url)
    val url = baseUrl +
            "origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "$waypoints&key=$apiKey"

    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("DirectionsAPI", "API Response: $response")

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
                Text("$it", fontSize = 16.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(destination, fontSize = 16.sp)
        }
    }
}

// Update TopBar to Show Ride Index
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


// Update RideTimeSection to Show Firebase Time
@Composable
fun RideTimeSection(time: String,driverId: String, navController: NavController) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.Blue)
        Spacer(modifier = Modifier.width(4.dp))
        Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RidePassengerListSection(rideId: String,rideTime: String,driverId: String,navController: NavController) {
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

                                db.collection("searchs")
                                    .whereEqualTo("passengerId", passengerId)
                                    .get()
                                    .addOnSuccessListener { searchSnapshot ->
                                        searchSnapshot.documents.firstOrNull()?.let { searchDoc ->
                                            val location = searchDoc.getString("location") ?: "Unknown"
                                            val destination = searchDoc.getString("destination") ?: "Unknown"
                                            val rideTime = searchDoc.getString("time") ?: "Unknown"

                                            passengers.add(
                                                Passenger(
                                                    passengerId,
                                                    name,
                                                    4.5,
                                                    2,
                                                    imageUrl,
                                                    rideTime,
                                                    location,
                                                    destination
                                                )
                                            )

                                            // Update state here if needed
                                            value = passengers
                                        }
                                    }
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
                PassengerCard(passenger,"",driverId, navController,"Confirmed",passenger.location,passenger.destination )
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
                        .whereEqualTo("firebaseUserId", passengerId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            userSnapshot.documents.firstOrNull()?.let { userDoc ->
                                val name = userDoc.getString("name") ?: "Unknown"
                                val imageUrl = userDoc.getString("profileImageUrl") ?: ""
                                val passengerId = userDoc.getString("firebaseUserId") ?: ""

                                db.collection("searchs")
                                    .whereEqualTo("passengerId", passengerId)
                                    .get()
                                    .addOnSuccessListener { searchSnapshot ->
                                        searchSnapshot.documents.firstOrNull()?.let { searchDoc ->
                                            val location = searchDoc.getString("location") ?: "Unknown"
                                            val destination = searchDoc.getString("destination") ?: "Unknown"
                                            val rideTime = searchDoc.getString("time") ?: "Unknown"

                                            passengers.add(
                                                Passenger(
                                                    passengerId,
                                                    name,
                                                    4.5,
                                                    2,
                                                    imageUrl,
                                                    rideTime,
                                                    location,
                                                    destination
                                                )
                                            )

                                            // Update state here if needed
                                            value = passengers
                                        }
                                    }
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
                PassengerCard(passenger, rideId, "",navController,"Pending",passenger.location,passenger.destination) // Pass rideId
            }
        }
    }
}

@Composable
fun StartNavigationButton(
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
                firestore.collection("rides").document(rideId)
                    .get()
                    .addOnSuccessListener { rideSnapshot ->
                        if (rideSnapshot.exists()) {
                            val driverId = rideSnapshot.getString("driverId") ?: return@addOnSuccessListener

                            // Search for any requests associated with this ride
                            firestore.collection("requests")
                                .whereEqualTo("rideId", rideId) // Find requests with this rideId
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty) {
                                        for (document in querySnapshot.documents) {
                                            firestore.collection("requests")
                                                .document(document.id)
                                                .update("status", "startBoarding")
                                        }
                                        firestore.collection("rides").document(rideId)
                                            .get()
                                            .addOnSuccessListener { rideSnapshot ->

                                                val passengerIds = rideSnapshot.get("passengerIds") as? List<String> ?: emptyList()

                                                passengerIds.forEach { passengerId ->
                                                    startChatWithPassengerAndSendMessage(
                                                        driverId = driverId,
                                                        passengerId = passengerId,
                                                        message = "I start ride now. Please prepare."
                                                    )
                                                }

                                                openGoogleMapsNavigation(context, pickup, stop, destination)
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to fetch ride info", Toast.LENGTH_SHORT).show()
                                            }
                                    }else {
                                        // No passengers will Directly update the ride status to "complete"
                                        firestore.collection("rides").document(rideId)
                                            .update("rideStatus", "complete")
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "No passengers. Ride completed!", Toast.LENGTH_SHORT).show()
                                                openGoogleMapsNavigation(context, pickup, stop, destination)
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Failed to update ride status", Toast.LENGTH_SHORT).show()
                                                Log.e("FirestoreError", "Error updating ride status: ${e.message}")
                                            }
                                        val newRequest = hashMapOf(
                                            "rideId" to rideId,
                                            "driverId" to driverId,
                                            "status" to "startBoarding",
                                            "timestamp" to System.currentTimeMillis()
                                        )

                                        firestore.collection("requests")
                                            .add(newRequest)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "No passengers. Ride completed!", Toast.LENGTH_SHORT).show()
                                                openGoogleMapsNavigation(context, pickup, stop, destination)
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Failed to update ride status", Toast.LENGTH_SHORT).show()
                                                Log.e("FirestoreError", "Error updating ride status: ${e.message}")
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error finding ride requests: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("FirestoreError", "Error finding ride requests: ${e.message}")
                                }
                        } else {
                            Toast.makeText(context, "Ride not found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error retrieving ride data: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("FirestoreError", "Error retrieving ride data: ${e.message}")
                    }
            } else {
                Toast.makeText(context, "Invalid location data", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
    ) {
        Text("Start Navigation")
    }
}

fun startChatWithPassengerAndSendMessage(
    driverId: String,
    passengerId: String,
    message: String
) {
    val db = FirebaseFirestore.getInstance()
    val chatsRef = db.collection("chats")

    chatsRef
        .whereEqualTo("driverId", driverId)
        .whereEqualTo("passengerId", passengerId)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val chatId = documents.documents[0].id
                sendMessageToChat(chatId, driverId, message)
            } else {
                val newChat = hashMapOf(
                    "driverId" to driverId,
                    "passengerId" to passengerId,
                    "lastMessage" to message,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )

                chatsRef.add(newChat)
                    .addOnSuccessListener { docRef ->
                        sendMessageToChat(docRef.id, driverId, message)
                    }
            }
        }
}

fun sendMessageToChat(chatId: String, senderId: String, text: String) {
    val messageData = hashMapOf(
        "senderId" to senderId,
        "text" to text,
        "timestamp" to FieldValue.serverTimestamp()
    )

    FirebaseFirestore.getInstance()
        .collection("chats")
        .document(chatId)
        .collection("messages")
        .add(messageData)

    //  update lastMessage and lastMessageTimestamp
    FirebaseFirestore.getInstance()
        .collection("chats")
        .document(chatId)
        .update(
            mapOf(
                "lastMessage" to text,
                "lastMessageTimestamp" to FieldValue.serverTimestamp()
            )
        )
}




// Cancel Ride Button
@Composable
fun CancelRideButton(firestore: FirebaseFirestore, navController: NavController, rideId: String, context: Context) {
    Button(
        onClick = {
            cancelRide(firestore, navController, rideId, context)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("Cancel Ride", color = Color.White)
    }
}

fun cancelRide(firestore: FirebaseFirestore, navController: NavController, rideId: String, context: Context) {
    firestore.collection("requests")
        .whereEqualTo("rideId", rideId)
        .get()
        .addOnSuccessListener { documents ->
            val batch = firestore.batch()

            // Delete all related requests
            for (document in documents) {
                batch.update(document.reference, "status", "canceled") // Update status to "canceled"
            }

            // Delete the ride itself
            val rideRef = firestore.collection("rides").document(rideId)
            batch.delete(rideRef)

            // Commit batch deletion
            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(context, "Ride canceled successfully!", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
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
fun CompleteRideButton(
    firestore: FirebaseFirestore,
    rideId: String,
    navController: NavController,
    context: Context
) {
    Button(
        onClick = {
            completeRide(
                firestore = firestore,
                rideId = rideId,
                onSuccess = {
                    Toast.makeText(context, "Ride completed successfully", Toast.LENGTH_SHORT).show()
                    navController.navigate("rate_ride/${"Driver"}/${rideId}")
                },
                onFailure = {
                    Toast.makeText(context, "Failed to complete ride", Toast.LENGTH_SHORT).show()
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000))
    ) {
        Text("Complete Ride", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun completeRide(firestore: FirebaseFirestore, rideId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val rideRef = firestore.collection("rides").document(rideId)
    val requestRef = firestore.collection("request").document(rideId)
    rideRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                rideRef.update("rideStatus", "complete")
                    .addOnSuccessListener {
                        Log.d("Firestore", "Ride marked as complete.")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update ride status: ${e.message}")
                        onFailure(e)
                    }
            } else {
                Log.e("Firestore", "Ride not found!")
                onFailure(Exception("Ride not found"))
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error fetching ride: ${e.message}")
            onFailure(e)
        }

    rideRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                rideRef.update("status", "complete")
                    .addOnSuccessListener {
                        Log.d("Firestore", "Ride marked as complete.")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update ride status: ${e.message}")
                        onFailure(e)
                    }
            } else {
                Log.e("Firestore", "Ride not found!")
                onFailure(Exception("Ride not found"))
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error fetching ride: ${e.message}")
            onFailure(e)
        }
}



@Composable
fun PassengerCard(passenger: Passenger, rideId: String, driverId: String, navController: NavController, type: String, location:String, destination: String) {
    var showDialog by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }
    val amount = stringResource(id = R.string.fare_amount).toInt()

    loadRatingScore(firestore, passenger.passengerId) { Rating, TotalRating ->
        rating = Rating
        totalRating = TotalRating
    }


    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0075FD))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (passenger.imageRes.isNotEmpty()) {
                    AsyncImage(
                        model = passenger.imageRes,
                        contentDescription = "Passenger Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profile_ico),
                        contentDescription = "Default Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(26.dp))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        passenger.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "⭐ $rating/5.0 ($totalRating)",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Row{
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            passenger.time,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Icon",
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Icon",
                            tint = Color.Blue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = destination,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ){
                Text(
                    text = "RM $amount",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(BookmarkShape())
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp) // Internal padding
                )

            }
        }
        if(type == "Pending"){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008000)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RectangleShape
                        ) {
                            Text("Accept", color = Color.White)
                        }

                        Button(
                            onClick = {
                                rejectPassenger(passenger.passengerId, rideId) {
                                    navController.popBackStack() // Navigate back after accepting
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
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

        if(type == "Confirmed" && driverId.isNotEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Button(
                    onClick = {
                        startChatWithPassenger(
                            driverId = driverId,
                            passenger = passenger,
                            navController = navController
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Text("Chat", color = Color.White)
                }
            }
        }
    }
    // Show confirmation dialog
    if (showDialog && type == "Pending") {
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
                .delete()
        }
        onSuccess() // Navigate back after deletion
    }
}


fun startChatWithPassenger(
    driverId: String,
    passenger: Passenger,
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val chatsRef = db.collection("chats")

    chatsRef
        .whereEqualTo("driverId", driverId)
        .whereEqualTo("passengerId", passenger.passengerId)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                // Chat already exists, navigate
                val chatId = documents.documents[0].id
                navController.navigate("chat_screen/$chatId")
            } else {
                // Create new chat
                val newChat = hashMapOf(
                    "driverId" to driverId,
                    "passengerId" to passenger.passengerId,
                    "lastMessage" to "",
                    "lastMessageTimestamp" to null,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                chatsRef.add(newChat)
                    .addOnSuccessListener { docRef ->
                        navController.navigate("chat_screen/${docRef.id}")
                    }
            }
        }
}







