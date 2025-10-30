package com.example.socialgate.controller

import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.LoginView

class LoginController(private val view: LoginView, dbHelper: SocialDatabaseHelper) {

    private val userRepository = UserRepository(dbHelper)

    fun login(userInput: String, password: String) {
        if (userInput.isEmpty() || password.isEmpty()) {
            view.showValidationError("Por favor, ingrese sus credenciales")
            return
        }
        if (password.length < 6 || password.length > 20) {
            view.showValidationError("La contraseña debe tener entre 6 y 20 caracteres.")
            return
        }

        val user = userRepository.findUserByCredentials(userInput, password)

        if (user != null) {
            view.onLoginSuccess(user.name, user.id)
        } else {
            view.onLoginFailure("Credenciales incorrectas")
        }
    }
}