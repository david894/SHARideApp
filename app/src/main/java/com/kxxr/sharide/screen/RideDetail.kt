@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kxxr.sharide.R

@Composable
fun RideDetailScreen(navController: NavController, index: Int, rideId: String) {
    val db = Firebase.firestore

    // Fetch ride details using produceState
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
                        RouteDetails(ride.location, ride.destination)
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        RideTimeSection(ride.time)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> {
                    Text("Error loading ride details. Please try again.", color = Color.Red)
                }
            }

            PassengerListSection()
            Spacer(modifier = Modifier.height(16.dp))
            CancelRideButton()
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

// **Update RouteDetails to Accept Dynamic Data**
@Composable
fun RouteDetails(pickupLocation: String, destination: String) {
    Row {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(24.dp))
            Box(modifier = Modifier.width(2.dp).height(20.dp).background(Color.Blue))
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(pickupLocation, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(destination, fontSize = 16.sp)
        }
    }
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

// Passenger List Section
@Composable
fun PassengerListSection() {
    Column {
        passengerList.forEach { passenger ->
            PassengerCard(passenger)
        }
    }
}

// Cancel Ride Button
@Composable
fun CancelRideButton() {
    Button(
        onClick = { /* Handle cancel ride */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
    ) {
        Text("Cancel Ride", color = Color.White)
    }
}


@Composable
fun PassengerCard(passenger: Passenger) {
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
            Image(
                painter = painterResource(id = passenger.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
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

            // Accept & Reject Buttons (Filling the entire bottom of the card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Ensures buttons fully occupy the bottom space
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(), // Ensures buttons take full space
                    horizontalArrangement = Arrangement.Start
                ) {
                    Button(
                        onClick = { /* Handle accept */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier
                            .weight(1f) // Ensures equal width for both buttons
                            .fillMaxHeight(), // Ensures button covers the full height
                        shape = RectangleShape // Makes sure button is rectangular
                    ) {
                        Text("Accept", color = Color.White)
                    }

                    Button(
                        onClick = { /* Handle reject */ },
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
}







data class Passenger(
    val name: String,
    val rating: Double,
    val reviews: Int,
    val imageRes: Int,
    val time: String
)

val passengerList = listOf(
    Passenger("Uzair Ali", 4.5, 2, R.drawable.profile_ico, "9.40"),
    Passenger("Asad", 4.5, 2, R.drawable.profile_ico, "9.40"),
    Passenger("Empty", 0.0, 0, R.drawable.profile_ico, "9.40")
)


data class RideDetail(
    val rideId: String = "",
    val driverId: String = "",
    val location: String = "",  // Ensure lowercase 'l'
    val destination: String = "",
    val time: String = ""
)
