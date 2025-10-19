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

                val emailEditText = findViewById<EditText>(R.id.editTextPersonName)
                val passwordEditText = findViewById<EditText>(R.id.editTextPasswordName)
                val loginButton = findViewById<Button>(R.id.btnIngresar)
                val registerButton = findViewById<Button>(R.id.btnCuenta)

                loginButton.setOnClickListener {
                    handleLogin(emailEditText, passwordEditText)
                }

                registerButton.setOnClickListener {
                    val intent = Intent(this, RegisterActivity::class.java)
                    startActivity(intent)
                }

            } catch (e: Exception) {

                Log.e("MainActivity", "Error en la inicialización de la UI o DB", e)
                Toast.makeText(this, "Error crítico al iniciar la pantalla. Por favor, reinicie la aplicación.", Toast.LENGTH_LONG).show()
            }
        }

        private fun handleLogin(emailEditText: EditText, passwordEditText: EditText) {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return
            }

            var db: SQLiteDatabase? = null
            var cursor: Cursor? = null
            try {
                db = dbHelper.readableDatabase
                val selection = "${SocialDatabaseHelper.KEY_EMAIL} = ? AND ${SocialDatabaseHelper.KEY_CLAVE} = ?"
                val selectionArgs = arrayOf(email, password)

                cursor = db.query(
                    SocialDatabaseHelper.TABLE_USUARIO,
                    null,
                    selection,
                    selectionArgs,
                    null, null, null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()

                    val userIdColumnIndex = cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_ID_USUARIO)
                    val userId = cursor.getInt(userIdColumnIndex)

                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_LONG).show()
                }

            } catch (e: SQLiteException) {

                Log.e("MainActivity", "Error de base de datos durante el login", e)
                Toast.makeText(this, "Error al acceder a la base de datos.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {

                Log.e("MainActivity", "Error inesperado durante el login", e)
                Toast.makeText(this, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
            } finally {

                cursor?.close()
                db?.close()
            }
        }
    }