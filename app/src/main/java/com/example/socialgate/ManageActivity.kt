package com.example.socialgate

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ManageActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private lateinit var etUsername: EditText
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        dbHelper = SocialDatabaseHelper(this)
        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            Toast.makeText(this, "Error de usuario. Vuelva a iniciar sesión.", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        etUsername = findViewById(R.id.etUsername)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        loadUserData()

        findViewById<Button>(R.id.btnGuardarDatos).setOnClickListener {
            updateUserData()
        }
        findViewById<LinearLayout>(R.id.llAbout).setOnClickListener {
            showAboutDialog()
        }
        findViewById<LinearLayout>(R.id.llLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun loadUserData() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            arrayOf(SocialDatabaseHelper.KEY_USUARIO, SocialDatabaseHelper.KEY_NOMBRE, SocialDatabaseHelper.KEY_EMAIL),
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString()), null, null, null
        )

        if (cursor.moveToFirst()) {
            etUsername.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO)))
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE)))
            etEmail.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL)))
        }
        cursor.close()
        db.close()
    }

    private fun updateUserData() {
        val username = etUsername.text.toString().trim()
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Usuario, nombre y correo no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_USUARIO, username)
            put(SocialDatabaseHelper.KEY_NOMBRE, name)
            put(SocialDatabaseHelper.KEY_EMAIL, email)
            if (password.isNotEmpty()) {
                put(SocialDatabaseHelper.KEY_CLAVE, password)
            }
        }

        val rowsAffected = db.update(
            SocialDatabaseHelper.TABLE_USUARIO,
            values,
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString())
        )

        if (rowsAffected > 0) {
            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
        }
        db.close()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Acerca de Social Gate")
            .setMessage("Social Gate v1.0\n\nUna aplicación para el monitoreo y control de acceso a redes sociales en dispositivos móviles Android.\n\nDesarrollado por James Romero")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro de que desea cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        stopService(Intent(this, AppUsageMonitorService::class.java))

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}