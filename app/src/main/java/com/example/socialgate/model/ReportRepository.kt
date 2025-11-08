package com.example.socialgate.model

import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class ReportRepository(private val dbHelper: SocialDatabaseHelper) {

    fun getReportData(userId: Int): ReportData {
        val db = dbHelper.readableDatabase
        var userName = ""
        var userEmail = ""
        var totalSeconds = 0
        var totalDays = 0
        var facebookSeconds = 0
        var instagramSeconds = 0
        val dailyUsage = FloatArray(7) { 0f }

        db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            arrayOf(SocialDatabaseHelper.KEY_NOMBRE, SocialDatabaseHelper.KEY_EMAIL),
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString()), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                userName =
                    cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
                userEmail =
                    cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL))
            }
        }

        db.rawQuery(
            "SELECT SUM(duracion), COUNT(DISTINCT date(fecha_hora)) FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                totalSeconds = cursor.getInt(0)
                totalDays = cursor.getInt(1)
            }
        }

        db.rawQuery(
            "SELECT ${SocialDatabaseHelper.KEY_RED_SOCIAL_ACTIVIDAD}, SUM(${SocialDatabaseHelper.KEY_DURACION}) FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? GROUP BY ${SocialDatabaseHelper.KEY_RED_SOCIAL_ACTIVIDAD}",
            arrayOf(userId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                when (cursor.getString(0).lowercase()) {
                    "facebook" -> facebookSeconds = cursor.getInt(1)
                    "instagram" -> instagramSeconds = cursor.getInt(1)
                }
            }
        }

        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        db.rawQuery(
            "SELECT duracion, fecha_hora FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND fecha_hora >= ?",
            arrayOf(userId.toString(), dateFormat.format(sevenDaysAgo))
        ).use { cursor ->
            val calendar = Calendar.getInstance()
            while (cursor.moveToNext()) {
                val durationSeconds = cursor.getInt(0)
                val dateStr = cursor.getString(1)
                val date = dateFormat.parse(dateStr)
                if (date != null) {
                    calendar.time = date
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    dailyUsage[dayOfWeek - 1] += durationSeconds / 60f
                }
            }
        }

        return ReportData(
            userName,
            userEmail,
            totalSeconds,
            totalDays,
            facebookSeconds,
            instagramSeconds,
            dailyUsage
        )
    }
    fun saveReport(userId: Int, tipoReporte: String, contenido: String): Long {
        val db = dbHelper.writableDatabase

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
            put(SocialDatabaseHelper.KEY_TIPO_REPORTE, tipoReporte)
            put(SocialDatabaseHelper.KEY_CONTENIDO, contenido)
            put(SocialDatabaseHelper.KEY_FECHA_GENERACION, currentDate)
        }

        return db.insert(SocialDatabaseHelper.TABLE_REPORTE, null, values)
    }
}