package com.kxxr.sharide.screen

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun OtpVerificationScreen(
    navController: NavController,
    verificationId: String
) {
    var otpCode by remember { mutableStateOf("") }
    var isResendEnabled by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(60) }
    val context = LocalContext.current

    // Timer for Resend OTP
    LaunchedEffect(timeLeft) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        isResendEnabled = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Back Button
        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.Start)) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
        }

        // Title
        Text(
            text = "2FA Login",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Instructions
        Text(
            text = "Verification code sent!\nEnter the verification code sent to xxx@tarc.edu.my.\nCheck your spam mail box.",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // OTP Input Fields
        OtpTextField(otpCode) { newCode -> otpCode = newCode }

        Spacer(modifier = Modifier.height(20.dp))

        // Resend Code with Timer
        if (!isResendEnabled) {
            Text(
                text = "Please wait ${timeLeft}s before requesting another code.",
                fontSize = 14.sp,
                color = Color.Gray
            )
        } else {
            ClickableText(
                text = AnnotatedString("Resend Now"),
                onClick = {
                    isResendEnabled = false
                    timeLeft = 60 // Reset timer
                    //resendOtp() // Function to resend OTP
                },
                style = TextStyle(color = Color.Blue, fontSize = 14.sp, textDecoration = TextDecoration.Underline)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Login Button
        Button(
            onClick = {
                //verifyOtp(context, verificationId, otpCode, navController)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Login", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun OtpTextField(otpCode: String, onOtpChange: (String) -> Unit) {
    val focusRequesterList = List(5) { FocusRequester() }
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(5) { index ->
            TextField(
                value = otpCode.getOrNull(index)?.toString() ?: "",
                onValueChange = { newValue ->
                    if (newValue.length <= 1) {
                        val newOtpCode = otpCode.toMutableList()
                        if (newValue.isNotEmpty()) {
                            newOtpCode[index] = newValue.first()
                        } else {
                            newOtpCode.removeAt(index)
                        }
                        onOtpChange(newOtpCode.joinToString(""))
                        if (newValue.isNotEmpty() && index < 4) {
                            focusRequesterList[index + 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .focusRequester(focusRequesterList[index])
                    .padding(4.dp),
                textStyle = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center),
                singleLine = true
            )
        }
    }
}

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
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
                if (phoneNumber.isNotEmpty() && error.isEmpty()) {
                    sendOtp(phoneNumber, firebaseAuth, context, navController, onResult = {
                        if (it != null) {
                            error = it
                        }
                    })
                } else {
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


fun sendOtp(
    phoneNumber: String,
    firebaseAuth: FirebaseAuth,
    context: Context,
    navController: NavController,
    onResult: (String?) -> Unit // Callback to return success or error
) {
    var error = ""
    val activity = context as? Activity
    if (activity == null) {
        Toast.makeText(context, "Invalid Context!", Toast.LENGTH_SHORT).show()
        return
    }

    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Toast.makeText(context, "Phone Verified Automatically", Toast.LENGTH_SHORT).show()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onResult(e.localizedMessage) // Return the error message
                Toast.makeText(context, "Verification Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Toast.makeText(context, "OTP Sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                navController.navigate("verifyOtp/$verificationId")
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

@Composable
fun VerifyOtpScreen(navController: NavController, verificationId: String, firebaseAuth: FirebaseAuth) {
    var otp by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter OTP", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            label = { Text("OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (otp.isNotEmpty()) {
                    verifyOtp(verificationId, otp, firebaseAuth, context, navController)
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

fun verifyOtp(
    verificationId: String,
    otp: String,
    firebaseAuth: FirebaseAuth,
    context: Context,
    navController: NavController
) {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)

    firebaseAuth.currentUser?.multiFactor?.enroll(
        PhoneMultiFactorGenerator.getAssertion(credential),
        "My Phone"
    )?.addOnSuccessListener {
        Toast.makeText(context, "Phone Number Registered Successfully!", Toast.LENGTH_SHORT).show()
        navController.navigate("home")
    }?.addOnFailureListener {
        Toast.makeText(context, "Failed to Register Phone: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
