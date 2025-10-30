package com.example.socialgate.model

import android.content.ContentValues

class TimeControlRepository(private val dbHelper: SocialDatabaseHelper) {

    fun createDefaultEntries(userId: Long) {
        val db = dbHelper.writableDatabase
        val defaultLimit = 0

        val valuesFb = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
            put(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL, "Facebook")
            put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, defaultLimit)
            put(SocialDatabaseHelper.KEY_TIEMPO_USADO, 0)
            put(SocialDatabaseHelper.KEY_ESTADO_ALERTA, "OK")
        }
        db.insert(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, null, valuesFb)

        val valuesIg = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
            put(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL, "Instagram")
            put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, defaultLimit)
            put(SocialDatabaseHelper.KEY_TIEMPO_USADO, 0)
            put(SocialDatabaseHelper.KEY_ESTADO_ALERTA, "OK")
        }
        db.insert(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, null, valuesIg)
    }

    fun getTimeLimit(userId: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
            arrayOf(SocialDatabaseHelper.KEY_TIEMPO_LIMITE),
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString()), null, null, null, "1"
        )

        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))
            }
        }
        return 0
    }

    fun saveTimeLimit(userId: Int, limitInMinutes: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, limitInMinutes)
        }
        db.update(
            SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
            values,
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString())
        )
    }
}