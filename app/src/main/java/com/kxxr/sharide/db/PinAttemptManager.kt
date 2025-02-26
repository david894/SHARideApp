package com.kxxr.sharide.db

import android.content.Context

object PinAttemptManager {
    private const val PREFS_NAME = "PinPrefs"
    private const val KEY_ATTEMPTS = "PinAttempts"
    private const val MAX_ATTEMPTS = 3

    fun getAttempts(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ATTEMPTS, 0)
    }

    fun incrementAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAttempts = getAttempts(context) + 1
        prefs.edit().putInt(KEY_ATTEMPTS, currentAttempts).apply()
    }

    fun resetAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ATTEMPTS, 0).apply()
    }

    fun isLockedOut(context: Context): Boolean {
        return getAttempts(context) >= MAX_ATTEMPTS
    }
}
