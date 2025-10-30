package com.example.socialgate.view.view_interfaces

interface RegisterView {
    fun onRegistrationSuccess()
    fun onRegistrationFailure()
    fun showValidationError(message: String)
    fun showUserExistsError(message: String)
    fun passwordsDoNotMatchError()
}