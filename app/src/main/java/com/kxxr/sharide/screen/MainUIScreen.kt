package com.kxxr.sharide.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.kxxr.sharide.R

@Composable
fun Testing123(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun HomeScreen(firebaseAuth: FirebaseAuth, navController: NavController) {
    val currentUser = firebaseAuth.currentUser
    val context = LocalContext.current
    val firestore = Firebase.firestore

    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDialog by remember { mutableStateOf(false) }

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
                        studentId = it.getString("studentId").orEmpty()
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
        }
    }

    // **Scaffold Layout for BottomAppBar Placement**
    Scaffold(
        bottomBar = { BottomNavBar("home",navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Ensures content is not overlapped by BottomBar
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Text(text = "Welcome to Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            profileBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(150.dp)
                        .border(5.dp, Color.Gray)
                )
            } ?: Text(text = "No Profile Image", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Name: $userName", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = "Student ID: $studentId", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = "Email: ${currentUser?.email.orEmpty()}", fontSize = 18.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(36.dp))

            // **Driver Button**
            Button(
                onClick = {
                    currentUser?.let {
                        firestore.collection("driver")
                            .whereEqualTo("userId", it.uid)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (querySnapshot.isEmpty) {
                                    navController.navigate("driverintro")
                                } else {
                                    navController.navigate("home")
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Driver", color = Color.White, fontSize = 16.sp)
            }

            // **Log Out Button**
            Button(
                onClick = {
                    firebaseAuth.signOut()
                    navController.navigate("intro") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Log Out", color = Color.White, fontSize = 16.sp)
            }
        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun ProfileScreen(firebaseAuth: FirebaseAuth, navController: NavController) {
    val currentUser = firebaseAuth.currentUser
    val context = LocalContext.current
    val firestore = Firebase.firestore

    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDialog by remember { mutableStateOf(false) }

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
                        studentId = it.getString("studentId").orEmpty()
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
        }
    }

    // **Scaffold Layout for BottomAppBar Placement**
    Scaffold(
        bottomBar = { BottomNavBar("profile",navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(Color.Blue)
                .fillMaxWidth()
                .height(280.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp)) // Rounded bottom corners
        ) {
            Spacer(modifier = Modifier.height(36.dp).padding(paddingValues))

            Text(text = "My Profile", fontSize = 26.sp,color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))

            Spacer(modifier = Modifier.height(16.dp))

            profileBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } ?: Image(
                painter = painterResource(id = R.drawable.profile_ico),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(126.dp))

            Text(text = "$userName", fontSize = 20.sp, color = Color.White ,fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            Text(text = "Student ID: $studentId", color = Color.White,fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = "Email: ${currentUser?.email.orEmpty()}", color = Color.White,fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 280.dp)
                .padding(bottom = 150.dp)
                .verticalScroll(rememberScrollState()), // Enable scrolling
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileCard(title = "Reset Password", onClick = { /* Navigate */ })
                ProfileCard(title = "Enable 2FA Login", onClick = { /* Navigate */ })
                ProfileCard(title = "Edit Phone Number", onClick = { /* Navigate */ })

            }

            Spacer(modifier = Modifier.height(50.dp)) // Pushes Log Out button to bottom

            // **Log Out Button**
            Button(
                onClick = {
                    firebaseAuth.signOut()
                    navController.navigate("intro") {
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
        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun ProfileCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun BottomNavBar(screen:String,navController: NavController) {
    BottomAppBar(
        containerColor = Color.White,
        contentColor = Color.White,
        modifier = Modifier
            .border(1.dp, Color.Gray)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Complete Task Button
            Button(
                onClick = {
                    navController.navigate("home")
                },
                modifier = Modifier
                    .size(100.dp) ,
                // Adjust size as needed
                shape = RoundedCornerShape(8.dp), // Adjust corner radius as needed
                contentPadding = PaddingValues(3.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    if(screen == "home") Color.hsl(195F, 0.6F, 0.89F) else Color.White
                ) // Customize color

            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.home_ico),
                        contentDescription = "Home",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Add some space
                    Text("Home", fontSize = 11.sp)
                }
            }
            // eWallet Button
            Button(
                onClick = {
                },
                modifier = Modifier
                    .size(100.dp), // Adjust size as needed
                // Adjust size as needed
                shape = RoundedCornerShape(8.dp), // Adjust corner radius as needed
                contentPadding = PaddingValues(3.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    if(screen == "eWallet") Color.hsl(195F, 0.6F, 0.89F) else Color.White
                ) // Customize color

            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ewallet_ico),
                        contentDescription = "eWallet",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Add some space
                    Text("eWallet", fontSize = 11.sp)
                }
            }
            // Profile Button
            Button(
                onClick = {
                    navController.navigate("profile")
                },
                modifier = Modifier
                    .size(100.dp), // Adjust size as needed
                // Adjust size as needed
                shape = RoundedCornerShape(8.dp), // Adjust corner radius as needed
                contentPadding = PaddingValues(3.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    if(screen == "profile") Color.hsl(195F, 0.6F, 0.89F) else Color.White
                ) // Customize color

            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_ico),
                        contentDescription = "eWallet",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Add some space
                    Text("Profile", fontSize = 11.sp)
                }
            }
        }
    }
}
