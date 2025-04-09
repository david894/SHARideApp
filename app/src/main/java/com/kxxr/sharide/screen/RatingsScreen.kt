package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.kxxr.logiclibrary.Ratings.Ratings
import com.kxxr.logiclibrary.Ratings.fetchPassengerInfoForRide
import com.kxxr.logiclibrary.Ratings.loadRatingHistory
import com.kxxr.logiclibrary.Ratings.loadRatingScore
import com.kxxr.logiclibrary.Ratings.ratingAPI
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.sharide.R

@Composable
fun RatingsScreen(navController: NavController, firebaseAuth: FirebaseAuth, receiverID: String, rideId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var passengerInfo by remember { mutableStateOf<List<User>>(emptyList()) }

    var isDriver by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    val userFeedback = remember {
        mutableStateMapOf<String, Pair<Double, String>>() // userId -> (rating, comment)
    }
    val initializedUserIds = remember { mutableStateListOf<String>() }
    val ratedUserIds = remember { mutableStateListOf<String>() }

    val currentUser = firebaseAuth.currentUser

    // Fetch user data when screen is opened
    if(receiverID == "Driver"){
        LaunchedEffect(rideId) {
            showDialog = true

            fetchPassengerInfoForRide(rideId) { users ->
                passengerInfo = users

                // Initialize feedback map with default values for each user
                users.forEach { user ->
                    val uid = user.firebaseUserId
                    if (uid !in initializedUserIds) {
                        userFeedback[uid] = 0.0 to ""
                        initializedUserIds.add(uid)
                    }
                }
                showDialog = false
            }
        }
    }else{
        LaunchedEffect(receiverID) {
            showDialog = true
            loadUserDetails(firestore, receiverID) { users ->
                passengerInfo = listOf(users!!)
                searchDriver(firestore, passengerInfo.firstOrNull()!!.firebaseUserId, context, onResult = {
                    isDriver = it
                })

                passengerInfo.forEach { user ->
                    val uid = user.firebaseUserId
                    if (uid !in initializedUserIds) {
                        userFeedback[uid] = 0.0 to ""
                        initializedUserIds.add(uid)
                    }
                }
                showDialog = false
            }
        }
    }

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

        passengerInfo.forEachIndexed { index, userData ->

            var pre_rating by remember { mutableStateOf(0.0) }
            var totalRating by remember { mutableStateOf(0) }

            loadRatingScore(firestore, userData.firebaseUserId) { Rating,TotalRating ->
                pre_rating = Rating
                totalRating = TotalRating
            }

            val rating = userFeedback[userData.firebaseUserId]?.first ?: 0.0
            val comment = userFeedback[userData.firebaseUserId]?.second ?: ""
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {

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
                    Text("${index+1}) ${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    isDriver.firstOrNull()?.let { driver ->
                        Text("${driver.vehiclePlate}")
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text("Gender: ${if (userData.gender == "M") "Male" else "Female"}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ratings_ico),
                            contentDescription = "Ratings Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "$pre_rating/5.0 ($totalRating)",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if(rating != 0.0) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        IconToggleButton(
                            checked = rating >= 1,
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
                            Text(
                                "$rating",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
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
                        rating  >= i -> Icons.Filled.Star
                        rating  >= i - 0.5 -> Icons.Filled.StarHalf
                        else -> Icons.Outlined.StarBorder
                    }

                    IconToggleButton(
                        checked = rating  >= i,
                        onCheckedChange = {
                            val newRating = if (rating == i.toDouble()) i - 0.5 else i.toDouble()
                            userFeedback[userData.firebaseUserId] = Pair(newRating, comment)
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
                onValueChange = {
                    userFeedback[userData.firebaseUserId] = Pair(rating, it)
                },
                label = { Text("Leave a comment for ${userData.name} ?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Blue, focusedLabelColor = Color.Black)
            )
            // Submit button
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(thickness = 3.dp)
            Spacer(modifier = Modifier.height(20.dp))

        }
        // Submit button
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                currentUser?.uid?.let { userId ->
                    passengerInfo.forEach { user ->
                        val (rating, comment) = userFeedback[user.firebaseUserId] ?: return@forEach
                        if (rating > 0 && comment.isNotBlank()) {
                            userFeedback.forEach { (firebaseUserId, feedback) ->
                                val (rating, comment) = feedback
                                val uid = firebaseUserId
                                if (uid !in ratedUserIds) {
                                    ratingAPI(userId,firebaseUserId,rating,comment,rideId,
                                        onSuccess = {

                                        },
                                        onFailure = {
                                            Toast.makeText(context, "Rating failed to submmit, Try Again", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    ratedUserIds.add(uid)
                                }
                            }

                        }
                    }
                    Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
                    navController.navigate("home")
                }
            },
            enabled = userFeedback.values.all { it.first > 0 && it.second.isNotBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Submit Rating")
        }

    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })

}

@Composable
fun RatingDashboardScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // State to hold score and transactions
    var rating by remember { mutableStateOf(0.0) }
    var totalRating by remember { mutableStateOf(0) }

    var transactions by remember { mutableStateOf<List<Ratings>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch data from Firestore
    LaunchedEffect(Unit) {
        isLoading = true

        // Fetch rating
        if (userId != null) {
            loadRatingScore(firestore, userId) { Rating, TotalRating ->
                rating = Rating
                totalRating = TotalRating
            }
            loadRatingHistory(firestore,userId){
                transactions = it
            }
            isLoading = false
        }

    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.Blue)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(54.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { navController.navigate("profile") }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Ratings Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
        Text(
            "Overall Ratings \n",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ){
            Image(
                painter = painterResource(id = R.drawable.ratings_ico),
                contentDescription = "Ratings Icon",
                modifier = Modifier.size(20.dp)
            )
            Text("$rating/5.0 ($totalRating)",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 280.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
    ) {
        // Transaction History Section
        Text("Rating History", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(15.dp))

        if (transactions.isEmpty()) {
            Text("No Rating History", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp))
        } else {

            transactions.forEach { transaction ->
                var profileUrl by remember { mutableStateOf("") }
                loadUserDetails(firestore,transaction.from){
                    if (it != null) {
                        profileUrl = it.profileImageUrl
                    }
                }
                RatingsItem(transaction, profileUrl)
            }
        }

        Spacer(modifier = Modifier.height(130.dp))

    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = isLoading, onDismiss = { isLoading = false })

}

@Composable
fun RatingsItem(transaction: Ratings, profileUrl: String) {

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardColors(containerColor = Color.White, contentColor = Color.Black, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
        //elevation = 4,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(profileUrl),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            val score = transaction.Score
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Image(
                        painter = painterResource(id = R.drawable.ratings_ico),
                        contentDescription = "Ratings Icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = score.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = transaction.description, fontSize = 15.sp)
                Text(text = transaction.date, fontSize = 12.sp, color = Color.Gray)

            }
        }
    }
}


