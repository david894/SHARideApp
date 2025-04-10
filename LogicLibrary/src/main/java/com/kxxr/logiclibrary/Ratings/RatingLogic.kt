package com.kxxr.logiclibrary.Ratings

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kxxr.logiclibrary.User.User
import com.kxxr.logiclibrary.User.loadUserDetails
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

fun loadRatingScore(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (Double,Int) -> Unit
) {
    var score = 0.0
    var totalRatings = 0.0
    var totalRatingsAsInt = 0

    firestore.collection("Ratings").document(userId).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                score = document.getDouble("Score") ?: 0.0
                totalRatings = document.getDouble("TotalRatings") ?: 0.0
                totalRatingsAsInt = totalRatings.toInt()
            }
            val average = if (totalRatingsAsInt > 0) score / totalRatingsAsInt else 0.0
            val roundedAverage = BigDecimal(average).setScale(2, RoundingMode.HALF_UP).toDouble()

            onResult(roundedAverage, totalRatingsAsInt)
        }
}

fun updateUserRating(userId: String, score: Double,onSuccess: () -> Unit,onFailure: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    val userRef = firestore.collection("Ratings").document(userId)
    userRef.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val currentScore = document.getDouble("Score") ?: 0.0
            val totalRatings = document.getDouble("TotalRatings") ?: 0.0

            userRef.update("Score",  currentScore + score )
            userRef.update("TotalRatings",  totalRatings + 1 )

            onSuccess()
        }else{
            val initialData = hashMapOf(
                "userId" to userId,
                "Score" to score,
                "TotalRatings" to 1.0
            )
            userRef.set(initialData).addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener{
                onFailure("Error Update Ratings")
            }
        }
    }.addOnFailureListener{
        onFailure("Error Update Ratings")
    }
}

fun recordRatings(userId: String, score: Double, description: String,from :String,to:String,rideId:String) {
    val firestore = FirebaseFirestore.getInstance()
    // Set Malaysia Time Zone (MYT)
    val timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    sdf.timeZone = timeZone

    val currentDate = Timestamp.now() // Firestore's Timestamp
    val formattedDate = sdf.format(currentDate.toDate()) // Format with MYT Time Zone

    // Prepare transaction data
    val RatingTransaction = hashMapOf(
        "userId" to userId,
        "date" to formattedDate, // Store Timestamp
        "Score" to score,
        "description" to description,
        "from" to from,
        "to" to to,
        "rideId" to rideId
    )

    // Add transaction to Firestore
    firestore.collection("RatingsTransaction")
        .add(RatingTransaction)
        .addOnSuccessListener {
            println("Rating Transaction Recorded Successfully")
        }
        .addOnFailureListener { e ->
            println("Failed to Record Transaction: ${e.message}")
        }

    // Check and add to RatingsWarning only if score is low and rideId doesn't already exist
    if (score <= 1.00) {
        firestore.collection("RatingsWarning")
            .whereEqualTo("rideId", rideId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // No duplicate rideId, safe to upload
                    firestore.collection("RatingsWarning")
                        .add(RatingTransaction)
                        .addOnSuccessListener {
                            println("Warning Recorded Successfully")
                        }
                        .addOnFailureListener { e ->
                            println("Failed to Record Warning: ${e.message}")
                        }
                } else {
                    println("Warning already exists for rideId: $rideId — skipping upload")
                }
            }
            .addOnFailureListener { e ->
                println("Error checking existing warning: ${e.message}")
            }
    }
}

fun ratingAPI(
    userId: String,
    receiver: String,
    score : Double,
    description: String,
    rideId: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    var hasRecordedUserTransaction = false

    updateUserRating(receiver,score,
        onSuccess={
            if(!hasRecordedUserTransaction){
                hasRecordedUserTransaction = true
                recordRatings(receiver,score,description,userId,receiver,rideId)
                onSuccess()
            }
        },onFailure={
            onFailure(it)
        })
}

fun loadRatingHistory(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (List<Ratings>) -> Unit
) {
    firestore.collection("RatingsTransaction")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val data = snapshot.map { documents ->
                    val transactionData = documents.data
                    Ratings(
                        date = transactionData["date"].toString(),
                        description = transactionData["description"].toString(),
                        Score = transactionData["Score"].toString().toDouble(),
                        from = transactionData["from"].toString(),
                        to = transactionData["to"].toString(),
                        userId = transactionData["userId"].toString(),
                        rideId = transactionData["rideId"].toString()
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

fun loadRatingWarningHistory(
    firestore: FirebaseFirestore,
    userId: String,
    onResult: (List<Ratings>) -> Unit
) {
    firestore.collection("RatingsWarning")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                sdf.timeZone = timeZone

                val now = Calendar.getInstance(timeZone).time

                val filteredData = snapshot.mapNotNull { document ->
                    val data = document.data
                    try {
                        val dateStr = data["date"].toString()
                        val ratingDate = sdf.parse(dateStr)

                        // Check if within the last 30 days
                        if (ratingDate != null) {
                            val diffMillis = now.time - ratingDate.time
                            val daysDiff = diffMillis / (1000 * 60 * 60 * 24)
                            if (daysDiff <= 30) {
                                Ratings(
                                    date = dateStr,
                                    description = data["description"].toString(),
                                    Score = data["Score"].toString().toDouble(),
                                    from = data["from"].toString(),
                                    to = data["to"].toString(),
                                    userId = data["userId"].toString(),
                                    rideId = data["rideId"].toString()
                                )
                            } else null
                        } else null
                    } catch (e: Exception) {
                        null // skip bad date formats
                    }
                }

                val sorted = filteredData.sortedByDescending {
                    sdf.parse(it.date)
                }

                onResult(sorted)
            } else {
                onResult(emptyList())
            }
        }
        .addOnFailureListener {
            onResult(emptyList())
        }
}

fun fetchPassengerInfoForRide(
    rideId: String,
    onResult: (List<User>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("rides")
        .whereEqualTo("rideId", rideId)
        .get()
        .addOnSuccessListener { rideDocs ->
            if (!rideDocs.isEmpty) {
                val passengerIds = rideDocs.documents[0].get("passengerIds") as? List<String> ?: emptyList()
                val filteredIds = passengerIds.filter { it != "null" && it.isNotBlank() }

                if (filteredIds.isEmpty()) {
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val userList = mutableListOf<User>()
                val total = filteredIds.size
                var completed = 0

                for (id in filteredIds) {
                    loadUserDetails(db,id){ user ->
                        user?.let { userList.add(it) }

                        completed++
                        if (completed == total) {
                            onResult(userList)
                        }
                    }
                }
            } else {
                onResult(emptyList())
            }
        }
        .addOnFailureListener {
            onResult(emptyList())
        }
}
