package com.kxxr.sharide.screen

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.kxxr.sharide.DataClass.Transaction
import com.kxxr.sharide.db.PinAttemptManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EWalletIntro(navController: NavController) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmPinDialog by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var errorPIN by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = FirebaseFirestore.getInstance()

    // Fetch data from Firestore
    LaunchedEffect(Unit) {
        showDialog = true

        // Fetch balance
        if (userId != null) {
            firestore.collection("eWallet").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        navController.navigate("ewalletDashboard")
                        showDialog = false
                    }else{
                        showDialog = false
                    }
                }
        }
    }
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
            onClearPin = false,
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
            onClearPin = false,
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
    onClearPin: Boolean,  // This will trigger a PIN reset
    onDismiss: () -> Unit
) {
    var pinCode by remember { mutableStateOf("") }

    // Reset PIN when onClearPin changes
    LaunchedEffect(onClearPin) {
        pinCode = "" // Clear the PIN field
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                .background(Color.White),
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
                                            .border(2.dp, Color.Black, CircleShape),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
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
        // Reference to Firestore
        val firestore = FirebaseFirestore.getInstance()
        val documentRef = firestore.collection("eWallet").document(userId)

        // Check if document exists first
        documentRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Document already exists, return an error
                    onFailure(Exception("eWallet account already exists for this user."))
                } else {
                    // Document does not exist, proceed with creating a new one
                    val eWalletData = hashMapOf(
                        "userId" to userId,
                        "pinCode" to hashedPin,
                        "balance" to 0.00,
                        "securityQuestion1" to securityQuestion1,
                        "securityAnswer1" to securityAnswer1,
                        "securityQuestion2" to securityQuestion2,
                        "securityAnswer2" to securityAnswer2,
                        "securityQuestion3" to securityQuestion3,
                        "securityAnswer3" to securityAnswer3
                    )

                    // Store data in Firestore
                    documentRef.set(eWalletData)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onFailure(Exception("Failed to check for existing document: ${e.message}"))
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
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    showDialog = true
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
                                showDialog = false
                                navController.navigate("ewallet") // Navigate to home after saving
                            }
                            .addOnFailureListener { exception ->
                                showDialog = false
                                Toast.makeText(context, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    showDialog = false
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
    // Show Loading Dialog
    LoadingDialog(text = "Uploading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun AnswerTextField(answer: String, onAnswerChanged: (String) -> Unit) {
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChanged,
        label = { Text("Your Answer...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
            //.shadow(5.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White, shape = RoundedCornerShape(8.dp))
        ,
        shape = RoundedCornerShape(8.dp), // Rounded corners for the text field
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),

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
            .padding(start = 20.dp, end = 20.dp)
            .padding(vertical = 8.dp) // Add spacing between dropdowns
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
                    .shadow(5.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
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
                    .padding(start = 20.dp, end = 20.dp)
                    .background(Color.White)
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EWalletDashboardScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // State to hold balance and transactions
    var balance by remember { mutableStateOf(0.0) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch data from Firestore
    LaunchedEffect(Unit) {
        isLoading = true

        // Fetch balance
        if (userId != null) {
            firestore.collection("eWallet").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        balance = document.getDouble("balance") ?: 0.0
                    }
                }
        }

        if (userId != null) {
            firestore.collection("Transaction")
                .whereEqualTo("userId", userId) // Filter by userId field inside the document
                .get()
                .addOnSuccessListener { document ->
                    if (!document.isEmpty) {
                        val data = document.map { documents ->
                            val transactionData = documents.data
                            Transaction(
                                date = transactionData["date"].toString(),
                                description = transactionData["description"].toString(),
                                amount = transactionData["amount"].toString().toDouble()
                            )
                        }
                        // Sort transactions by date in descending order
                        transactions = data.sortedByDescending {
                            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(it.date)
                        }
                    } else {
                        // No transactions found for this user
                        transactions = emptyList()
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar("eWallet", navController) }
    ) { paddingValues ->
        // UI Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.Blue)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(54.dp))
            Text("SHARide eWallet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 10.dp))

            // Display Balance
            Text(
                text = "RM %.2f".format(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 25.dp)
                    .fillMaxWidth()
            )

        }

        Column (
            modifier = Modifier
                .width(600.dp)
                .height(350.dp)
                .padding(top = 200.dp, start = 25.dp, end = 25.dp)
                .shadow(5.dp, shape = RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
        ){
            // Action Buttons
            Row(
                modifier = Modifier
                    .width(600.dp)
                    .height(300.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically

                ) {
                ActionButton("TOP UP\n","topup") { navController.navigate("topup")}
                ActionButton("Change Payment PIN","change_pin") { navController.navigate("changePIN") }
                ActionButton("Security Question","security") {  navController.navigate("reset_question")}
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 380.dp)
                .verticalScroll(rememberScrollState()), // Enable scrolling
        ) {
            // Transaction History Section
            Text("Transaction History", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(15.dp))

           if (transactions.isEmpty()) {
                Text("No Transaction History", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp))
            } else {
                transactions.forEach { transaction ->
                    TransactionItem(transaction)
                }
            }

            Spacer(modifier = Modifier.height(130.dp))
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = isLoading, onDismiss = { isLoading = false })

}

@Composable
fun ActionButton(label: String, img: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageResId = remember(img) {
        context.resources.getIdentifier(img, "drawable", context.packageName)
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .width(100.dp) // Set a fixed width for consistent alignment
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Box ensures the image stays aligned consistently
        Box(
            modifier = Modifier
                .size(70.dp) // Fixed size for image box
                .padding(bottom = 5.dp) // Space between image and text
                .shadow(4.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = label,
                    modifier = Modifier.size(48.dp) // Image size inside the box
                )
            } else {
                Text("Image Not Found", color = Color.Red, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        // Text expands downward without affecting the image alignment
        Text(
            label,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
    
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val color = if (transaction.amount >= 0) Color.Green else Color.Red
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(transaction.description, fontWeight = FontWeight.Bold)
                Text(transaction.date, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = if (transaction.amount >= 0) "+ RM %.2f".format(transaction.amount) else "- RM %.2f".format(-transaction.amount),
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TopUpScreen(navController: NavController) {
    var balance by remember { mutableStateOf(0.00)}
    var isLoading by remember { mutableStateOf(false) }
    var topUpPin by remember { mutableStateOf("") }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Fetch data from Firestore
    LaunchedEffect(Unit) {
        isLoading = true

        // Fetch balance
        if (userId != null) {
            firestore.collection("eWallet").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        balance = document.getDouble("balance") ?: 0.0
                        isLoading = false
                    }else{
                        isLoading = false
                    }
                }
        }
    }
    Scaffold(
        bottomBar = { BottomNavBar("eWallet", navController) }
    ) { paddingValues ->
        // UI Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color.Blue)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(54.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("ewalletDashboard") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "TOP UP",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Display Balance
            Text(
                text = "Current Balance",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .padding(top = 40.dp)
                    .fillMaxWidth()
            )
            Text(
                text = "RM %.2f".format(balance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 280.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Transaction History Section
            Text("Please Enter Your Reload PIN", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(15.dp))

            OutlinedTextField(
                value = topUpPin,
                onValueChange = {
                    topUpPin = it.filter { char -> char.isDigit() } // Allow only digits (0-9)
                },
                label = { Text("Enter Top Up Pin...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )


            Image(
                painter = painterResource(id = R.drawable.reload_pin),
                contentDescription = "Reload PIN",
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.CenterHorizontally), // Image size inside the box
            )

            // Top-Up Button
            Button(
                onClick = {
                    isLoading = true
                    if (userId != null) {
                        validateTopUpPin(
                            userId = userId,
                            pin = topUpPin,
                            onSuccess = { newbalance,amount ->
                                navController.navigate("topupsuccess/$amount/$newbalance")
                                isLoading = false
                            },
                            onFailure = { error ->
                                isLoading = false
                                topUpPin = ""
                                Toast.makeText(context, "Oops! $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }, modifier = Modifier
                    .width(300.dp)
                    .height(50.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ){
                Text("Top Up", color = Color.White)
            }
        }

    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = isLoading, onDismiss = { isLoading = false })

}

fun validateTopUpPin(
    userId: String,
    pin: String,
    onSuccess: (String,String) -> Unit,
    onFailure: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("Topup")
        .whereEqualTo("TopupPIN", pin)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val document = documents.documents[0]
                val amount = document.get("amount")?.toString()?.toDoubleOrNull() ?: 0.0

                // Valid PIN found, update user balance
                updateUserBalance(userId, amount
                    ,onSuccess = { balance ->
                        deleteReloadPIN(pin
                            ,onSuccess = { message->
                                recordTransaction(userId, amount,"Top Up","add")
                                onSuccess(balance, amount.toString())
                            }
                            ,onFailure={ error ->
                                onFailure(error)
                            }
                        )
                    }
                    ,onFailure={ error ->
                        onFailure(error)
                    }
                )
            } else {
                onFailure("Invalid Reload PIN") // Invalid PIN
            }
        }
        .addOnFailureListener {
            onFailure("Error Validating PIN")
        }
}

fun updateUserBalance(userId: String, amount: Double,onSuccess: (String) -> Unit,onFailure: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    val userRef = firestore.collection("eWallet").document(userId)
    userRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val currentBalance = document.getDouble("balance") ?: 0.0
            userRef.update("balance", currentBalance + amount)
            val balance = currentBalance + amount
            onSuccess(balance.toString())
        }
    }.addOnFailureListener{
        onFailure("Error Update Balance")
    }
}

fun deleteReloadPIN(pin: String,onSuccess: (String) -> Unit,onFailure: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("Topup")
        .whereEqualTo("TopupPIN", pin) // Find the document with this PIN
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                firestore.collection("Topup").document(document.id)
                    .delete()
                    .addOnSuccessListener {
                        onSuccess("Success")
                        Log.d("Firestore", "Successfully deleted the reload PIN")
                    }
                    .addOnFailureListener { e ->
                        onFailure("Error Delete reload PIN")
                        Log.e("Firestore", "Error deleting document", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error finding document", e)
        }
}

fun recordTransaction(userId: String, amount: Double, description: String, operator: String) {
    val firestore = FirebaseFirestore.getInstance()
// Set Malaysia Time Zone (MYT)
    val timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    sdf.timeZone = timeZone

    val currentDate = Timestamp.now() // Firestore's Timestamp
    val formattedDate = sdf.format(currentDate.toDate()) // Format with MYT Time Zone

    // Format amount with operator
    val formattedAmount = when (operator.lowercase()) {
        "add" -> "+${"%.2f".format(amount)}"
        "minus" -> "-${"%.2f".format(amount)}"
        else -> "%.2f".format(amount) // No operator, just the amount
    }

    // Prepare transaction data
    val transaction = hashMapOf(
        "userId" to userId,
        "date" to formattedDate, // Store Timestamp
        "amount" to formattedAmount,
        "description" to description
    )

    // Add transaction to Firestore
    firestore.collection("Transaction")
        .add(transaction)
        .addOnSuccessListener {
            println("Transaction Recorded Successfully")
        }
        .addOnFailureListener { e ->
            println("Failed to Record Transaction: ${e.message}")
        }
}



@Composable
fun TopUpSuccessScreen(navController: NavController, topUpAmount: Double, newBalance: Double) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(180.dp))

        // Success Icon
        Image(
            painter = painterResource(id = R.drawable.completed_icon),
            contentDescription = "Complete ICON",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(56.dp))

        // Top Up Amount Text
        Text(
            text = "+ RM %.2f".format(topUpAmount),
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Success Message
        Text(
            text = "TOP UP SUCCESSFUL",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        // **Pushes everything above up**
        Spacer(modifier = Modifier.weight(1f))

        // Updated Balance Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.Blue,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(24.dp)
                .height(200.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Current Balance",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "RM %.2f".format(newBalance),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Done Button
                Button(
                    onClick = { navController.navigate("ewalletDashboard") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .width(300.dp)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ChangePaymentPIN(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    // Read max attempts from strings.xml
    val maxAttempts = stringResource(id = R.string.max_pin_attempts).toInt()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(true) }
    var attemptsLeft by remember { mutableStateOf(3 - PinAttemptManager.getAttempts(context)) }
    var clearPinTrigger by remember { mutableStateOf(false) } // To trigger PIN reset

    LaunchedEffect(Unit) {
        if (PinAttemptManager.isLockedOut(context)) {
            navController.navigate("resetPIN") // Redirect if already locked out
        }
    }

    if (showPinDialog) {
        PinInputDialog(
            title = "Enter Current PIN",
            description = buildString {
                append("Please enter your current payment PIN to proceed.")
                if (attemptsLeft < 3) {
                    append("\n\nYou have $attemptsLeft attempts left.")
                }
            },
            onPinEntered = { enteredPin ->
                verifyCurrentPin(
                    context = context,
                    userId = userId,
                    enteredPin = enteredPin,
                    onSuccess = {
                        PinAttemptManager.resetAttempts(context) // Reset attempt count
                        showPinDialog = false
                        navController.navigate("updatePIN") // Navigate to new PIN setup
                    },
                    onFailure = { error ->
                        attemptsLeft = maxAttempts - PinAttemptManager.getAttempts(context) // Update attempts left
                        errorMessage = error
                        clearPinTrigger = !clearPinTrigger
                    },
                    onLockout = {
                        navController.navigate("resetPIN") // Navigate to reset PIN screen
                    }
                )
            },
            onClearPin = clearPinTrigger,
            onDismiss = { navController.popBackStack() }
        )
    }

    errorMessage?.let { error ->
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        errorMessage = null
    }
}


fun verifyCurrentPin(
    context: Context,
    userId: String,
    enteredPin: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
    onLockout: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val hashedPin = hashPin(enteredPin)

    firestore.collection("eWallet")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val storedHashedPin = document.getString("pinCode") ?: ""

                if (storedHashedPin == hashedPin) {
                    PinAttemptManager.resetAttempts(context) // Reset attempts on success
                    onSuccess()
                } else {
                    PinAttemptManager.incrementAttempts(context) // Increment attempt count

                    if (PinAttemptManager.isLockedOut(context)) {
                        onLockout() // Lock the user out after 3 failed attempts
                    } else {
                        val attemptsLeft = 3 - PinAttemptManager.getAttempts(context)
                        onFailure("Incorrect PIN. You have $attemptsLeft attempts left.")
                    }
                }
            } else {
                onFailure("User data not found.")
            }
        }
        .addOnFailureListener { e ->
            onFailure("Error: ${e.localizedMessage}")
        }
}

@Composable
fun ResetPinScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var securityQuestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var correctAnswers by remember { mutableStateOf<List<String>>(emptyList()) }
    var userAnswers = remember { mutableStateListOf("", "", "") }
    var showDialog by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    // Fetch security questions from Firestore
    LaunchedEffect(userId) {
        showDialog = true
        firestore.collection("eWallet").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    securityQuestions = listOf(
                        document.getString("securityQuestion1") ?: "",
                        document.getString("securityQuestion2") ?: "",
                        document.getString("securityQuestion3") ?: ""
                    )
                    correctAnswers = listOf(
                        document.getString("securityAnswer1") ?: "",
                        document.getString("securityAnswer2") ?: "",
                        document.getString("securityAnswer3") ?: ""
                    )
                }
                showDialog = false
            }
            .addOnFailureListener {
                showDialog = false
                Toast.makeText(context, "Error fetching security questions", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { navController.navigate("ewalletDashboard") } ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Text(
            text = "Reset Payment PIN",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.Black
        )

        Text(
            text = "Answer your security questions to reset your PIN.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        securityQuestions.forEachIndexed { index, question ->
            Text(question, fontWeight = FontWeight.Bold)
            AnswerTextField(
                answer = userAnswers[index],
                onAnswerChanged = { userAnswers[index] = it }
            )
            Spacer(modifier = Modifier.height(26.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (validateAnswers(userAnswers, correctAnswers)) {
                    navController.navigate("updatePIN") // Navigate to PIN reset screen
                } else {
                    Toast.makeText(context, "Incorrect answers. Try again.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text(text = "Verify", color = Color.White, fontSize = 18.sp)
        }
    }

    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

fun validateAnswers(userAnswers: List<String>, correctAnswers: List<String>): Boolean {
    return userAnswers.map { hashAnswer(it) } == correctAnswers
}

@Composable
fun UpdatePinScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var showPinDialog by remember { mutableStateOf(true) }
    var showConfirmPinDialog by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var errorPIN by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var clearPIN by remember { mutableStateOf(false) }

    // Step 1: User enters the first PIN
    if (showPinDialog) {
        PinInputDialog(
            title = if (errorPIN) "Error! PINs do not match\n\nCreate PIN" else "Create PIN",
            description = "Please create a 6-digit PIN for your eWallet",
            onPinEntered = { enteredPin ->
                firstPin = enteredPin
                showPinDialog = false
                showConfirmPinDialog = true // Move to confirmation step
            },
            onClearPin = clearPIN,
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
                    updateUserPin(userId, confirmedPin,
                        onSuccess = {
                            Toast.makeText(context, "PIN updated successfully!", Toast.LENGTH_SHORT).show()
                            showDialog = false
                            showConfirmPinDialog = false
                            PinAttemptManager.resetAttempts(context) // Reset attempt count
                            navController.navigate("ewalletDashboard") // Navigate after successful update
                        },
                        onFailure = { exception ->
                            Toast.makeText(context, "Failed to update PIN: ${exception.message}", Toast.LENGTH_SHORT).show()
                            showDialog = false
                            showConfirmPinDialog = false
                        }
                    )
                } else {
                    Toast.makeText(context, "PINs do not match! Try again.", Toast.LENGTH_SHORT).show()
                    showConfirmPinDialog = false
                    errorPIN = true
                    showPinDialog = true // Restart PIN entry
                }
            },
            onClearPin = clearPIN,
            onDismiss = {
                showConfirmPinDialog = false
            }
        )
    }

    // Loading Dialog
    LoadingDialog(text = "Updating PIN...", showDialog = showDialog, onDismiss = { showDialog = false })
}

fun updateUserPin(userId: String, newPin: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val hashedPin = hashPin(newPin) // Hash PIN before storing

    val userRef = firestore.collection("eWallet").document(userId)

    userRef.update("pinCode", hashedPin)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

@Composable
fun ResetSecurityQuestions(navController: NavController) {
    val securityQuestions = stringArrayResource(id = R.array.security_questions).toList()
    val selectedQuestions = remember { mutableStateListOf<String?>(null, null, null) }
    val answers = remember { mutableStateListOf("", "", "") }
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var showDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(true) }
    var pinVerified by remember { mutableStateOf(false) }

    // Read max attempts from strings.xml
    val maxAttempts = stringResource(id = R.string.max_pin_attempts).toInt()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attemptsLeft by remember { mutableStateOf(3 - PinAttemptManager.getAttempts(context)) }
    var clearPinTrigger by remember { mutableStateOf(false) } // To trigger PIN reset

    // PIN Verification Dialog
    if (showPinDialog) {
        PinInputDialog(
            title = "Verify Your PIN",
            description = buildString {
                append("Please enter your current payment PIN to reset security questions.")
                if (attemptsLeft < 3) {
                    append("\n\nYou have $attemptsLeft attempts left.")
                }
            },
            onPinEntered = { enteredPin ->
                if (userId != null) {
                    verifyCurrentPin(
                        context = context,
                        userId = userId,
                        enteredPin = enteredPin,
                        onSuccess = {
                            PinAttemptManager.resetAttempts(context) // Reset attempt count
                            showPinDialog = false
                            pinVerified = true

                            // Fetch Current Security Questions
                            firestore.collection("eWallet").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    selectedQuestions[0] = document.getString("securityQuestion1")
                                    selectedQuestions[1] = document.getString("securityQuestion2")
                                    selectedQuestions[2] = document.getString("securityQuestion3")
                                }
                        },
                        onFailure = { error ->
                            pinVerified = false
                            attemptsLeft = maxAttempts - PinAttemptManager.getAttempts(context) // Update attempts left
                            errorMessage = error
                            clearPinTrigger = !clearPinTrigger
                        },
                        onLockout = {
                            navController.navigate("resetPIN") // Navigate to reset PIN screen
                        }
                    )
                }
            },
            onClearPin = clearPinTrigger,
            onDismiss = {
                navController.popBackStack()
            }
        )
    }

    if (pinVerified) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text(
                text = "Reset Security Questions",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.Black
            )

            Text(
                text = "Select new security questions and provide new answers.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display Dropdowns and Answer Fields
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

            // Submit Button
            Button(
                onClick = {
                    if (selectedQuestions.all { it != null } && answers.all { it.isNotBlank() }) {
                        showDialog = true
                        val questionsWithHashedAnswers = selectedQuestions.mapIndexed { index, question ->
                            question!! to hashAnswer(answers[index])
                        }.toMap()

                        val dataToUpdate = hashMapOf(
                            "securityQuestion1" to questionsWithHashedAnswers.keys.elementAt(0),
                            "securityAnswer1" to questionsWithHashedAnswers.values.elementAt(0),
                            "securityQuestion2" to questionsWithHashedAnswers.keys.elementAt(1),
                            "securityAnswer2" to questionsWithHashedAnswers.values.elementAt(1),
                            "securityQuestion3" to questionsWithHashedAnswers.keys.elementAt(2),
                            "securityAnswer3" to questionsWithHashedAnswers.values.elementAt(2)
                        )

                        // Update Firestore Without Overwriting Other Fields
                        firestore.collection("eWallet").document(userId!!)
                            .update(dataToUpdate as Map<String, Any>)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Security Questions Reset Successfully!", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                navController.navigate("ewallet")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to reset questions: ${e.message}", Toast.LENGTH_SHORT).show()
                                showDialog = false
                            }
                    } else {
                        Toast.makeText(context, "Please fill out all questions.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .height(90.dp)
                    .width(180.dp)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) {
                Text("Reset", color = Color.White, fontSize = 18.sp)
            }
        }
    }

    // Loading Dialog
    LoadingDialog(text = "Resetting...", showDialog = showDialog, onDismiss = { showDialog = false })
}

