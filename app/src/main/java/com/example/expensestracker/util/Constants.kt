package com.example.expensestracker.util

/**
 * A utility class to hold all constant values used throughout the application.
 * This is good practice to avoid hardcoding strings and prevent typos.
 */
object Constants {
    // SharedPreferences file name for authentication data
    const val PREFS_AUTH = "auth_prefs"

    // SharedPreferences keys
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_IDENTIFIER = "identifier"
    const val KEY_PASSWORD = "password"
    const val KEY_USER_NAME = "user_name"
}
