@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RideRequestScreen(firebaseAuth: FirebaseAuth, navController: NavController, driverId: String, rideId: String, searchId:String) {
    val firestore = FirebaseFirestore.getInstance()
    var driverList by remember { mutableStateOf<List<DriverInfo>>(emptyList()) }
    var rideDriverPairs by remember { mutableStateOf<List<Pair<RideInfo, DriverInfo>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(driverId, rideId) {
        isLoading = true

        // Convert driverId and rideId strings into lists
        val driverIds = driverId.split(",").map { it.trim() }
        val rideIds = rideId.split(",").map { it.trim() }

        if (driverIds.size != rideIds.size) {
            isLoading = false // Prevent mismatched data issues
            return@LaunchedEffect
        }

        val tempRideDriverPairs = mutableListOf<Pair<RideInfo, DriverInfo>>()

        // Fetch driver and ride details together
        rideIds.zip(driverIds).forEach { (rideId, driverId) ->
            val driverRef = firestore.collection("users").document(driverId)
            val rideRef = firestore.collection("rides").document(rideId)

            driverRef.get().addOnSuccessListener { driverDoc ->
                rideRef.get().addOnSuccessListener { rideDoc ->
                    val driver = DriverInfo(
                        driverId = driverDoc.id,
                        name = driverDoc.getString("name") ?: "Unknown",
                        imageUrl = driverDoc.getString("profileImageUrl") ?: "",
                        rating = driverDoc.getDouble("rating") ?: 4.5,
                        price = "RM 1"
                    )

                    val ride = RideInfo(
                        rideId = rideDoc.id,
                        pickupLocation = rideDoc.getString("pickupLocation") ?: "Unknown",
                        destination = rideDoc.getString("destination") ?: "Unknown",
                        time = rideDoc.getString("time") ?: "Unknown"
                    )

                    tempRideDriverPairs.add(ride to driver)
                    rideDriverPairs = tempRideDriverPairs.toList() // Ensure recomposition
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Driver", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Matched Drivers and Rides", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            when {
                isLoading -> CircularProgressIndicator()
                rideDriverPairs.isEmpty() -> Text("No drivers or rides found", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(rideDriverPairs) { index, (ride, driver) ->
                            DriverCard(navController, firebaseAuth, firestore, index + 1, driver, ride.rideId,searchId)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun DriverCard(
    navController: NavController,
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    driverNumber: Int,
    driver: DriverInfo,
    rideId: String,
    searchId: String,

) {
    var userName by remember { mutableStateOf("Unknown") }
    var profileImageUrl by remember { mutableStateOf("") }
    var rideDetails by remember { mutableStateOf<RideDetails?>(null) }

    // Fetch both ride and driver details when the Composable is launched
    LaunchedEffect(driver.driverId) {
        fetchRideAndDriverDetails(firestore, driver.driverId, rideId) { fetchedRide, name, imageUrl ->
            rideDetails = fetchedRide
            userName = name.ifEmpty { "Unknown" }
            profileImageUrl = imageUrl
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Driver $driverNumber", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Profile Image or Default Icon
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("⭐ 4.5 (2)", color = Color.White, fontSize = 14.sp) // Hardcoded rating
                Text("RM 1", color = Color.Green, fontSize = 14.sp) // Hardcoded price

                Spacer(modifier = Modifier.height(8.dp))

                // 🚀 Show Ride Details from Firestore
                rideDetails?.let {
                    Text("📍 Location: ${it.location}", color = Color.White, fontSize = 14.sp)
                    Text("🚏 Stop: ${it.stop}", color = Color.White, fontSize = 14.sp)
                    Text("🕒 Time: ${it.time}", color = Color.White, fontSize = 14.sp)
                    Text("🎯 Destination: ${it.destination}", color = Color.White, fontSize = 14.sp)
                } ?: Text("Loading ride details...", color = Color.White, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        sendRequestToDriver(
                            firestore = firestore,
                            rideId = rideId,
                            driverId = driver.driverId,
                            passengerId = firebaseAuth.currentUser?.uid ?: "",
                            searchId = searchId,
                            onSuccess = { navController.navigate("home") }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Request")
                }
            }
        }
    }
}


fun fetchRideAndDriverDetails(
    firestore: FirebaseFirestore,
    driverId: String,
    rideId: String,
    onResult: (RideDetails?, String, String) -> Unit
) {
    // Fetch specific ride details using rideId
    firestore.collection("rides").document(rideId)
        .get()
        .addOnSuccessListener { rideDoc ->
            val rideDetails = rideDoc?.let {
                RideDetails(
                    time = it.getString("time") ?: "N/A",
                    destination = it.getString("destination") ?: "N/A",
                    stop = it.getString("stop") ?: "N/A",
                    location = it.getString("location") ?: "N/A"
                )
            }

            // Fetch driver details from "users"
            firestore.collection("users")
                .whereEqualTo("firebaseUserId", driverId)
                .limit(1)
                .get()
                .addOnSuccessListener { userDocs ->
                    val userDoc = userDocs.firstOrNull()
                    val name = userDoc?.getString("name") ?: "Unknown"
                    val imageUrl = userDoc?.getString("profileImageUrl") ?: ""

                    onResult(rideDetails, name, imageUrl)
                }
                .addOnFailureListener {
                    onResult(rideDetails, "Unknown", "")
                }
        }
        .addOnFailureListener {
            onResult(null, "Unknown", "")
        }
}


data class RideDetails(
    val time: String,
    val destination: String,
    val stop: String,
    val location: String
)

data class DriverInfo(
    val driverId: String,
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val price: String
)
data class RideInfo(
    val rideId: String,
    val pickupLocation: String,
    val destination: String,
    val time: String
)

fun sendRequestToDriver(
    firestore: FirebaseFirestore,
    rideId: String,
    driverId: String,
    passengerId: String,
    searchId: String,
    onSuccess: () -> Unit
) {
    val request = hashMapOf(
        "rideId" to rideId,
        "driverId" to driverId,
        "passengerId" to passengerId,
        "searchId" to searchId,
        "status" to "pending",
        "timestamp" to System.currentTimeMillis()
    )

    firestore.collection("requests")
        .add(request)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { /* Handle failure if needed */ }
}
