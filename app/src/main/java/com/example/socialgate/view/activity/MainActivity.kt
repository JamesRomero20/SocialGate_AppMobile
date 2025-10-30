package com.example.socialgate.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socialgate.R
import com.example.socialgate.controller.LoginController
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.view.view_interfaces.LoginView

class MainActivity : AppCompatActivity(), LoginView {
        private lateinit var dbHelper: SocialDatabaseHelper
        private lateinit var controller: LoginController
        private lateinit var userInputEditText: EditText
        private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        dbHelper = SocialDatabaseHelper(this)
        controller = LoginController(this, dbHelper)
        userInputEditText = findViewById(R.id.editTextPersonName)
        passwordEditText = findViewById(R.id.editTextPasswordName)
        val loginButton = findViewById<Button>(R.id.btnIngresar)
        val registerButton = findViewById<Button>(R.id.btnCuenta)

        loginButton.setOnClickListener {
            val userInput = userInputEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            controller.login(userInput, password)
        }

        registerButton.setOnClickListener {
            userInputEditText.setText("")
            passwordEditText.setText("")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onLoginSuccess(userName: String, userId: Int) {
        Toast.makeText(this, "¡Bienvenido, $userName!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    override fun onLoginFailure(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    override fun showValidationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.main).requestFocus()
    }
}
