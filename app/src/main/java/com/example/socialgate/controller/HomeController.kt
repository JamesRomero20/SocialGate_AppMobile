package com.example.socialgate.controller

import android.os.Handler
import android.os.Looper
import com.example.socialgate.model.HomeRepository
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.view.view_interfaces.HomeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeController(
    private val view: HomeView,
    private val userId: Int,
    dbHelper: SocialDatabaseHelper
) {
    private val repository = HomeRepository(dbHelper)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    init {
        setupUpdateLoop()
    }

    private fun setupUpdateLoop() {
        updateRunnable = Runnable {
            loadData()
            handler.postDelayed(updateRunnable, 1000)
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = repository.getDashboardData(userId)
                withContext(Dispatchers.Main) {
                    view.displayDashboardData(data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.displayError("No se pudieron cargar los datos.")
                }
            }
        }
    }

    fun startUpdating() {
        handler.post(updateRunnable)
    }

    fun stopUpdating() {
        handler.removeCallbacks(updateRunnable)
    }
}