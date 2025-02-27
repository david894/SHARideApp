@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun RideRequestScreen(firebaseAuth: FirebaseAuth,navController: NavController, rideId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var driverInfo by remember { mutableStateOf<DriverInfo?>(null) }

    // Fetch driverId using rideId
    LaunchedEffect(rideId) {
        firestore.collection("rides").document(rideId)
            .get()
            .addOnSuccessListener { rideDoc ->
                val driverId = rideDoc.getString("driverId") ?: return@addOnSuccessListener

                // Fetch driver details using driverId
                firestore.collection("users").document(driverId)
                    .get()
                    .addOnSuccessListener { driverDoc ->
                        driverInfo = DriverInfo(
                            name = driverDoc.getString("name") ?: "Unknown",
                            imageUrl = driverDoc.getString("profilePic") ?: "",
                            rating = 4.5, // Hardcoded rating
                            price = "RM 1" // Hardcoded price
                        )
                    }
            }
    }

    Scaffold(
        topBar = { // ✅ Replacing header with TopAppBar
            TopAppBar(
                title = {
                    Text(
                        "Ride Request",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Navigates back
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0075FD)) // ✅ Blue background
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // ✅ Adjust padding to avoid overlap with AppBar
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Matched Driver List",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            driverInfo?.let { driver ->
                DriverCard(firebaseAuth, firestore, 1, driver)
                {
                    navController.navigate("confirm_ride/$rideId/${driver.name}") // ✅ Pass rideId & driver
                }
            } ?: LoadingIndicator()
        }
    }
}


@Composable
fun DriverCard(firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore, driverNumber: Int, driver: DriverInfo, onRequestClick: () -> Unit) {
    val userName = fetchUserName(firebaseAuth, firestore)
    val profileImage = fetchProfileImage(firebaseAuth)
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0075FD))
    ) {
        Box( // ✅ Wrap content in a Box to center it
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center // ✅ Ensure center alignment
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // ✅ Align all content to center
                verticalArrangement = Arrangement.Center, // ✅ Center items vertically
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Driver $driverNumber", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Display Profile Image or Default Icon
                if (profileImage != null) {
                    Image(
                        bitmap = profileImage.asImageBitmap(),
                        contentDescription = "Driver Image",
                        modifier = Modifier.size(60.dp)
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Default Driver", tint = Color.White, modifier = Modifier.size(60.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                // ✅ Display Retrieved Name
                Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("⭐ 4.5 (2)", color = Color.White, fontSize = 14.sp) // Hardcoded rating
                Text("RM 1", color = Color.Green, fontSize = 14.sp) // Hardcoded price

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


@Composable
fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CircularProgressIndicator(color = Color(0xFF0075FD))
    }
}


data class DriverInfo(
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val price: String
)
