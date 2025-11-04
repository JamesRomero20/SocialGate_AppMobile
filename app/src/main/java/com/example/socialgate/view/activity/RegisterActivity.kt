package com.example.socialgate.view.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import com.example.socialgate.R
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.view.view_interfaces.RegisterView
import com.example.socialgate.controller.RegisterController

class RegisterActivity : AppCompatActivity(), RegisterView {

    private lateinit var controller: RegisterController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val dbHelper = SocialDatabaseHelper(this)
        controller = RegisterController(this, dbHelper)

        val editTextUsername = findViewById<EditText>(R.id.editTextPersonNameUser)
        val editTextName = findViewById<EditText>(R.id.editTextName)
        val editTextSubName = findViewById<EditText>(R.id.editTextSubName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPasswordName)
        val editTextConfirmPassword = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.button2)

        val btnBackToLogin = findViewById<ImageButton>(R.id.btnBackToLogin)
        btnBackToLogin.setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            controller.registerUser(
                editTextUsername.text.toString().trim(),
                editTextName.text.toString().trim(),
                editTextSubName.text.toString().trim(),
                editTextEmail.text.toString().trim(),
                editTextPassword.text.toString().trim(),
                editTextConfirmPassword.text.toString().trim()
            )
        }
    }

    override fun onRegistrationSuccess() {
        Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onRegistrationFailure() {
        Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_LONG).show()
    }

    override fun showValidationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showUserExistsError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun passwordsDoNotMatchError() {
        Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
    }
}