package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Driver.Driver
import com.kxxr.logiclibrary.Driver.searchDriver
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.logiclibrary.Ratings.ratingAPI
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.sharide.R

@Composable
fun RatingsScreen(navController: NavController, firebaseAuth: FirebaseAuth, receiverID: String, rideId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }

    var isDriver by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    // User input states
    var userRating by remember { mutableStateOf(0.0) }
    var comment by remember { mutableStateOf("") }

    val currentUser = firebaseAuth.currentUser

    // Fetch user data when screen is opened
    LaunchedEffect(receiverID) {
        showDialog = true
        loadUserDetails(firestore, receiverID) { user = it }
        loadRatingScore(firestore, receiverID) { Rating,TotalRating ->
            rating = Rating
            totalRating = TotalRating
        }
        showDialog = false
    }

    user?.let { userData ->
        searchDriver(firestore, userData.firebaseUserId, context, onResult = {
            isDriver = it
        })

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Rate Your Ride",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ){
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = rememberAsyncImagePainter(userData.profileImageUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    isDriver.firstOrNull()?.let { driver ->
                        Text("${driver.vehiclePlate}")
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text("Gender: ${if(userData.gender == "M")"Male" else "Female"}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        Image(
                            painter = painterResource(id = R.drawable.ratings_ico),
                            contentDescription = "Ratings Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("$rating/5.0 ($totalRating)",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rate Your Ride :" , fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Star rating row (supports half-stars)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..5) {
                    val icon: ImageVector = when {
                        userRating >= i -> Icons.Filled.Star
                        userRating >= i - 0.5 -> Icons.Filled.StarHalf
                        else -> Icons.Outlined.StarBorder
                    }

                    IconToggleButton(
                        checked = userRating >= i,
                        onCheckedChange = {
                            userRating = if (userRating == i.toDouble()) i - 0.5 else i.toDouble()
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            // Star rating row (supports half-stars)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..5) {

                    if(i == 1){
                        Text("Not Satisfactory", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(36.dp))
                    if(i == 5){
                        Text("Very Satisfactory", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comment input
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Leave a comment") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Blue, focusedLabelColor = Color.Black)
            )

            // Submit button
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    currentUser?.uid?.let { userId ->
                        ratingAPI(userId,receiverID,userRating,comment,
                            onSuccess = {
                                Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
                                navController.navigate("home")
                            },
                            onFailure = {
                                Toast.makeText(context, "Rating failed to submmit, Try Again", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                },
                enabled = userRating > 0.0 && comment.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Submit Rating")
            }

        }

        if(userRating != 0.0){
            Spacer(modifier = Modifier.height(150.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(26.dp)
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.End
            ) {
                IconToggleButton(
                    checked = userRating >= 1,
                    onCheckedChange = {
                    },
                    enabled = false,
                    modifier = Modifier
                        .size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(86.dp)
                    )
                    Text("$userRating",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)
                }
            }

        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}