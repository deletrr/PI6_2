package com.smartparking.shared.repository

import com.smartparking.shared.api.TokenStorage
import com.smartparking.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppSession {
    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser

    val isLoggedIn get() = _currentUser.value != null
    val isAdmin get() = _currentUser.value?.role == "ADMIN"

    fun login(user: UserModel, token: String) {
        TokenStorage.setToken(token)
        _currentUser.value = user
    }

    fun updateUser(user: UserModel) {
        _currentUser.value = user
    }

    fun logout() {
        TokenStorage.clear()
        _currentUser.value = null
    }
}
