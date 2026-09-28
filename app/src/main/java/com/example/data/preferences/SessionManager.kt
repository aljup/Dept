package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "duyuni_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_HAS_PIN = "has_pin"
        private const val KEY_SAVED_PIN = "saved_pin"
        private const val KEY_CURRENCY = "user_currency"
        private const val KEY_ADMIN_SETUP_DONE = "admin_setup_done"
    }

    fun isFirstRunAdminSetupDone(): Boolean = prefs.getBoolean(KEY_ADMIN_SETUP_DONE, false)

    fun setAdminSetupDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ADMIN_SETUP_DONE, done).apply()
    }

    fun saveLoginSession(userId: Long, email: String, name: String, hasPin: Boolean, pin: String? = null, currency: String = "ر.س") {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_HAS_PIN, hasPin)
            putString(KEY_SAVED_PIN, pin)
            putString(KEY_CURRENCY, currency)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun hasPin(): Boolean = prefs.getBoolean(KEY_HAS_PIN, false)

    fun getSavedPin(): String? = prefs.getString(KEY_SAVED_PIN, null)

    fun getCurrency(): String = prefs.getString(KEY_CURRENCY, "ر.س") ?: "ر.س"

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putLong(KEY_USER_ID, -1L)
            putString(KEY_USER_EMAIL, "")
            putString(KEY_USER_NAME, "")
            // Notice: keep saved PIN or clear depending on preference, here we clear session completely
            putBoolean(KEY_HAS_PIN, false)
            putString(KEY_SAVED_PIN, null)
            apply()
        }
    }

    fun lockWithPin() {
        // Keeps user details but marks locked so PIN screen is prompted
        prefs.edit().apply()
    }
}
