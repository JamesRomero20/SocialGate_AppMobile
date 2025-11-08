package com.example.socialgate.controller

import android.content.Context
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.LoginView

class LoginController(private val view: LoginView, dbHelper: SocialDatabaseHelper, private val context: Context) {

    private val userRepository = UserRepository(dbHelper)
    companion object {
        const val PREFS_NAME = "SocialGatePrefs"
        const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
    }

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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_LOGGED_IN_USER_ID, user.id).apply()
            val permissionsGranted = prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)

            if (permissionsGranted) {
                view.onLoginSuccess(user.name, user.id)
            } else {
                view.navigateToPermissions(user.name, user.id)
            }
        } else {
            view.onLoginFailure("Credenciales incorrectas")
        }
    }
}