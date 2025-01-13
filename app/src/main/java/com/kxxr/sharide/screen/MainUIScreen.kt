package com.kxxr.sharide.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

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
    // Dialog visibility state
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

                        // Fetch the profile image from Firebase Storage using the URL
                        if (profileImageUrl.isNotEmpty()) {
                            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(profileImageUrl)
                            storageReference.getBytes(1024 * 1024) // Limit file size to 1MB
                                .addOnSuccessListener { bytes ->
                                    showDialog = false

                                    profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                                .addOnFailureListener { e ->
                                    showDialog = false

                                    Toast.makeText(context, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Text(text = "Welcome to Home Screen", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Display Profile Image
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

        // Display User Information
        Text(text = "Name: $userName", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(text = "Student ID: $studentId", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(text = "Email: ${currentUser?.email.orEmpty()}", fontSize = 18.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(36.dp))

        // Driver Button
        Button(
            onClick = {
                if (currentUser != null) {
                    firestore.collection("driver")
                        .whereEqualTo("userId", currentUser.uid)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                // no driver record
                                navController.navigate("driverintro")
                            }else{
                                //navigate to driver screen
                                navController.navigate("home")
                            }
                        }
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text(text = "Driver", color = Color.White, fontSize = 16.sp)
        }

        // Log Out Button
        Button(
            onClick = {
                firebaseAuth.signOut() // Log out the user
                navController.navigate("intro") {
                    popUpTo("home") { inclusive = true } // Clear back stack
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text(text = "Log Out", color = Color.White, fontSize = 16.sp)
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...",showDialog = showDialog, onDismiss = { showDialog = false })
}
