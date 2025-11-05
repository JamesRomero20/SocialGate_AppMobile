package com.example.socialgate.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.socialgate.R
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.controller.ManageController
import com.example.socialgate.model.User
import com.example.socialgate.view.view_interfaces.ManageView
import com.google.android.material.bottomnavigation.BottomNavigationView

class ManageActivity : AppCompatActivity(), ManageView {

    private lateinit var controller: ManageController
    private var userId: Int = -1
    private lateinit var btnGuardarDatos: Button
    private lateinit var etUsername: EditText
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private var originalUsername: String = ""
    private var originalName: String = ""
    private var originalEmail: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error de usuario. Vuelva a iniciar sesión.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        val dbHelper = SocialDatabaseHelper(this)
        controller = ManageController(this, userId, applicationContext, dbHelper)
        etUsername = findViewById(R.id.etUsername)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnGuardarDatos = findViewById(R.id.btnGuardarDatos)
        btnGuardarDatos.isEnabled = false
        setupListeners()
        setupTextWatchers()
        setupBottomNavigation()
        controller.loadInitialData()

    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_gestionar
        findViewById<View>(R.id.header_layout).requestFocus()
    }

    override fun displayUserData(user: User) {
        originalUsername = user.username
        originalName = user.name
        originalEmail = user.email
        etUsername.setText(user.username)
        etName.setText(user.name)
        etEmail.setText(user.email)
    }

    override fun showUpdateSuccess() {
        Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
        originalUsername = etUsername.text.toString().trim()
        originalName = etName.text.toString().trim()
        originalEmail = etEmail.text.toString().trim()
        etPassword.text.clear()
        btnGuardarDatos.isEnabled = false
    }

    override fun showUpdateFailure() {
        Toast.makeText(this, "No se realizaron cambios o hubo un error.", Toast.LENGTH_SHORT).show()
    }

    override fun showValidationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showUsernameTakenError() {
        Toast.makeText(this, "Ese nombre de usuario ya está en uso.", Toast.LENGTH_LONG).show()
    }

    override fun showEmailTakenError() {
        Toast.makeText(this, "Ese correo electrónico ya está en uso.", Toast.LENGTH_LONG).show()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupListeners() {
        btnGuardarDatos.setOnClickListener {
            controller.updateUserData(
                etUsername.text.toString().trim(),
                etName.text.toString().trim(),
                etEmail.text.toString().trim(),
                etPassword.text.toString().trim()
            )
        }
        findViewById<LinearLayout>(R.id.llAbout).setOnClickListener {
            showAboutDialog()
        }
        findViewById<LinearLayout>(R.id.llLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }
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
                controller.logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkForChanges()
            }
        }

        etUsername.addTextChangedListener(textWatcher)
        etName.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
    }
    private fun checkForChanges() {
        val currentUsername = etUsername.text.toString().trim()
        val currentName = etName.text.toString().trim()
        val currentEmail = etEmail.text.toString().trim()
        val currentPassword = etPassword.text.toString().trim()

        val hasChanged = currentUsername != originalUsername ||
                currentName != originalName ||
                currentEmail != originalEmail ||
                currentPassword.isNotEmpty()

        btnGuardarDatos.isEnabled = hasChanged
    }

}