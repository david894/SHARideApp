package com.kxxr.logiclibrary.eWallet

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

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
suspend fun isPinDuplicate(firestore: FirebaseFirestore, pin: Long): Boolean {
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
fun generateRandomPin(): Long {
    return Random.nextLong(1000000000L, 9999999999L)
}

// Load All Available Top-up PINs
fun loadAvailablePins(
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
fun loadSoldPins(
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

fun markAsSold(
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

fun loadTransactionHistory(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (List<Transaction>) -> Unit
) {
    firestore.collection("Transaction")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val data = snapshot.map { documents ->
                    val transactionData = documents.data
                    Transaction(
                        date = transactionData["date"].toString(),
                        description = transactionData["description"].toString(),
                        amount = transactionData["amount"].toString().toDouble()
                    )
                }
                val transactions = data.sortedByDescending {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(it.date)
                }

                onResult(transactions)
            }else{
                onResult(emptyList()) // No transactions found
                return@addOnSuccessListener
            }
        }
        .addOnFailureListener { e ->
            onResult(emptyList())
        }
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
                updateUserBalance(userId, amount, "add"
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

fun updateUserBalance(userId: String, amount: Double,operator: String,onSuccess: (String) -> Unit,onFailure: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    val userRef = firestore.collection("eWallet").document(userId)
    userRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val currentBalance = document.getDouble("balance") ?: 0.0
            userRef.update("balance", if (operator == "add") currentBalance + amount else currentBalance - amount)
            val balance = if(operator == "add")currentBalance + amount else currentBalance - amount
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

fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(pin.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
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

fun validateAnswers(userAnswers: List<String>, correctAnswers: List<String>): Boolean {
    return userAnswers.map { hashAnswer(it) } == correctAnswers
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