package com.example.socialgate.view.view_interfaces

interface LoginView {
    fun onLoginSuccess(userName: String, userId: Int)
    fun onLoginFailure(errorMessage: String)
    fun showValidationError(message: String)
    fun navigateToPermissions(userId: Int)
}