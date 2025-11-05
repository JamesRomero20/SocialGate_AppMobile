package com.example.socialgate.controller

import android.content.Context
import android.content.Intent
import com.example.socialgate.model.ScheduleRepository
import com.example.socialgate.model.UserRepository
import com.example.socialgate.service.AppUsageMonitorService
import com.example.socialgate.view.view_interfaces.ManageView

class ManageController(
    private val view: ManageView,
    private val userId: Int,
    private val context: Context,
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository
) {
    fun loadInitialData() {
        val user = userRepository.findUserById(userId)
        if (user != null) {
            view.displayUserData(user)
        } else {
            view.showUpdateFailure()
        }
    }

    fun updateUserData(username: String, name: String, email: String, password: String) {
        if (username.isEmpty() || name.isEmpty() || email.isEmpty()) {
            view.showValidationError("Usuario, nombre y correo no pueden estar vacíos.")
            return
        }
        if (!isValid(username, name, email, password)) {
            return
        }

        if (userRepository.isUsernameTaken(username, userId)) {
            view.showUsernameTakenError()
            return
        }

        if (userRepository.isEmailTaken(email, userId)) {
            view.showEmailTakenError()
            return
        }

        if (password.isNotEmpty()) {
            if (userRepository.checkPasswordExists(password)) {
                view.showValidationError("La contraseña ya está en uso. Por favor, ingrese otra.")
                return
            }
        }


        val rowsAffected = userRepository.updateUser(userId, username, name, email, password)
        if (rowsAffected > 0) {
            view.showUpdateSuccess()
        } else {
            view.showUpdateFailure()
        }
    }

    fun logout() {
        context.stopService(Intent(context, AppUsageMonitorService::class.java))
        scheduleRepository.deleteSchedulesForUser(userId)
        view.navigateToLogin()
    }

    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        return emailRegex.matches(email)
    }

    private fun isValid(username: String, name: String, email: String, password: String): Boolean {
        if (!username.matches("^[a-zA-Z0-9]+$".toRegex()) || username.length > 15) {
            view.showValidationError("El nombre de usuario solo puede contener letras, números y no debe exceder los 15 caracteres.")
            return false
        }
        if (!isEmailValid(email)) {
            view.showValidationError("Por favor, ingrese un correo electrónico válido.")
            return false
        }
        if (name.length > 60 ) {
            view.showValidationError("El nombre y el apellido no deben exceder los 60 caracteres.")
            return false
        }
        if (!name.matches("^[a-zA-Z\\s]+$".toRegex())) {
            view.showValidationError("El nombre solo puede contener letras y espacios.")
            return false
        }
        if (password.isNotEmpty() && (password.length < 6 || password.length > 20)) {
            view.showValidationError("La nueva contraseña debe tener entre 6 y 20 caracteres.")
            return false
        }
        return true
    }
}