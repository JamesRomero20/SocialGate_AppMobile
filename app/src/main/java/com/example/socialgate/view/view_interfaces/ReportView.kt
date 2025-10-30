package com.example.socialgate.view.view_interfaces

import com.example.socialgate.model.ReportData

interface ReportView {
    fun displayReportData(data: ReportData)
    fun displayError(message: String)
    fun showPdfSuccessMessage()
    fun showPdfErrorMessage(error: String)
    fun showNoDataToGenerateReportError()
    fun generatePdf(data: ReportData)
}