package com.kxxr.sharide.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Banned.isBanned
import com.kxxr.logiclibrary.Login.ResolverHolder
import com.kxxr.logiclibrary.Login.sendOtp
import com.kxxr.logiclibrary.Login.verifyOtp
import com.kxxr.logiclibrary.ManualCase.sendEmail
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.sharide.R


@Composable
fun RegisterPhoneNumberScreen(firebaseAuth: FirebaseAuth, navController: NavController) {
    var phoneNumber by remember { mutableStateOf("+60") }
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    var showDialog by remember { mutableStateOf(false) }

    // Retrieve Phone Number based on UserId Field inside the Document
    LaunchedEffect(Unit) {
        showDialog = true
        val firebaseUserId = firebaseAuth.currentUser?.uid
        if (firebaseUserId != null) {
            // Search for Document where "userId" field equals to firebaseUserId
            firestore.collection("users")
                .whereEqualTo("firebaseUserId", firebaseUserId) // 🔥 Filter by userId field
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0] // Get the first document
                        phoneNumber = document.getString("phoneNumber") ?: "+60"

                        // Auto-Validation when PhoneNumber is fetched
                        if (!phoneNumber.startsWith("+60")) {
                            error = "Invalid phone number, should start with +60"
                        }
                        showDialog = false
                    } else {
                        error = "Phone number not found for this user."
                        showDialog = false
                    }
                }
                .addOnFailureListener { e ->
                    error = "Failed to retrieve phone number: ${e.localizedMessage}"
                    showDialog = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { navController.navigate("profile")}) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text(
            "Register Phone Number for \nMulti-Factor Authentication",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                // Validate immediately while typing
                if (!phoneNumber.startsWith("+60")) {
                    error = "Invalid phone number, should start with +60"
                } else if (phoneNumber.length <= 11 && phoneNumber.length >= 10){
                    error = "Invalid Malaysia Number"
                } else {
                    error = ""
                }
            },
            label = { Text("Enter Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) {
            Text(text = error, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                showDialog = true
                if (phoneNumber.isNotEmpty() && error.isEmpty()) {
                    sendOtp(phoneNumber, firebaseAuth, context, navController, onResult = {
                        if (it != null) {
                            error = it
                            showDialog = false
                        }
                    })
                } else {
                    showDialog = false
                    Toast.makeText(context, "Please enter phone number!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Register", color = Color.White)
        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}



@Composable
fun VerifyOtpScreen(navController: NavController, verificationId: String, firebaseAuth: FirebaseAuth,phoneNumber: String,route:String) {
    var otp by remember { mutableStateOf("") }
    val context = LocalContext.current
    val resolver = ResolverHolder.resolver

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("OTP sent successful !", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter the One Time Passcode sent to \n $phoneNumber via SMS", fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(26.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
        ) {
            Text("Enter OTP", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
            //Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (otp.isNotEmpty()) {
                    if(route == "Register"){
                        verifyOtp(verificationId, otp, firebaseAuth, context, navController)
                    }else{
                        if (resolver != null) {
                            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                            val assertion = PhoneMultiFactorGenerator.getAssertion(credential)

                            resolver.resolveSignIn(assertion)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "MFA Verified Successfully", Toast.LENGTH_SHORT).show()
                                    isBanned(FirebaseFirestore.getInstance(),firebaseAuth.currentUser?.uid ?: "",
                                        onResult = { remark,banned ->
                                            if(banned){
                                                navController.navigate("banned_user/${firebaseAuth.currentUser?.uid ?: ""}/$remark")
                                            }else{
                                                navController.navigate("home")
                                            }
                                        }
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Invalid OTP: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Resolver is null", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Enter OTP!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Verify", color = Color.White)
        }
    }
}
@Composable
fun ShowUnbindDialog(phoneNumber: String, onUnbind: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color.White, // Force White Background
        title = { Text("MFA Already Enabled", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("This account is already registered with $phoneNumber.\nDo you want to unbind MFA?",textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Button(
                onClick = onUnbind,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Unbind")
            }
        },
        dismissButton = {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CheckMfaEnrollment(firebaseAuth: FirebaseAuth, navController: NavController) {
    val context = LocalContext.current
    val user = firebaseAuth.currentUser
    var showDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    val enrolledFactors = user?.multiFactor?.enrolledFactors ?: emptyList()
    var errormsg by remember { mutableStateOf("") }


    LaunchedEffect(user) {
        if (user != null) {
            if (enrolledFactors.isNotEmpty()) {
                val phoneInfo = enrolledFactors[0] as? PhoneMultiFactorInfo
                if (phoneInfo != null) {
                    phoneNumber = phoneInfo.phoneNumber ?: ""
                    showDialog = true // Show Unbind Dialog
                }
            } else {
                navController.navigate("reg_otp") // No MFA -> Go to Register Screen
            }
        } else {
            Toast.makeText(context, "User Not Logged In", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topPadding = (0.3f * screenHeight.value).dp

    Text(
        text = errormsg,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color.Red,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
    )

    if (showDialog) {
        ShowUnbindDialog(
            phoneNumber = phoneNumber,
            onUnbind = {
                if (user != null) {
                    user.multiFactor.unenroll(enrolledFactors[0]) // 🔥 Direct Unbind
                        .addOnSuccessListener {
                            Toast.makeText(context, "MFA Unbound Successfully", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Navigate back on success
                        }
                        .addOnFailureListener { e ->
                            errormsg = "Failed to Unbind MFA: ${e.localizedMessage}"
                        }
                }
            },
            onCancel = {
                showDialog = false
                navController.navigate("profile") // Go back to Check MFA()
            }
        )
    }
}

@Composable
fun BannedUserScreen(navController: NavController, userId: String, remark: String) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    var username by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loadUserDetails(FirebaseFirestore.getInstance(), userId) { user ->
            if (user != null) {
                username = user.name
                userEmail = user.email
            }
        }
        firebaseAuth.signOut()
    }

    val emailSubject = "Dispute of Banned User - User : $username"
    val emailBody = """
        Dear SHARide Team,

        I would like to dispute my account ban due to a policy violation.

        **User Name :** $username
        **User Email :** $userEmail


        **Reason for Violation:** 
        $remark

        I believe this was an error, please review my case. 
        I have also attached any supporting documents for your consideration.

        Best regards,
        $username
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                firebaseAuth.signOut()
                sendEmail(context,context.getString(R.string.cs_email),emailSubject,emailBody)
            },
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
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
