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
                val matchingRides = if (getMatchingDrivers(ridesSnapshot, searchSnapshot, usersSnapshot).isNotEmpty()) {
                    ridesSnapshot.documents
                } else {
                    emptyList()
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

fun getMatchingDrivers(
    rideSnapshot: QuerySnapshot,
    searchSnapshot: QuerySnapshot,
    usersSnapshot: QuerySnapshot
): List<String> {
    val matchingDriverIds = mutableListOf<String>()

    rideSnapshot.documents.forEach { ride ->
        val rideLocation = ride.getString("location")
        val rideStop = ride.getString("stop")
        val rideDestination = ride.getString("destination")
        val rideTime = ride.getString("time") ?: "00:00"
        val rideTimeMinutes = timeToMinutes(rideTime)
        val rideCapacity = ride.getLong("capacity")?.toInt() ?: 0
        val driverId = ride.getString("driverId") ?: return@forEach

        val driverGender = usersSnapshot.documents.find {
            it.getString("firebaseUserId") == driverId
        }?.getString("gender") ?: ""

        searchSnapshot.documents.forEach { search ->
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
            val isCapacityValid = checkCapacity(rideCapacity, passengerCapacity)

            if (isTimeValid && isLocationValid && isDestinationValid && isGenderValid && isCapacityValid) {
                matchingDriverIds.add(driverId) // Adds even if it's a duplicate
            }
        }
    }

    return matchingDriverIds // Keeps duplicates
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
