package com.kxxr.logiclibrary.Admin

data class AdminGroup(
    val groupId: String = "",
    val groupName: String = "",
    val isAdminSettings: Boolean = false,
    val isEWalletSettings: Boolean = false,
    val isUserSettings: Boolean = false,
)
