@file:OptIn(ExperimentalMaterial3Api::class)

package com.kxxr.sharide.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

//1. if the request status is "complete". add the driver Id into A list , this call "PassedDriverScreen".
//the PassedDriverScreen show th e passed driver card. bellow have two button, add to Favourite
//with blue color and black list with gray color.
//if user click add to favourite, create  a new collection call favourites inside the collection has passengerId, driverIds(array).
//if already have the favourite correction, update the driverId into the arrays.
//
//if user click black List, create a new collection call blackLists inside the collection has passengerId, driverIds(array).
//if already have the blackLists correction, update the driverId into the arrays.
//
//

@Composable
fun FavouriteDriverScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var driverIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current

    // Load completed drivers
    LaunchedEffect(Unit) {
        db.collection("requests")
            .whereEqualTo("passengerId", currentUserId)
            .whereEqualTo("status", "complete")
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.documents.mapNotNull { it.getString("driverId") }.distinct()
                driverIds = ids
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favourite Drivers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding->

            LazyColumn(contentPadding = padding) {
                items(driverIds) { driverId ->
                    FavouriteDriverCard(driverId = driverId, navController = navController)
                }
            }
    }
}


@Composable
fun FavouriteDriverCard(driverId: String, navController: NavController) {
    val db = Firebase.firestore
    var name by remember { mutableStateOf("Unknown") }
    var imageUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0.0) }
    var model by remember { mutableStateOf("") }
    var colour by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }

    LaunchedEffect(driverId) {
        // Fetch user info
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

        // Fetch rating
        db.collection("Ratings")
            .whereEqualTo("userId", driverId)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                docs.firstOrNull()?.let { doc ->
                    rating = doc.getDouble("TotalRatings") ?: 0.0
                }
            }

        // Fetch vehicle info
        db.collection("Vehicle")
            .whereEqualTo("userId", driverId)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                docs.firstOrNull()?.let { doc ->
                    model = doc.getString("CarModel") ?: ""
                    colour = doc.getString("CarColour") ?: ""
                    plate = doc.getString("CarRegistrationNumber") ?: ""
                }
            }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("⭐ $rating", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("🚗 Model: $model", fontSize = 14.sp)
            Text("🎨 Colour: $colour", fontSize = 14.sp)
            Text("📋 Plate: $plate", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        addToList("favourites", driverId)
                    }
                ) {
                    Text("Add to Favourites")
                }

                Button(
                    onClick = {
                        addToList("blackLists", driverId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Blacklist", color = Color.White)
                }
            }
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

