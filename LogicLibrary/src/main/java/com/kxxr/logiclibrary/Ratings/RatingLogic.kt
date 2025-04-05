package com.kxxr.logiclibrary.Ratings

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
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
            onResult(score/totalRatingsAsInt,totalRatingsAsInt)
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

fun recordRatings(userId: String, score: Double, description: String,from :String,to:String) {
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
}

fun ratingAPI(
    userId: String,
    receiver: String,
    score : Double,
    description: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    var hasRecordedUserTransaction = false

    updateUserRating(receiver,score,
        onSuccess={
            if(!hasRecordedUserTransaction){
                hasRecordedUserTransaction = true
                recordRatings(receiver,score,description,userId,receiver)
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
