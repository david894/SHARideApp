package com.kxxr.logiclibrary.Admin

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.ManualCase.DriverCase
import com.kxxr.logiclibrary.User.User

fun fetchAdminDetails(
    firestore: FirebaseFirestore,
    userId: String,
    context: Context,
    onAdminFetched: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    firestore.collection("Admin")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val document = querySnapshot.documents.firstOrNull()
            val adminGroupId = document?.getString("groupId").orEmpty()

            if (adminGroupId.isNotEmpty()) {
                fetchAdminGroupName(firestore, adminGroupId, context, onAdminFetched, onError)
            } else {
                onAdminFetched("", "Not found")
            }
        }
        .addOnFailureListener { e ->
            onError("Error fetching admin data: ${e.message}")
        }
}

fun fetchAdminGroupName(
    firestore: FirebaseFirestore,
    groupId: String,
    context: Context,
    onGroupFetched: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    firestore.collection("AdminGroup")
        .whereEqualTo("groupId", groupId)
        .get()
        .addOnSuccessListener { groupSnapshot ->
            val groupName = groupSnapshot.documents.firstOrNull()?.getString("groupName").orEmpty()
            onGroupFetched(groupId, groupName)
        }
        .addOnFailureListener { e ->
            onError("Error fetching admin group data: ${e.message}")
        }
}

fun searchAdminGroup(
    firestore: FirebaseFirestore,
    query: String,
    context: Context,
    onResult: (List<AdminGroup>) -> Unit
) {

    firestore.collection("AdminGroup")
        .whereEqualTo("groupName", query.uppercase()) // Match exact query
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                //Toast.makeText(context, "No user found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val group = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminGroup::class.java)
            }

            onResult(group) // Return the correct list of users
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
}

fun loadAllAdminGroup(
    firestore: FirebaseFirestore,
    onResult: (List<AdminGroup>) -> Unit
) {
    firestore.collection("AdminGroup")
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                //Toast.makeText(context, "No user found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val group = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AdminGroup::class.java)
            }

            onResult(group) // Return the correct list of users
        }
        .addOnFailureListener { e ->
            onResult(emptyList())
        }
}


fun loadAdminGroupDetails(
    firestore: FirebaseFirestore,
    groupId: String,
    onResult: (AdminGroup) -> Unit
) {
    firestore.collection("AdminGroup")
        .document(groupId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) { // Ensure the document exists
                val group = document.toObject(AdminGroup::class.java)
                if (group != null) {
                    // Firestore might not map missing Boolean fields, so use `getBoolean()`
                    onResult(
                        AdminGroup(
                            groupId = group.groupId,
                            groupName = group.groupName,
                            isAdminSettings = document.getBoolean("isAdminSettings") ?: false,
                            isEWalletSettings = document.getBoolean("isEWalletSettings") ?: false,
                            isUserSettings = document.getBoolean("isUserSettings") ?: false
                        )
                    )
                } else {
                    onResult(AdminGroup()) // Return a default object if mapping fails
                }
            } else {
                onResult(AdminGroup()) // Handle case where document doesn’t exist
            }
        }
        .addOnFailureListener {
            onResult(AdminGroup()) // Handle failure gracefully
        }
}


// Function to Update Admin Group
fun updateAdminGroup(
    firestore: FirebaseFirestore,
    groupId: String,
    groupName: String,
    isEWallet: Boolean,
    isUserSettings: Boolean,
    isAdminSettings: Boolean,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val updatedGroup = mapOf(
        "groupName" to groupName,
        "isEWalletSettings" to isEWallet,
        "isUserSettings" to isUserSettings,
        "isAdminSettings" to isAdminSettings
    )

    firestore.collection("AdminGroup")
        .document(groupId)
        .update(updatedGroup)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
}

// Function to Save to Firestore
fun saveAdminGroupToFirestore(
    firestore: FirebaseFirestore,
    groupName: String,
    isEWallet: Boolean,
    isUserSettings: Boolean,
    isAdminSettings: Boolean,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val groupId = firestore.collection("AdminGroup").document().id // Generate unique ID

    val newGroup = hashMapOf(
        "groupId" to groupId,
        "groupName" to groupName,
        "isEWalletSettings" to isEWallet,
        "isUserSettings" to isUserSettings,
        "isAdminSettings" to isAdminSettings
    )

    firestore.collection("AdminGroup")
        .document(groupId)
        .set(newGroup)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
}

fun searchAdmins(
    firestore: FirebaseFirestore,
    searchQuery: String,
    context: Context,
    onComplete: (List<User>) -> Unit
) {
    firestore.collection("Admin")
        .get()
        .addOnSuccessListener { adminSnapshot ->
            val userIds = adminSnapshot.documents.mapNotNull { it.getString("userId") }

            if (userIds.isEmpty()) {
                onComplete(emptyList()) // No admin users found
                return@addOnSuccessListener
            }

            // Fetch user details for retrieved userIds
            firestore.collection("users")
                .whereIn("firebaseUserId", userIds)
                .get()
                .addOnSuccessListener { userSnapshot ->
                    if(searchQuery.isEmpty()){
                        val users = userSnapshot.documents.mapNotNull { it.toObject(User::class.java) }

                        onComplete(users)
                    }else{
                        val users = userSnapshot.documents.mapNotNull { it.toObject(User::class.java) }
                            .filter { user ->
                                // Filter users based on search query
                                user.name.contains(searchQuery, ignoreCase = true) ||
                                        user.email.contains(searchQuery, ignoreCase = true) ||
                                        user.studentId.contains(searchQuery, ignoreCase = true)
                            }
                        onComplete(users)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(emptyList())
                }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error loading admins: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
        }
}

fun checkIfUserIsAdmin(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (String?) -> Unit
) {
    firestore.collection("Admin")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val groupId = document.getString("groupId")
                onResult(groupId)  // User is an admin, return the groupId
            } else {
                onResult(null)  // User is not an admin
            }
        }
        .addOnFailureListener { onResult(null) }
}

fun enrollAdminUser(
    firestore: FirebaseFirestore,
    userId: String,
    groupId: String,
    onComplete: () -> Unit
) {
    val adminData = mapOf(
        "userId" to userId,
        "groupId" to groupId
    )

    firestore.collection("Admin")
        .document(userId)  // Set document ID as userId
        .set(adminData)
        .addOnSuccessListener { onComplete() }
        .addOnFailureListener { onComplete() }
}
