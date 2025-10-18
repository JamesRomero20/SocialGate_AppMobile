package com.example.socialgate

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        dbHelper = SocialDatabaseHelper(this)

        val editTextUsername = findViewById<EditText>(R.id.editTextPersonNameUser)
        val editTextName = findViewById<EditText>(R.id.editTextName)
        val editTextSubName = findViewById<EditText>(R.id.editTextSubName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPasswordName)
        val editTextConfirmPassword = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.button2)

        registerButton.setOnClickListener {

            val username = editTextUsername.text.toString().trim()
            val name = editTextName.text.toString().trim()
            val subname = editTextSubName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            if (username.isEmpty() || name.isEmpty() || subname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_USUARIO, username)
                put(SocialDatabaseHelper.KEY_NOMBRE, "$name $subname")
                put(SocialDatabaseHelper.KEY_EMAIL, email)
                put(SocialDatabaseHelper.KEY_CLAVE, password)
                put(SocialDatabaseHelper.KEY_TIPO_USUARIO, "Usuario")

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = sdf.format(Date())
                put(SocialDatabaseHelper.KEY_FECHA_REGISTRO, currentDate)
            }


            val newRowId = db.insert(SocialDatabaseHelper.TABLE_USUARIO, null, values)

            if (newRowId != -1L) {
                Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_LONG).show()
            }
        }
    }
}