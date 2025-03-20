package com.kxxr.logiclibrary.User

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

fun searchUsers(
    firestore: FirebaseFirestore,
    query: String,
    context: Context,
    onResult: (List<User>) -> Unit
) {
    val searchField = when {
        query.contains(" ") -> "name"         // Assume "name" if space is present
        query.endsWith("tarc.edu.my") -> "email" // Alphabetic values for "name"
        query.matches(Regex("[0-9]{2}[A-z]{1}")) -> "studentId" // Numeric values for "studentId"
        else -> "studentId"        // Otherwise, search by "email"
    }

    firestore.collection("users")
        .whereEqualTo(searchField, if(searchField == "email")query else query.uppercase()) // Match exact query
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No user found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val users = snapshot.documents.mapNotNull { doc ->
                User(
                    firebaseUserId = doc.getString("firebaseUserId") ?: "",
                    name = doc.getString("name") ?: "Unknown",
                    gender = doc.getString("gender") ?: "Unknown",
                    phoneNumber = doc.getString("phoneNumber") ?: "Unknown",
                    email = doc.getString("email") ?: "Unknown",
                    studentId = doc.getString("studentId") ?: "Unknown",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                )
            }

            onResult(users) // Return the correct list of users
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
}

fun loadUserDetails(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (User?) -> Unit
) {
    firestore.collection("users")
        .whereEqualTo("firebaseUserId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first() // Get the first matching document
                val user = User(
                    firebaseUserId = doc.getString("firebaseUserId") ?: "",
                    name = doc.getString("name") ?: "Unknown",
                    gender = doc.getString("gender") ?: "Unknown",
                    phoneNumber = doc.getString("phoneNumber") ?: "Unknown",
                    email = doc.getString("email") ?: "Unknown",
                    studentId = doc.getString("studentId") ?: "Unknown",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                )
                onResult(user) // Return the user object
            } else {
                onResult(null) // No matching user found
            }
        }
        .addOnFailureListener { e ->
            onResult(null) // Handle error case by returning null
        }
}

fun loadWalletBalance(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (Double) -> Unit
) {
    var balance = 0.0

    firestore.collection("eWallet").document(userId).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                balance = document.getDouble("balance") ?: 0.0
            }
            onResult(balance)
        }
}