package com.example.socialgate.view.activity

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialgate.R
import com.example.socialgate.controller.HomeController
import com.example.socialgate.model.HomeDisplayData
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.service.AppUsageMonitorService
import com.example.socialgate.view.view_interfaces.HomeView
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity(), HomeView {

    private lateinit var controller: HomeController
    private var userId: Int = -1
    private lateinit var tvNombre: TextView
    private lateinit var tvTiempoHoy: TextView
    private lateinit var tvLimite: TextView
    private lateinit var tvFacebookTime: TextView
    private lateinit var tvInstagramTime: TextView

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Todos los permisos necesarios han sido concedidos.", Toast.LENGTH_SHORT).show()
                startMonitoringService()
            } else {
                Toast.makeText(this, "Algunos permisos son necesarios para el funcionamiento completo de la app.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error: No se pudo obtener la información del usuario.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val dbHelper = SocialDatabaseHelper(this)
        controller = HomeController(this, userId, dbHelper)

        tvNombre = findViewById(R.id.tvNombre)
        tvTiempoHoy = findViewById(R.id.tvTiempoHoy)
        tvLimite = findViewById(R.id.tvLimite)
        tvFacebookTime = findViewById(R.id.tvFacebookTime)
        tvInstagramTime = findViewById(R.id.tvInstagramTime)

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStartService()
        controller.startUpdating()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_inicio
    }

    override fun onPause() {
        super.onPause()
        controller.stopUpdating()
    }

    override fun displayDashboardData(data: HomeDisplayData) {
        tvNombre.text = data.userName
        tvFacebookTime.text = formatSecondsToHoursAndMinutes(data.facebookTimeSeconds)
        tvInstagramTime.text = formatSecondsToHoursAndMinutes(data.instagramTimeSeconds)
        tvTiempoHoy.text = formatSecondsToHoursAndMinutes(data.totalTimeSeconds)
        tvLimite.text = "Tiempo límite establecido: ${formatMinutesToHoursAndMinutes(data.timeLimitMinutes)}"
    }

    override fun displayError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatMinutesToHoursAndMinutes(totalMinutes: Int): String {
        if (totalMinutes < 0) return "0h 0m"

        val hours = totalMinutes / 60

        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }
    private fun formatSecondsToHoursAndMinutes(totalSeconds: Int): String {
        if (totalSeconds < 0) return "0h 0m"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }
    private fun checkPermissionsAndStartService() {

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {

            startMonitoringService()
        }
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Por favor, active el permiso para que SocialGate pueda bloquear Facebook o Instagram", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
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
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            }
            true
        }
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, AppUsageMonitorService::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startService(serviceIntent)
    }
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(this, "Por favor, active el permiso para SocialGate", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

}