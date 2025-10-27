package com.example.socialgate

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.app.AppOpsManager
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.os.Handler
import android.os.Looper

class HomeActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private val updateHandler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var tvNombre: TextView? = null
    private var tvTiempoHoy: TextView? = null
    private var tvLimite: TextView? = null
    private var tvFacebookTime: TextView? = null
    private var tvInstagramTime: TextView? = null


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

        try {
            dbHelper = SocialDatabaseHelper(this)
            userId = intent.getIntExtra("USER_ID", -1)

            tvNombre = findViewById(R.id.tvNombre)
            tvTiempoHoy = findViewById(R.id.tvTiempoHoy)
            tvLimite = findViewById(R.id.tvLimite)
            tvFacebookTime = findViewById(R.id.tvFacebookTime)
            tvInstagramTime = findViewById(R.id.tvInstagramTime)

            updateRunnable = Runnable {
                lifecycleScope.launch {
                    loadUserData()
                    loadUsageData()
                }
                updateHandler.postDelayed(updateRunnable, 1000)
            }

            setupBottomNavigation()

            if (userId == -1) {
                Toast.makeText(
                    this,
                    "Error: No se pudo obtener la información del usuario.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }

        }catch (e: Exception) {
            Log.e("HomeActivity", "Error crítico en la inicialización de HomeActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla principal. Intente de nuevo.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            checkPermissionsAndStartService()
            updateHandler.post(updateRunnable)
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_inicio

    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun formatMinutesToHoursAndMinutes(totalMinutes: Int): String {
        if (totalMinutes < 0) return "0h 0m"

        val hours = totalMinutes / 60

        val minutes = totalMinutes % 60
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
                overridePendingTransition(0, 0)
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
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
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

    private suspend fun loadUserData() {

        withContext(Dispatchers.IO) {
            var db: SQLiteDatabase? = null
            var cursor: Cursor? = null
            try {
                db = dbHelper.readableDatabase

                cursor = db.query(
                    SocialDatabaseHelper.TABLE_USUARIO,
                    arrayOf(SocialDatabaseHelper.KEY_NOMBRE),
                    "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
                    arrayOf(userId.toString()), null, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val nombre =
                        cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))

                    withContext(Dispatchers.Main) {
                        tvNombre?.text = nombre
                    }
                }
            } catch (e: SQLiteException) {
                Log.e("HomeActivity", "Error de base de datos al cargar datos del usuario", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "No se pudieron cargar los datos del usuario.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error inesperado al cargar datos del usuario", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                cursor?.close()
            }
        }
    }
    private suspend fun loadUsageData() {
        withContext(Dispatchers.IO) {
            var db: SQLiteDatabase? = null
            var cursor: Cursor? = null
            var totalTimeUsed = 0
            var timeLimit = 0
            var facebookTime = 0
            var instagramTime = 0
            try {
                db = dbHelper.readableDatabase
                cursor = db.query(
                    SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
                    null, "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                    arrayOf(userId.toString()), null, null, null
                )

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val redSocial =
                            cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL))
                        val tiempoUsado =
                            cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))

                        totalTimeUsed += tiempoUsado
                        timeLimit =
                            cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))

                        when (redSocial.lowercase()) {
                            "facebook" -> facebookTime = tiempoUsado
                            "instagram" -> instagramTime = tiempoUsado
                        }
                    }
                }
            } catch (e: SQLiteException) {
                Log.e("HomeActivity", "Error de base de datos al cargar tiempo de uso", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "No se pudo cargar el tiempo de uso.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error inesperado al cargar tiempo de uso", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Ocurrió un error inesperado en inicio.", Toast.LENGTH_SHORT).show()
                }
            } finally {
                cursor?.close()
            }

            withContext(Dispatchers.Main) {
                tvFacebookTime?.text = formatMinutesToHoursAndMinutes(facebookTime)
                tvInstagramTime?.text = formatMinutesToHoursAndMinutes(instagramTime)
                tvTiempoHoy?.text = formatMinutesToHoursAndMinutes(totalTimeUsed)
                tvLimite?.text = "Tiempo límite establecido: ${formatMinutesToHoursAndMinutes(timeLimit)}"
            }
        }
    }

}