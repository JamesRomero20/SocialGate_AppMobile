package com.example.socialgate

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
class HomeActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private lateinit var tvNombre: TextView
    private lateinit var tvTiempoHoy: TextView
    private lateinit var tvLimite: TextView
    private lateinit var tvFacebookTime: TextView
    private lateinit var tvInstagramTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dbHelper = SocialDatabaseHelper(this)
        userId = intent.getIntExtra("USER_ID", -1)

        if (userId != -1) {
            loadUserData()
            loadUsageData()

            if (!hasUsageStatsPermission()) {

                requestUsageStatsPermission()
            } else {

                val serviceIntent = Intent(this, AppUsageMonitorService::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                startService(serviceIntent)
            }

        } else {
            Toast.makeText(this, "Error: No se pudo obtener la información del usuario.", Toast.LENGTH_LONG).show()
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {

                    true
                }
                R.id.nav_horario -> {

                    val intent = Intent(this, ScheduleActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_reporte -> {

                    Toast.makeText(this, "Ir a Reportes", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_gestionar -> {

                    Toast.makeText(this, "Ir a Gestionar", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_inicio
        dbHelper = SocialDatabaseHelper(this)

        tvNombre = findViewById(R.id.tvNombre)
        tvTiempoHoy = findViewById(R.id.tvTiempoHoy)
        tvLimite = findViewById(R.id.tvLimite)
        tvFacebookTime = findViewById(R.id.tvFacebookTime)
        tvInstagramTime = findViewById(R.id.tvInstagramTime)

        userId = intent.getIntExtra("USER_ID", -1)

        if (userId != -1) {
            loadUserData()
            loadUsageData()
        } else {
            Toast.makeText(this, "Error: No se pudo obtener la información del usuario.", Toast.LENGTH_LONG).show()
            finish()
        }

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
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            arrayOf(SocialDatabaseHelper.KEY_NOMBRE),
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            val nombre = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
            tvNombre.text = nombre
        }
        cursor.close()
    }

    private fun loadUsageData() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
            null,
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        var totalTimeUsed = 0
        var timeLimit = 0

        while (cursor.moveToNext()) {
            val redSocial = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL))
            val tiempoUsado = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))
            val tiempoLimite = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))

            totalTimeUsed += tiempoUsado
            timeLimit = tiempoLimite

            when (redSocial.lowercase()) {
                "facebook" -> tvFacebookTime.text = formatMinutesToHours(tiempoUsado)
                "instagram" -> tvInstagramTime.text = formatMinutesToHours(tiempoUsado)
            }
        }
        cursor.close()

        tvTiempoHoy.text = formatMinutesToHours(totalTimeUsed)
        tvLimite.text = "Límite establecido: ${formatMinutesToHours(timeLimit, showUnit = true)}"
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