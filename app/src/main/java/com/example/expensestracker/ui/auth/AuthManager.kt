package com.example.expensestracker.ui.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.expensestracker.util.Constants

class AuthManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.PREFS_AUTH, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }

    // Login method.
    fun login(identifier: String, password: String): Boolean {
        val storedIdentifier = sharedPreferences.getString(Constants.KEY_IDENTIFIER, null)
        val storedPassword = sharedPreferences.getString(Constants.KEY_PASSWORD, null)

        return if (identifier == storedIdentifier && password == storedPassword) {
            editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true)
            editor.apply()
            true // Login successful
        } else {
            false // Incorrect credentials
        }
    }

    // The register method now saves the user's name as well.
    fun register(identifier: String, password: String, userName: String) {
        editor.putString(Constants.KEY_IDENTIFIER, identifier)
        editor.putString(Constants.KEY_PASSWORD, password)
        editor.putString(Constants.KEY_USER_NAME, userName) // Save the user's name here
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true) // Auto-login after registration
        editor.apply()
    }

    fun logout() {
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, false)
        editor.remove(Constants.KEY_IDENTIFIER)
        editor.remove(Constants.KEY_PASSWORD)
        editor.remove(Constants.KEY_USER_NAME) // Clear the user's name on logout
        editor.apply()
    }

    // NEW METHOD: Get the logged-in user's name.
    fun getLoggedInUserName(): String {
        // Return the stored user name. If not found, return "Guest" as a default.
        return sharedPreferences.getString(Constants.KEY_USER_NAME, "Guest") ?: "Guest"
    }
}
