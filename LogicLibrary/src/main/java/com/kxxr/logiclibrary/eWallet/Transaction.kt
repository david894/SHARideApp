package com.kxxr.logiclibrary.eWallet

// Data class for transaction
data class Transaction(
    val date: String,
    val description: String,
    val amount: Double,
    val from: String,
    val to: String
)