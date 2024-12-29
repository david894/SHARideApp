package com.kxxr.sharide.screen

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kxxr.sharide.R
import kotlinx.coroutines.delay

@Composable
fun AppNavHost(firebaseAuth: FirebaseAuth) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "intro") {
        composable("intro") { IntroScreen(navController) }
        composable("login") { LoginScreen(navController, firebaseAuth) }
        composable("signup") { SignupIntroScreen(navController) }
        composable("signup1") { IdVerificationScreen(navController) }
        composable("signupScreen/{name}/{studentId}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
                SignUpScreen(navController, name, studentId)
        }
        composable("signupFailed") { IntroScreen(navController) }
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
                navController.navigate("signup") // Navigate to Sign Up
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
                        firebaseAuth
                            .fetchSignInMethodsForEmail(email)
                            .addOnCompleteListener { fetchTask ->
                                if (fetchTask.isSuccessful) {
                                    val signInMethods = fetchTask.result?.signInMethods
                                    if (signInMethods.isNullOrEmpty()) {
                                        // No user found with this email
                                        Toast
                                            .makeText(
                                                context,
                                                "No user found with this email!",
                                                Toast.LENGTH_LONG
                                            )
                                            .show()
                                    } else {
                                        // Email exists, send reset link
                                        firebaseAuth
                                            .sendPasswordResetEmail(email)
                                            .addOnCompleteListener { resetTask ->
                                                if (resetTask.isSuccessful) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Password reset email sent!",
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                } else {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Error: ${resetTask.exception?.message}",
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                }
                                            }
                                    }
                                } else {
                                    // Error while fetching sign-in methods
                                    Toast
                                        .makeText(
                                            context,
                                            "Error: ${fetchTask.exception?.message}",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                }
                            }
                    } else {
                        Toast
                            .makeText(context, "Enter your email to proceed!", Toast.LENGTH_LONG)
                            .show()
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

@Composable
fun SignupIntroScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(90.dp))

        // Title
        Text(
            text = "Few Steps to get started!",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Steps
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepCard(
                stepNumber = "1",
                title = "Verify your TAR UMT ID",
                description = "Scan and Upload your Valid TAR UMT ID to verify"
            )
            StepCard(
                stepNumber = "2",
                title = "Fill in additional information",
                description = "Fill in all required information to get started"
            )
            StepCard(
                stepNumber = "3",
                title = "Done :D",
                description = "Experience new commute way starting from today!"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Start Button
        Button(
            onClick = {
                navController.navigate("signup1")
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Start",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Sign-Up Text
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Already have an account?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign In",
                color = Color.Blue,
                modifier = Modifier.clickable {
                    navController.navigate("login")
                }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun StepCard(stepNumber: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step Number
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Blue, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Description
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun IdVerificationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var validTARUMT by remember { mutableStateOf("") }
    val context = LocalContext.current
    var profilePicture by remember { mutableStateOf<Bitmap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Text(
            text = "1. Verify Your TAR UMT ID",
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please Upload the front of your TARUMT ID for verification purpose",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.tarumt_id),
            contentDescription = "TARUMT ID image",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please Make Sure : \n1. No obstacle or blur image of the ID\n" +
                    "2. The Whole ID is visible and center in the picture",
            fontSize = 16.sp,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(36.dp))

        UploadIdButton { uri ->
            val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            detectFaceFromIdCard(originalBitmap) { faceBitmap ->
                if (faceBitmap != null) {
                    // Save the face image
                    profilePicture = faceBitmap
                }
            }
            extractInfoFromIdCard(context, uri) { extractedName, extractedId, TARUMT ->
                name = extractedName
                studentId = extractedId
                validTARUMT = TARUMT
            }
        }
        if(validTARUMT == "Verified"){
            validTARUMT = ""
            navController.navigate("signupScreen/$name/$studentId")
        }else if(validTARUMT == "Error"){
            validTARUMT = ""
            navController.navigate("signupFailed")
        }
        Text(
            text = "All information is processed by AI and store securely in our database.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        profilePicture?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(150.dp))
        }
        Text(text = "Name: $name", fontSize = 16.sp)
        Text(text = "Student ID: $studentId", fontSize = 16.sp)
    }
}
@Composable
fun UploadIdButton(onImageSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    Button(
        onClick = { launcher.launch("image/*") },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = "Upload ID")
    }
}

fun extractInfoFromIdCard(
    context: Context,
    imageUri: Uri,
    onResult: (String, String, String) -> Unit // Name, Student ID, Profile Picture
) {
    val inputImage = InputImage.fromFilePath(context, imageUri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            if (!extractedText.contains("TARUMT", ignoreCase = true)) {
                onResult("", "", "Error") // Not a valid TARUMT ID
                return@addOnSuccessListener
            }

            var name = ""
            var studentId = ""

            // Loop through the text blocks and lines to extract Name and Student ID
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val text = line.text
                    if (Regex("[0-9]{2}").containsMatchIn(text)){
                        studentId = text // Matches Student ID pattern
                    } else if (text.contains(" ")) {
                        name = text // Heuristics for Name
                    }
                }
            }

            // Return results
            onResult(name, studentId, "Verified")
        }
        .addOnFailureListener { e ->
            onResult("", "", "Error" ) // Failed to process image
        }
}

fun detectFaceFromIdCard(
    idCardBitmap: Bitmap,
    onResult: (Bitmap?) -> Unit
) {
    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    val detector = FaceDetection.getClient(options)
    val image = InputImage.fromBitmap(idCardBitmap, 0)

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0].boundingBox

                // Crop the face region
                val faceBitmap = Bitmap.createBitmap(
                    idCardBitmap,
                    face.left.coerceAtLeast(0),
                    face.top.coerceAtLeast(0),
                    face.width().coerceAtMost(idCardBitmap.width),
                    face.height().coerceAtMost(idCardBitmap.height)
                )
                onResult(faceBitmap)
            } else {
                onResult(null) // No face detected
            }
        }
        .addOnFailureListener {
            onResult(null) // Handle error
        }
}

@Composable
fun SignUpScreen(navController: NavController, name: String, studentId: String) {
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") } // Default to "M"
    var isEmailValid by remember { mutableStateOf(true) }
    var isPhoneValid by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf(name) }
    var userid by remember { mutableStateOf(studentId.replace('O', '0')) }
    val context = LocalContext.current
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly
    val firebaseAuth = FirebaseAuth.getInstance() // Firebase Authentication instance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "2. Fill in Additional Information",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it.uppercase()
            },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = userid,
            onValueChange = {
                userid = it
            },
            label = { Text("Student ID") },
            modifier = Modifier.fillMaxWidth()
        )
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                isEmailValid = email.endsWith("tarc.edu.my")
            },
            label = { Text("Email") },
            isError = !isEmailValid,
            modifier = Modifier.fillMaxWidth()
        )
        if (!isEmailValid) {
            Text(text = "Only email ending with tarc.edu.my is accepted", color = Color.Red, fontSize = 12.sp)
        }

        // Phone Number Field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                isPhoneValid = phoneNumber.startsWith("01") && phoneNumber.length <= 11
            },
            label = { Text("Phone Number") },
            isError = !isPhoneValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (!isPhoneValid) {
            Text(text = "Invalid Malaysian phone number", color = Color.Red, fontSize = 12.sp)
        }

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gender Selection (Switch)
        Text(text = "Gender", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.clickable { gender = "M" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gender == "M",
                    onClick = { gender = "M" }
                )
                Text(text = "Male")
            }
            Row(
                modifier = Modifier.clickable { gender = "F" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gender == "F",
                    onClick = { gender = "F" }
                )
                Text(text = "Female")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        Button(
            onClick = {
                // Generate a unique user identifier
                val uniqueUserId = firestore.collection("users").document().id

                if (isEmailValid && isPhoneValid && userName.isNotEmpty() && userid.isNotEmpty()) {
                    // Create User in Firebase Authentication
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Get the Firebase user ID
                                val firebaseUserId = task.result?.user?.uid

                                // Save user data to Firestore
                                val userData = hashMapOf(
                                    "firebaseUserId" to firebaseUserId, // Firebase Authentication user ID
                                    "name" to userName,
                                    "studentId" to userid,
                                    "email" to email,
                                    "phoneNumber" to phoneNumber,
                                    "gender" to gender
                                )
                                firestore.collection("users")
                                    .add(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                                        navController.navigate("home")
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }else {
                                // Show error if Firebase Authentication failed
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            //colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue)
        ) {
            Text(text = "Sign Up", color = Color.White)
        }
    }
}



