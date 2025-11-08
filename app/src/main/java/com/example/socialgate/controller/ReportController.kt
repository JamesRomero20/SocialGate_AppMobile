package com.example.socialgate.controller

import android.os.Handler
import android.os.Looper
import com.example.socialgate.model.ReportData
import com.example.socialgate.model.ReportRepository
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.view.view_interfaces.ReportView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportController(
    private val view: ReportView,
    private val userId: Int,
    dbHelper: SocialDatabaseHelper
) {
    private val repository = ReportRepository(dbHelper)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private var currentData: ReportData? = null

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
                val data = repository.getReportData(userId)
                currentData = data
                withContext(Dispatchers.Main) {
                    view.displayReportData(data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.displayError("Error al cargar los datos del reporte.")
                }
            }
        }
    }
    fun onGeneratePdfClicked() {
        if (currentData != null && currentData!!.totalUsageSeconds > 0) {
            view.generatePdf(currentData!!)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val contenidoResumen = "Reporte de uso para ${currentData!!.userName}. " +
                            "Tiempo total: ${currentData!!.totalUsageSeconds} segundos."
                    repository.saveReport(userId, "PDF", contenidoResumen)
                } catch (e: Exception) {

                }
            }

        } else {
            view.showNoDataToGenerateReportError()
        }
    }
    fun startUpdating() {
        handler.post(updateRunnable)
    }
    fun stopUpdating() {
        handler.removeCallbacks(updateRunnable)
    }
}