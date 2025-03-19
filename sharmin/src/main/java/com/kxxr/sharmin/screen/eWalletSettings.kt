package com.kxxr.sharmin.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.sharmin.DataClass.TopupPin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random


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
                    .background(if(type == "available")Color.Blue else Color.Gray)
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

// Generate Top-up PIN(s) and Store in Firestore (with onSuccess callback)
fun generateTopupPins(
    firestore: FirebaseFirestore,
    amount: Int,
    quantity: Int,
    context: Context,
    onSuccess: () -> Unit
) {
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
        var generatedCount = 0

        repeat(quantity) {
            // Ensure unique PINs are generated
            var uniquePin: Long
            do {
                uniquePin = generateRandomPin()
            } while (isPinDuplicate(firestore, uniquePin))

            val topupPin = mapOf(
                "TopupPIN" to uniquePin.toString(),
                "amount" to amount.toDouble(),
                "status" to "available"
            )

            // Add to Firestore
            try {
                firestore.collection("Topup").add(topupPin).await()
                withContext(Dispatchers.Main) {
                    //Toast.makeText(context, "Top-up PIN Generated", Toast.LENGTH_SHORT).show()
                }

                // Check if all PINs are generated
                generatedCount++
                if (generatedCount == quantity) {
                    withContext(Dispatchers.Main) {
                        onSuccess() // Trigger onSuccess after all are created
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to generate PIN: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// Check if the PIN already exists in Firestore
private suspend fun isPinDuplicate(firestore: FirebaseFirestore, pin: Long): Boolean {
    return try {
        val querySnapshot = firestore.collection("Topup")
            .whereEqualTo("TopupPIN", pin)
            .get()
            .await()

        !querySnapshot.isEmpty // Return true if duplicate exists
    } catch (e: Exception) {
        false // Assume no duplicate if an error occurs
    }
}

// Random 10-digit PIN Generator
private fun generateRandomPin(): Long {
    return Random.nextLong(1000000000L, 9999999999L)
}

// Load All Available Top-up PINs
private fun loadAvailablePins(
    firestore: FirebaseFirestore,
    onComplete: (List<TopupPin>) -> Unit
) {
    firestore.collection("Topup")
        .whereEqualTo("status", "available")
        .get()
        .addOnSuccessListener { snapshot ->
            val pins = snapshot.documents.mapNotNull { document ->
                try {
                    TopupPin(
                        TopupPIN = document.getString("TopupPIN") ?: "",
                        amount = document.getLong("amount")?.toInt() ?: 0,
                        status = document.getString("status") ?: "unknown"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            onComplete(pins) // Pass the list of pins
        }
        .addOnFailureListener { exception ->
            onComplete(emptyList())
        }
}

// Load All Available Top-up PINs
private fun loadSoldPins(
    firestore: FirebaseFirestore,
    onComplete: (List<TopupPin>) -> Unit
) {
    firestore.collection("Topup")
        .whereEqualTo("status", "sold")
        .get()
        .addOnSuccessListener { snapshot ->
            val pins = snapshot.documents.mapNotNull { document ->
                try {
                    TopupPin(
                        TopupPIN = document.getString("TopupPIN") ?: "",
                        amount = document.getLong("amount")?.toInt() ?: 0,
                        status = document.getString("status") ?: "unknown"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            onComplete(pins) // Pass the list of pins
        }
        .addOnFailureListener { exception ->
            onComplete(emptyList())
        }
}

private fun markAsSold(
    firestore: FirebaseFirestore,
    pin: String,
    context: Context,
    onUpdate: () -> Unit
) {
    firestore.collection("Topup")
        .whereEqualTo("TopupPIN", pin) // Ensure the correct type (String)
        .get()
        .addOnSuccessListener { snapshot ->
            for (document in snapshot.documents) {
                document.reference.update("status", "sold")
            }
            Toast.makeText(context, "Marked as Sold", Toast.LENGTH_SHORT).show()
            onUpdate() // Refresh the list
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
