package com.kxxr.sharide.db

data class SuccessfulUserInfo(
    val firebaseUserId: String = "",
    val name: String = "Unknown",
    val imageUrl: String = "",
    val rating: Double = 4.5,
    val location: String = "",
    val destination: String = "",
)

