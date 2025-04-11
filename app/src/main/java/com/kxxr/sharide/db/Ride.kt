package com.kxxr.sharide.db

data class Ride(
    val id: String,
    val status: String,
    val timeLeftMillis: Long,
    val date: String,
    val time: String
)
