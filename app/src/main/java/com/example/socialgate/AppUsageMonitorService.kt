package com.example.socialgate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class AppUsageMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var lastTrackedApp: String? = null
    private var startTime: Long = 0

    private lateinit var dbHelper: SocialDatabaseHelper

    private var userId: Int = -1
    private val targetApps = setOf("com.facebook.katana", "com.instagram.android")

    override fun onCreate() {
        super.onCreate()
        dbHelper = SocialDatabaseHelper(this) // Inicializa el helper aquí
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getIntExtra("USER_ID", -1) ?: -1

        if (userId == -1) {
            Log.e("AppMonitor", "Servicio iniciado sin un USER_ID válido. Deteniendo.")
            stopSelf() // Detiene el servicio
            return START_NOT_STICKY
        }

        val notification = Notification.Builder(this, "USAGE_MONITOR_CHANNEL")
            .setContentTitle("SocialGate")
            .setContentText("Monitoreando el uso de aplicaciones.")
            .setSmallIcon(R.drawable.LogoSocialGate)
            .build()

        createNotificationChannel()

        startForeground(1, notification)

        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        runnable = Runnable {
            val foregroundApp = getForegroundApp()

            if (targetApps.contains(foregroundApp)) {

                if (foregroundApp != lastTrackedApp) {

                    startTime = System.currentTimeMillis()
                    lastTrackedApp = foregroundApp
                    Log.d("AppMonitor", "Empezó a usar: $foregroundApp")
                }
            } else {

                if (lastTrackedApp != null) {
                    val endTime = System.currentTimeMillis()
                    val timeInSeconds = (endTime - startTime) / 1000
                    Log.d("AppMonitor", "Dejó de usar: $lastTrackedApp. Duración: $timeInSeconds segundos.")

                    updateUserUsageTime(lastTrackedApp!!, timeInSeconds)

                    lastTrackedApp = null
                }
            }


            handler.postDelayed(runnable, 2000)
        }
        handler.post(runnable)
    }


    private fun updateUserUsageTime(packageName: String, durationSeconds: Long) {
        if (userId == -1) return

        val socialNetworkName = when (packageName) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> return
        }

        val db = dbHelper.writableDatabase

        val columns = arrayOf(SocialDatabaseHelper.KEY_TIEMPO_USADO)
        val selection = "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND ${SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL} = ?"
        val selectionArgs = arrayOf(userId.toString(), socialNetworkName)

        val cursor = db.query(
            SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
            columns,
            selection,
            selectionArgs,
            null, null, null
        )

        var currentMinutes = 0
        if (cursor.moveToFirst()) {
            currentMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))
        }
        cursor.close()

        val newDurationMinutes = (durationSeconds / 60).toInt()
        val newTotalMinutes = currentMinutes + newDurationMinutes

        if (newDurationMinutes < 1) {
            db.close()
            return
        }

        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_TIEMPO_USADO, newTotalMinutes)
        }

        db.update(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, values, selection, selectionArgs)
        Log.d("AppMonitor", "Tiempo actualizado para $socialNetworkName: $newTotalMinutes minutos.")

        db.close()
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        if (stats != null && stats.isNotEmpty()) {
            return stats.sortedBy { it.lastTimeUsed }.lastOrNull()?.packageName
        }
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("USAGE_MONITOR_CHANNEL", "Monitor de Uso", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}