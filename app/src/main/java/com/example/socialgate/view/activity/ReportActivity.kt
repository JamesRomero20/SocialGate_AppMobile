package com.example.socialgate.view.activity

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialgate.R
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.controller.ReportController
import com.example.socialgate.model.ReportData
import com.example.socialgate.model.ReportRepository
import com.example.socialgate.view.view_interfaces.ReportView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class ReportActivity : AppCompatActivity(), ReportView {

    private lateinit var controller: ReportController
    private var userId: Int = -1

    private val activityScope = MainScope()
    private lateinit var tvTiempoTotal: TextView
    private lateinit var tvTiempoPromedio: TextView
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error de usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val dbHelper = SocialDatabaseHelper(this)
        val repository = ReportRepository(dbHelper)
        controller = ReportController(this, userId, repository)
        tvTiempoTotal = findViewById(R.id.tvTiempoTotal)
        tvTiempoPromedio = findViewById(R.id.tvTiempoPromedio)
        barChart = findViewById(R.id.barChart)
        findViewById<Button>(R.id.btnGenerarReporte).setOnClickListener {
            controller.onGeneratePdfClicked()
        }
        setupBottomNavigation()

    }

    override fun onResume() {
        super.onResume()
        activityScope.launch {
            controller.loadData()
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_reporte

    }

    override fun onPause() {
        super.onPause()
        activityScope.cancel()
    }

    override fun displayReportData(data: ReportData) {
        tvTiempoTotal.text = formatSecondsToHoursAndMinutes(data.totalUsageSeconds)
        tvTiempoPromedio.text = formatSecondsToHoursAndMinutes(data.averageUsageSeconds)
        setupBarChart(data.dailyUsageMinutes)
    }

    override fun displayError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showPdfSuccessMessage() {
        Toast.makeText(this, "PDF guardado en la carpeta Descargas/SocialGate", Toast.LENGTH_LONG)
            .show()
    }

    override fun showPdfErrorMessage(error: String) {
        Toast.makeText(this, "Error al guardar el PDF: $error", Toast.LENGTH_LONG).show()
    }

    override fun showNoDataToGenerateReportError() {
        Toast.makeText(
            this,
            "No hay datos de actividad para generar un reporte.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun generatePdf(data: ReportData) {
        val fileName = "Reporte_SocialGate_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SocialGate")
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = PdfWriter(outputStream)
                    val pdfDocument = PdfDocument(writer)
                    val document = Document(pdfDocument)

                    val logoDrawable = ContextCompat.getDrawable(this, R.drawable.logosocial)
                    if (logoDrawable is BitmapDrawable) {
                        val bitmap = logoDrawable.bitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val logoImage = Image(imageData)
                            .setHeight(100f)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        document.add(logoImage)
                        document.add(
                            Paragraph("Control de acceso a redes sociales")
                                .setBold()
                                .setFontSize(15f)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setMarginBottom(15f)
                        )
                    }

                    document.add(
                        Paragraph("Reporte de Actividad - SocialGate")
                            .setBold()
                            .setFontSize(20f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(20f)
                    )

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = dateFormat.format(Date())

                    document.add(Paragraph("Datos del Usuario").setBold().setFontSize(16f))
                    val userInfoTable = Table(
                        UnitValue.createPercentArray(
                            floatArrayOf(
                                30f,
                                70f
                            )
                        )
                    ).useAllAvailableWidth().setMarginBottom(20f)
                    userInfoTable.addCell("Nombre y Apellido:")
                    userInfoTable.addCell(data.userName)
                    userInfoTable.addCell("Correo Electrónico:")
                    userInfoTable.addCell(data.userEmail)
                    userInfoTable.addCell("Fecha de Reporte:")
                    userInfoTable.addCell(currentDate)
                    document.add(userInfoTable)

                    document.add(Paragraph("Resumen General").setBold().setFontSize(16f))
                    val summaryTable = Table(
                        UnitValue.createPercentArray(
                            floatArrayOf(
                                50f,
                                50f
                            )
                        )
                    ).useAllAvailableWidth().setMarginBottom(20f)
                    summaryTable.addCell("Tiempo Total de Uso:")
                    summaryTable.addCell(formatSecondsToHoursAndMinutes(data.totalUsageSeconds))
                    summaryTable.addCell("Tiempo Promedio Diario:")
                    summaryTable.addCell(formatSecondsToHoursAndMinutes(data.averageUsageSeconds))
                    document.add(summaryTable)

                    document.add(Paragraph("Tiempo por Red Social").setBold().setFontSize(16f))
                    val appTimeTable = Table(
                        UnitValue.createPercentArray(
                            floatArrayOf(
                                50f,
                                50f
                            )
                        )
                    ).useAllAvailableWidth().setMarginBottom(20f)
                    appTimeTable.addCell("Facebook:")
                    appTimeTable.addCell(formatSecondsToHoursAndMinutes(data.facebookUsageSeconds))
                    appTimeTable.addCell("Instagram:")
                    appTimeTable.addCell(formatSecondsToHoursAndMinutes(data.instagramUsageSeconds))
                    document.add(appTimeTable)

                    document.add(
                        Paragraph("Uso Diario de la Última Semana").setBold().setFontSize(16f)
                            .setMarginTop(20f)
                    )
                    barChart.let { nonNullChart ->
                        val chartBitmap = getChartBitmap(nonNullChart)
                        val stream = ByteArrayOutputStream()
                        chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val chartImage =
                            Image(imageData).setWidth(UnitValue.createPercentValue(100f))
                                .setAutoScale(true)
                        document.add(chartImage)
                    }
                    document.close()
                    showPdfSuccessMessage()
                }
            } catch (e: IOException) {
                showPdfErrorMessage(e.message ?: "Error desconocido")
            }
        }
    }

    private fun formatSecondsToHoursAndMinutes(totalSeconds: Int): String {
        if (totalSeconds < 0) return "0h 0m"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->

            val intent = when (item.itemId) {
                R.id.nav_inicio -> Intent(this, HomeActivity::class.java)
                R.id.nav_horario -> Intent(this, ScheduleActivity::class.java)
                R.id.nav_reporte -> Intent(this, ReportActivity::class.java)
                R.id.nav_gestionar -> Intent(this, ManageActivity::class.java)
                else -> null
            }
            intent?.apply {
                putExtra("USER_ID", userId)
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

            }

            if (intent != null && item.itemId != bottomNav.selectedItemId) {
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            true
        }
    }
    private fun setupBarChart(dailyDataInMinutes: FloatArray) {
        if (dailyDataInMinutes.size < 7) return
        val entries = ArrayList<BarEntry>()
        val orderedLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        entries.add(BarEntry(0f, dailyDataInMinutes[1]))
        entries.add(BarEntry(1f, dailyDataInMinutes[2]))
        entries.add(BarEntry(2f, dailyDataInMinutes[3]))
        entries.add(BarEntry(3f, dailyDataInMinutes[4]))
        entries.add(BarEntry(4f, dailyDataInMinutes[5]))
        entries.add(BarEntry(5f, dailyDataInMinutes[6]))
        entries.add(BarEntry(6f, dailyDataInMinutes[0]))
        val dataSet = BarDataSet(entries, "Tiempo de uso")
        dataSet.color = ContextCompat.getColor(this, R.color.header_blue)
        dataSet.valueTextSize = 10f
        dataSet.valueFormatter = HoursMinutesFormatter()
        barChart.data = BarData(dataSet)
        barChart.description?.isEnabled = false
        barChart.legend?.isEnabled = false
        barChart.setFitBars(true)
        val xAxis = barChart.xAxis
        xAxis?.valueFormatter = IndexAxisValueFormatter(orderedLabels)
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.granularity = 1f
        xAxis?.setDrawGridLines(false)
        barChart.axisLeft?.axisMinimum = 0f
        barChart.axisRight?.isEnabled = false
        barChart.invalidate()
    }

    private fun getChartBitmap(chart: BarChart): Bitmap {
        chart.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)
        chart.isDrawingCacheEnabled = false
        return bitmap
    }
    class HoursMinutesFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            if (value == 0f) {
                return ""
            }
            val totalMinutes = value.toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m"
        }
    }

}