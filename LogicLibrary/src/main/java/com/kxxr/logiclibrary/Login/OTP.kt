package com.kxxr.logiclibrary.Login

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import java.util.concurrent.TimeUnit

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
                val route = "Register"
                navController.navigate("verifyOtp/$verificationId/$phoneNumber/$route")
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
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
