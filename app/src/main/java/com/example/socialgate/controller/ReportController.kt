
package com.example.socialgate.controller

import com.example.socialgate.model.ReportData
import com.example.socialgate.model.ReportRepository
import com.example.socialgate.view.view_interfaces.ReportView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ReportController(
    private val view: ReportView,
    private val userId: Int,
    private val repository: ReportRepository
) {
    private var currentData: ReportData? = null

    suspend fun loadData() {
        try {

            val data = withContext(Dispatchers.IO) {
                repository.getReportData(userId)
            }

            currentData = data
            view.displayReportData(data)

        } catch (e: Exception) {

            view.displayError("Error al cargar los datos del reporte.")
        }
    }

    fun onGeneratePdfClicked() {

        if (currentData != null && currentData!!.totalUsageSeconds > 0) {
            view.generatePdf(currentData!!)
        } else {
            view.showNoDataToGenerateReportError()
        }
    }
}