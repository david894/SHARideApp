package com.kxxr.logiclibrary.Banned

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.User.User

fun banUser(
    firestore: FirebaseFirestore,
    userId: String,
    remark: String,
    onComplete: (Boolean) -> Unit
) {
    val vehicleData = hashMapOf(
        "userId" to userId,
        "remark" to remark,
        "status" to "Active",
    )

    firestore.collection("Banned")
        .document(userId)
        .set(vehicleData)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}

fun isBanned(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (String?,Boolean) -> Unit
){
    firestore.collection("Banned")
        .document(userId)
        .get()
        .addOnSuccessListener { query ->
            if(query.exists()){
                val remark = query.getString("remark")
                onResult(remark,true)
            }else{
                onResult("",false)
            }
        }
}

fun unbanUser(
    firestore: FirebaseFirestore,
    userId: String,
    onComplete: (Boolean) -> Unit
){
    firestore.collection("Banned")
        .document(userId)
        .delete()
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}

fun loadAllBannedUsers(
    firestore: FirebaseFirestore,
    context: Context,
    onComplete: (List<User>) -> Unit
) {
    firestore.collection("Banned")
        .whereEqualTo("status", "Active")
        .get()
        .addOnSuccessListener { bannedSnapshot ->
            if (bannedSnapshot.isEmpty) {
                onComplete(emptyList()) // No banned users
                return@addOnSuccessListener
            }

            val bannedUserIds = bannedSnapshot.documents.mapNotNull { it.getString("userId") }

            if (bannedUserIds.isEmpty()) {
                onComplete(emptyList())
                return@addOnSuccessListener
            }

            // Fetch user details for all banned users
            firestore.collection("users")
                .whereIn("firebaseUserId", bannedUserIds) // Correctly filters multiple user IDs
                .get()
                .addOnSuccessListener { userSnapshot ->
                    val users = userSnapshot.documents.mapNotNull { doc ->
                        User(
                            firebaseUserId = doc.getString("firebaseUserId") ?: "",
                            name = doc.getString("name") ?: "Unknown",
                            gender = doc.getString("gender") ?: "Unknown",
                            phoneNumber = doc.getString("phoneNumber") ?: "Unknown",
                            email = doc.getString("email") ?: "Unknown",
                            studentId = doc.getString("studentId") ?: "Unknown",
                            profileImageUrl = doc.getString("profileImageUrl") ?: ""
                        )
                    }

                    onComplete(users) // Return the list of users
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(emptyList())
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error loading banned users: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
        }
}
