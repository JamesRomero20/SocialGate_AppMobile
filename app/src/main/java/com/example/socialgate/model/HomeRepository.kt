package com.example.socialgate.model

class HomeRepository(private val dbHelper: SocialDatabaseHelper) {

    fun getDashboardData(userId: Int): HomeDisplayData {
        val db = dbHelper.readableDatabase
        var userName = ""
        var facebookTime = 0
        var instagramTime = 0
        var timeLimit = 0

        db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            arrayOf(SocialDatabaseHelper.KEY_NOMBRE),
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString()), null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                userName = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
            }
        }

        db.query(
            SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
            null,
            "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
            arrayOf(userId.toString()), null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val redSocial = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL))
                val tiempoUsado = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_USADO))
                timeLimit = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))

                when (redSocial.lowercase()) {
                    "facebook" -> facebookTime = tiempoUsado
                    "instagram" -> instagramTime = tiempoUsado
                }
            }
        }

        return HomeDisplayData(
            userName = userName,
            facebookTimeSeconds = facebookTime * 60,
            instagramTimeSeconds = instagramTime * 60,
            timeLimitMinutes = timeLimit
        )
    }
}