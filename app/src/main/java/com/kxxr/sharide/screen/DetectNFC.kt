package com.kxxr.sharide.screen

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun DetectNFC(navController: NavController, NFCData: String?, name: String, studentId: String, imagePath: String, onComplete: () -> Unit) {
    val hasNavigated = remember { mutableStateOf(false) }

    LaunchedEffect(NFCData) {
        if (NFCData != null) {
            // Reset navigation flag on new NFC data
            hasNavigated.value = false
            NFCData?.let { data ->
                if (!hasNavigated.value) {
                    val trimmedData = data.trim()
                    val first10Chars = trimmedData.take(10)
                    val encodedFilePath = Uri.encode(imagePath) // Encode the file path

                    // If student
                    if (NFCData.contains(studentId.replace('O', '0'), ignoreCase = true) || NFCData.contains(studentId.replace('0', 'O'), ignoreCase = true) ) {
                        hasNavigated.value = true
                        val updatedId = first10Chars
                        navController.navigate("signupScreen/$name/$updatedId/$encodedFilePath")
                        onComplete()
                    }
                    // If staff
                    else if (studentId.equals("STAFF", ignoreCase = true)) {
                        val staffId = trimmedData.takeWhile { it.isDigit() }
                        if (staffId.isNotEmpty()) {
                            hasNavigated.value = true
                            navController.navigate("signupScreen/$name/$staffId/$encodedFilePath")
                            onComplete()
                        } else {
                            hasNavigated.value = true
                            navController.navigate("signupFailed")
                            onComplete()
                        }
                    }
                    // Invalid
                    else {
                        hasNavigated.value = true
                        navController.navigate("signupFailed")
                        onComplete()
                    }
                }
            }
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
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "NFC Verification",
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please enable NFC on your phone and tap your TARUMT ID at the back of the phone for verification purpose.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        MatchingIndicator("NFC")

        Text(
            text = "Please Make Sure : \n1. Enable Your NFC in Phone Settings\n" +
                    "2. Tap Your TARUMT ID at the back of your phone",
            fontSize = 16.sp,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "All information is processed by AI and store securely in our database.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
