package com.kxxr.sharide.screen

import android.annotation.SuppressLint
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.kxxr.sharide.R
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.input.KeyboardType


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EWalletIntro(navController: NavController) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmPinDialog by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var errorPIN by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavBar("eWallet", navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Blue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to \n\nSHARide eWallet",
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ewallet_intro),
                contentDescription = "EWallet Intro",
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ditch the Cash, Glide with Ease \n\nSimplify Carpool Payments with Our New eWallet Payment system",
                fontSize = 21.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(54.dp))

            // "Create PIN" Button
            Button(
                onClick = {
                    showPinDialog = true
                },
                modifier = Modifier
                    .width(300.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(text = "Create PIN", color = Color.Blue)
            }

            Spacer(modifier = Modifier.height(66.dp))
        }
    }

    // Step 1: User enters the first PIN
    if (showPinDialog) {
        PinInputDialog(
            title = if(errorPIN){"Error! PIN do not match\n\nCreate PIN"}else{"Create PIN"},
            description = "Please create a 6-digit PIN to associate with your eWallet",
            onPinEntered = { enteredPin ->
                firstPin = enteredPin
                showPinDialog = false
                showConfirmPinDialog = true // Move to confirmation step
            },
            onDismiss = {
                showPinDialog = false
            }
        )
    }

    // Step 2: User confirms the PIN
    if (showConfirmPinDialog) {
        PinInputDialog(
            title = "Confirm PIN",
            description = "Please re-enter your 6-digit PIN",
            onPinEntered = { confirmedPin ->
                if (confirmedPin == firstPin) {
                    showDialog = true
                    // Store in Firestore with security questions
                    storeEWalletData(
                        pin = confirmedPin,
                        securityQuestion1 = "", securityAnswer1 = "",
                        securityQuestion2 = "", securityAnswer2 = "",
                        securityQuestion3 = "", securityAnswer3 = "",
                        onSuccess = {
                            Toast.makeText(context, "PIN set and saved successfully!", Toast.LENGTH_SHORT).show()
                            println("eWallet data stored securely.")
                            // Navigate to the home screen
                            showConfirmPinDialog = false
                            showDialog = false
                            navController.navigate("security_question") // Navigate after successful setup
                        },
                        onFailure = { exception ->
                            Toast.makeText(context, "Failed to save PIN: ${exception.message}", Toast.LENGTH_SHORT).show()
                            showDialog = false
                            showConfirmPinDialog = false
                        }
                    )
                    showConfirmPinDialog = false
                } else {
                    Toast.makeText(context, "PINs do not match! Try again.", Toast.LENGTH_SHORT).show()
                    showConfirmPinDialog = false
                    errorPIN = true
                    showPinDialog = true // Restart PIN entry
                }
            },
            onDismiss = {
                showConfirmPinDialog = false
            }
        )
    }

    // Show Loading Dialog
    LoadingDialog(text = "Creating...", showDialog = showDialog, onDismiss = { showDialog = false })
}


@Composable
fun PinInputDialog(
    title: String,
    description: String,
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp)), // Black outline
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), // White background
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Title & Description
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.Black)

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Display
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, Color.Black, RoundedCornerShape(8.dp)) // Black border
                                .background(Color.White), // White fill
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < pinCode.length) {
                                Text(text = "•", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numpad
                Column {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                if (key.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            if (key == "⌫") {
                                                pinCode = pinCode.dropLast(1)
                                            } else if (pinCode.length < 6) {
                                                pinCode += key
                                            }
                                            if (pinCode.length == 6) {
                                                onPinEntered(pinCode)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(60.dp)
                                            .border(2.dp, Color.Black, CircleShape), // Black outline
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White) // White fill
                                    ) {
                                        Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(60.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(pin.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

fun storeEWalletData(
    pin: String,
    securityQuestion1: String, securityAnswer1: String,
    securityQuestion2: String, securityAnswer2: String,
    securityQuestion3: String, securityAnswer3: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId != null) {
        val hashedPin = hashPin(pin)

        // Data to be stored in Firestore
        val eWalletData = hashMapOf(
            "userId" to userId,
            "pinCode" to hashedPin,
            "securityQuestion1" to securityQuestion1,
            "securityAnswer1" to securityAnswer1,
            "securityQuestion2" to securityQuestion2,
            "securityAnswer2" to securityAnswer2,
            "securityQuestion3" to securityQuestion3,
            "securityAnswer3" to securityAnswer3
        )

        // Upload data to Firestore
        FirebaseFirestore.getInstance().collection("eWallet")
            .document(userId)
            .set(eWalletData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    } else {
        onFailure(Exception("User not authenticated"))
    }
}
fun hashAnswer(answer: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(answer.toByteArray())
    return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
}

@Composable
fun SetSecurityQuestionsScreen(navController: NavController) {
    // Load questions from XML resource
    val securityQuestions = stringArrayResource(id = R.array.security_questions).toList()

    val selectedQuestions = remember { mutableStateListOf<String?>(null, null, null) }
    val answers = remember { mutableStateListOf("", "", "") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Security Question",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color.Black
        )

        Text(
            text = "Please set up your security questions for PIN recovery. Answer any 3 questions below.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display dropdowns and answers
        for (i in 0 until 3) {
            SecurityQuestionDropdown(
                questionIndex = i,
                questions = securityQuestions.filter { it !in selectedQuestions.filterNotNull() },
                selectedQuestion = selectedQuestions[i],
                onQuestionSelected = { selectedQuestions[i] = it }
            )
            AnswerTextField(
                answer = answers[i],
                onAnswerChanged = { answers[i] = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (selectedQuestions.all { it != null } && answers.all { it.isNotBlank() }) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        // Prepare questions and securely hashed answers
                        val questionsWithHashedAnswers = selectedQuestions.mapIndexed { index, question ->
                            question!! to hashAnswer(answers[index])
                        }.toMap()

                        // Prepare data for Firestore
                        val dataToUpdate = hashMapOf(
                            "securityQuestion1" to questionsWithHashedAnswers.keys.elementAt(0),
                            "securityAnswer1" to questionsWithHashedAnswers.values.elementAt(0),
                            "securityQuestion2" to questionsWithHashedAnswers.keys.elementAt(1),
                            "securityAnswer2" to questionsWithHashedAnswers.values.elementAt(1),
                            "securityQuestion3" to questionsWithHashedAnswers.keys.elementAt(2),
                            "securityAnswer3" to questionsWithHashedAnswers.values.elementAt(2)
                        )

                        // Update Firestore under the current user's document
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("eWallet").document(userId)
                            .update(dataToUpdate as Map<String, Any>)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Security questions set successfully!", Toast.LENGTH_SHORT).show()
                                navController.navigate("home") // Navigate to home after saving
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(context, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Please answer all questions.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .height(90.dp)
                .width(180.dp)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Submit", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun AnswerTextField(answer: String, onAnswerChanged: (String) -> Unit) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChanged,
        label = { Text("Your Answer...") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionDropdown(
    questionIndex: Int,
    questions: List<String>,
    selectedQuestion: String?,
    onQuestionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp) // Add spacing between dropdowns
    ) {
        Text(
            text = "Question ${questionIndex + 1}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Using ExposedDropdownMenuBox for better dropdown behavior
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor() // Correctly anchors the dropdown
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedQuestion ?: "Select a question",
                        color = if (selectedQuestion == null) Color.Gray else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            // Proper dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(Color.White)
            ) {
                questions.forEach { question ->
                    DropdownMenuItem(
                        onClick = {
                            onQuestionSelected(question)
                            expanded = false
                        },
                        text = {
                            Text(text = question)
                        }
                    )
                }
            }
        }
    }
}
