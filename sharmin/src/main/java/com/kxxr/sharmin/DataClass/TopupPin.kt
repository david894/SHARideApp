package com.kxxr.sharmin.DataClass

// Top-up Data Class
data class TopupPin(
    val TopupPIN: String = "",
    val amount: Int = 0,
    val status: String = "available"
)
