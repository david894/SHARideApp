package com.kxxr.sharmin.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.loadUserDetails
import com.kxxr.logiclibrary.User.loadWalletBalance
import com.kxxr.logiclibrary.User.searchUsers
import com.kxxr.logiclibrary.eWallet.TopupPin
import com.kxxr.logiclibrary.eWallet.Transaction
import com.kxxr.logiclibrary.eWallet.deleteReloadPIN
import com.kxxr.logiclibrary.eWallet.generateTopupPins
import com.kxxr.logiclibrary.eWallet.loadAvailablePins
import com.kxxr.logiclibrary.eWallet.loadSoldPins
import com.kxxr.logiclibrary.eWallet.loadTransactionHistory
import com.kxxr.logiclibrary.eWallet.markAsSold
import com.kxxr.logiclibrary.eWallet.recordTransaction
import com.kxxr.logiclibrary.eWallet.updateUserBalance

// Main Generate Top-up PIN Screen
@Composable
fun GenerateTopupPinScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }

    // For dropdown and available PINs
    var isDropdownOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var availablePins by remember { mutableStateOf(emptyList<TopupPin>()) }
    var isSoldOpen by remember { mutableStateOf(false) }
    var soldPins by remember { mutableStateOf(emptyList<TopupPin>()) }

    var showDialog by remember { mutableStateOf(false) }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { navController.navigate("home") }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Generate Top-up PIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = "RM $amount",
            onValueChange = { newValue ->
                // Remove "RM " and validate digits only
                val numericPart = newValue.removePrefix("RM ").filter { it.isDigit() }
                amount = numericPart
            },
            label = { Text("Enter Top-up Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )


        // Quantity Control (+ and -)
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Quantity: ")

            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                Text("-")
            }

            OutlinedTextField(
                value = quantity.toString(),
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        val newQuantity = it.toIntOrNull() ?: 1
                        // Ensure quantity is between 1 and 10
                        quantity = newQuantity.coerceIn(1, 10)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)), // Floating shadow effect
            )

            // Increase Quantity (Max: 10)
            IconButton(onClick = { if (quantity < 10) quantity++ }) {
                Text("+")
            }
        }

        // Generate Button
        Button(
            onClick = {
                if (amount.isNotEmpty() && amount.toIntOrNull() != null) {
                    showDialog = true
                    generateTopupPins(firestore, amount.toInt(), quantity, context, onSuccess = {
                        showDialog = false
                        Toast.makeText(context, "All Reload PIN created successfully !", Toast.LENGTH_SHORT).show()
                    })
                } else {
                    Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
        ) {
            Text("Generate Top-up PIN(s)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(thickness = 2.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isDropdownOpen = !isDropdownOpen
                        if (isDropdownOpen) loadAvailablePins(firestore) { availablePins = it }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isDropdownOpen) "Hide Available PINs ▲ " else "Show Available PINs ▼", modifier = Modifier.padding(start = 10.dp))

        }
        if (isDropdownOpen) {
            OutlinedTextField(
                value = "RM " + searchQuery,
                onValueChange = { newValue ->
                    // Remove "RM " and validate digits only
                    val numericPart = newValue.removePrefix("RM ").filter { it.isDigit() }
                    searchQuery = numericPart
                },
                label = { Text("Search by Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        // Apply safe filtering
        val filteredPins = if (searchQuery.isBlank()) {
            availablePins
        } else {
            availablePins.filter {
                it.amount.toString().contains(searchQuery)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Available Top-up PIN List (Show All Pins)
        if (isDropdownOpen) {

            Column {
                filteredPins.forEach { pin ->
                    TopupPinItem(pin, firestore, context,"available") {
                        // Refresh the available pins after marking as sold
                        loadAvailablePins(firestore) { availablePins = it }
                    }
                }
            }
        }
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                .clickable(
                    onClick = {
                        isSoldOpen = !isSoldOpen
                        if (isSoldOpen) loadSoldPins(firestore) { soldPins = it }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(if (isSoldOpen) "Hide Sold PINs ▲ " else "Show Sold PINs ▼", modifier = Modifier.padding(start = 10.dp))
        }

        // Available Top-up PIN List (Show All Pins)
        if (isSoldOpen) {

            Column {
                soldPins.forEach { pin ->
                    TopupPinItem(pin, firestore, context,"sold") {
                        // Refresh the available pins after marking as sold
                        loadSoldPins(firestore) { availablePins = it }
                    }
                }
            }
        }
    }

    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

@Composable
fun TopupPinItem(pin: TopupPin, firestore: FirebaseFirestore, context: Context, type:String,onUpdate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
    ) {
        Column {
            // Top Section - Amount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (type == "available") Color.Blue else Color.Gray)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RM ${pin.amount}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Perforated Divider (Ticket Effect)
            Canvas(modifier = Modifier.fillMaxWidth()) {
                val circleRadius = 8.dp.toPx()
                val gap = 28.dp.toPx()

                for (i in 0..size.width.toInt() step gap.toInt()) {
                    drawCircle(
                        color = Color.LightGray,
                        radius = circleRadius,
                        center = Offset(i.toFloat(), size.height / 2)
                    )
                }
            }

            // Bottom Section - PIN and Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text("PIN: ${pin.TopupPIN}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Status: ${pin.status}", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                if (pin.status == "available") {
                    Button(
                        onClick = {
                            markAsSold(firestore, pin.TopupPIN, context, onUpdate)
                        },
                        colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Mark as Sold")
                    }
                }
            }
        }
    }
}

// Main Screen to Search and Select User
@Composable
fun SearchUserScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ){
        Spacer(modifier = Modifier.height(50.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { navController.navigate("home") }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Search User",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name, Email, or Student ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                showDialog = true
                searchUsers(firestore, searchQuery, context) {
                    users = it
                    if(users.isEmpty()){
                        Toast.makeText(context, "No user found!", Toast.LENGTH_SHORT).show()
                    }
                    showDialog = false
                }
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if(users.isNotEmpty()){
            Text("Search Results:")
        }

        Column {
            users.forEach { user ->
                UserCard(user) {
                    navController.navigate("adjust_bal/${user.firebaseUserId}")
                }
            }
        }
    }
    // Show Loading Dialog
    LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
}

// User Card Component
@Composable
fun UserCard(user: User, onSelect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardColors(containerColor = Color.White, contentColor = Color.Black, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
        //elevation = 4,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(user.profileImageUrl),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = user.email, fontSize = 15.sp)
                Text(text = "ID: ${user.studentId}", fontSize = 15.sp)
                Button(
                    onClick = onSelect,
                    colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray)
                ) {
                    Text("Select")
                }
            }
        }
    }
}

@Composable
fun AdjustBalanceScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf<User?>(null) }
    var amount by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("+") }
    var balance by remember { mutableStateOf(0.0) }
    var remark by remember { mutableStateOf("") }

    //Transaction
    var isTransOpen by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    // Fetch user data when screen is opened
    LaunchedEffect(userId) {
        showDialog = true
        loadUserDetails(firestore, userId) { user = it }
        loadWalletBalance(firestore, userId) { balance = it }
        showDialog = false
    }

    loadWalletBalance(firestore, userId) { balance = it }

    user?.let { userData ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { navController.navigate("home") }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Adjust Balance",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ){
                Spacer(modifier = Modifier.width(15.dp))
                Image(
                    painter = rememberAsyncImagePainter(userData.profileImageUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("${userData.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${userData.email}")
                    Text("ID: ${userData.studentId}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Current Balance", fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("RM $balance", fontSize = 23.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Adjust eWallet Credit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = "RM" + amount,
                    onValueChange = { newValue ->
                        // Remove "RM " and validate digits only
                        val numericPart = newValue.removePrefix("RM ").filter { it.isDigit() }
                        amount = numericPart
                    },
                    label = { Text("Enter Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.clickable { operator = "+" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = operator == "+",
                            onClick = { operator = "+" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.Blue,
                                unselectedColor = Color.Gray
                            )
                        )
                        Text(text = "+ Add")
                    }
                    Spacer(modifier = Modifier.width(28.dp))
                    Row(
                        modifier = Modifier.clickable { operator = "-" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = operator == "-",
                            onClick = { operator = "-" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.Blue,
                                unselectedColor = Color.Gray
                            )
                        )
                        Text(text = "- Subtract")
                    }
                }

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Enter Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if(operator == "-" && (amount.toDoubleOrNull() ?: 0.0) > balance){
                            Toast.makeText(context, "Subtract Amount should not exceed current balance", Toast.LENGTH_SHORT).show()
                        }else{
                            showDialog = true
                            updateUserBalance(userId, amount.toDoubleOrNull() ?: 0.0, if(operator == "+") "add" else "minus"
                                ,onSuccess = { balance ->
                                    recordTransaction(userId, amount.toDoubleOrNull() ?: 0.0,remark,if(operator == "+") "add" else "minus")
                                    showDialog = false
                                    Toast.makeText(context, "Success update user balance!", Toast.LENGTH_SHORT).show()
                                }
                                ,onFailure={ error ->
                                    Toast.makeText(context, "Error! $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonColors(containerColor = Color.Blue, contentColor = Color.White, disabledContentColor = Color.Gray, disabledContainerColor = Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Balance")
                }

            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp)) // Rounded edges like a pebble
                        .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)) // Floating shadow effect
                        .clickable(
                            onClick = {
                                isTransOpen = !isTransOpen
                                if (isTransOpen)loadTransactionHistory(firestore, userId) { transactionList ->
                                    transactions = transactionList
                                }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(if (isTransOpen) "Hide User Transaction History ▲ " else "View User Transaction History ▼", modifier = Modifier.padding(start = 10.dp))
                }

                if(isTransOpen){
                    loadTransactionHistory(firestore, userId) { transactionList ->
                        transactions = transactionList
                    }
                    if (transactions.isEmpty()) {
                        Text("No Transaction History", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp))
                    } else {
                        transactions.forEach { transaction ->
                            TransactionItem(transaction)
                        }
                    }
                }

            }
        }
        // Show Loading Dialog
        LoadingDialog(text = "Loading...", showDialog = showDialog, onDismiss = { showDialog = false })
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val color = if (transaction.amount >= 0) Color(0xFF008000) else Color(0xFFCC0000)
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