package com.kxxr.sharmin.screen

import android.app.Activity
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Login.ResolverHolder
import com.kxxr.logiclibrary.Login.handleGoogleSignIn
import com.kxxr.logiclibrary.Login.handleMultiFactorAuthentication
import com.kxxr.logiclibrary.Login.resetPassword
import com.kxxr.logiclibrary.Login.signInWithEmailPassword
import com.kxxr.logiclibrary.Login.verifyOtp
import com.kxxr.logiclibrary.ManualCase.sendEmail
import com.kxxr.logiclibrary.Network.NetworkViewModel
import com.kxxr.sharmin.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdminAppNavHost() {
    val firebaseAuth = FirebaseAuth.getInstance()
    val navController = rememberNavController()
    val startDestination = if (firebaseAuth.currentUser != null) "home" else "login"

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
        composable("login") { AdminLoginScreen(navController, firebaseAuth) }
        composable("home") { AdminHome(firebaseAuth, navController)}
        composable("check_mfa") { CheckMfaEnrollment(firebaseAuth, navController) }
        composable("reg_otp") { RegisterPhoneNumberScreen(firebaseAuth, navController) }
        composable("verifyOtp/{verifyID}/{phone}/{route}") { backStackEntry ->
            val verifyID = backStackEntry.arguments?.getString("verifyID").orEmpty()
            val phone = backStackEntry.arguments?.getString("phone").orEmpty()
            val route = backStackEntry.arguments?.getString("route").orEmpty()

            VerifyOtpScreen(navController, verifyID,firebaseAuth,phone,route)
        }
        composable("banned_user/{userId}/{remark}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val remark = backStackEntry.arguments?.getString("remark") ?: ""
            BannedUserScreen(navController,userId,remark)
        }
        //ewallet settings
        composable("generate_pin") { GenerateTopupPinScreen(navController) }
        composable("search_user/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            SearchUserScreen(navController,type)
        }
        composable("adjust_bal/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()

            AdjustBalanceScreen(navController, userId)
        }

        //user settings
        composable("reviewUserScreen") { ReviewUserScreen(navController) }
        composable("caseDetail/{caseId}") { backStackEntry ->
            val caseId = backStackEntry.arguments?.getString("caseId") ?: ""
            CaseDetailScreen(navController, caseId)
        }
        composable("reviewDriverScreen") { ReviewDriverScreen(navController) }
        composable("driverCaseDetail/{caseId}") { backStackEntry ->
            val caseId = backStackEntry.arguments?.getString("caseId") ?: ""
            DriverCaseDetailScreen(navController, caseId)
        }
        composable("user_details/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()

            UserDetailScreen(navController, userId)
        }
        composable("ban_user/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()

            BanUserScreen(navController, userId)
        }
    }
}
@Composable
fun BannedUserScreen(navController: NavController, userId: String, remark: String) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val emailSubject = "Dispute of Banned User - User ID: $userId"
    val emailBody = """
        Dear SHARide Team,

        I would like to dispute my account ban due to a policy violation.

        **User ID:** $userId

        **Reason for Violation:** 
        $remark

        If you believe this was an error, please review my case. 
        I have also attached any supporting documents for your consideration.

        Best regards,
        [Your Name]
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! You've been banned :(",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Image(
            painter = painterResource(id = R.drawable.ban), // Replace with your error image resource
            contentDescription = "Error Icon",
            modifier = Modifier.size(100.dp),
            colorFilter = ColorFilter.tint(Color.Red) // Apply red tint

        )
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "Your account has been banned as your previous action may violate our policy. \n If you think this is a mistake, please contact us via email",
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Reason of the ban :\n" +
                    "$remark`",
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                firebaseAuth.signOut()
                sendEmail(context,context.getString(R.string.cs_email),emailSubject,emailBody)
            },
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue))
        {
            Text("Contact Us")
        }
        TextButton(
            onClick = {
                firebaseAuth.signOut()
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Try Again", color = Color.Blue)
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
fun AdminLoginScreen(navController: NavController, firebaseAuth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var errormsg by remember { mutableStateOf("") }

    // Password TextField with Visibility Toggle
    var passwordVisible by remember { mutableStateOf(false) }

    // Dialog visibility state
    var showDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    val type = "admin"

    Box(
        modifier = Modifier
            .fillMaxSize() // Ensure Box takes the whole screen
    ){
        Image(
            painter = painterResource(id = R.drawable.admin_introbg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop, // Crop to fill the box
            modifier = Modifier.fillMaxSize() // Match the parent Box size
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Optional padding for content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Spacer(modifier = Modifier.height(70.dp))

            // Title
            Text(
                text = "SHARide \n\n Admin Backend Login",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .shadow(5.dp, shape = RoundedCornerShape(15.dp))
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White)
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column (
                    modifier = Modifier
                        .padding(16.dp)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ){
                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Sign-In Logic
                    val token = "424509601720-l27h6t59dlr9dk2t8sto3tg6lu9a7tsv.apps.googleusercontent.com"
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(token)
                        .requestEmail()
                        .build()

                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            handleGoogleSignIn(result.data, firebaseAuth, navController, context, type)
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
                        singleLine = true,
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
                                Image(
                                    painter = painterResource(id = R.drawable.hide ),
                                    contentDescription = "password visibility",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    //forgot password
                    Text(
                        text = "Forgot Password?",
                        color = Color.Blue,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                if (email.isNotEmpty() && email.endsWith("tarc.edu.my")) {
                                    resetPassword(email, context,
                                        onSuccess = {
                                            showDialog = false
                                            showEmailDialog = true
                                        },
                                        onFailure = {
                                            showDialog = false
                                        }
                                    )
                                } else {
                                    Toast
                                        .makeText(
                                            context,
                                            "Please enter your email first",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "$errormsg",color = Color.Red , textAlign = TextAlign.Center)

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
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(text = "Login", color = Color.White)
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
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


