package com.kxxr.logiclibrary.Login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.Banned.isBanned
import java.util.concurrent.TimeUnit

fun handleGoogleSignIn(
    data: Intent?,
    firebaseAuth: FirebaseAuth,
    navController: NavController,
    context: Context,
    type: String,
    onComplete: (String) -> Unit
) {
    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
    try {
        val account = task.getResult(ApiException::class.java)!!
        val googleCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        val email = account.email ?: ""

        // Step 1: Allow Only @tarc.edu.my Emails
        if (!email.endsWith("tarc.edu.my")) {
            Toast.makeText(context, "Only TARC emails are allowed", Toast.LENGTH_SHORT).show()
            firebaseAuth.signOut()
            navController.navigate("login")
            onComplete("TARUMT EMAIL ERROR")
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Step 2: Check if User Exists in Firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    firebaseAuth.signOut()
                    Toast.makeText(
                        context, "Account does not exist. Please register first.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete("ACCOUNT NOT EXIST")
                    return@addOnSuccessListener
                }

                // Step 3: Proceed to Sign-In with Google Credential
                firebaseAuth.signInWithCredential(googleCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            if (user != null) {
                                if (user.isEmailVerified) {
                                    isBanned(FirebaseFirestore.getInstance(),user.uid, onResult = { remark,banned ->
                                        if(banned){
                                            firebaseAuth.signOut()
                                            onComplete("BANNED")
                                            navController.navigate("banned_user/${user.uid}/$remark")
                                        }else{
                                            if (type == "admin") {
                                                checkIfAdmin(user.uid, db, firebaseAuth, navController, context)
                                                onComplete("ADMIN")
                                            } else {
                                                Toast.makeText(context, "Sign-In Successful", Toast.LENGTH_LONG).show()
                                                navController.navigate("home")
                                                onComplete("SIGN IN SUCCESSFULLY")
                                            }
                                        }
                                    })
                                } else {
                                    firebaseAuth.signOut()
                                    Toast.makeText(
                                        context, "Please verify your email before signing in.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onComplete("VERIFY EMAIL")
                                }
                            }
                        } else {
                            val exception = task.exception
                            if (exception is FirebaseAuthMultiFactorException) {
                                // MFA required – trigger the resolver
                                Toast.makeText(context, "MFA Required. Please Verify.", Toast.LENGTH_LONG).show()
                                handleMultiFactorAuthentication(exception.resolver, firebaseAuth, navController, context, type)
                                onComplete("MFA")
                            } else {
                                Toast.makeText(context, "Login Failed: ${exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                onComplete("ERROR")
                            }
                        }
                    }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to check account existence", Toast.LENGTH_LONG).show()
            }
    } catch (e: ApiException) {
        Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// Helper Function: Check if User is Admin
private fun checkIfAdmin(
    userId: String,
    db: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
    navController: NavController,
    context: Context
) {
    db.collection("Admin")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                isBanned(FirebaseFirestore.getInstance(),userId, onResult = { remark,banned ->
                    if(banned){
                        firebaseAuth.signOut()
                        navController.navigate("banned_user/${userId}/$remark")
                    }else{
                        Toast.makeText(context, "Admin Sign-In Successful", Toast.LENGTH_LONG).show()
                        navController.navigate("home")
                    }
                })
            } else {
                firebaseAuth.signOut()
                Toast.makeText(context, "You're not an Admin!", Toast.LENGTH_LONG).show()
                navController.navigate("login")
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to check admin status", Toast.LENGTH_LONG).show()
        }
}

fun handleMultiFactorAuthentication(
    resolver: MultiFactorResolver,
    firebaseAuth: FirebaseAuth,
    navController: NavController,
    context: Context,
    type: String
) {
    try {
        val activity = context as? Activity ?: throw Exception("Invalid Context")

        if (resolver.hints.isEmpty()) {
            Toast.makeText(context, "No Phone Number Enrolled for MFA", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneInfo = resolver.hints[0] as? PhoneMultiFactorInfo
            ?: throw Exception("PhoneMultiFactorInfo is null")

        ResolverHolder.resolver = resolver // Store Resolver Here

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setActivity(activity)
            .setMultiFactorSession(resolver.session) // Must Set Session
            .setMultiFactorHint(phoneInfo) // Use MultiFactor Hint Here
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
                    resolver.resolveSignIn(assertion)
                        .addOnSuccessListener {
                            Toast.makeText(context, "MFA Verified Successfully", Toast.LENGTH_SHORT).show()
                            navController.navigate("home")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "MFA Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(context, "OTP Verification Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Toast.makeText(context, "OTP Sent to ${phoneInfo.phoneNumber}", Toast.LENGTH_SHORT).show()
                    val route = "Login"
                    navController.navigate("verifyOtp/$verificationId/${phoneInfo.phoneNumber}/$route") // Optional Navigate to OTP Screen
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

    } catch (e: Exception) {
        Toast.makeText(context, "Unexpected Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun signInWithEmailPassword(
    email: String,
    password: String,
    firebaseAuth: FirebaseAuth,
    context: Context,
    navController: NavController,
    onMfaRequired: (MultiFactorResolver) -> Unit,
    onFailure: (String) -> Unit,
    type: String
) {
    firebaseAuth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                // Step 4: Check if User Already Exists in Firestore by userId FIELD
                val db = FirebaseFirestore.getInstance()

                user?.reload()?.addOnSuccessListener {
                    if (user.isEmailVerified) {
                        if(type == "admin"){
                            db.collection("Admin")
                                .whereEqualTo("userId", user.uid) // Check if userId field exists
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (!documents.isEmpty) {
                                        isBanned(FirebaseFirestore.getInstance(),user.uid, onResult = { remark,banned ->
                                            if(banned){
                                                firebaseAuth.signOut()
                                                navController.navigate("banned_user/${user.uid}/$remark")
                                            }else{
                                                Toast.makeText(context, "Sign-In Successful", Toast.LENGTH_LONG).show()
                                                navController.navigate("home")
                                            }
                                        })
                                    }else{
                                        firebaseAuth.signOut()
                                        Toast.makeText(context, "You're not an Admin!", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to check account existence", Toast.LENGTH_LONG).show()
                                }
                        }else{
                            isBanned(FirebaseFirestore.getInstance(),user.uid, onResult = { remark,banned ->
                                if(banned){
                                    firebaseAuth.signOut()
                                    navController.navigate("banned_user/${user.uid}/$remark")
                                }else{
                                    Toast.makeText(context, "Sign-In Successful", Toast.LENGTH_LONG).show()
                                    navController.navigate("home")
                                }
                            })
                        }
                    } else {
                        firebaseAuth.signOut()
                        onFailure("Please verify your email before logging in!")
                    }
                }?.addOnFailureListener { e ->
                    onFailure("Error checking email verification: ${e.message}")
                }
            }
        }
        .addOnFailureListener { exception ->
            when (exception) {
                is FirebaseAuthMultiFactorException -> {
                    // Prompt user for Multi-Factor Authentication
                    onMfaRequired(exception.resolver)
                }

                is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> {
                    onFailure("Invalid Credentials! Try again.")
                }

                else -> {
                    onFailure("Login failed: ${exception.localizedMessage}")
                }
            }
        }
}

fun resetPassword(email:String,context: Context, onSuccess: () -> Unit,onFailure: () -> Unit){
    val firebaseAuth = FirebaseAuth.getInstance()

    val trimmedEmail = email.trim() // Remove unnecessary spaces
    if (trimmedEmail.isNotEmpty()) {
        Toast
            .makeText(context, "Checking email...", Toast.LENGTH_SHORT)
            .show()
        firebaseAuth
            .sendPasswordResetEmail(trimmedEmail)
            .addOnCompleteListener { resetTask ->
                if (resetTask.isSuccessful) {
                    Toast.makeText(context, "Password reset email sent successfully!", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    Log.e("ResetPassword", "Error sending reset email", resetTask.exception)
                    Toast.makeText(context, "Error: ${resetTask.exception?.message}", Toast.LENGTH_LONG).show()
                    onFailure()
                }
            }
    }
}
