package com.kxxr.sharide.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay

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
                    resendOtp() // Function to resend OTP
                },
                style = TextStyle(color = Color.Blue, fontSize = 14.sp, textDecoration = TextDecoration.Underline)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Login Button
        Button(
            onClick = {
                verifyOtp(context, verificationId, otpCode, navController)
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

fun verifyOtp(context: Context, verificationId: String, otpCode: String, navController: NavController) {
    val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)

    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                navController.navigate("home")
            } else {
                Toast.makeText(context, "Invalid OTP. Try again!", Toast.LENGTH_SHORT).show()
            }
        }
}

fun resendOtp() {
    // Firebase logic to resend OTP
}
