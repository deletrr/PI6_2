package com.smartparking.shared.api

actual object TokenStorage {
    private var token: String? = null
    private var userJson: String? = null

    actual fun getToken(): String? = token
    actual fun setToken(token: String?) { this.token = token }
    actual fun getUserJson(): String? = userJson
    actual fun setUserJson(json: String?) { this.userJson = json }
    actual fun clear() { 
        token = null
        userJson = null
    }
}
