package com.example.socialgate

import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

        private lateinit var dbHelper: SocialDatabaseHelper

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            try {
                dbHelper = SocialDatabaseHelper(this)

                val userInputEditText = findViewById<EditText>(R.id.editTextPersonName)
                val passwordEditText = findViewById<EditText>(R.id.editTextPasswordName)
                val loginButton = findViewById<Button>(R.id.btnIngresar)
                val registerButton = findViewById<Button>(R.id.btnCuenta)

                loginButton.setOnClickListener {
                    handleLogin(userInputEditText, passwordEditText)
                }

                registerButton.setOnClickListener {
                    userInputEditText.setText("")
                    passwordEditText.setText("")
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                }

            } catch (e: Exception) {

                Log.e("MainActivity", "Error en la inicialización de la UI o DB", e)
                Toast.makeText(this, "Error crítico al iniciar la pantalla. Por favor, reinicie la aplicación.", Toast.LENGTH_LONG).show()
            }
        }

        private fun handleLogin(userInputEditText: EditText, passwordEditText: EditText) {
            val userInput = userInputEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (userInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese sus credenciales", Toast.LENGTH_SHORT).show()
                return
            }

            if (!validateLoginInputs(userInput, password)) {
                return
            }

            var db: SQLiteDatabase? = null
            var cursor: Cursor? = null
            try {
                db = dbHelper.readableDatabase
                val selection = "((${SocialDatabaseHelper.KEY_EMAIL} = ?) OR (${SocialDatabaseHelper.KEY_USUARIO} = ?)) AND ${SocialDatabaseHelper.KEY_CLAVE} = ?"
                val selectionArgs = arrayOf(userInput, userInput, password)

                cursor = db.query(
                    SocialDatabaseHelper.TABLE_USUARIO,
                    null,
                    selection,
                    selectionArgs,
                    null, null, null
                )

                if (cursor != null && cursor.moveToFirst()) {

                    val userName = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
                    val userId = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_ID_USUARIO))

                    Toast.makeText(this, "¡Bienvenido, $userName!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                }

            } catch (e: SQLiteException) {

                Log.e("MainActivity", "Error de base de datos durante el login", e)
                Toast.makeText(this, "Error al acceder a la base de datos.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {

                Log.e("MainActivity", "Error inesperado durante el login", e)
                Toast.makeText(this, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
            } finally {
                cursor?.close()
            }
        }
    private fun validateLoginInputs(email: String, password: String): Boolean {

        if (password.length < 6 || password.length > 20) {
            Toast.makeText(
                this,
                "La contraseña debe tener entre 6 y 20 caracteres.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        findViewById<android.view.View>(R.id.main).requestFocus()

    }

}