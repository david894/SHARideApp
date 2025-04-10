@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kxxr.logiclibrary.Driver.Driver
import com.kxxr.logiclibrary.Driver.Vehicle
import com.kxxr.logiclibrary.Driver.searchDriver
import com.kxxr.logiclibrary.Driver.searchVehicle
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.logiclibrary.eWallet.PinAttemptManager
import com.kxxr.logiclibrary.eWallet.paymentAPI
import com.kxxr.logiclibrary.eWallet.verifyCurrentPin
import com.kxxr.sharide.R
import com.kxxr.sharide.db.RideDetail
import kotlinx.coroutines.launch

@Composable
fun SuccessfulRequestRideScreen(navController: NavController, index:Int, searchId: String) {
    val db = Firebase.firestore
    val context = LocalContext.current
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    val rideState by produceState<RideDetail?>(initialValue = null, searchId) {
        db.collection("requests")
            .whereEqualTo("searchId", searchId)
            .limit(1)
            .get()
            .addOnSuccessListener { requestDocs ->
                val rideId = requestDocs.documents.firstOrNull()?.getString("rideId") ?: return@addOnSuccessListener

                db.collection("rides").document(rideId).get()
                    .addOnSuccessListener { document ->
                        value = document.toObject(RideDetail::class.java)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error fetching ride", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching request", exception)
            }
    }
    var isOnBoarding by remember { mutableStateOf(false) }
    // Check if any request has "onBoarding" status
    LaunchedEffect(searchId) {
        db.collection("requests")
            .whereEqualTo("searchId", searchId)
            .whereEqualTo("status", "onBoarding")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    isOnBoarding = true
                }
            }
    }
    var isStartBoarding by remember { mutableStateOf(false) }
    // Check if any request has "startBoarding" status
    LaunchedEffect(searchId) {
        db.collection("requests")
            .whereEqualTo("searchId", searchId)
            .whereEqualTo("status", "startBoarding")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    isStartBoarding = true
                }
            }
    }
    Scaffold(
        topBar = { SuccessfulSearchTopBar(navController, index) } // Change to search index
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

                        RideTimeSection(ride.time,ride.driverId,navController)

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        EmergencyCallButton()
                        Spacer(modifier = Modifier.height(16.dp))
                        Column {
                            // 🚗 **Driver Section**
                            Text(
                                text = "Driver",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            DriverSection(driverId = ride.driverId, navController = navController)

                            Spacer(modifier = Modifier.height(16.dp))

                            // 👥 **Ride Participants Section**
                            Text(
                                text = "Ride Participants",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            RideParticipantsSection(rideId = ride.rideId, navController = navController)

                            Spacer(modifier = Modifier.height(16.dp))
                            val timeLeftMillis = convertToTimestamp(ride.date, ride.time) - System.currentTimeMillis()
                            val oneHourMillis = 60 * 60 * 1000

                            // 🚀 If "Start Boarding", show Complete Button
                            if (isOnBoarding) {
                                CompleteButton(
                                    firestore = db,
                                    navController = navController,
                                    rideId = ride.rideId,
                                    driverId = ride.driverId,
                                    searchId = searchId,
                                    context = context
                                )
                            } else if (timeLeftMillis <= oneHourMillis && isStartBoarding) {
                                BoardRideButton(
                                    firestore = db,
                                    navController = navController,
                                    searchId = searchId,
                                    context = context
                                )
                            } else {
                                CancelSuccessfulRideButton(
                                    firestore = db,
                                    navController = navController,
                                    rideId = ride.rideId,
                                    currentUserId = currentUserId,
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

@Composable
fun EmergencyCallButton() {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tarumt_phone = stringResource(id = R.string.tarumt_phone)
    val polis_phone = stringResource(id = R.string.polis_phone)
    val ambulance_phone = stringResource(id = R.string.ambulance_phone)
    val fire_phone = stringResource(id = R.string.fire_phone)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)) {
            Text("Emergency Call")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Emergency Services") },
                text = { Text("Who would you like to call?") },
                confirmButton = {
                    Column {
                        Button(
                            onClick = {
                                showDialog = false
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tarumt_phone"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ContextCompat.startActivity(context, intent, null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
                        ) {
                            Text("Call TAR UMT Security")
                        }

                        Button(
                            onClick = {
                                showDialog = false
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$polis_phone"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ContextCompat.startActivity(context, intent, null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
                        ) {
                            Text("Call Police Station")
                        }

                        Button(
                            onClick = {
                                showDialog = false
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ambulance_phone"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ContextCompat.startActivity(context, intent, null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
                        ) {
                            Text("Call Ambulance")
                        }

                        Button(
                            onClick = {
                                showDialog = false
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$fire_phone"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ContextCompat.startActivity(context, intent, null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
                        ) {
                            Text("Call Fire Fighting Station")
                        }
                        Button(
                            onClick = { showDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)

                        ) {
                            Text("Cancel")
                        }
                    }
                },
                dismissButton = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessfulSearchTopBar(navController: NavController, index: Int) {
    TopAppBar(
        title = { Text("Search $index", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun DriverSection(driverId: String, navController: NavController) {
    val db = Firebase.firestore
    var userName by remember { mutableStateOf("Unknown") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }
    var driverDetails by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var vehicleDetails by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val amount = stringResource(id = R.string.fare_amount).toInt()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch driver info from Firestore
    LaunchedEffect(driverId) {
        db.collection("users")
            .whereEqualTo("firebaseUserId", driverId) // Compare with firebaseUserId
            .limit(1)
            .get()
            .addOnSuccessListener { userDocs ->
                val userDoc = userDocs.documents.firstOrNull()
                userName = userDoc?.getString("name") ?: "Unknown"
                profileImageUrl = userDoc?.getString("profileImageUrl") ?: ""
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMessage = "Failed to load driver info"
                Log.e("Firestore", "Error fetching driver info", exception)
                isLoading = false
            }
        loadRatingScore(db, driverId) { Rating,TotalRating ->
            rating = Rating
            totalRating = TotalRating
        }
        // Fetch vehicle info
        searchDriver(firestore, driverId, context, onResult = { drivers ->
            if(drivers.isNotEmpty()){
                driverDetails = drivers
                searchVehicle(firestore,driverDetails.first().vehiclePlate,context, onResult = { vehicles ->
                    vehicleDetails = vehicles
                })
            }
        })

    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0075FD))
                .padding(16.dp),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                errorMessage != null -> {
                    Text(text = errorMessage ?: "Error", color = Color.Red, fontSize = 16.sp)
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Driver Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Default Driver",
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(26.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    userName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "⭐ $rating/5.0 ($totalRating)",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                    }

                }
            }
        }
        vehicleDetails.forEach { vehicle ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.width(26.dp))

                    Image(
                        painter = painterResource(id = R.drawable.car_front),
                        contentDescription = "Car",
                        modifier = Modifier.size(60.dp)
                    )

                    Spacer(modifier = Modifier.width(26.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "${vehicle.CarRegistrationNumber}",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(8.dp)
                        )
                        Text(
                            "${vehicle.CarMake} ${vehicle.CarModel} (${vehicle.CarColour})",
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ){
            Button(
                onClick = {
                    startChatWithDriver(driverId, navController)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FD), contentColor = Color.White)
            ) {
                Text("Chat")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

    }
}

@Composable
fun RideParticipantsSection(rideId: String, navController: NavController) {
    val db = Firebase.firestore
    var participants by remember { mutableStateOf<List<SuccessfulUserInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rideId) {
        db.collection("rides").document(rideId).get()
            .addOnSuccessListener { document ->
                val passengerIds = document.get("passengerIds") as? List<String> ?: emptyList()
                if (passengerIds.isEmpty()) {
                    isLoading = false
                    return@addOnSuccessListener
                }

                val usersList = mutableListOf<SuccessfulUserInfo>()

                passengerIds.forEach { passengerId ->
                    db.collection("users")
                        .whereEqualTo("firebaseUserId", passengerId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { userDocs ->
                            val userDoc = userDocs.documents.firstOrNull()
                            if (userDoc != null) {
                                val user = SuccessfulUserInfo(
                                    firebaseUserId = userDoc.getString("firebaseUserId") ?: "",
                                    name = userDoc.getString("name") ?: "Unknown",
                                    imageUrl = userDoc.getString("profileImageUrl") ?: "",
                                    rating = userDoc.getDouble("rating") ?: 4.5
                                )
                                usersList.add(user)
                                participants = usersList.toList() // Update state
                            }
                        }
                        .addOnFailureListener { exception ->
                            errorMessage = "Error fetching participant data"
                            Log.e("Firestore", "Error fetching passenger", exception)
                        }
                }
                isLoading = false
            }
            .addOnFailureListener { exception ->
                errorMessage = "Error fetching ride details"
                Log.e("Firestore", "Error fetching ride", exception)
                isLoading = false
            }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            errorMessage != null -> Text(text = errorMessage ?: "Error", color = Color.Red, fontSize = 16.sp)
            participants.isEmpty() -> Text("No participants yet.", fontSize = 16.sp, color = Color.Gray)
            else -> {
                participants.forEachIndexed { index,user ->
                    var rating by remember { mutableStateOf(0.0) }
                    var totalRating by remember { mutableStateOf(0) }
                    loadRatingScore(db, user.firebaseUserId) { Rating, TotalRating ->
                        rating = Rating
                        totalRating = TotalRating
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD))
                    ) {
                        Text("Passanger ${index+1}",
                            modifier = Modifier.padding(start = 18.dp, top = 10.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

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
                                // Profile Image or Default Icon
                                if (user.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = user.imageUrl,
                                        contentDescription = "Passenger Image",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Default Passenger",
                                        tint = Color.White,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(26.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        user.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "⭐ $rating/5.0 ($totalRating)",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun CancelSuccessfulRideButton(
    firestore: FirebaseFirestore,
    navController: NavController,
    rideId: String,
    currentUserId: String, // Pass the current user ID
    context: android.content.Context
) {
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            coroutineScope.launch {
                cancelRide(firestore, rideId, currentUserId, context, navController)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("Cancel Ride", color = Color.White)
    }
}

private fun cancelRide(
    firestore: FirebaseFirestore,
    rideId: String,
    currentUserId: String,
    context: android.content.Context,
    navController: NavController
) {
    firestore.collection("rides").document(rideId).get()
        .addOnSuccessListener { document ->
            val passengerIds = document.get("passengerIds") as? MutableList<String> ?: mutableListOf()

            if (passengerIds.contains(currentUserId)) {
                val updatedPassengerIds = passengerIds.map { if (it == currentUserId) "null" else it }

                firestore.collection("rides").document(rideId)
                    .update("passengerIds", updatedPassengerIds)
                    .addOnSuccessListener {
                        firestore.collection("requests")
                            .whereEqualTo("rideId", rideId)
                            .whereEqualTo("passengerId", currentUserId)
                            .get()
                            .addOnSuccessListener { requestDocs ->
                                val batch = firestore.batch()

                                for (doc in requestDocs.documents) {
                                    batch.delete(doc.reference) // 👈 directly delete the request
                                }

                                batch.commit()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Ride canceled successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to remove request", Toast.LENGTH_SHORT).show()
                                        Log.e("Firestore", "Error removing request", e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to fetch request", Toast.LENGTH_SHORT).show()
                                Log.e("Firestore", "Error fetching request", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update ride", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore", "Error updating ride", e)
                    }
            } else {
                Toast.makeText(context, "You are not part of this ride", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to fetch ride details", Toast.LENGTH_SHORT).show()
            Log.e("Firestore", "Error fetching ride", e)
        }
}

@Composable
fun BoardRideButton(
    firestore: FirebaseFirestore,
    navController: NavController,
    searchId: String,  // Change from searchId to rideId
    context: Context
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // Read max attempts from strings.xml
    val maxAttempts = stringResource(id = R.string.max_pin_attempts).toInt()
    val amount = stringResource(id = R.string.fare_amount).toInt()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableStateOf(3 - PinAttemptManager.getAttempts(context)) }
    var clearPinTrigger by remember { mutableStateOf(false) } // To trigger PIN reset
    var driverId by remember { mutableStateOf("") }
    var documentId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (PinAttemptManager.isLockedOut(context)) {
            navController.navigate("resetPIN") // Redirect if already locked out
        }
    }

    Button(
        onClick = {
            // Find the document where rideId matches
            firestore.collection("requests")
                .whereEqualTo("searchId", searchId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        documentId = querySnapshot.documents[0].id // Get the correct document ID
                        driverId = querySnapshot.documents[0].getString("driverId") ?: ""
                        showPinDialog = true

                    } else {
                        Toast.makeText(context, "No matching ride found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error finding ride: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800080))
    ) {
        Text("Get On Ride")
    }
    if (showPinDialog) {
        PinInputDialog(
            title = "Enter Payment PIN",
            description = buildString {
                append("Please enter your current payment PIN to make payment.")
                if (attemptsLeft < 3) {
                    append("\n\nYou have $attemptsLeft attempts left.")
                }
            },
            onPinEntered = { enteredPin ->
                verifyCurrentPin(
                    context = context,
                    userId = userId,
                    enteredPin = enteredPin,
                    onSuccess = {
                        PinAttemptManager.resetAttempts(context) // Reset attempt count
                        showPinDialog = false
                        if(driverId != "" && documentId != ""){
                            paymentAPI(userId,driverId,amount.toDouble(), onSuccess = { result, amount ->
                                Toast.makeText(context, "Payment Successful", Toast.LENGTH_SHORT).show()
                                updateRideStatus(firestore, documentId, context, navController)
                            }, onFailure = {
                                Toast.makeText(context, "Failed to make payment to driver", Toast.LENGTH_SHORT).show()
                            })
                        }
                    },
                    onFailure = { error ->
                        attemptsLeft = maxAttempts - PinAttemptManager.getAttempts(context) // Update attempts left
                        errorMessage = error
                        clearPinTrigger = !clearPinTrigger
                    },
                    onLockout = {
                        navController.navigate("resetPIN") // Navigate to reset PIN screen
                    }
                )
            },
            onClearPin = clearPinTrigger,
            onDismiss = { navController.popBackStack() }
        )
    }

    errorMessage?.let { error ->
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        errorMessage = null
    }
}

fun updateRideStatus(firestore: FirebaseFirestore, documentId: String, context: Context, navController: NavController) {
    // Perform batched updates
    firestore.runBatch { batch ->
        val requestRef = firestore.collection("requests").document(documentId)
        batch.update(requestRef, "boardingStatus", "yes")
        batch.update(requestRef, "status", "onBoarding") // Update request status
    }.addOnSuccessListener {
        Toast.makeText(context, "You have boarded the ride!", Toast.LENGTH_SHORT).show()
        navController.navigate("home") // Navigate to home after confirming
    }.addOnFailureListener {
        Toast.makeText(context, "Failed to update boarding status", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CompleteButton(firestore: FirebaseFirestore, navController: NavController, rideId: String, driverId: String, searchId: String, context: Context) {
    Button(
        onClick = {
            firestore.collection("requests")
                .whereEqualTo("searchId", searchId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        firestore.collection("requests").document(document.id)
                            .update("status", "complete")
                            .addOnSuccessListener {
                                Toast.makeText(context, "Ride completed!", Toast.LENGTH_SHORT).show()
                                navController.navigate("rate_ride/${driverId}/${rideId}")
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Complete Ride", color = Color.White)
    }
}

fun startChatWithDriver(
    driverId: String,
    navController: NavController
) {
    val db = FirebaseFirestore.getInstance()
    val chatsRef = db.collection("chats")
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


    chatsRef
        .whereEqualTo("driverId", driverId)
        .whereEqualTo("passengerId", currentUserId)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                // Chat already exists
                val chatId = documents.documents[0].id
                navController.navigate("chat_screen/$chatId")
            } else {
                // Create new chat
                val newChat = hashMapOf(
                    "driverId" to driverId,
                    "passengerId" to currentUserId,
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



data class SuccessfulUserInfo(
    val firebaseUserId: String = "",
    val name: String = "Unknown",
    val imageUrl: String = "",
    val rating: Double = 4.5,
    )

