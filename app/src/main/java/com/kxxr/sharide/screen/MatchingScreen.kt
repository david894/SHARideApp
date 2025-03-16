package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun MatchingScreen(navController: NavController, firestore: FirebaseFirestore) {
    var location by remember { mutableStateOf("Loading...") }
    var destination by remember { mutableStateOf("Loading...") }
    var searchId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var matchingRides by remember { mutableStateOf<List<DocumentSnapshot>?>(null) } // Keep DocumentSnapshot

    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid ?: ""

    LaunchedEffect(userId) {
        fetchLatestSearchId(userId, { latestSearchId ->
            searchId = latestSearchId
            if (latestSearchId != null) {
                fetchSearchDetails(latestSearchId, { loc, dest, date ->
                    location = loc
                    destination = dest
                    isLoading = false
                    val passengerSearch = mapOf("location" to loc, "destination" to dest, "date" to date, "passengerId" to userId)
                    findMatchingRides(firestore, passengerSearch, { rides ->
                        matchingRides = rides
                    }, {
                        errorMessage = "No matching rides found."
                    })
                }, { error ->
                    errorMessage = error
                    isLoading = false
                })
            } else {
                errorMessage = "No search found."
                isLoading = false
            }
        }, { error ->
            errorMessage = error
            isLoading = false
        })
    }

    MatchingScreenContent(location, destination, isLoading, matchingRides, searchId, firestore, navController)
}

@Composable
fun MatchingScreenContent(
    location: String,
    destination: String,
    isLoading: Boolean,
    matchingRides: List<DocumentSnapshot>?, // Still using DocumentSnapshot
    searchId: String?, // Add searchId parameter
    firestore: FirebaseFirestore, // Add Firestore instance
    navController: NavController
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0075FD)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else {
            LocationBox(location, Icons.Default.LocationOn)
            Spacer(modifier = Modifier.height(8.dp))
            LocationBox(destination, Icons.Default.MyLocation)
            Spacer(modifier = Modifier.weight(1f))
            MatchingIndicator()
            Spacer(modifier = Modifier.weight(1f))

            if (matchingRides.isNullOrEmpty()) {
                MatchingText() // Always show this if no rides are found
            } else {
                Text(
                    "Matching Rides Found:",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                val driverIds = matchingRides.mapNotNull { it.getString("driverId") }
                val driverIdsString = driverIds.joinToString(",")

                Button(onClick = {
                    // ✅ Update Firestore's "searchs" collection with driverIdsString
                    if (searchId != null) {
                        firestore.collection("searchs")
                            .document(searchId)
                            .update("driverIdsString", driverIdsString)
                            .addOnSuccessListener {
                                navController.navigate("request_ride/$driverIdsString")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to update drivers: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }) {
                    Text("Click Me to Choose Ride Driver", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}






fun fetchLatestSearchId(userId: String, onSuccess: (String?) -> Unit, onFailure: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("searchs")
        .whereEqualTo("passengerId", userId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(1)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                onSuccess(documents.documents[0].id)
            } else {
                onSuccess(null)
            }
        }
        .addOnFailureListener { e ->
            onFailure("Failed to fetch searchs: ${e.message}")
        }
}

fun fetchSearchDetails(searchId: String, onSuccess: (String, String, String) -> Unit, onFailure: (String) -> Unit) {
    // Fetch search details from Firestore using searchId
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("searchs").document(searchId).get()
        .addOnSuccessListener { document ->
            val loc = document.getString("location") ?: ""
            val dest = document.getString("destination") ?: ""
            val date = document.getString("date") ?: ""
            onSuccess(loc, dest, date)  // Now returns three parameters
        }
        .addOnFailureListener { exception ->
            onFailure(exception.localizedMessage ?: "Unknown error")
        }
}


@Composable
fun LocationBox(location: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(location, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun MatchingText() {
    Text(
        text = "Matching Your Ride Participants ...",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    ClickableText(
        text = AnnotatedString("Can't Match Ride Participant? CLICK ME"),
        style = TextStyle(color = Color.White, fontSize = 14.sp),
        onClick = { /* Handle click */ }
    )
}

@Composable
fun MatchingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()

    val circle1Size by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val circle2Size by infiniteTransition.animateFloat(
        initialValue = 80f,
        targetValue = 350f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val circle3Size by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(500.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(circle1Size.dp)
                .background(Color(0xFF005BBB).copy(alpha = 0.3f), shape = CircleShape)
        )

        Box(
            modifier = Modifier
                .size(circle2Size.dp)
                .background(Color(0xFF005BBB).copy(alpha = 0.5f), shape = CircleShape)
        )

        Box(
            modifier = Modifier
                .size(circle3Size.dp)
                .background(Color(0xFF005BBB).copy(alpha = 0.7f), shape = CircleShape)
        )

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location Pin",
            modifier = Modifier.size(80.dp),
            tint = Color.Red,
        )
    }
}

