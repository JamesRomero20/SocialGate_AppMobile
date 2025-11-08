
package com.example.socialgate.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.socialgate.controller.LoginController


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(LoginController.PREFS_NAME, Context.MODE_PRIVATE)

        val savedUserId = prefs.getInt(LoginController.KEY_LOGGED_IN_USER_ID, -1)

        if (savedUserId != -1) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_ID", savedUserId)
            startActivity(intent)
        } else {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}