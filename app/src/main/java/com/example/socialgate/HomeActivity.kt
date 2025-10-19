package com.example.socialgate

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.AppOpsManager
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Process
import android.provider.Settings
import android.util.Log

class HomeActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private var tvNombre: TextView? = null
    private var tvTiempoHoy: TextView? = null
    private var tvLimite: TextView? = null
    private var tvFacebookTime: TextView? = null
    private var tvInstagramTime: TextView? = null


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

            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission()
            } else {
                startMonitoringService()
            }
        }catch (e: Exception) {
            Log.e("HomeActivity", "Error crítico en la inicialización de HomeActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla principal. Intente de nuevo.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_inicio

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_horario -> Intent(this, ScheduleActivity::class.java)
                R.id.nav_reporte -> Intent(this, ReportActivity::class.java)
                R.id.nav_gestionar -> Intent(this, ManageActivity::class.java)
                else -> null
            }

            intent?.apply { putExtra("USER_ID", userId) }
            startActivity(intent)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            loadUserData()
            loadUsageData()
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

    private fun loadUserData() {
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
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
                tvNombre?.text = nombre
            }
        } catch (e: SQLiteException) {
            Log.e("HomeActivity", "Error de base de datos al cargar datos del usuario", e)
            Toast.makeText(this, "No se pudieron cargar los datos del usuario.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error inesperado al cargar datos del usuario", e)
            Toast.makeText(this, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
            db?.close()
        }
    }
    private fun loadUsageData() {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        try {
            db = dbHelper.readableDatabase
            cursor = db.query(
                SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
                null, "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString()), null, null, null
            )
            var totalTimeUsed = 0
            var timeLimit = 0

            tvFacebookTime?.text = formatMinutesToHours(0)
            tvInstagramTime?.text = formatMinutesToHours(0)

            while (cursor.moveToNext()) {
                val redSocial = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL))
                val tiempoUsado = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))
                val tiempoLimite = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))
                totalTimeUsed += tiempoUsado
                timeLimit = tiempoLimite
                when (redSocial.lowercase()) {
                    "facebook" -> tvFacebookTime?.text = formatMinutesToHours(tiempoUsado)
                    "instagram" -> tvInstagramTime?.text = formatMinutesToHours(tiempoUsado)
                }
            }
            tvTiempoHoy?.text = formatMinutesToHours(totalTimeUsed)
            tvLimite?.text = "Límite: ${formatMinutesToHours(timeLimit, showUnit = true)}"
        } catch (e: SQLiteException) {
            Log.e("HomeActivity", "Error de base de datos al cargar tiempo de uso", e)
            Toast.makeText(this, "No se pudo cargar el tiempo de uso.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error inesperado al cargar tiempo de uso", e)
            Toast.makeText(this, "Ocurrió un error inesperado.", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
            db?.close()
        }
    }
    private fun formatMinutesToHours(minutes: Int, showUnit: Boolean = true): String {
        val hours = minutes / 60.0
        return if (showUnit) {
            String.format("%.1f horas", hours)
        } else {
            String.format("%.1f H", hours)
        }
    }
}