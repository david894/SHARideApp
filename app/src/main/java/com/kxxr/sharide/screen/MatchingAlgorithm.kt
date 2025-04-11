package com.kxxr.sharide.screen

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
fun findMatchingRides(
    firestore: FirebaseFirestore,
    onMatchFound: (List<DocumentSnapshot>) -> Unit,
    onNoMatch: () -> Unit
) {
    fetchRides(firestore) { ridesSnapshot ->
        fetchUsers(firestore) { usersSnapshot ->
            fetchSearches(firestore) { searchSnapshot ->

                // Get matching ride documents
                val matchingRides = ridesSnapshot.documents.filter { ride ->
                    getMatchingDrivers(listOf(ride), searchSnapshot, usersSnapshot).isNotEmpty()
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
    onSuccess: (QuerySnapshot) -> Unit
) {
    firestore.collection("rides")
        .get()
        .addOnSuccessListener(onSuccess)
        .addOnFailureListener { }
}

fun fetchSearches(
    firestore: FirebaseFirestore,
    onSuccess: (QuerySnapshot) -> Unit
) {
    firestore.collection("searchs")
        .orderBy("timestamp", Query.Direction.DESCENDING) // Order by latest timestamp
        .limit(1) // Get only one latest search
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
    rideSnapshot: List<DocumentSnapshot>,
    searchSnapshot: QuerySnapshot,
    usersSnapshot: QuerySnapshot,
): List<DocumentSnapshot> {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
    val matchingRides = mutableListOf<DocumentSnapshot>()

    rideSnapshot.forEach { ride ->
        val driverId = ride.getString("driverId") ?: return@forEach

        //  Skip ride if it's driven by the current user
        if (driverId == currentUserId) return@forEach

        val rideDate = ride.getString("date") ?: return@forEach
        val rideLocation = ride.getString("location") ?: return@forEach
        val rideStop = ride.getString("stop") // Can be null
        val rideDestination = ride.getString("destination") ?: return@forEach
        val rideTime = ride.getString("time") ?: "00:00"
        val rideTimeMinutes = timeToMinutes(rideTime)
        val rideCapacity = ride.getLong("capacity")?.toInt() ?: 0
        val passengerIds = ride.get("passengerIds") as? List<String> ?: emptyList()

        val driverGender = usersSnapshot.documents.find {
            it.getString("firebaseUserId") == driverId
        }?.getString("gender") ?: ""

        searchSnapshot.documents.forEach { search ->
            val searchDate = search.getString("date") ?: return@forEach
            val searchLocation = search.getString("location") ?: return@forEach
            val searchDestination = search.getString("destination") ?: return@forEach
            val searchTime = search.getString("time") ?: "00:00"
            val searchTimeMinutes = timeToMinutes(searchTime)
            val genderPreference = search.getString("genderPreference") ?: "Both"
            val passengerCapacity = search.getLong("capacity")?.toInt() ?: 1

            //  Check all conditions only if the date matches
            val isMatch = isDateValid(rideDate,searchDate) && isTimeValid(searchTimeMinutes, rideTimeMinutes) &&
                    isLocationValid(searchLocation, rideLocation, rideStop) &&
                    isDestinationValid(searchDestination, rideStop, rideDestination) &&
                    isGenderValid(driverGender, genderPreference) &&
                    isCapacityValid(rideCapacity, passengerIds , passengerCapacity)

            if (isMatch) {
                matchingRides.add(ride)
                return@forEach // Stop checking once a match is found for this ride
            }
        }
    }

    return matchingRides
}



fun isDateValid(searchDate: String, rideDate: String): Boolean {
    return rideDate == searchDate
}

fun isTimeValid(searchTimeMinutes: Int, rideTimeMinutes: Int): Boolean {
    val timeDiff = kotlin.math.abs(rideTimeMinutes - searchTimeMinutes)
    return timeDiff <= 30
}

fun isLocationValid(searchLocation: String, rideLocation: String, rideStop: String?): Boolean {
    return searchLocation == rideLocation || searchLocation == rideStop
}

fun isDestinationValid(searchDestination: String, rideStop: String?, rideDestination: String): Boolean {
    return searchDestination == rideStop || searchDestination == rideDestination
}

fun isGenderValid(driverGender: String, genderPreference: String): Boolean {
    return when (genderPreference.lowercase()) {
        "male" -> driverGender == "M"
        "female" -> driverGender == "F"
        "both" -> driverGender == "M" || driverGender == "F"
        else -> false
    }
}

fun isCapacityValid(rideCapacity: Int, passengerIds: List<String>?, passengerCapacity: Int): Boolean {
    val currentPassengerCount = passengerIds?.filter { it != "null" }?.size ?: 0
    return (rideCapacity - currentPassengerCount) >= passengerCapacity
}


fun timeToMinutes(time: String): Int {
    val parts = time.split(":").map { it.toInt() }
    return parts[0] * 60 + parts[1]
}