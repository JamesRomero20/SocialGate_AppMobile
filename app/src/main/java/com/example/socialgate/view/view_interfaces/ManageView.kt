package com.example.socialgate.view.view_interfaces

import com.example.socialgate.model.User

interface ManageView {
    fun displayUserData(user: User)
    fun showUpdateSuccess()
    fun showUpdateFailure()
    fun showValidationError(message: String)
    fun showUserExistsError()
    fun navigateToLogin()
}