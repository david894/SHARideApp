@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kxxr.logiclibrary.Driver.Driver
import com.kxxr.logiclibrary.Driver.Vehicle
import com.kxxr.logiclibrary.Driver.searchDriver
import com.kxxr.logiclibrary.Driver.searchVehicle
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.sharide.R
import kotlinx.coroutines.tasks.await


@Composable
fun ManageDriverScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ManageDriverTopBar(title = "Manage Driver", navController = navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            ManageDriverCard(
                title = "Recent History Driver",
                description = "View the list of drivers you recently interacted with.",
                buttonText = "View History",
                backgroundColor = Color(0xFF0075FD),
                onClick = { navController.navigate("recent_driver") },
                imageResId = R.drawable.driver
            )

            Spacer(modifier = Modifier.height(16.dp))

            ManageDriverCard(
                title = "Favourite Driver",
                description = "Manage your favourite drivers here.",
                buttonText = "View Favourites",
                backgroundColor = Color(0xFFFFC0CB),
                onClick = { navController.navigate("favourite_driver") },
                imageResId = R.drawable.favourite
            )

            Spacer(modifier = Modifier.height(16.dp))

            ManageDriverCard(
                title = "BlackList Driver",
                description = "View and remove drivers from your blacklist.",
                buttonText = "View Blacklist",
                backgroundColor = Color.Gray,
                onClick = { navController.navigate("black_list_driver") },
                imageResId = R.drawable.ban
            )
        }
    }
}

@Composable
fun ManageDriverTopBar(title: String, navController: NavController) {
    TopAppBar(
        title = { Text(text = title, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0075FD)
        )
    )
}

@Composable
fun ManageDriverCard(
    title: String,
    description: String,
    buttonText: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    imageResId: Int // Pass drawable resource ID here
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(60.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side (Text and Button)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(buttonText, color = backgroundColor)
                }
            }

            // Right side (Image)
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(start = 12.dp)
            )
        }
    }
}



@Composable
fun RecentDriverScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val context = LocalContext.current

    var driverIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var actionType by remember { mutableStateOf("") } // "favourite" or "blackList"

    // Load recent completed drivers not in favourite or blacklist
    LaunchedEffect(Unit) {
        val requestSnapshot = db.collection("requests")
            .whereEqualTo("passengerId", currentUserId)
            .whereEqualTo("status", "complete")
            .get().await()

        val allDriverIds = requestSnapshot.documents.mapNotNull { it.getString("driverId") }.distinct()

        val favouriteSnapshot = db.collection("favourites")
            .whereEqualTo("passengerId", currentUserId)
            .get().await()

        val blackListSnapshot = db.collection("blackLists")
            .whereEqualTo("passengerId", currentUserId)
            .get().await()

        val favouriteDrivers = favouriteSnapshot.firstOrNull()?.get("driverIds") as? List<String> ?: emptyList()
        val blackListDrivers = blackListSnapshot.firstOrNull()?.get("driverIds") as? List<String> ?: emptyList()

        driverIds = allDriverIds.filterNot { it in favouriteDrivers || it in blackListDrivers }
    }

    // Confirmation Dialog
    if (showDialog && selectedDriverId != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Action") },
            text = {
                Text("Are you sure you want to add this driver to your ${if (actionType == "favourite") "favourites" else "blacklist"}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedDriverId?.let { driverId ->
                        if (actionType == "favourite") {
                            addToList("favourites", driverId)
                        } else {
                            addToList("blackLists", driverId)
                        }
                        showDialog = false
                        navController.popBackStack()
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ManageDriverTopBar(title = "Recent History Driver", navController = navController)

        LazyColumn {
            items(driverIds) { driverId ->
                DriverCard(
                    driverId = driverId,
                    navController = navController,
                    cardColor = Color(0xFF0075FD)
                ) {
                    Button(
                        onClick = {
                            selectedDriverId = driverId
                            actionType = "favourite"
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC0CB))
                    ) {
                        Text("Add to Favourites", color = Color.White)
                    }

                    Button(
                        onClick = {
                            selectedDriverId = driverId
                            actionType = "blackList"
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Blacklist", color = Color.White)
                    }
                }
            }
        }
    }
}


@Composable
fun DriverCard(
    driverId: String,
    navController: NavController,
    cardColor: Color,
    buttons: @Composable RowScope.() -> Unit
) {
    val db = Firebase.firestore
    val context = LocalContext.current

    var name by remember { mutableStateOf("Unknown") }
    var imageUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }
    var vehicleDetails by remember { mutableStateOf<List<Vehicle>>(emptyList()) }

    LaunchedEffect(driverId) {
        db.collection("users")
            .whereEqualTo("firebaseUserId", driverId)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                docs.firstOrNull()?.let { doc ->
                    name = doc.getString("name") ?: "Unknown"
                    imageUrl = doc.getString("profileImageUrl") ?: ""
                }
            }

        loadRatingScore(db, driverId) { score, total ->
            rating = score
            totalRating = total
        }

        searchDriver(db, driverId, context) { drivers ->
            if (drivers.isNotEmpty()) {
                val plateNumber = drivers.first().vehiclePlate
                searchVehicle(db, plateNumber, context) { vehicles ->
                    vehicleDetails = vehicles
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text("⭐ $rating ($totalRating)", fontSize = 14.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Info
            vehicleDetails.firstOrNull()?.let { vehicle ->
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vehicle.CarRegistrationNumber.ifBlank { "Unknown Plate" },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(8.dp)
                        )
                        Text(
                            text = "${vehicle.CarMake} ${vehicle.CarModel} (${vehicle.CarColour})",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Custom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = buttons
            )
        }
    }
}


fun addToList(collection: String, driverId: String) {
    val db = Firebase.firestore
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val userRef = db.collection(collection).document(userId)
    userRef.get().addOnSuccessListener { doc ->
        if (doc.exists()) {
            userRef.update("driverIds", FieldValue.arrayUnion(driverId))
        } else {
            val data = hashMapOf("passengerId" to userId, "driverIds" to listOf(driverId))
            userRef.set(data)
        }
    }
}

@Composable
fun FavouriteDriverScreen(passengerId: String, navController: NavController) {
    val db = Firebase.firestore
    var driverIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // Step 1: Load driverIds
    LaunchedEffect(Unit) {
        db.collection("favourites")
            .whereEqualTo("passengerId", passengerId)
            .get()
            .addOnSuccessListener { docs ->
                driverIds = docs.firstOrNull()?.get("driverIds") as? List<String> ?: emptyList()
            }
    }

    Column {
        ManageDriverTopBar(title = "Favourite Driver", navController = navController)

        if (driverIds.isEmpty()) {
            Text("No favourite drivers.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn {
                items(driverIds) { driverId ->
                    DriverCard(
                        driverId = driverId,
                        navController = navController,
                        cardColor = Color(0xFFFFC0CB) // Pink
                    ) {
                        Button(
                            onClick = {
                                // Step 2: Check or create chat
                                db.collection("chats")
                                    .whereEqualTo("passengerId", passengerId)
                                    .whereEqualTo("driverId", driverId)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { docs ->
                                        val chatId = if (!docs.isEmpty) {
                                            docs.first().id
                                        } else {
                                            // Create new chat if not exist
                                            val newChat = hashMapOf(
                                                "passengerId" to passengerId,
                                                "driverId" to driverId,
                                                "timestamp" to FieldValue.serverTimestamp()
                                            )
                                            val newDocRef = db.collection("chats").document()
                                            newDocRef.set(newChat)
                                            newDocRef.id
                                        }
                                        // Navigate to Chat screen
                                        navController.navigate("chat_screen/$chatId")
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White) // Pink
                        ) {
                            Text("Chat", color = Color(0xFFFFC0CB))
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun BlackListDriverScreen(passengerId: String,navController: NavController) {
    val db = Firebase.firestore
    var driverList by remember { mutableStateOf<List<DriverInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("blackLists")
            .whereEqualTo("passengerId", passengerId)
            .get()
            .addOnSuccessListener { docs ->
                val driverIds = docs.firstOrNull()?.get("driverIds") as? List<String> ?: emptyList()
                if (driverIds.isNotEmpty()) {
                    db.collection("users")
                        .whereIn("firebaseUserId", driverIds)
                        .get()
                        .addOnSuccessListener { users ->
                            driverList = users.map {
                                DriverInfo(
                                    driverId = it.getString("firebaseUserId") ?: "",
                                    name = it.getString("name") ?: "",
                                    imageUrl = it.getString("profileImageRes") ?: "",
                                    rating = 0.0,
                                    price = ""
                                )
                            }
                        }
                }
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ManageDriverTopBar(title = "Manage Driver", navController = navController)

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(driverList) { driver ->
                DriverCard(driverId = driver.driverId, navController = navController,  cardColor = Color.Gray) {
                    Button(
                        onClick = {
                            // Remove from blacklist
                            db.collection("blackLists")
                                .whereEqualTo("passengerId", passengerId)
                                .get()
                                .addOnSuccessListener { docs ->
                                    val doc = docs.firstOrNull()
                                    doc?.reference?.update(
                                        "driverIds",
                                        FieldValue.arrayRemove(driver.driverId)
                                    )
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Unblock", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


