package com.example.socialgate

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper

    private fun userExists(db: SQLiteDatabase, username: String, email: String): Boolean {
        val query = "SELECT * FROM ${SocialDatabaseHelper.TABLE_USUARIO} WHERE " +
                "${SocialDatabaseHelper.KEY_USUARIO} = ? OR ${SocialDatabaseHelper.KEY_EMAIL} = ?"
        val cursor = db.rawQuery(query, arrayOf(username, email))

        if (cursor.moveToFirst()) {

            val existingUsername = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO))
            if (existingUsername.equals(username, ignoreCase = true)) {
                Toast.makeText(this, "El nombre de usuario ya está en uso.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "El correo electrónico ya está registrado.", Toast.LENGTH_SHORT).show()
            }
            cursor.close()
            return true
        }

        cursor.close()
        return false
    }

    private fun validateInputs(username: String, name: String, subname: String, email: String, password: String): Boolean {

        if (username.length > 20) {
            Toast.makeText(this, "El nombre de usuario no debe exceder los 20 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!username.matches("^[a-zA-Z0-9]+$".toRegex())) {
            Toast.makeText(this, "El nombre de usuario solo puede contener letras y números.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, ingrese un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (name.length > 30 || subname.length > 30) {
            Toast.makeText(this, "El nombre y el apellido no deben exceder los 30 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!name.matches("^[a-zA-Z\\s]+$".toRegex()) || !subname.matches("^[a-zA-Z\\s]+$".toRegex())) {
            Toast.makeText(this, "El nombre y el apellido solo pueden contener letras y espacios.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length > 20) {
            Toast.makeText(this, "La contraseña no debe exceder los 20 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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

            if (!validateInputs(username, name, subname, email, password)) {
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = dbHelper.writableDatabase

            if (userExists(db, username, email)) {
                db.close()
                return@setOnClickListener
            }

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
            db.close()
        }
    }
}