package com.example.socialgate

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.MediaStore
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import java.io.ByteArrayOutputStream
import java.io.IOException

class ReportActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private lateinit var tvTiempoTotal: TextView
    private lateinit var tvTiempoPromedio: TextView
    private lateinit var barChart: BarChart

    private var totalHoursData: Double = 0.0
    private var averageHoursData: Double = 0.0
    private val dailyUsageData = FloatArray(7)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permiso concedido. Generando PDF...", Toast.LENGTH_SHORT).show()
                createPdf() // Llama a la función de creación si se concede el permiso
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede guardar el reporte.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        dbHelper = SocialDatabaseHelper(this)
        userId = intent.getIntExtra("USER_ID", -1)

        tvTiempoTotal = findViewById(R.id.tvTiempoTotal)
        tvTiempoPromedio = findViewById(R.id.tvTiempoPromedio)
        barChart = findViewById(R.id.barChart)

        findViewById<Button>(R.id.btnGenerarReporte).setOnClickListener {
            generarPdf()
        }

        if (userId != -1) {
            loadReportData()
        } else {
            Toast.makeText(this, "Error de usuario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReportData() {
        val db = dbHelper.readableDatabase
        var totalMinutes = 0
        var totalDays = 0

        var cursor = db.rawQuery("SELECT SUM(duracion), COUNT(DISTINCT date(fecha_hora)) FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?", arrayOf(userId.toString()))
        if (cursor.moveToFirst()) {
            totalMinutes = cursor.getInt(0)
            totalDays = cursor.getInt(1)
        }
        cursor.close()

        totalHoursData = if (totalMinutes > 0) totalMinutes / 60.0 else 0.0
        averageHoursData = if (totalDays > 0) totalHoursData / totalDays else 0.0

        dailyUsageData.fill(0f)

        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        cursor = db.rawQuery("SELECT duracion, fecha_hora FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND fecha_hora >= ?", arrayOf(userId.toString(), dateFormat.format(sevenDaysAgo)))

        val calendar = Calendar.getInstance()
        while(cursor.moveToNext()){
            val duration = cursor.getInt(0)
            val dateStr = cursor.getString(1)
            val date = dateFormat.parse(dateStr)
            if(date != null){
                calendar.time = date
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                dailyUsageData[dayOfWeek - 1] += duration / 60f
            }
        }
        cursor.close()
        db.close()

        tvTiempoTotal.text = String.format("%.1f H", totalHoursData)
        tvTiempoPromedio.text = String.format("%.1f H", averageHoursData)
        setupBarChart(dailyUsageData)
    }

    private fun setupBarChart(dailyData: FloatArray) {

        if (dailyData.size < 7) return

        val entries = ArrayList<BarEntry>()
        val labels = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

        val orderedLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        entries.add(BarEntry(0f, dailyData[1]))
        entries.add(BarEntry(1f, dailyData[2]))
        entries.add(BarEntry(2f, dailyData[3]))
        entries.add(BarEntry(3f, dailyData[4]))
        entries.add(BarEntry(4f, dailyData[5]))
        entries.add(BarEntry(5f, dailyData[6]))
        entries.add(BarEntry(6f, dailyData[0]))

        val dataSet = BarDataSet(entries, "Tiempo de uso (horas)")
        dataSet.color = ContextCompat.getColor(this, R.color.header_blue)
        dataSet.valueTextSize = 10f

        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setFitBars(true)

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(orderedLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        barChart.invalidate() // Refrescar el gráfico
    }

    private fun generarPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createPdf()
        } else {

            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    createPdf()
                }
                else -> {

                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun createPdf() {
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

                    document.add(Paragraph("Reporte de Actividad - SocialGate")
                        .setBold()
                        .setFontSize(20f)
                        .setTextAlignment(TextAlignment.CENTER))


                    document.add(Paragraph("Resumen General").setBold().setFontSize(16f).setMarginTop(20f))
                    val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
                    summaryTable.addCell("Tiempo Total de Uso:")
                    summaryTable.addCell(String.format("%.1f horas", totalHoursData))
                    summaryTable.addCell("Tiempo Promedio Diario:")
                    summaryTable.addCell(String.format("%.1f horas", averageHoursData))
                    document.add(summaryTable)


                    document.add(Paragraph("Uso Diario de la Última Semana").setBold().setFontSize(16f).setMarginTop(20f))
                    val chartBitmap = getChartBitmap(barChart)
                    val stream = ByteArrayOutputStream()
                    chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val imageData = ImageDataFactory.create(stream.toByteArray())
                    val chartImage = Image(imageData)
                    document.add(chartImage)

                    document.close()
                    Toast.makeText(this, "PDF guardado en la carpeta Descargas/SocialGate", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al guardar el PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getChartBitmap(chart: BarChart): Bitmap {
        chart.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(chart.width, chart.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)
        chart.isDrawingCacheEnabled = false
        return bitmap
    }

}