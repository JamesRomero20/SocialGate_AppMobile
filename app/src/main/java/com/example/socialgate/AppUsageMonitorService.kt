package com.example.socialgate

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.database.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppUsageMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var lastTrackedApp: String? = null
    private var startTime: Long = 0

    private lateinit var dbHelper: SocialDatabaseHelper

    private var userId: Int = -1
    private val targetApps = setOf("com.facebook.katana", "com.instagram.android")

    private fun mostrarAlertaDeUso(appName: String) {
        val channelId = "USAGE_ALERT_CHANNEL"
        val notificationId = 2

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Uso", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logosocial)
            .setContentTitle("Alerta de Uso de Red Social")
            .setContentText("Has comenzado a usar $appName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } else {
            Log.w("AppMonitor", "No se puede mostrar la alerta: permiso POST_NOTIFICATIONS denegado.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            dbHelper = SocialDatabaseHelper(this)
        } catch (e: Exception) {
            Log.e("AppMonitor", "Error al inicializar DatabaseHelper. Deteniendo servicio.", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            userId = intent?.getIntExtra("USER_ID", -1) ?: -1

            if (userId == -1) {
                Log.e("AppMonitor", "Servicio iniciado sin un USER_ID válido. Deteniendo.")
                stopSelf()
                return START_NOT_STICKY
            }

            createNotificationChannel()

            val notification = NotificationCompat.Builder(this, "USAGE_MONITOR_CHANNEL")
                .setContentTitle("SocialGate")
                .setContentText("Monitoreando el uso de aplicaciones.")
                .setSmallIcon(R.drawable.logosocial)
                .build()

            startForeground(1, notification)
            startMonitoring()
        }catch (e: Exception) {
            Log.e("AppMonitor", "Error crítico al iniciar el servicio. Deteniendo.", e)
            stopSelf()
        }
        return START_STICKY

    }

    private fun startMonitoring() {
        if (runnable != null) return
        runnable = Runnable {
            try {
                val foregroundApp = getForegroundApp()

                if (targetApps.contains(foregroundApp)) {

                    if (foregroundApp != lastTrackedApp) {

                        startTime = System.currentTimeMillis()
                        lastTrackedApp = foregroundApp
                        Log.d("AppMonitor", "Empezó a usar: $foregroundApp")
                        val appName = if (foregroundApp == "com.facebook.katana") "Facebook" else "Instagram"
                        mostrarAlertaDeUso(appName)
                    }
                } else {

                    if (lastTrackedApp != null) {
                        val endTime = System.currentTimeMillis()
                        val timeInSeconds = (endTime - startTime) / 1000
                        Log.d(
                            "AppMonitor",
                            "Dejó de usar: $lastTrackedApp. Duración: $timeInSeconds segundos."
                        )

                        updateUserUsageTime(lastTrackedApp!!, timeInSeconds)

                        lastTrackedApp = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AppMonitor", "Error dentro del bucle de monitoreo", e)
            } finally {
                handler.postDelayed(runnable!!, 2000)
            }
        }
        runnable?.let { handler.post(it) }

    }

    private fun updateUserUsageTime(packageName: String, durationSeconds: Long) {
        if (userId == -1 || durationSeconds < 10) return

        val socialNetworkName = when (packageName) {
            "com.facebook.katana" -> "Facebook"
            "com.instagram.android" -> "Instagram"
            else -> return
        }

        var db: SQLiteDatabase? = null
        try {
            db = dbHelper.writableDatabase
            val columns = arrayOf(SocialDatabaseHelper.KEY_TIEMPO_USADO)
            val selection =
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND ${SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL} = ?"
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
                currentMinutes =
                    cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))
            }
            cursor.close()

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val activityValues = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
                put(SocialDatabaseHelper.KEY_RED_SOCIAL_ACTIVIDAD, socialNetworkName)
                put(SocialDatabaseHelper.KEY_FECHA_HORA, sdf.format(Date()))
                put(SocialDatabaseHelper.KEY_DURACION, durationSeconds.toInt())
                put(SocialDatabaseHelper.KEY_DESCRIPCION, "Uso de la aplicación.")
            }
            db.insert(SocialDatabaseHelper.TABLE_ACTIVIDAD, null, activityValues)

            val newDurationMinutes = (durationSeconds / 60).toInt()
            if (newDurationMinutes < 1) {

                return
            }

            val newTotalMinutes = currentMinutes + newDurationMinutes
            val values = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_TIEMPO_USADO, newTotalMinutes)
            }

            db.update(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, values, selection, selectionArgs)
            Log.d("AppMonitor", "Tiempo actualizado para $socialNetworkName: $newTotalMinutes minutos.")

        }catch (e: SQLiteException) {
            Log.e("AppMonitor", "Error de BD al actualizar tiempo de uso", e)
        } catch (e: Exception) {
            Log.e("AppMonitor", "Error inesperado al actualizar tiempo de uso", e)
        } finally {
            db?.close()
        }
    }

    private fun getForegroundApp(): String? {
        try {

            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )

            if (stats != null && stats.isNotEmpty()) {
                return stats.sortedBy { it.lastTimeUsed }.lastOrNull()?.packageName
            }
        } catch (e: Exception) {
            Log.e("AppMonitor", "Error al obtener la app en primer plano", e)
        }
        return null
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("USAGE_MONITOR_CHANNEL", "Monitor de Uso", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}