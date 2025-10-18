package com.example.socialgate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = SocialDatabaseHelper(this)

        val emailEditText = findViewById<EditText>(R.id.editTextPersonName)
        val passwordEditText = findViewById<EditText>(R.id.editTextPasswordName)
        val loginButton = findViewById<Button>(R.id.btnIngresar)
        val registerButton = findViewById<Button>(R.id.btnCuenta)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = dbHelper.readableDatabase
            val selection = "${SocialDatabaseHelper.KEY_EMAIL} = ? AND ${SocialDatabaseHelper.KEY_CLAVE} = ?"
            val selectionArgs = arrayOf(email, password)

            val cursor = db.query(
                SocialDatabaseHelper.TABLE_USUARIO,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            if (cursor.count > 0) {
                if (cursor.moveToFirst()) {
                    Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()

                    val userIdColumnIndex = cursor.getColumnIndex(SocialDatabaseHelper.KEY_ID_USUARIO)
                    val userId = cursor.getInt(userIdColumnIndex)
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_LONG).show()
            }

            cursor.close()
            db.close()
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}