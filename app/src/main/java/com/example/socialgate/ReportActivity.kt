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
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream
import java.io.IOException
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.mikephil.charting.formatter.ValueFormatter
import android.graphics.drawable.BitmapDrawable
import com.itextpdf.layout.property.HorizontalAlignment

class ReportActivity : AppCompatActivity() {

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private val updateHandler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var userName: String = ""
    private var userEmail: String = ""
    private var facebookTotalSeconds: Int = 0
    private var instagramTotalSeconds: Int = 0

    private var tvTiempoTotal: TextView? = null
    private var tvTiempoPromedio: TextView? = null
    private var barChart: BarChart? = null

    private var totalHoursData: Double = 0.0
    private var averageHoursData: Double = 0.0
    private val dailyUsageData = FloatArray(7)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permiso concedido. Generando PDF...", Toast.LENGTH_SHORT).show()
                createPdf()
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede guardar el reporte.", Toast.LENGTH_LONG).show()
            }
        }


    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            updateHandler.post(updateRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        try {

            dbHelper = SocialDatabaseHelper(this)
            userId = intent.getIntExtra("USER_ID", -1)

            tvTiempoTotal = findViewById(R.id.tvTiempoTotal)
            tvTiempoPromedio = findViewById(R.id.tvTiempoPromedio)
            barChart = findViewById(R.id.barChart)

            updateRunnable = Runnable {
                lifecycleScope.launch {
                    loadUserData()
                    loadReportData()
                }
                updateHandler.postDelayed(updateRunnable, 2000)
            }

            findViewById<Button>(R.id.btnGenerarReporte).setOnClickListener {
                generarPdf()
            }

            setupBottomNavigation()

            if (userId != -1) {
                return
            } else {
                Toast.makeText(this, "Error de usuario", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e("ReportActivity", "Error crítico en la inicialización de ReportActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla de reportes.", Toast.LENGTH_LONG).show()
            finish()
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
        bottomNav.selectedItemId = R.id.nav_reporte

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId) return@setOnItemSelectedListener true
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
            startActivity(intent)
            overridePendingTransition(0, 0)
            true
        }
    }

    private suspend fun loadUserData() {
        withContext(Dispatchers.IO) {
            var db: SQLiteDatabase? = null
            try {
                db = dbHelper.readableDatabase
                val cursor = db.query(
                    SocialDatabaseHelper.TABLE_USUARIO,
                    arrayOf(SocialDatabaseHelper.KEY_NOMBRE, SocialDatabaseHelper.KEY_EMAIL),
                    "${SocialDatabaseHelper.KEY_ID_USUARIO} = ?",
                    arrayOf(userId.toString()), null, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    userName = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_NOMBRE))
                    userEmail = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_EMAIL))
                }
                cursor?.close()
            } catch (e: Exception) {
                Log.e("ReportActivity", "Error al cargar datos del usuario", e)
            } finally {
                db?.close()
            }
        }
    }

    private suspend fun loadReportData() {
        withContext(Dispatchers.IO) {
            var db: SQLiteDatabase? = null
            var totalSeconds = 0
            var totalDays = 0
            val localDailyUsage = FloatArray(7) { 0f }

            try {
                db = dbHelper.readableDatabase

                val summaryCursor = db.rawQuery(
                    "SELECT SUM(duracion), COUNT(DISTINCT date(fecha_hora)) FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                    arrayOf(userId.toString())
                )
                if (summaryCursor != null && summaryCursor.moveToFirst()) {
                    totalSeconds = summaryCursor.getInt(0)
                    totalDays = summaryCursor.getInt(1)
                }
                summaryCursor?.close()

                val appTimeCursor = db.rawQuery(
                    "SELECT ${SocialDatabaseHelper.KEY_RED_SOCIAL_ACTIVIDAD}, SUM(${SocialDatabaseHelper.KEY_DURACION}) " +
                            "FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} " +
                            "WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? " +
                            "GROUP BY ${SocialDatabaseHelper.KEY_RED_SOCIAL_ACTIVIDAD}",
                    arrayOf(userId.toString())
                )

                if (appTimeCursor != null) {
                    facebookTotalSeconds = 0
                    instagramTotalSeconds = 0
                    while (appTimeCursor.moveToNext()) {
                        val redSocial = appTimeCursor.getString(0)
                        val totalSeconds = appTimeCursor.getInt(1)
                        when (redSocial.lowercase()) {
                            "facebook" -> facebookTotalSeconds = totalSeconds
                            "instagram" -> instagramTotalSeconds = totalSeconds
                        }
                    }
                }
                appTimeCursor?.close()

                val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                val dailyCursor = db.rawQuery(
                    "SELECT duracion, fecha_hora FROM ${SocialDatabaseHelper.TABLE_ACTIVIDAD} WHERE ${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ? AND fecha_hora >= ?",
                    arrayOf(userId.toString(), dateFormat.format(sevenDaysAgo))
                )

                if (dailyCursor != null) {
                    val calendar = Calendar.getInstance()
                    while (dailyCursor.moveToNext()) {
                        val durationSeconds = dailyCursor.getInt(0)
                        val dateStr = dailyCursor.getString(1)
                        val date = dateFormat.parse(dateStr)
                        if (date != null) {
                            calendar.time = date
                            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                            localDailyUsage[dayOfWeek - 1] += durationSeconds / 60f
                        }
                    }
                }
                dailyCursor?.close()

            } catch (e: Exception) {
                Log.e("ReportActivity", "Error al cargar datos del reporte", e)
            } finally {
                db?.close()
            }

            withContext(Dispatchers.Main) {

                totalHoursData = totalSeconds / 3600.0
                averageHoursData = if (totalDays > 0) totalHoursData / totalDays else 0.0

                tvTiempoTotal?.text = formatSecondsToHoursAndMinutes(totalSeconds)

                val averageSeconds = (averageHoursData * 3600).toInt()
                tvTiempoPromedio?.text = formatSecondsToHoursAndMinutes(averageSeconds)

                localDailyUsage.copyInto(dailyUsageData)
                setupBarChart(dailyUsageData)
            }
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

        barChart?.data = BarData(dataSet)
        barChart?.description?.isEnabled = false
        barChart?.legend?.isEnabled = false
        barChart?.setFitBars(true)

        val xAxis = barChart?.xAxis
        xAxis?.valueFormatter = IndexAxisValueFormatter(orderedLabels)
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.granularity = 1f
        xAxis?.setDrawGridLines(false)

        barChart?.axisLeft?.axisMinimum = 0f
        barChart?.axisRight?.isEnabled = false

        barChart?.invalidate()
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


                    val logoDrawable = ContextCompat.getDrawable(this, R.drawable.logosocialgate)
                    if (logoDrawable is BitmapDrawable) {
                        val bitmap = logoDrawable.bitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val logoImage = Image(imageData)
                            .setHeight(100f)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        document.add(logoImage)
                        document.add(Paragraph("Control de acceso a redes sociales")
                            .setBold()
                            .setFontSize(15f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(15f))
                    }



                    document.add(Paragraph("Reporte de Actividad - SocialGate")
                        .setBold()
                        .setFontSize(20f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f))

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = dateFormat.format(Date())

                    document.add(Paragraph("Datos del Usuario").setBold().setFontSize(16f))
                    val userInfoTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f))).useAllAvailableWidth().setMarginBottom(20f)
                    userInfoTable.addCell("Nombre y Apellido:")
                    userInfoTable.addCell(userName)
                    userInfoTable.addCell("Correo Electrónico:")
                    userInfoTable.addCell(userEmail)
                    userInfoTable.addCell("Fecha de Reporte:")
                    userInfoTable.addCell(currentDate)
                    document.add(userInfoTable)

                    document.add(Paragraph("Resumen General").setBold().setFontSize(16f))
                    val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth().setMarginBottom(20f)

                    val totalSeconds = (totalHoursData * 3600).toInt()
                    val averageSeconds = (averageHoursData * 3600).toInt()

                    summaryTable.addCell("Tiempo Total de Uso:")
                    summaryTable.addCell(formatSecondsToHoursAndMinutes(totalSeconds))
                    summaryTable.addCell("Tiempo Promedio Diario:")
                    summaryTable.addCell(formatSecondsToHoursAndMinutes(averageSeconds))
                    document.add(summaryTable)

                    document.add(Paragraph("Tiempo por Red Social").setBold().setFontSize(16f))
                    val appTimeTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth().setMarginBottom(20f)
                    appTimeTable.addCell("Facebook:")
                    appTimeTable.addCell(formatSecondsToHoursAndMinutes(facebookTotalSeconds))
                    appTimeTable.addCell("Instagram:")
                    appTimeTable.addCell(formatSecondsToHoursAndMinutes(instagramTotalSeconds))
                    document.add(appTimeTable)

                    document.add(Paragraph("Uso Diario de la Última Semana").setBold().setFontSize(16f).setMarginTop(20f))
                    barChart?.let { nonNullChart ->
                        val chartBitmap = getChartBitmap(nonNullChart)
                        val stream = ByteArrayOutputStream()
                        chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val imageData = ImageDataFactory.create(stream.toByteArray())
                        val chartImage = Image(imageData)

                            .setWidth(UnitValue.createPercentValue(100F))
                            .setAutoScale(true)
                        document.add(chartImage)
                    }

                    document.close()
                    Toast.makeText(this, "PDF guardado en la carpeta Descargas/SocialGate", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Log.e("ReportActivity", "Error de I/O al generar el PDF", e)
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

