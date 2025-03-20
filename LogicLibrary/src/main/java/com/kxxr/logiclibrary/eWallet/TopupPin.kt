package com.kxxr.logiclibrary.eWallet

data class TopupPin(
    val TopupPIN: String = "",
    val amount: Int = 0,
    val status: String = "available"
)
