package com.kxxr.sharmin.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.sharmin.R

@Composable
fun AdminHome(firebaseAuth: FirebaseAuth, navController: NavController) {
    val currentUser = firebaseAuth.currentUser
    val context = LocalContext.current
    val firestore = Firebase.firestore

    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var updatedPhoneNumber by remember { mutableStateOf(phoneNumber) }

    //admin
    var adminGroupId by remember { mutableStateOf("") }
    var adminGroupName by remember { mutableStateOf("") }

    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        showDialog = true
        currentUser?.uid?.let { userId ->
            firestore.collection("users")
                .whereEqualTo("firebaseUserId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val document = querySnapshot.documents.firstOrNull()
                    document?.let {
                        userName = it.getString("name").orEmpty()
                        email = it.getString("email").orEmpty()
                        studentId = it.getString("studentId").orEmpty()
                        gender = it.getString("gender").orEmpty()
                        phoneNumber = it.getString("phoneNumber").orEmpty()
                        profileImageUrl = it.getString("profileImageUrl").orEmpty()

                        if (profileImageUrl.isNotEmpty()) {
                            val storageReference =
                                FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl)
                            storageReference.getBytes(1024 * 1024)
                                .addOnSuccessListener { bytes ->
                                    showDialog = false
                                    profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                                .addOnFailureListener { e ->
                                    showDialog = false
                                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    showDialog = false
                    Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            firestore.collection("Admin")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val document = querySnapshot.documents.firstOrNull()
                    document?.let {
                        adminGroupId = it.getString("groupId").orEmpty()
                        if (adminGroupId.isNotEmpty()) {
                            firestore.collection("AdminGroup")
                                .whereEqualTo("groupId", adminGroupId)
                                .get()
                                .addOnSuccessListener { groupSnapshot ->
                                    val group = groupSnapshot.documents.firstOrNull()
                                    group?.let {
                                        adminGroupName = it.getString("groupName").orEmpty()
                                    }
                                }
                                .addOnFailureListener{ e ->
                                    showDialog = false
                                    Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }else{
                            showDialog = false
                            adminGroupName = "Not found"
                        }
                    }
                }
                .addOnFailureListener{ e ->
                    showDialog = false
                    Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .background(Color.Blue)
            .fillMaxWidth()
            .height(280.dp)
            .padding(20.dp)
            .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp)) // Rounded bottom corners
    ) {
        Spacer(modifier = Modifier.height(36.dp).padding(16.dp))

        Text(text = "Admin Dashboard", fontSize = 26.sp,color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))

        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = rememberAsyncImagePainter(profileImageUrl),
            contentDescription = "Selfie Image",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .align(Alignment.End)
                .fillMaxWidth()
        )

    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(126.dp))
        Text(text = "Welcome,", fontSize = 20.sp, color = Color.White ,fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        Text(text = "$userName", fontSize = 20.sp, color = Color.White ,fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        Text(text = "Group: $adminGroupName", color = Color.White,fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 290.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = " Personal Settings", color = Color.Gray, fontSize = 20.sp, modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            ProfileCard(title = "Reset Password", img = "reset_password", onClick = {
                showDialog = true
                resetPassword(email,context,
                    onSuccess = {
                        showDialog = false
                        showEmailDialog = true},
                    onFailure = {
                        showDialog = false
                    }
                )
            })
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

            ProfileCard(title = "Enable 2FA Login", img = "authentication", onClick = {navController.navigate("check_mfa")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom


        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(text = "eWallet Settings", color = Color.Gray, fontSize = 20.sp, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp))

            ProfileCard(title = "Generate New Reload PIN", img = "authentication", onClick = {navController.navigate("generate_pin")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

            ProfileCard(title = "Adjust User eWallet Balance",img = "edit", onClick = {navController.navigate("search_user/${"eWallet"}")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(text = "User Settings", color = Color.Gray, fontSize = 20.sp, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp))

            ProfileCard(title = "Review User Verification Case", img = "profile_ico", onClick = {navController.navigate("reviewUserScreen")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

            ProfileCard(title = "Review Driver Verification Case",img = "car_front", onClick = {navController.navigate("reviewDriverScreen")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

            ProfileCard(title = "View User Details",img = "search", onClick = {navController.navigate("search_user/${"User"}")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom

            ProfileCard(title = "Ban User",img = "ban", onClick = {navController.navigate("search_user/${"Ban"}")})
            Spacer(modifier = Modifier.height(10.dp)) // Pushes Log Out button to bottom
        }
        Spacer(modifier = Modifier.height(50.dp)) // Pushes Log Out button to bottom

        // **Log Out Button**
        Button(
            onClick = {
                firebaseAuth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            },
            modifier = Modifier
                .width(180.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text(text = "Log Out", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(text = "© SHARide 2025 \n Ziegler Tan & David Ng\nTARUMT Project ONLY", color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(35.dp))

    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Email Sent") },
            text = { Text("An email has been sent to $email. Please check your inbox to reset your password.") },
            confirmButton = {
                Button(
                    onClick = { showEmailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Done")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun ProfileCard(title: String,img: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageResId = remember(img) {
        context.resources.getIdentifier(img, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, // Align items in the center
            modifier = Modifier
                .padding(8.dp)
                .padding(start = 10.dp) // Add padding for better spacing
        ) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = title,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(end = 8.dp) // Add spacing between image and text
                )
            } else {
                Text(
                    "Image Not Found",
                    color = Color.Red,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f) // Allow text to take remaining space
            )
        }

    }
}

