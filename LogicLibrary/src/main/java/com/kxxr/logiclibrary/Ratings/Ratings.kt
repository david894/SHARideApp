package com.kxxr.logiclibrary.Ratings

data class Ratings(
    val userId: String,
    val date: String,
    val Score: Double,
    val description: String,
    val from: String,
    val to: String,
    val rideId: String
)
