
package com.example.socialgate.model

import android.content.ContentValues

data class ScheduleItem(val startTime: String, val endTime: String, val days: Set<String>, val isActive: Boolean)

class ScheduleRepository(private val dbHelper: SocialDatabaseHelper) {

    fun getSchedules(userId: Int): ScheduleItem? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
            arrayOf(
                SocialDatabaseHelper.KEY_HORA_INICIO,
                SocialDatabaseHelper.KEY_HORA_FIN,
                SocialDatabaseHelper.KEY_DIA_SEMANA,
                SocialDatabaseHelper.KEY_HORARIO_ACTIVO
            ),
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString()), null, null, null
        )

        var startTime = ""
        var endTime = ""
        var isActive = false
        val selectedDays = mutableSetOf<String>()

        cursor.use {
            while (it.moveToNext()) {
                if (it.isFirst) {
                    startTime =
                        it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORA_INICIO))
                    endTime =
                        it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORA_FIN))
                    isActive =
                        it.getInt(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORARIO_ACTIVO)) == 1
                }
                selectedDays.add(it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_DIA_SEMANA)))
            }
            if (selectedDays.isEmpty()) {
                return null
            }
            return ScheduleItem(startTime, endTime, selectedDays, isActive)
        }
    }

    fun saveSchedule(userId: Int, startTime: String, endTime: String, days: Set<String>) {
        val db = dbHelper.writableDatabase
        db.delete(
            SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString())
        )

        for (day in days) {
            val values = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
                put(SocialDatabaseHelper.KEY_DIA_SEMANA, day)
                put(SocialDatabaseHelper.KEY_HORA_INICIO, startTime)
                put(SocialDatabaseHelper.KEY_HORA_FIN, endTime)
                put(SocialDatabaseHelper.KEY_HORARIO_ACTIVO, 1)
            }
            db.insert(SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO, null, values)
        }
    }

    fun updateScheduleStatus(userId: Int, isActive: Boolean) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_HORARIO_ACTIVO, if (isActive) 1 else 0)
        }
        db.update(
            SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
            values,
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString())
        )
    }

    fun deleteSchedulesForUser(userId: Int) {
        val db = dbHelper.writableDatabase
        try {
            db.delete(
                SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString())
            )
        } catch (e: Exception) {
            android.util.Log.e("ScheduleRepo", "Error al borrar horarios para el usuario $userId", e)
        }
    }

    fun isScheduleFeatureActive(userId: Int): Boolean {
        val db = dbHelper.readableDatabase
        val query = "SELECT 1 FROM ${SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO} WHERE " +
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND " +
                "${SocialDatabaseHelper.KEY_HORARIO_ACTIVO} = 1 LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        val isActive = cursor.moveToFirst()
        cursor.close()
        return isActive
    }

}