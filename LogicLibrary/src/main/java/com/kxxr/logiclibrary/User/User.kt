package com.kxxr.logiclibrary.User

// User Data Class
data class User(
    val firebaseUserId: String = "",
    val name: String = "",
    val gender: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val studentId: String = "",
    val profileImageUrl: String = "",
)