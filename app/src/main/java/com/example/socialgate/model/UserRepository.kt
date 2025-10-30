package com.example.socialgate.model

import android.content.ContentValues

enum class UserExistsResult {
    NONE, USERNAME_TAKEN, EMAIL_TAKEN
}

class UserRepository(private val dbHelper: SocialDatabaseHelper) {
    fun findUserByCredentials(userInput: String, password: String): User? {
        val db = dbHelper.readableDatabase
        val selection = "((${SocialDatabaseHelper.KEY_EMAIL} = ?) OR (${SocialDatabaseHelper.KEY_USUARIO} = ?)) AND ${SocialDatabaseHelper.KEY_CLAVE} = ?"
        val selectionArgs = arrayOf(userInput, userInput, password)

        val cursor = db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            null,
            selection,
            selectionArgs,
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return User(
                    id = it.getInt(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_ID_USUARIO)),
                    name = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE)),
                    username = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO)),
                    email = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL))
                )
            }
        }
        return null
    }
    fun findUserById(userId: Int): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            SocialDatabaseHelper.TABLE_USUARIO,
            null,
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString()), null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return User(
                    id = it.getInt(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_ID_USUARIO)),
                    name = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE)),
                    username = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO)),
                    email = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL))
                )
            }
        }
        return null
    }

    fun updateUser(userId: Int, username: String, name: String, email: String, password: String?): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_USUARIO, username)
            put(SocialDatabaseHelper.KEY_NOMBRE, name)
            put(SocialDatabaseHelper.KEY_EMAIL, email)
            if (password != null && password.isNotEmpty()) {
                put(SocialDatabaseHelper.KEY_CLAVE, password)
            }
        }

        return db.update(
            SocialDatabaseHelper.TABLE_USUARIO,
            values,
            "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
            arrayOf(userId.toString())
        )
    }

    fun isUsernameOrEmailTaken(username: String, email: String, currentUserId: Int): Boolean {
        val db = dbHelper.readableDatabase
        val query = "SELECT * FROM ${SocialDatabaseHelper.TABLE_USUARIO} WHERE " +
                "(${SocialDatabaseHelper.KEY_USUARIO} = ? OR ${SocialDatabaseHelper.KEY_EMAIL} = ?) AND " +
                "${SocialDatabaseHelper.KEY_ID_USUARIO} != ?"
        val cursor = db.rawQuery(query, arrayOf(username, email, currentUserId.toString()))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun checkUserExists(username: String, email: String): UserExistsResult {
        val db = dbHelper.readableDatabase
        val query = "SELECT * FROM ${SocialDatabaseHelper.TABLE_USUARIO} WHERE " +
                "${SocialDatabaseHelper.KEY_USUARIO} = ? OR ${SocialDatabaseHelper.KEY_EMAIL} = ?"
        val cursor = db.rawQuery(query, arrayOf(username, email))

        cursor.use {
            if (it.moveToFirst()) {
                val existingUsername = it.getString(it.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_USUARIO))
                if (existingUsername.equals(username, ignoreCase = true)) {
                    return UserExistsResult.USERNAME_TAKEN
                }
                return UserExistsResult.EMAIL_TAKEN
            }
        }
        return UserExistsResult.NONE
    }

    fun createUser(username: String, fullName: String, email: String, password: String, registrationDate: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(SocialDatabaseHelper.KEY_USUARIO, username)
            put(SocialDatabaseHelper.KEY_NOMBRE, fullName)
            put(SocialDatabaseHelper.KEY_EMAIL, email)
            put(SocialDatabaseHelper.KEY_CLAVE, password)
            put(SocialDatabaseHelper.KEY_TIPO_USUARIO, "Usuario")
            put(SocialDatabaseHelper.KEY_FECHA_REGISTRO, registrationDate)
        }
        return db.insert(SocialDatabaseHelper.TABLE_USUARIO, null, values)
    }
}