package com.kxxr.sharide.screen

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

fun findMatchingRides(
    firestore: FirebaseFirestore,
    passengerSearch: Map<String, Any>, // Search details
    onMatchFound: (List<DocumentSnapshot>) -> Unit,
    onNoMatch: () -> Unit
) {
    val passengerDate = passengerSearch["date"] as String

    fetchRides(firestore, passengerDate) { ridesSnapshot ->
        fetchUsers(firestore) { usersSnapshot ->
            fetchSearches(firestore, passengerDate) { searchSnapshot ->
                val matchingRides = ridesSnapshot.documents.filter { ride ->
                    isRideMatching(ride, searchSnapshot, usersSnapshot)
                }
                if (matchingRides.isNotEmpty()) {
                    onMatchFound(matchingRides)
                } else {
                    onNoMatch()
                }
            }
        }
    }
}

fun fetchRides(
    firestore: FirebaseFirestore,
    date: String,
    onSuccess: (QuerySnapshot) -> Unit
) {
    firestore.collection("rides")
        .whereEqualTo("date", date)
        .get()
        .addOnSuccessListener(onSuccess)
        .addOnFailureListener { }
}

fun fetchSearches(
    firestore: FirebaseFirestore,
    date: String,
    onSuccess: (QuerySnapshot) -> Unit
) {
    firestore.collection("searchs")
        .whereEqualTo("date", date)
        .get()
        .addOnSuccessListener(onSuccess)
        .addOnFailureListener { }
}

fun fetchUsers(
    firestore: FirebaseFirestore,
    onSuccess: (QuerySnapshot) -> Unit
) {
    firestore.collection("users")
        .get()
        .addOnSuccessListener(onSuccess)
        .addOnFailureListener { }
}

fun isRideMatching(
    ride: DocumentSnapshot,
    searchSnapshot: QuerySnapshot,
    usersSnapshot: QuerySnapshot
): Boolean {
    val rideLocation = ride.getString("location")
    val rideStop = ride.getString("stop")
    val rideDestination = ride.getString("destination")
    val rideTime = ride.getString("time") ?: "00:00"
    val rideTimeMinutes = timeToMinutes(rideTime)
    val rideCapacity = ride.getLong("capacity")?.toInt() ?: 0
    val driverId = ride.getString("driverId")

    val driverGender = usersSnapshot.documents.find {
        it.getString("firebaseUserId") == driverId
    }?.getString("gender") ?: ""

    return searchSnapshot.documents.any { search ->
        val searchLocation = search.getString("location")
        val searchDestination = search.getString("destination")
        val searchTime = search.getString("time") ?: "00:00"
        val searchTimeMinutes = timeToMinutes(searchTime)
        val genderPreference = search.getString("genderPreference") ?: "Both"
        val passengerCapacity = search.getLong("capacity")?.toInt() ?: 1

        val timeDiff = kotlin.math.abs(rideTimeMinutes - searchTimeMinutes)
        val isTimeValid = timeDiff <= 30
        val isLocationValid = searchLocation == rideLocation || searchLocation == rideStop
        val isDestinationValid = searchDestination == rideStop || searchDestination == rideDestination
        val isGenderValid = checkGenderPreference(driverGender, genderPreference)
        val isCapacityValid = checkCapacity(rideCapacity, passengerCapacity) // Call capacity check function

        isTimeValid && isLocationValid && isDestinationValid && isGenderValid && isCapacityValid
    }
}

fun checkGenderPreference(driverGender: String, genderPreference: String): Boolean {
    return when (genderPreference.lowercase()) {
        "male" -> driverGender == "M"
        "female" -> driverGender == "F"
        "both" -> driverGender == "M" || driverGender == "F"
        else -> false
    }
}

// Function to check if ride has enough capacity
fun checkCapacity(rideCapacity: Int, passengerCapacity: Int): Boolean {
    return rideCapacity >= passengerCapacity
}

fun timeToMinutes(time: String): Int {
    val parts = time.split(":" ).map { it.toInt() }
    return parts[0] * 60 + parts[1]
}
