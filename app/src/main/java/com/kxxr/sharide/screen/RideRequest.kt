@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RideRequestScreen(firebaseAuth: FirebaseAuth, navController: NavController, rideId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var driverList by remember { mutableStateOf<List<DriverInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Show loading until data is fetched

    // Fetch driver details
    LaunchedEffect(rideId) {
        isLoading = true

        // Ensure rideId contains multiple IDs, and split them properly
        val driverIds = rideId.split(",").map { it.trim() }//.distinct()

        if (driverIds.isEmpty()) {
            isLoading = false // No drivers found
        } else {
            val tempDriverList = mutableListOf<DriverInfo>()

            driverIds.forEach { driverId ->
                firestore.collection("users").document(driverId)
                    .get()
                    .addOnSuccessListener { driverDoc ->
                        val driver = DriverInfo(
                            driverId = driverDoc.id,
                            name = driverDoc.getString("name") ?: "Unknown",
                            imageUrl = driverDoc.getString("profilePic") ?: "",
                            rating = driverDoc.getDouble("rating") ?: 4.5,
                            price = "RM 1" // Example price
                        )
                        tempDriverList.add(driver)
                        driverList = tempDriverList.toList() // Ensure proper recomposition
                    }
                    .addOnFailureListener {
                        isLoading = false // Stop loading if there’s an error
                    }
                    .addOnCompleteListener {
                        isLoading = false // Data fetching completed
                    }
            }
        }
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
            Text("Matched Driver List", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            when {
                isLoading -> CircularProgressIndicator()
                driverList.isEmpty() -> Text("No drivers found", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp), // Add space between cards
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // Add padding to screen,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(driverList) { index, driver ->
                            DriverCard(firebaseAuth, firestore, index + 1, driver) {
                                navController.navigate("requested_ride/$rideId/${driver.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun DriverCard(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    driverNumber: Int,
    driver: DriverInfo,
    onRequestClick: () -> Unit
) {
    val userName = fetchUserName(firebaseAuth, firestore)
    val profileImage = fetchProfileImage(firebaseAuth)

    val rideDetails = remember { mutableStateOf<RideDetails?>(null) }

    // Fetch ride details when the Composable is launched
    LaunchedEffect(driver.driverId) {
        fetchRideDetails(firestore, driver.driverId) { fetchedDetails ->
            rideDetails.value = fetchedDetails
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
                if (profileImage != null) {
                    Image(
                        bitmap = profileImage.asImageBitmap(),
                        contentDescription = "Driver Image",
                        modifier = Modifier.size(60.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Person, contentDescription = "Default Driver",
                        tint = Color.White, modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("⭐ 4.5 (2)", color = Color.White, fontSize = 14.sp) // Hardcoded rating
                Text("RM 1", color = Color.Green, fontSize = 14.sp) // Hardcoded price

                Spacer(modifier = Modifier.height(8.dp))

                // 🚀 Show Ride Details from Firestore
                rideDetails.value?.let {
                    Text("📍 Location: ${it.location}", color = Color.White, fontSize = 14.sp)
                    Text("🚏 Stop: ${it.stop}", color = Color.White, fontSize = 14.sp)
                    Text("🕒 Time: ${it.time}", color = Color.White, fontSize = 14.sp)
                    Text("🎯 Destination: ${it.destination}", color = Color.White, fontSize = 14.sp)
                } ?: Text("Loading ride details...", color = Color.White, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Request")
                }
            }
        }
    }
}


fun fetchRideDetails(
    firestore: FirebaseFirestore,
    driverId: String,
    onResult: (RideDetails?) -> Unit
) {
    firestore.collection("rides")
        .whereEqualTo("driverId", driverId) // Find the ride for the given driverId
        .limit(1)
        .get()
        .addOnSuccessListener { documents ->
            val doc = documents.firstOrNull()
            if (doc != null) {
                val rideDetails = RideDetails(
                    time = doc.getString("time") ?: "N/A",
                    destination = doc.getString("destination") ?: "N/A",
                    stop = doc.getString("stop") ?: "N/A",
                    location = doc.getString("location") ?: "N/A"
                )
                onResult(rideDetails) // Pass the retrieved details
            } else {
                onResult(null) // No ride found
            }
        }
        .addOnFailureListener {
            onResult(null) // Handle failure
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

@Composable
fun SendRequestToDriver(
    navController: NavController,
    firestore: FirebaseFirestore,
    rideId: String,
    driverId: String,
    passengerId: String,
    driverName: String
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Confirm Ride with $driverName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Ride ID: $rideId", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val request = hashMapOf(
                "rideId" to rideId,
                "driverId" to driverId,
                "passengerId" to passengerId,
                "status" to "pending",  // Initially set as "pending"
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("requests")
                .add(request)
                .addOnSuccessListener {
                    Toast.makeText(context, "Request Sent!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to send request", Toast.LENGTH_SHORT).show()
                }
        }) {
            Text("Send Request")
        }
    }
}
