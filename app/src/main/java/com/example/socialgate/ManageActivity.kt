package com.example.socialgate

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Patterns

class ManageActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private var etUsername: EditText? = null
    private var etName: EditText? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        try {
            dbHelper = SocialDatabaseHelper(this)
            userId = intent.getIntExtra("USER_ID", -1)

            if (userId == -1) {
                Toast.makeText(
                    this,
                    "Error de usuario. Vuelva a iniciar sesión.",
                    Toast.LENGTH_LONG
                ).show()
                logout()
                return
            }

            etUsername = findViewById(R.id.etUsername)
            etName = findViewById(R.id.etName)
            etEmail = findViewById(R.id.etEmail)
            etPassword = findViewById(R.id.etPassword)

            loadUserData()

            setupBottomNavigation()

            findViewById<Button>(R.id.btnGuardarDatos).setOnClickListener {
                updateUserData()
            }
            findViewById<LinearLayout>(R.id.llAbout).setOnClickListener {
                showAboutDialog()
            }
            findViewById<LinearLayout>(R.id.llLogout).setOnClickListener {
                showLogoutConfirmationDialog()
            }
        }catch (e: Exception) {
            Log.e("ManageActivity", "Error crítico en la inicialización de ManageActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla de gestión. Intente de nuevo.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_gestionar
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->

            val intent = when (item.itemId) {
                R.id.nav_inicio -> Intent(this, HomeActivity::class.java)
                R.id.nav_horario -> Intent(this, ScheduleActivity::class.java)
                R.id.nav_reporte -> Intent(this, ReportActivity::class.java)
                R.id.nav_gestionar -> Intent(this, ManageActivity::class.java)
                else -> null
            }
            intent?.apply {
                putExtra("USER_ID", userId)
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }

            if (intent != null && item.itemId != bottomNav.selectedItemId) {
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            true
        }
    }

    private fun loadUserData() {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        try {
            db = dbHelper.readableDatabase
            cursor = db.query(
                SocialDatabaseHelper.TABLE_USUARIO,
                arrayOf(SocialDatabaseHelper.KEY_USUARIO, SocialDatabaseHelper.KEY_NOMBRE, SocialDatabaseHelper.KEY_EMAIL),
                "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
                arrayOf(userId.toString()), null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                etUsername?.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO)))
                etName?.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE)))
                etEmail?.setText(cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL)))
            }
        } catch (e: SQLiteException) {
            Log.e("ManageActivity", "Error de base de datos al cargar datos del usuario", e)
            Toast.makeText(this, "No se pudieron cargar los datos del usuario.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ManageActivity", "Error inesperado al cargar datos del usuario", e)
            Toast.makeText(this, "Ocurrió un error inesperado en gestionar.", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
            db?.close()
        }

    }

    private fun updateUserData() {
        val username = etUsername?.text.toString().trim()
        val name = etName?.text.toString().trim()
        val email = etEmail?.text.toString().trim()
        val password = etPassword?.text.toString().trim()

        if (username.isEmpty() || name.isEmpty() || email.isEmpty()) {
            Toast.makeText(
                this,
                "Usuario, nombre y correo no pueden estar vacíos.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!validateInputs(username, name, email, password)) {
            return
        }

        var db: SQLiteDatabase? = null
        try {
            db = dbHelper.writableDatabase
            val query = "SELECT * FROM ${SocialDatabaseHelper.TABLE_USUARIO} WHERE " +
                    "(${SocialDatabaseHelper.KEY_USUARIO} = ? OR ${SocialDatabaseHelper.KEY_EMAIL} = ?) AND " +
                    "${SocialDatabaseHelper.KEY_ID_USUARIO} != ?"
            val cursor = db.rawQuery(query, arrayOf(username, email, userId.toString()))
            if (cursor.moveToFirst()) {
                Toast.makeText(
                    this,
                    "El usuario o correo ya está en uso por otra cuenta.",
                    Toast.LENGTH_LONG
                ).show()
                cursor.close()
                return
            }
            cursor.close()

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
        } catch (e: SQLiteException) {
            Log.e("ManageActivity", "Error de base de datos al actualizar datos", e)
            Toast.makeText(this, "Error al guardar en la base de datos.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ManageActivity", "Error inesperado al actualizar datos", e)
            Toast.makeText(this, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
        } finally {
            db?.close()
        }
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

    private fun validateInputs(username: String, name: String, email: String, password: String): Boolean {

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

        if (name.length > 30) {
            Toast.makeText(this, "El nombre no debe exceder los 30 caracteres.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!name.matches("^[a-zA-Z\\s]+$".toRegex())) {
            Toast.makeText(this, "El nombre solo puede contener letras y espacios.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isNotEmpty()) {
            if (password.length < 6) {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (password.length > 20) {
                Toast.makeText(this, "La nueva contraseña no debe exceder los 20 caracteres.", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun logout() {
        stopService(Intent(this, AppUsageMonitorService::class.java))

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}