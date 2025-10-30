package com.example.socialgate.controller

import android.util.Patterns
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.model.TimeControlRepository
import com.example.socialgate.model.UserExistsResult
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.RegisterView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterController(private val view: RegisterView, dbHelper: SocialDatabaseHelper) {

    private val userRepository = UserRepository(dbHelper)
    private val timeControlRepository = TimeControlRepository(dbHelper)

    fun registerUser(username: String, name: String, subname: String, email: String, password: String, confirmPass: String) {
        if (username.isEmpty() || name.isEmpty() || subname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            view.showValidationError("Por favor, complete todos los campos")
            return
        }
        if (password != confirmPass) {
            view.passwordsDoNotMatchError()
            return
        }
        if (!validateInputs(username, name, subname, email, password)) {
            return
        }

        when (userRepository.checkUserExists(username, email)) {
            UserExistsResult.USERNAME_TAKEN -> {
                view.showUserExistsError("El nombre de usuario ya está en uso.")
                return
            }
            UserExistsResult.EMAIL_TAKEN -> {
                view.showUserExistsError("El correo electrónico ya está registrado.")
                return
            }
            UserExistsResult.NONE -> { /* Continuar */ }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val newRowId = userRepository.createUser(username, "$name $subname", email, password, currentDate)

        if (newRowId != -1L) {
            timeControlRepository.createDefaultEntries(newRowId)
            view.onRegistrationSuccess()
        } else {
            view.onRegistrationFailure()
        }
    }
    private fun validateInputs(username: String, name: String, subname: String, email: String, password: String): Boolean {
        if (!username.matches("^[a-zA-Z0-9]+$".toRegex()) || username.length > 15) {
            view.showValidationError("El nombre de usuario solo puede contener letras y números, y no debe exceder los 15 caracteres.")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view.showValidationError("Por favor, ingrese un correo electrónico válido.")
            return false
        }
        if (name.length > 30 || subname.length > 30 ) {
            view.showValidationError("El nombre y el apellido no deben exceder los 30 caracteres.")
            return false
        }
        if (!name.matches("^[a-zA-Z\\s]+$".toRegex()) || !subname.matches("^[a-zA-Z\\s]+$".toRegex())) {
            view.showValidationError("El nombre y el apellido solo pueden contener letras y espacios.")
            return false
        }
        if (password.length < 6) {
            view.showValidationError("La contraseña debe tener al menos 6 caracteres.")
            return false
        }
        if (password.length > 20) {
            view.showValidationError("La contraseña no debe exceder los 20 caracteres.")
            return false
        }
        return true
    }
}