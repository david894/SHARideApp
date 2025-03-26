package com.kxxr.logiclibrary.Driver

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

fun searchDriver(
    firestore: FirebaseFirestore,
    query: String,
    context: Context,
    onResult: (List<Driver>) -> Unit
) {
    val searchField = when {
        query.contains(" ") -> "driverName"         // Assume "name" if space is present
        query.matches(Regex("[0-9]{12}")) -> "drivingId" // Numeric values for "studentId"
        query.matches(Regex("^[A-Za-z]{1,3}\\s?\\d{1,4} ?[A-Za-z]?$")) -> "vehiclePlate" // Numeric values for "studentId"
        query.equals("") -> "status"
        else -> "status"        // Otherwise, search by "name"
    }

    firestore.collection("driver")
        .whereEqualTo(searchField, if(searchField == "status")query else query.uppercase()) // Match exact query
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                //Toast.makeText(context, "No Driver found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val driver = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Driver::class.java)
            }

            onResult(driver) // Return the correct list of users
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
}

fun searchVehicle(
    firestore: FirebaseFirestore,
    query: String,
    context: Context,
    onResult: (List<Vehicle>) -> Unit
) {

    firestore.collection("Vehicle")
        .whereEqualTo("CarRegistrationNumber",query) // Match exact query
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                //Toast.makeText(context, "No Vehicle found!", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
                return@addOnSuccessListener
            }

            val vehicle = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Vehicle::class.java)
            }

            onResult(vehicle) // Return the correct list of users
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
}