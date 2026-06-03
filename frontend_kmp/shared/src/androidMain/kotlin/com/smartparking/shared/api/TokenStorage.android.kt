package com.smartparking.shared.api

import android.content.Context
import android.content.SharedPreferences

private lateinit var prefs: SharedPreferences

fun initTokenStorage(context: Context) {
    prefs = context.getSharedPreferences("smartparking_prefs", Context.MODE_PRIVATE)
}

actual object TokenStorage {
    actual fun getToken(): String? = prefs.getString("jwt_token", null)
    actual fun setToken(token: String?) {
        prefs.edit().putString("jwt_token", token).apply()
    }
    actual fun clear() {
        prefs.edit().clear().apply()
    }
}
