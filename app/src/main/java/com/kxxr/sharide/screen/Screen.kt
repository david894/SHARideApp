package com.kxxr.sharide.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import com.kxxr.sharide.R
import kotlinx.coroutines.delay

@Composable
fun AppNavHost(firebaseAuth: FirebaseAuth) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "intro") {
        composable("intro") { IntroScreen(navController) }
        composable("login") { LoginScreen(navController, firebaseAuth) }
        // Add more screens like SignUp if needed
    }
}


@Composable
fun IntroScreen(navController: NavController) {
    val images = listOf(
        painterResource(id = R.drawable.intro1), // Replace with your image resource IDs
        painterResource(id = R.drawable.intro2),
        painterResource(id = R.drawable.intro3)
    )
    val introMain = listOf(
        "Reduce Cost",
        "Save Environment",
        "Stress Free Commute"
    )

    val introDesc = listOf(
        "Join the carpooling community and save time and money on your daily commute",
        "Reduce your carbon footprint and help to reduce traffic congestion",
        "Enjoy a stress-free commute with real-time carpool tracking and notifications"
    )

    var currentImageIndex by remember { mutableStateOf(0) }
    //val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5 seconds delay
            currentImageIndex = (currentImageIndex + 1) % images.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        // Logo
        Text(
            text = "SHARide",
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp,
            color = Color.Blue
        )

        Spacer(modifier = Modifier.height(50.dp))

        // Rotating Image
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = images[currentImageIndex],
                contentDescription = "Rotating image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Text
        Text(
            text = introMain[currentImageIndex],
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = Color.Blue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = introDesc[currentImageIndex],
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        // Three-dot Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (currentImageIndex == index) 12.dp else 8.dp)
                        .background(
                            color = if (currentImageIndex == index) Color.Blue else Color.Gray,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(4.dp)
                )
                if (index != images.size - 1) {
                    Spacer(modifier = Modifier.width(5.dp)) // Space between dots
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        Button(
            onClick = {
                navController.navigate("login") // Navigate to Login Screen
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .shadow(10.dp, shape = RoundedCornerShape(25.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(text = "Login", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Navigate to Sign Up
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .shadow(10.dp, shape = RoundedCornerShape(25.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(text = "Sign Up", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LoginScreen(navController: NavController, firebaseAuth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Login",
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Google Sign-In Logic
        val token = "1023520031753-ivf24ojn18h5fe6i8beh838rhmmk6etb.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(token)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    val email = account.email

                    // Check if the email ends with tarc.edu.my
                    if (email != null && email.endsWith("tarc.edu.my")) {
                        val db = Firebase.firestore

                        // Check if the email exists in Firestore
                        db.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    // User exists in Firestore, proceed with Firebase authentication
                                    val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
                                    firebaseAuth.signInWithCredential(credential)
                                        .addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) {
                                                val userName = firebaseAuth.currentUser?.displayName
                                                Toast.makeText(context, "Welcome Back, $userName.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Handle Firebase authentication failure
                                                Toast.makeText(
                                                    context,
                                                    "Authentication failed: ${authTask.exception?.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    // User does not exist in Firestore
                                    Toast.makeText(
                                        context,
                                        "Access denied. Only registered users are allowed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                // Handle Firestore query failure
                                Toast.makeText(
                                    context,
                                    "Failed to verify user: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        // Email does not end with tarc.edu.my
                        Toast.makeText(context, "Only tarc.edu.my emails are allowed.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    // Handle Google sign-in exception
                    Toast.makeText(context, "Google sign in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Google Sign-In Button
        Button(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Continue with Google", color = Color.Black)
            }
        }

        Text(
            text = "* Only email ending with .tarc.edu.my",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Divider with text
        Text(text = "-- OR --", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Email TextField
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password TextField
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password Text
        Text(
            text = "Forgot Password?",
            color = Color.Blue,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    if (email.isNotEmpty()) {
                        // Check if email exists in Firebase
                        firebaseAuth.fetchSignInMethodsForEmail(email)
                            .addOnCompleteListener { fetchTask ->
                                if (fetchTask.isSuccessful) {
                                    val signInMethods = fetchTask.result?.signInMethods
                                    if (signInMethods.isNullOrEmpty()) {
                                        // No user found with this email
                                        Toast.makeText(
                                            context,
                                            "No user found with this email!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        // Email exists, send reset link
                                        firebaseAuth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener { resetTask ->
                                                if (resetTask.isSuccessful) {
                                                    Toast.makeText(
                                                        context,
                                                        "Password reset email sent!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Error: ${resetTask.exception?.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                    }
                                } else {
                                    // Error while fetching sign-in methods
                                    Toast.makeText(
                                        context,
                                        "Error: ${fetchTask.exception?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Enter your email to proceed!", Toast.LENGTH_LONG).show()
                    }
                }
        )


        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                if(email.isNotEmpty() && password.isNotEmpty()){
                    // Trigger Firebase email/password authentication
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show()
                                // Navigate to the next screen
                                navController.navigate("home")
                            } else {
                                Toast.makeText(context, "Invalid Credentials ! Try Again", Toast.LENGTH_LONG).show()
                            }
                        }
                }else{
                    Toast.makeText(context, "Fill in Your Details To Proceed!", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(text = "Login", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign-Up Text
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Don’t have an account?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign Up",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    navController.navigate("signup")
                }
            )
        }
    }
}
