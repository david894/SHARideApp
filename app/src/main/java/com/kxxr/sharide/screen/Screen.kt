package com.kxxr.sharide.screen

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kxxr.logiclibrary.AiDetector.detectFaceFromIdCard
import com.kxxr.logiclibrary.AiDetector.detectPlateNumber
import com.kxxr.logiclibrary.AiDetector.extractInfoFromIdCard
import com.kxxr.logiclibrary.AiDetector.saveBitmapToCache
import com.kxxr.logiclibrary.Login.handleGoogleSignIn
import com.kxxr.logiclibrary.Login.handleMultiFactorAuthentication
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.Login.signInWithEmailPassword
import com.kxxr.logiclibrary.Network.NetworkViewModel
import com.kxxr.logiclibrary.SignUp.handleVehicleSubmission
import com.kxxr.logiclibrary.SignUp.loadVehicleData
import com.kxxr.logiclibrary.SignUp.performSignUp
import com.kxxr.logiclibrary.SignUp.saveDriverToFirestore
import com.kxxr.logiclibrary.SignUp.uploadDriverData
import com.kxxr.logiclibrary.SignUp.uploadImagesAndSaveData
import com.kxxr.logiclibrary.SignUp.uploadToFirebaseStorage
import com.kxxr.sharide.R
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore,
    networkViewModel: NetworkViewModel,
    context: Context
) {
    val navController = rememberNavController()
    val isConnected by networkViewModel.isConnected.collectAsState(initial = true) // Observe connectivity

    if (!isConnected) {
        // Show the "No Internet Connection" screen
        NoInternetScreen(onRetry = { })
    } else {
        // Determine the start destination based on user login status
        val startDestination = if (firebaseAuth.currentUser != null) "home" else "intro"

        AnimatedNavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(700)) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -800 },
                    animationSpec = tween(600)
                ) + fadeOut(animationSpec = tween(600))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                    animationSpec = tween(700, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(700)) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 800 },
                    animationSpec = tween(600, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(600)) + scaleOut(
                    targetScale = 1.2f,
                    animationSpec = tween(500)
                )
            }
        ){
            composable("intro") { IntroScreen(navController) }
            composable("login") { LoginScreen(navController, firebaseAuth) }
            composable("signup") { SignupIntroScreen(navController) }
            composable("signup1") { IdVerificationScreen(navController) }
            composable("signupScreen/{name}/{studentId}/{imagePath}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name").orEmpty()
                val studentId = backStackEntry.arguments?.getString("studentId").orEmpty()
                val imagePath = Uri.decode(backStackEntry.arguments?.getString("imagePath").orEmpty())

                SignUpScreen(navController, name, studentId, imagePath)
            }
            composable("emailverify") { EmailVerify(navController) }
            composable("signupFailed") { UnableToVerifyScreen(navController) }
            composable("signupFailedFace") { UnableToVerifyFace(navController) }
            composable("duplicateID") { UnableToVerifyDuplicateID(navController) }
            composable("customerServiceTARUMTID") { CustomerServiceScreen(navController) }
            composable("ReportSubmitted/{link}") { backStackEntry ->
                val link = backStackEntry.arguments?.getString("link").orEmpty()
                ReportSubmitted(navController, link)
            }

            // Driver Signup
            composable("driverintro") { DriverIntroScreen(navController) }
            composable("driversignup") { DriverSignupIntroScreen(navController) }
            composable("driversignup1") { DriverIdVerificationScreen(navController) }
            composable("driverFailed") { DriverFailed(navController) }
            composable("driverFailedFace") { UnableToVerifyDriverFace(navController) }
            composable("driversignupscreen/{drivingid}/{lesen}/{imagePath}") { backStackEntry ->
                val drivingid = backStackEntry.arguments?.getString("drivingid").orEmpty()
                val lesen = Uri.decode(backStackEntry.arguments?.getString("lesen").orEmpty())
                val imagePath = Uri.decode(backStackEntry.arguments?.getString("imagePath").orEmpty())

                DriverSignUpScreen(navController, drivingid, lesen, imagePath)
            }
            composable("duplicateDrivingID") { UnableToVerifyDuplicateDrivingID(navController) }
            composable("addnewcar") { AddNewVehicle(navController) }
            composable("driversuccess") { DriverSuccess(navController) }
            composable("duplicatecar") { DuplicateVehicle(navController) }
            composable("driverCustomerService") { DriverCustomerService(navController) }

            // Profile
            composable("profile") { ProfileScreen(firebaseAuth, navController) }
            composable("check_mfa") { CheckMfaEnrollment(firebaseAuth, navController) }
            composable("reg_otp") { RegisterPhoneNumberScreen(firebaseAuth, navController) }
            composable("verifyOtp/{verifyID}/{phone}/{route}") { backStackEntry ->
                val verifyID = backStackEntry.arguments?.getString("verifyID").orEmpty()
                val phone = backStackEntry.arguments?.getString("phone").orEmpty()
                val route = backStackEntry.arguments?.getString("route").orEmpty()

                VerifyOtpScreen(navController, verifyID,firebaseAuth,phone,route)
            }
            composable("ratingsDashboard") { RatingDashboardScreen(navController) }

            composable("banned_user/{userId}/{remark}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val remark = backStackEntry.arguments?.getString("remark") ?: ""
                BannedUserScreen(navController,userId,remark)
            }

            // Home (Updated to pass FirebaseAuth & Firestore)
            composable("home") { HomePage(navController, firebaseAuth, firestore) }

            // Ride & Location
            composable("create_ride") { CreateRideScreen(navController) }
            composable("search_location") { SearchLocationScreen(navController, locationType = 0) }
            composable("search_stop") { SearchLocationScreen(navController, locationType = 1) }
            composable("search_destination") { SearchLocationScreen(navController, locationType = 2) }
            composable("notification") { NotificationScreen(navController, firebaseAuth, firestore,context) }
            composable("matching_screen") { MatchingScreen(navController,firestore) }
            composable("search_ride") { SearchRideScreen(navController) }
            composable("request_ride/{driverId}/{rideId}/{searchId}") { backStackEntry ->
                val driverId = backStackEntry.arguments?.getString("driverId") ?: ""
                val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
                val searchId = backStackEntry.arguments?.getString("searchId") ?: ""
                RideRequestScreen(firebaseAuth,navController,driverId, rideId,searchId)
            }
            composable(
                "ride_detail/{index}/{rideId}",
                arguments = listOf(
                    navArgument("index") { type = NavType.IntType },
                    navArgument("rideId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                val rideId = backStackEntry.arguments?.getString("rideId") ?: ""

                RideDetailScreen(navController, firestore,index, rideId)
            }
            composable(
                "successful_ride_requested/{index}/{searchId}",
                arguments = listOf(
                    navArgument("index") { type = NavType.IntType },
                    navArgument("searchId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 0
                val searchId = backStackEntry.arguments?.getString("searchId") ?: ""
                SuccessfulRequestRideScreen(navController, index,searchId)
            }

            composable(
                "pending_ride_requested/{searchId}"){ backStackEntry ->
                    val searchId = backStackEntry.arguments?.getString("searchId") ?: ""
                PendingRideRequestScreen(firebaseAuth,navController,searchId,firestore)
            }


            composable(
                route = "chat_list/{userId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""

                ChatListScreen(
                    userId = userId,
                    navController = navController
                )
            }

            composable("chat_screen/{chatId}") { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.uid?.let { uid ->
                    ChatScreen(chatId = chatId, currentUserId = uid, navController = navController)
                }
            }
            //rating
            composable(
                "rate_ride/{receiverID}/{rideId}"){ backStackEntry ->
                val receiverID = backStackEntry.arguments?.getString("receiverID") ?: ""
                val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
                RatingsScreen(navController,firebaseAuth,receiverID,rideId)
            }

            // eWallet
            composable("ewallet") { EWalletIntro(navController) }
            composable("security_question") { SetSecurityQuestionsScreen(navController) }
            composable("ewalletDashboard") { EWalletDashboardScreen(navController) }
            composable("topup") { TopUpScreen(navController) }
            composable("topupsuccess/{topup}/{balance}") { backStackEntry ->
                val topup = backStackEntry.arguments?.getString("topup")?.toDoubleOrNull() ?: 0.0
                val balance = backStackEntry.arguments?.getString("balance")?.toDoubleOrNull() ?: 0.0
                TopUpSuccessScreen(navController, topup, balance)
            }
            composable("changePIN") { ChangePaymentPIN(navController) }
            composable("resetPIN") { ResetPinScreen(navController) }
            composable("updatePIN") { UpdatePinScreen(navController) }
            composable("reset_question") { ResetSecurityQuestions(navController) }

        }
    }
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! No Internet Connection",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "This app requires an internet connection to work properly. Please check your connection and try again.",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tips that you could do:\n" +
                    "1. Try turn on your mobile data\n" +
                    "2. Connect to a WIFI\n" +
                    "3. Turn off airplane mode",
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
            Text("Try Again")
        }
    }
}

@Composable
fun IntroScreen(navController: NavController) {
    val images = listOf(
        painterResource(id = R.drawable.intro1),
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
            .background(Color.White)
            .verticalScroll(rememberScrollState()), // Enable scrolling
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
                //.size(250.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = images[currentImageIndex],
                contentDescription = "Rotating image",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer { alpha = 1f }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Text
        Text(
            text = introMain[currentImageIndex],
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            color = Color.Blue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = introDesc[currentImageIndex],
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
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
    var errormsg by remember { mutableStateOf("") }

    // Password TextField with Visibility Toggle
    var passwordVisible by remember { mutableStateOf(false) }

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    val type = "user"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
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
        val token = context.getString(R.string.google_api)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(token)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showDialog = true
                handleGoogleSignIn(result.data, firebaseAuth, navController, context,type){
                    showDialog = false
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
            onValueChange = {
                email = it
                if (!email.endsWith("tarc.edu.my")){
                    errormsg = "Only TARC emails are allowed"
                }else{
                    errormsg = ""
                }
            },
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
            trailingIcon ={
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Image(painter = painterResource(id = R.drawable.hide ), contentDescription = "password visibility",modifier = Modifier.size(24.dp))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))

        //forgot password
        Text(
            text = "Forgot Password?",
            color = Color.Blue,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    if(email.isNotEmpty()&&email.endsWith("tarc.edu.my")){
                        resetPassword(email,context,
                            onSuccess = {
                                showDialog = false
                                showEmailDialog = true},
                            onFailure = {
                                showDialog = false
                            }
                        )
                    }else{
                        Toast.makeText(context,"Please enter your email first",Toast.LENGTH_SHORT).show()
                    }
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty() && email.endsWith("tarc.edu.my")) {
                    showDialog = true
                    signInWithEmailPassword(email, password, firebaseAuth, context, navController,
                        onMfaRequired = { resolver ->
                            showDialog = false
                            Toast.makeText(context, "MFA Required. Please Verify", Toast.LENGTH_SHORT).show()
                            handleMultiFactorAuthentication(resolver, firebaseAuth, navController, context,type)
                        },
                        onFailure = { error ->
                            showDialog = false
                            errormsg = error
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        },type
                    )
                } else {
                    errormsg = "Please fill in all fields to proceed!"
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
        Text(text = "$errormsg",color = Color.Red , textAlign = TextAlign.Center)

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

        Spacer(modifier = Modifier.height(16.dp))

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
    LoadingDialog(text="Logging in...",showDialog = showDialog, onDismiss = { showDialog = false })
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
        TextButton(
            onClick = { navController.navigate("customerServiceTARUMTID") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
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
    var filePath by remember { mutableStateOf("") }

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
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
            showDialog = true
            val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            detectFaceFromIdCard(originalBitmap) { faceBitmap ->
                if (faceBitmap != null) {
                    // Save the face image
                    profilePicture = faceBitmap
                    // Save the face image to cache
                    filePath = saveBitmapToCache(context, faceBitmap, "profile_picture.png")
                }else{
                    filePath = "Error"
                }
            }
            extractInfoFromIdCard(context, uri) { extractedName, extractedId, TARUMT ->
                name = extractedName
                studentId = extractedId
                validTARUMT = TARUMT
            }
            if(validTARUMT == "Verified" || validTARUMT == "Error"){
                showDialog = false
            }
        }
        if(validTARUMT == "Verified" && filePath != ""){
            validTARUMT = ""
            val encodedFilePath = Uri.encode(filePath) // Encode the file path
            navController.navigate("signupScreen/$name/$studentId/$encodedFilePath")
        }else if(validTARUMT == "Error"){
            validTARUMT = ""
            filePath = ""
            navController.navigate("signupFailed")
        }else if(filePath == "Error"){
            validTARUMT = ""
            filePath = ""
            navController.navigate("signupFailedFace")
        }
        Text(
            text = "All information is processed by AI and store securely in our database.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show Loading Dialog
        LoadingDialog(text = "Extracting Data from ID...", showDialog = showDialog, onDismiss = { showDialog = false })
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
        Text(text = "Upload")
    }
}


@Composable
fun SignUpScreen(navController: NavController, name: String, studentId: String, imagePath: String) {
    var email by remember { mutableStateOf("") }
    var reEmail by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+60") }
    var password by remember { mutableStateOf("") }
    var rePassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") } // Default to "M"
    var isEmailValid by remember { mutableStateOf(true) }
    var isReEmailValid by remember { mutableStateOf(false) }
    var isRePasswordValid by remember { mutableStateOf(true) }
    var duplicateEmail by remember { mutableStateOf(false) }
    var isPhoneValid by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf(name) }
    var userid by remember { mutableStateOf(studentId.replace('O', '0')) }
    val context = LocalContext.current
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly
    val firebaseAuth = FirebaseAuth.getInstance() // Firebase Authentication instance

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    // Password TextField with Visibility Toggle
    var passwordVisible by remember { mutableStateOf(false) }

//     Load Bitmap from the file path
    val profilePicture: Bitmap? = if (imagePath.isNotEmpty()) {
        BitmapFactory.decodeFile(File(imagePath).absolutePath)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "2. Fill in Additional Information",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(36.dp))

        if (profilePicture != null) {
            Image(
                bitmap = profilePicture.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .border(5.dp, Color.Gray)
            )
        }
        Spacer(modifier = Modifier.height(26.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it.uppercase()
            },
            label = { Text("Name", color = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        OutlinedTextField(
            value = userid,
            onValueChange = {
                userid = it.uppercase()
            },
            label = { Text("Student ID") },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
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
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (!isEmailValid) {
            Text(text = "Only email ending with tarc.edu.my is accepted", color = Color.Red, fontSize = 12.sp)
        }
        if (duplicateEmail) {
            Text(text = "Error! This Email has been registered before, Try Again!", color = Color.Red, fontSize = 12.sp)
        }
        OutlinedTextField(
            value = reEmail,
            onValueChange = {
                reEmail = it
                if (reEmail == email){
                    isReEmailValid = true
                }else{
                    isReEmailValid = false
                }
            },
            label = { Text("Retype Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (reEmail.isNotEmpty() && !isReEmailValid) {
            Text(text = "Email doesn't Match, Try Again !", color = Color.Red, fontSize = 12.sp)
        }

        // Phone Number Field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                isPhoneValid = phoneNumber.startsWith("+601") && phoneNumber.length <= 13 && phoneNumber.length >= 12
            },
            label = { Text("Phone Number") },
            isError = !isPhoneValid,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (!isPhoneValid) {
            Text(text = "Invalid Malaysian phone number", color = Color.Red, fontSize = 12.sp)
        }

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            trailingIcon ={
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Image(painter = painterResource(id = R.drawable.hide ), contentDescription = "password visibility",modifier = Modifier.size(24.dp))
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        Text(text = "Contain at least 6 alphabet")

        // Password Field
        OutlinedTextField(
            value = rePassword,
            onValueChange = {
                rePassword = it
                if(rePassword == password){
                    isRePasswordValid = true
                }else{
                    isRePasswordValid = false
                }
            },
            label = { Text("Retype Password") },
            trailingIcon ={
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Image(painter = painterResource(id = R.drawable.hide ), contentDescription = "password visibility",modifier = Modifier.size(24.dp))
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (rePassword.isNotEmpty() && !isRePasswordValid) {
            Text(text = "Password doesn't Match, Try Again !", color = Color.Red, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Gender Selection (Switch)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Align everything to the start
        ) {
            Text(text = "Gender", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(50.dp) // Space options evenly
            ) {
                Row(
                    modifier = Modifier.clickable { gender = "M" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gender == "M",
                        onClick = { gender = "M" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Blue,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(text = "Male")
                }
                Row(
                    modifier = Modifier.clickable { gender = "F" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gender == "F",
                        onClick = { gender = "F" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Blue,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(text = "Female")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        Button(
            onClick = {
                // Validate form inputs
                if (email.isNotEmpty() && isEmailValid && phoneNumber.isNotEmpty() && isPhoneValid
                    && userName.isNotEmpty() && userid.isNotEmpty() && password.isNotEmpty() && profilePicture != null
                    && isReEmailValid && isRePasswordValid)
                {
                    showDialog = true
                    performSignUp(firebaseAuth, firestore, email,
                        password, userid, userName, phoneNumber, gender, profilePicture,
                        context, navController, { showDialog = false }
                    )
                }else{
                    Toast.makeText(context, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Sign Up", color = Color.White)
        }
    }  

    // Show Loading Dialog
    LoadingDialog(text="Uploading..." , showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun UnableToVerifyScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to Verify",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your ID couldn't be verified at the moment. Please ensure the ID is TAR UMT ID and try again later or contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("signup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("customerServiceTARUMTID") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun UnableToVerifyFace(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to extract face from ID",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.profile_error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your ID couldn't be verified at the moment. Please ensure your face on ID is sharp and clear and without obstacle. Try again later or contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("signup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("customerServiceTARUMTID") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}
@Composable
fun EmailVerify(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Account Registered!",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.completed_icon), // Replace with your error image resource
            contentDescription = "Complete Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Account has been successfully registered, please verify your email to continue, Thank You. ",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("login")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Login", color = Color.White)
        }
    }
}

@Composable
fun UnableToVerifyDuplicateID(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Duplicate ID Detected",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your ID couldn't be verified at the moment. Please ensure the student ID is typed correctly and try again later. \n\n SHARide only allows student ID that are not currently registered on our platform., Contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("signup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("customerServiceTARUMTID") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun CustomerServiceScreen(navController: NavController) {
    val context = LocalContext.current
    val firebaseStorage = FirebaseStorage.getInstance()
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly

    // User inputs
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") } // Default to "M"

    // Image upload URIs
    var studentIdUri by remember { mutableStateOf<Uri?>(null) }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }

    var isEmailValid by remember { mutableStateOf(true) }
    var isPhoneValid by remember { mutableStateOf(true) }

    // Generate a unique case ID
    val caseId = UUID.randomUUID().toString()

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Spacer(modifier = Modifier.height(36.dp))

        Text("Manual ID Verification",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please Fill in all the field to submit the form")
        Spacer(modifier = Modifier.height(36.dp))

        // Personal Details
        Text("1. Personal Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.uppercase() },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it.uppercase() },
            label = { Text("Your Student ID (Eg.22WMR10099)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                isEmailValid = email.endsWith("tarc.edu.my")
            },
            label = { Text("Your Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (!isEmailValid) {
            Text(text = "Only email ending with tarc.edu.my is accepted", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                isPhoneValid = phone.startsWith("01") && phone.length <= 11
            },
            label = { Text("Your Phone") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        if (!isPhoneValid) {
            Text(text = "Invalid Malaysian phone number", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gender Selection (Switch)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Align everything to the start
        ) {
            Text(text = "Gender", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(50.dp) // Space options evenly
            ) {
                Row(
                    modifier = Modifier.clickable { gender = "M" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gender == "M",
                        onClick = { gender = "M" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Blue,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(text = "Male")
                }
                Row(
                    modifier = Modifier.clickable { gender = "F" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gender == "F",
                        onClick = { gender = "F" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Blue,
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(text = "Female")
                }
            }
        }
        if(name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty() && studentId.isNotEmpty()){
            Text(text = "Done",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .background(color = Color.Green)
                    .padding(10.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Upload ID
        Text("2. Upload ID",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Please Upload the front of your TARUMT ID for verification purpose", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.tarumt_id),
            contentDescription = "TARUMT ID image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the ID\n" +
                "2.The Whole ID is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            studentIdUri =  uri
        }

        if (studentIdUri != null) {
            Image(
                painter = rememberAsyncImagePainter(studentIdUri), // Load from URL
                contentDescription = "Student ID",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { studentIdUri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Upload Selfie
        Text("3. Selfie with your ID",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Please Upload your selfie with the ID", modifier = Modifier.fillMaxWidth())
        //Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.selfie_tarumt),
            contentDescription = "TARUMT ID image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the selfie\n" +
                "2.The Whole Face and ID is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            selfieUri =  uri
        }
        if (selfieUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selfieUri), // Load from URL
                contentDescription = "Selfie",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { selfieUri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

//      Submit Button
        Button(
            onClick = {
                if (name.isNotEmpty() && phone.isNotEmpty() && email.isNotEmpty() && studentId.isNotEmpty() && studentIdUri != null && selfieUri != null) {
                    // Show loading dialog
                    showDialog = true

                    uploadImagesAndSaveData(
                        context = context,
                        firestore = firestore,
                        firebaseStorage = firebaseStorage,
                        caseId = caseId,
                        name = name,
                        phone = phone,
                        email = email,
                        gender = gender,
                        studentId = studentId,
                        studentIdUri = studentIdUri!!,
                        selfieUri = selfieUri!!,
                        onUploadComplete = {
                            // Dismiss loading dialog
                            showDialog = false
                            navController.navigate("ReportSubmitted/intro")
                            Toast.makeText(context, "Data uploaded successfully!", Toast.LENGTH_LONG).show()
                        },
                        onError = {
                            // Dismiss loading dialog
                            showDialog = false
                            Toast.makeText(context, "Failed to upload data.", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Please fill all the fields and upload both images", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Submit")
        }

        // Show Loading Dialog
        LoadingDialog(text= "Uploading..." , showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

@Composable
fun LoadingDialog(text:String, showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.Blue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$text", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ReportSubmitted(navController: NavController,link:String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Request Submitted",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.completed_icon), // Replace with your error image resource
            contentDescription = "Complete Icon",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Request is submitted, please wait 3-5 Working days for manual verification, once verified we will update to you via email.\n\n" +
                    "Thanks for choosing SHARide",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                if (link == "intro"){
                    navController.navigate("intro")
                }else{
                    navController.navigate("home")
                }
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Login", color = Color.White)
        }
    }
}

//Driver Verification
@Composable
fun DriverIntroScreen(navController: NavController) {

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
            text = "SHARide Driver",
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp,
            color = Color.Blue
        )

        Spacer(modifier = Modifier.height(30.dp))
        Column (
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            // Rotating Image
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.intro3),
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
                text = "Your Car, Your Community",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.Blue,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Drive, connect, and earn – be part of the SHARide community now with few simple steps!",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate("driversignup") // Navigate to Sign Up
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
fun DriverSignupIntroScreen(navController: NavController) {
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
                title = "Verify your driving lesen",
                description = "Scan and Upload your Valid Driving Lesen to verify"
            )
            StepCard(
                stepNumber = "2",
                title = "Fill in your vehicle information",
                description = "Fill in all required information of your vehicle details to get started"
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
                navController.navigate("driversignup1")
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
        Spacer(modifier = Modifier.height(60.dp))
        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("driverCustomerService") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun DriverIdVerificationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var validLesen by remember { mutableStateOf("") }
    val context = LocalContext.current
    var profilePicture by remember { mutableStateOf<Bitmap?>(null) }
    var filePath by remember { mutableStateOf("") }

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }

    var idbitmap by remember { mutableStateOf<Bitmap?>(null) }

    var drivingLesen by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(120.dp))

        Text(
            text = "1. Verify Your Driving Lesen",
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please Upload the front of your driving lesen for verification purpose",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.drivinglesen),
            contentDescription = "Driving Lesen ID image",
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
            showDialog = true
            val originalBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            detectFaceFromIdCard(originalBitmap) { faceBitmap ->
                if (faceBitmap != null) {
                    // Save the face image
                    profilePicture = faceBitmap
                    // Save the face image to cache
                    filePath = saveBitmapToCache(context, faceBitmap, "profile_picture.png")
                }else{
                    filePath = "Error"
                }
            }
            drivingLesen = saveBitmapToCache(context, originalBitmap, "driving_lesen.png")

            extractInfoFromDrivingId(context, uri) { extractedName, extractedId, Lesen ->
                name = extractedName
                studentId = extractedId
                validLesen = Lesen
            }
            if(validLesen == "Verified" || validLesen == "Error"){
                showDialog = false
            }
        }
        if(validLesen == "Verified" && filePath != ""){
            validLesen = ""
            val encodedFilePath = Uri.encode(filePath) // Encode the file path
            val encodedDrivingPath = Uri.encode(drivingLesen) // Encode the file path

            navController.navigate("driversignupscreen/$studentId/$encodedDrivingPath/$encodedFilePath")
        }else if(validLesen == "Error"){
            validLesen = ""
            filePath = ""
            navController.navigate("driverFailed")
        }else if(filePath == "Error"){
            validLesen = ""
            filePath = ""
            navController.navigate("driverFailedFace")
        }
        Text(
            text = "All information is processed by AI and store securely in our database.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show Loading Dialog
        LoadingDialog(text = "Extracting Data from ID...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

fun extractInfoFromDrivingId(
    context: Context,
    imageUri: Uri,
    onResult: (String, String, String) -> Unit // Name, Student ID, Profile Picture
) {
    val inputImage = InputImage.fromFilePath(context, imageUri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            if (!extractedText.contains("DRIVING LICENCE", ignoreCase = true)||!extractedText.contains("MALAYSIA", ignoreCase = true)) {
                onResult("", "", "Error") // Not a valid TARUMT ID
                return@addOnSuccessListener
            }

            var name = ""
            var studentId = ""

            // Loop through the text blocks and lines to extract Name and Student ID
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val text = line.text

                    if (Regex("[0-9]{12}").containsMatchIn(text)){
                        studentId = text // Matches Student ID pattern
                    }

                    // Check if the line is likely a name
                    if (text.all { it.isLetter() || it.isWhitespace() } && text.contains(" ")) {
                        if (!text.contains("WARGANEGARA", ignoreCase = true) &&
                            !text.contains("ADDRESS", ignoreCase = true) &&
                            !text.contains("ALAMAT", ignoreCase = true) &&
                            !text.contains("TEMPAT", ignoreCase = true)) {
                            name = text
                        }
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

@Composable
fun DriverFailed(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to Verify",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your ID couldn't be verified at the moment. Please ensure the ID is valid Malaysian Driving Licence and try again later or contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("driversignup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("driverCustomerService") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun UnableToVerifyDriverFace(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to extract face from ID",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.profile_error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your ID couldn't be verified at the moment. Please ensure your face on ID is sharp and clear and without obstacle. Try again later or contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("driversignup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = { navController.navigate("driverCustomerService") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun DriverSignUpScreen(navController: NavController, drivingid: String, lesen: String, imagePath: String) {
    val context = LocalContext.current
    val firebaseStorage = FirebaseStorage.getInstance()
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly
    val firebaseAuth = FirebaseAuth.getInstance()
    var selfieUri by remember { mutableStateOf<Uri?>(null) }

    // User inputs
    var name by remember { mutableStateOf("") }
    var driverid by remember { mutableStateOf(drivingid) }

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }

    // Get logged-in user's ID
    val userId = firebaseAuth.currentUser?.uid ?: "unknown_user"

    //     Load Bitmap from the file path
    val profilePicture: Bitmap? = if (imagePath.isNotEmpty()) {
        BitmapFactory.decodeFile(File(imagePath).absolutePath)
    } else {
        null
    }

    //     Load Bitmap from the file path
    val lesen: Bitmap? = if (lesen.isNotEmpty()) {
        BitmapFactory.decodeFile(File(lesen).absolutePath)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "2. Fill in Additional Information",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Fill in all required information of your lesen details to get started", textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))

        Spacer(modifier = Modifier.height(36.dp))

        if (profilePicture != null) {
            Image(
                bitmap = profilePicture.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .border(5.dp, Color.Gray)
            )
        }
        Spacer(modifier = Modifier.height(38.dp))

        // Personal Details
        Text("1. Personal Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.uppercase() },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = driverid,
            onValueChange = { driverid = it.uppercase() },
            label = { Text("Your Driving ID (Eg.030101141999)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        Spacer(modifier = Modifier.height(18.dp))

        if(name.isNotEmpty() && driverid.isNotEmpty()){
            Text(text = "Done",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .background(color = Color.Green)
                    .padding(10.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(38.dp))

        // Personal Details
        Text("2. Selfie with your Driving Lesen",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Please take a selfie with your Driving Lesen for verification purpose, make sure your face is fully exposed with your Driving Lesen together ")

        //Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.selfie_driver),
            contentDescription = "TARUMT ID image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the selfie\n" +
                "2.The Whole Face and ID is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            selfieUri =  uri
        }
        if (selfieUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selfieUri), // Load from URL
                contentDescription = "Selfie",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { selfieUri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

//      Submit Button
        Button(
            onClick = {
                if (name.isNotEmpty() && driverid.isNotEmpty() && selfieUri != null) {
                    // Show loading dialog
                    showDialog = true

                    firestore.collection("driver")
                        .whereEqualTo("drivingId", driverid)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                // Upload lesen and profile picture to Firebase Storage
                                uploadToFirebaseStorage(userId, "Driving Lesen",
                                    "lesen.png", lesen, firebaseStorage
                                ) { lesenUrl ->
                                    uploadToFirebaseStorage(
                                        userId, "Driving Lesen", "profile_picture.png",
                                        profilePicture, firebaseStorage
                                    ) { selfieUrl ->
                                        val selfieBitmap = MediaStore.Images.Media.getBitmap(
                                            context.contentResolver,
                                            selfieUri
                                        )
                                        uploadToFirebaseStorage(
                                            userId, "Driving Lesen", "selfie.png",
                                            selfieBitmap, firebaseStorage
                                        ) { profileUrl ->
                                            // Save data to Firestore
                                            saveDriverToFirestore(userId, name,
                                                driverid, profileUrl, lesenUrl,
                                                selfieUrl, firestore
                                            ) {
                                                showDialog = false
                                                navController.navigate("addnewcar") // Navigate to the next screen
                                            }
                                        }
                                    }
                                }
                            } else {
                                showDialog = false
                                navController.navigate("duplicateDrivingID")
                            }
                        }
                }else{
                    Toast.makeText(context, "Please fill up all the details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Submit")
        }

        // Show Loading Dialog
        LoadingDialog(text= "Uploading..." , showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

@Composable
fun UnableToVerifyDuplicateDrivingID(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Duplicate Driving ID Detected",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Driving ID couldn't be verified at the moment. Please ensure the driving ID is typed correctly and try again later. \n\n SHARide only allows driving ID that are not currently registered on our platform, Contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("driversignup1")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = {
                navController.navigate("driverCustomerService")
                },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun DuplicateVehicle(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Duplicate Vehicle Detected",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.error), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Vehicle couldn't be verified at the moment. Please ensure the vehicle registration number is typed correctly and try again later. \n\n SHARide only allows vehicles that are not currently registered on our platform, Contact our customer service for assistance.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // "Try Again" Button
        Button(
            onClick = {
                navController.navigate("addnewcar") // Navigate to the next screen
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Try Again", color = Color.White)
        }
        //Spacer(modifier = Modifier.height(1.dp))

        // "Contact Customer Service" Button
        TextButton(
            onClick = {
                navController.navigate("driverCustomerService")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }
    }
}

@Composable
fun AddNewVehicle(navController: NavController) {
    val context = LocalContext.current
    val firebaseStorage = FirebaseStorage.getInstance()
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly

    // User inputs
    var carmake by remember { mutableStateOf("") }
    var carmodel by remember { mutableStateOf("") }
    var carcolor by remember { mutableStateOf("") }
    var registrationnum by remember { mutableStateOf("") }

    // Image upload URIs
    var carfronturi by remember { mutableStateOf<Uri?>(null) }
    var carbackuri by remember { mutableStateOf<Uri?>(null) }

    // Generate a unique case ID
    val caseId = UUID.randomUUID().toString()

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    val firebaseAuth = FirebaseAuth.getInstance()
    var verificationStatus by remember { mutableStateOf("Pending Verification") }
    var backVerificationStatus by remember { mutableStateOf("Pending Verification") }

    // Get logged-in user's ID
    val userId = firebaseAuth.currentUser?.uid ?: "unknown_user"

    //load user privious image
    var existingcarfrontUrl by remember { mutableStateOf<String?>(null) }
    var existingcarbackUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        showDialog = true
        loadVehicleData(userId) { vehicle ->
            vehicle?.let {
                carmake = it.carMake
                carmodel = it.carModel
                carcolor = it.carColor
                registrationnum = it.registrationNum
                existingcarfrontUrl = it.carFrontPhoto
                existingcarbackUrl = it.carBackPhoto
            }
            showDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Spacer(modifier = Modifier.height(56.dp))

        Text("3. Add New Vehicle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please Fill in all the field to register your vehicle")
        Spacer(modifier = Modifier.height(36.dp))

        // Personal Details
        Text("1. Vehicle Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carmake,
            onValueChange = { carmake = it.uppercase() },
            label = { Text("Car Make (Eg.Perodua) ") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carmodel,
            onValueChange = { carmodel = it.uppercase() },
            label = { Text("Car Model (Eg. Axia)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carcolor,
            onValueChange = { carcolor = it.uppercase() },
            label = { Text("Car Colour (Eg. Black)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = registrationnum,
            onValueChange = { registrationnum = it.uppercase() },
            label = { Text("Registration Number (Eg. VJQ 9999)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if(carmake.isNotEmpty() && carmodel.isNotEmpty() && carcolor.isNotEmpty() && registrationnum.isNotEmpty()){
            Text(text = "Done",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .background(color = Color.Green)
                    .padding(10.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Upload ID
        Text("2. Upload Your Vehicle Photo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Please take photos with your registered car for verification purpose, make sure car registration number plate is fully exposed with your car together ", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Front of the vehicle", fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(id = R.drawable.car_front),
            contentDescription = "Car Front Image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the vehicle\n" +
                "2.The Whole Registration Number is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            carfronturi =  uri
        }

        if(existingcarfrontUrl != null){
            Image(
                painter = rememberAsyncImagePainter(existingcarfrontUrl),
                contentDescription = "Front Car Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { existingcarfrontUrl = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        if (carfronturi != null) {
            existingcarfrontUrl = null
            detectPlateNumber(context, carfronturi!!, registrationnum) { isMatch, message ->
                verificationStatus = message
            }

            Image(
                painter = rememberAsyncImagePainter(carfronturi),
                contentDescription = "Front Car Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (verificationStatus.contains("Matched")) Color.Green else Color.Red,),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = verificationStatus,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { carfronturi = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Back of the vehicle", fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(id = R.drawable.car_back),
            contentDescription = "Car Back Image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the vehicle\n" +
                "2.The Whole Registration Number is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            carbackuri =  uri
        }
        if(existingcarbackUrl != null){
            Image(
                painter = rememberAsyncImagePainter(existingcarbackUrl),
                contentDescription = "Front Car Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { existingcarbackUrl = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }
        if (carbackuri != null) {
            existingcarbackUrl = null
            detectPlateNumber(context, carbackuri!!, registrationnum) { isMatch, message ->
                backVerificationStatus = message
            }
            carbackuri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Front Car Image",
                    modifier = Modifier
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (backVerificationStatus.contains("Matched")) Color.Green else Color.Red),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = backVerificationStatus,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { carbackuri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

//      Submit Button
        Button(
            onClick = {
                showDialog = true

                if(
                    carmake.isNotEmpty() && carmodel.isNotEmpty() && carcolor.isNotEmpty()
                    && registrationnum.isNotEmpty() && carfronturi != null && carbackuri != null
                    && backVerificationStatus.contains("Matched") && verificationStatus.contains("Matched")
                ){
                    handleVehicleSubmission(
                        context = context,
                        navController = navController,
                        userId = userId,
                        caseId = caseId,
                        carMake = carmake,
                        carModel = carmodel,
                        carColor = carcolor,
                        registrationNum = registrationnum,
                        carFrontUri = carfronturi,
                        carBackUri = carbackuri,
                        verificationStatus = verificationStatus,
                        backVerificationStatus = backVerificationStatus,
                        onComplete = { showDialog = false }
                    )
                }else{
                    showDialog = false
                    Toast.makeText(context, "Please fill up all the details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Submit")
        }

        TextButton(
            onClick = {
                navController.navigate("driverCustomerService")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Contact Customer Service", color = Color.Blue, fontSize = 16.sp)
        }

        // Show Loading Dialog
        LoadingDialog(text= "Uploading..." , showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

@Composable
fun DriverSuccess(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Registered Successful",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.completed_icon), // Replace with your error image resource
            contentDescription = "Complete Icon",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Thanks for being a part of SHARide Community, you are ready to go as a driver!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(34.dp))

        // Button
        Button(
            onClick = {
                navController.navigate("home")
            }, // Navigate to the verification screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Done", color = Color.White)
        }
    }
}

@Composable
fun DriverCustomerService(navController: NavController) {
    val context = LocalContext.current
    val firebaseStorage = FirebaseStorage.getInstance()
    val firestore = Firebase.firestore // Ensure Firebase Firestore is set up correctly

    // User inputs
    var name by remember { mutableStateOf("") }
    var driverid by remember { mutableStateOf("") }
    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var IDUri by remember { mutableStateOf<Uri?>(null) }
    var carmake by remember { mutableStateOf("") }
    var carmodel by remember { mutableStateOf("") }
    var carcolor by remember { mutableStateOf("") }
    var registrationnum by remember { mutableStateOf("") }

    // Image upload URIs
    var carfronturi by remember { mutableStateOf<Uri?>(null) }
    var carbackuri by remember { mutableStateOf<Uri?>(null) }

    // Generate a unique case ID
    val caseId = UUID.randomUUID().toString()

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    val firebaseAuth = FirebaseAuth.getInstance()

    // Get logged-in user's ID
    val userId = firebaseAuth.currentUser?.uid ?: "unknown_user"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Spacer(modifier = Modifier.height(56.dp))

        Text("Manual Verification",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please Fill in all the field to submit the form.")

        Spacer(modifier = Modifier.height(36.dp))

        // Personal Details
        Text("1. Driver Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.uppercase() },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = driverid,
            onValueChange = { driverid = it.uppercase() },
            label = { Text("Your Driving ID (Eg.030101141999)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )
        Spacer(modifier = Modifier.height(38.dp))

        // Personal Details
        Text("2. Upload Driving Lesen",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Please Upload the front of your Driving Lesen for verification purpose")

        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.drivinglesen),
            contentDescription = "Driving ID image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the driving lesen\n" +
                "2.The Whole Face and ID is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            IDUri =  uri
        }
        if (IDUri != null) {
            Image(
                painter = rememberAsyncImagePainter(IDUri), // Load from URL
                contentDescription = "ID Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { IDUri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(38.dp))

        // Personal Details
        Text("3. Selfie with your Driving Lesen",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Please take a selfie with your Driving Lesen for verification purpose, make sure your face is fully exposed with your Driving Lesen together ")

        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.selfie_driver),
            contentDescription = "TARUMT ID image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the selfie\n" +
                "2.The Whole Face and ID is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            selfieUri =  uri
        }
        if (selfieUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selfieUri), // Load from URL
                contentDescription = "Selfie",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { selfieUri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        // Personal Details
        Text("4. Vehicle Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carmake,
            onValueChange = { carmake = it.uppercase() },
            label = { Text("Car Make (Eg.Perodua) ") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carmodel,
            onValueChange = { carmodel = it.uppercase() },
            label = { Text("Car Model (Eg. Axia)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = carcolor,
            onValueChange = { carcolor = it.uppercase() },
            label = { Text("Car Colour (Eg. Black)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = registrationnum,
            onValueChange = { registrationnum = it.uppercase() },
            label = { Text("Registration Number (Eg. VJQ 9999)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Blue, // Blue border when focused
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color.Blue,
                focusedLabelColor = Color.Blue,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if(carmake.isNotEmpty() && carmodel.isNotEmpty() && carcolor.isNotEmpty() && registrationnum.isNotEmpty()){
            Text(text = "Done",
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .background(color = Color.Green)
                    .padding(10.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Upload ID
        Text("5. Upload Your Vehicle Photo",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Please take photos with your registered car for verification purpose, make sure car registration number plate is fully exposed with your car together ", modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Front of the vehicle", fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(id = R.drawable.car_front),
            contentDescription = "Car Front Image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the vehicle\n" +
                "2.The Whole Registration Number is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            carfronturi =  uri
        }
        if (carfronturi != null) {
            Image(
                painter = rememberAsyncImagePainter(carfronturi), // Load from URL
                contentDescription = "Front Car Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { carfronturi = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Back of the vehicle", fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(id = R.drawable.car_back),
            contentDescription = "Car Back Image",
            modifier = Modifier
                .fillMaxWidth()
                .size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please Make Sure:\n" +
                "1.No obstacle or blur image of the vehicle\n" +
                "2.The Whole Registration Number is visible and center in the picture ",
            modifier = Modifier.fillMaxWidth())
        UploadIdButton { uri ->
            carbackuri =  uri
        }
        if (carbackuri != null) {
            Image(
                painter = rememberAsyncImagePainter(carfronturi), // Load from URL
                contentDescription = "Front Car Image",
                modifier = Modifier
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // Ensures the "Done" text takes available space
                )

                IconButton(onClick = { carbackuri = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

//      Submit Button
        Button(
            onClick = {
                showDialog = true

                if(IDUri != null && name.isNotEmpty() && driverid.isNotEmpty() && selfieUri != null && carmake.isNotEmpty() && carmodel.isNotEmpty() && carcolor.isNotEmpty() && registrationnum.isNotEmpty() && carfronturi != null && carbackuri != null){
                    uploadDriverData(firebaseStorage, firestore, caseId,
                        name, driverid, IDUri!!, selfieUri!!, carmake,
                        carmodel, carcolor, registrationnum, carfronturi!!,
                        carbackuri!!, userId, context, navController,
                        onUploadFinished = { showDialog = false }
                    )
                }else{
                    showDialog = false
                    Toast.makeText(context, "Please fill up all the details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Submit")
        }

        // Show Loading Dialog
        LoadingDialog(text= "Uploading..." , showDialog = showDialog, onDismiss = { showDialog = false })
    }
}
