package com.example.socialgate

import android.content.ContentValues
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import androidx.core.content.ContextCompat

class ScheduleActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "SchedulePrefs"
        const val KEY_SAVED_LIMIT = "saved_limit_hours"
    }

    private lateinit var dbHelper: SocialDatabaseHelper
    private var userId: Int = -1

    private var switchLimiteTiempo: com.google.android.material.switchmaterial.SwitchMaterial? = null

    private var switchHorario: com.google.android.material.switchmaterial.SwitchMaterial? = null
    // ...
    private var tvTiempoSeleccionado: TextView? = null
    private var sliderTiempo: Slider? = null
    private var dayTextViews: Map<String, TextView>? = null
    private val selectedDays = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        try {

            dbHelper = SocialDatabaseHelper(this)
            userId = intent.getIntExtra("USER_ID", -1)

            if (userId == -1) {
                Toast.makeText(this, "Error de usuario. Intente de nuevo.", Toast.LENGTH_LONG)
                    .show()
                finish()
                return
            }

            setupViews()

            setupListeners()

            setupBottomNavigation()

            loadCurrentTimeLimit()

            loadCurrentSchedule()
        }catch (e: Exception) {
            Log.e("ScheduleActivity", "Error crítico en la inicialización de ScheduleActivity", e)
            Toast.makeText(this, "Error al cargar la pantalla de horario.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_horario
    }

    private fun setupViews() {
        tvTiempoSeleccionado = findViewById(R.id.tvTiempoSeleccionado)
        sliderTiempo = findViewById(R.id.sliderTiempo)
        dayTextViews = mapOf(
            "Lunes" to findViewById(R.id.dayLunes),
            "Martes" to findViewById(R.id.dayMartes),
            "Miércoles" to findViewById(R.id.dayMiercoles),
            "Jueves" to findViewById(R.id.dayJueves),
            "Viernes" to findViewById(R.id.dayViernes),
            "Sábado" to findViewById(R.id.daySabado),
            "Domingo" to findViewById(R.id.dayDomingo)
        )
        switchLimiteTiempo = findViewById(R.id.switchLimiteTiempo)
        switchHorario = findViewById(R.id.switchHorario)
    }

    private fun updateHorarioActivo(isActive: Boolean) {
        var db: SQLiteDatabase? = null
        try {
            db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_HORARIO_ACTIVO, if (isActive) 1 else 0)
            }
            db.update(
                SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
                values,
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString())
            )
        } catch (e: SQLiteException) {
            Log.e("ScheduleActivity", "Error al actualizar estado del horario", e)
        } finally {
            db?.close()
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnEditarHorario).setOnClickListener {
            showEditScheduleDialog()
        }

        sliderTiempo?.addOnChangeListener { _, value, _ ->
            val horas = value.toInt()
            val minutos = ((value - horas) * 60).toInt()
            tvTiempoSeleccionado?.text = if (minutos == 0) "$horas horas" else "$horas horas y $minutos minutos"
        }

        sliderTiempo?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                saveTimeLimitToDatabase(slider.value)
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putFloat(KEY_SAVED_LIMIT, slider.value).apply()
                Toast.makeText(this@ScheduleActivity, "Límite de tiempo guardado", Toast.LENGTH_SHORT).show()
            }
        })

        dayTextViews?.forEach { (dayName, textView) ->
            textView.setOnClickListener {
                toggleDaySelection(dayName, textView)
            }
        }

        switchLimiteTiempo?.setOnCheckedChangeListener { _, isChecked ->
            sliderTiempo?.isEnabled = isChecked
            if (isChecked) {
                Toast.makeText(this, "Límite de tiempo activado", Toast.LENGTH_SHORT).show()
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedLimit = prefs.getFloat(KEY_SAVED_LIMIT, 0f)
                sliderTiempo?.value = savedLimit
                saveTimeLimitToDatabase(savedLimit)
                val horas = savedLimit.toInt()
                val minutos = ((savedLimit - horas) * 60).toInt()
                tvTiempoSeleccionado?.text = if (minutos == 0) "$horas horas" else "$horas horas y $minutos minutos"
            } else {
                Toast.makeText(this, "Límite de tiempo desactivado", Toast.LENGTH_SHORT).show()
                saveTimeLimitToDatabase(0f)
                tvTiempoSeleccionado?.text = "Sin limite"
            }
        }

        switchHorario?.setOnCheckedChangeListener { _, isChecked ->
            updateHorarioActivo(isChecked)
            findViewById<Button>(R.id.btnEditarHorario).isEnabled = isChecked
            dayTextViews?.values?.forEach { it.isEnabled = isChecked }
            val message = if (isChecked) "Horario académico activado" else "Horario académico desactivado"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
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

    private fun toggleDaySelection(dayName: String, textView: TextView) {
        if (selectedDays.contains(dayName)) {
            selectedDays.remove(dayName)
            updateDayView(textView, false)
        } else {
            selectedDays.add(dayName)
            updateDayView(textView, true)
        }
    }
    private fun updateDayView(textView: TextView, isSelected: Boolean) {
        if (isSelected) {
            textView.background = ContextCompat.getDrawable(this, R.drawable.day_circle_selected)
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            textView.background = ContextCompat.getDrawable(this, R.drawable.day_circle_unselected)
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }
    private fun saveScheduleToDatabase(startTime: String, endTime: String) {

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Por favor, seleccione al menos un día", Toast.LENGTH_SHORT).show()
            return
        }
        var db: SQLiteDatabase? = null
        try {
            db = dbHelper.writableDatabase
            db.delete(
                SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString())
            )

            for (dia in selectedDays) {
                val values = ContentValues().apply {
                    put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
                    put(SocialDatabaseHelper.KEY_DIA_SEMANA, dia)
                    put(SocialDatabaseHelper.KEY_HORA_INICIO, startTime)
                    put(SocialDatabaseHelper.KEY_HORA_FIN, endTime)
                    put(SocialDatabaseHelper.KEY_HORARIO_ACTIVO, 1)
                }
                db.insert(SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO, null, values)
            }
            Toast.makeText(this, "Horario guardado correctamente", Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.tvHorario).text = "$startTime - $endTime"
        } catch (e: SQLiteException) {
            Log.e("ScheduleActivity", "Error de BD al guardar horario", e)
            Toast.makeText(this, "Error al guardar el horario.", Toast.LENGTH_SHORT).show()
        } finally {
            db?.close()
        }
    }

    private fun loadCurrentSchedule() {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        try {
            db = dbHelper.readableDatabase
            cursor = db.query(
                SocialDatabaseHelper.TABLE_HORARIO_BLOQUEO,
                arrayOf(
                    SocialDatabaseHelper.KEY_HORA_INICIO,
                    SocialDatabaseHelper.KEY_HORA_FIN,
                    SocialDatabaseHelper.KEY_DIA_SEMANA,
                    SocialDatabaseHelper.KEY_HORARIO_ACTIVO
                ),
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString()),
                null, null, null
            )
            selectedDays.clear()
            var startTime = ""
            var endTime = ""

            var isHorarioActive = false

            while (cursor.moveToNext()) {
                if (startTime.isEmpty()) {
                    startTime = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORA_INICIO))
                    endTime = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORA_FIN))
                }
                if (cursor.isFirst) {
                    isHorarioActive = cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_HORARIO_ACTIVO)) == 1
                }
                val dia = cursor.getString(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_DIA_SEMANA))
                selectedDays.add(dia)
            }

            if (startTime.isNotEmpty()) {
                findViewById<TextView>(R.id.tvHorario).text = "$startTime - $endTime"
            }

            dayTextViews?.forEach { (dayName, textView) ->
                updateDayView(textView, selectedDays.contains(dayName))
            }

            switchHorario?.isChecked = isHorarioActive
            findViewById<Button>(R.id.btnEditarHorario).isEnabled = isHorarioActive
            dayTextViews?.values?.forEach { it.isEnabled = isHorarioActive }

        } catch (e: Exception) {
            Log.e("ScheduleActivity", "Error inesperado al cargar horario", e)
            Toast.makeText(this, "Ocurrió un error inesperado en horario.", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
            db?.close()
        }

    }

    private fun showEditScheduleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_schedule, null)
        val tvStartTime = dialogView.findViewById<TextView>(R.id.tvStartTime)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tvEndTime)

        var selectedStartHour = -1
        var selectedStartMinute = -1
        var selectedEndHour = -1
        var selectedEndMinute = -1

        tvStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedStartHour = hourOfDay
                selectedStartMinute = minute
                tvStartTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }

        tvEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedEndHour = hourOfDay
                selectedEndMinute = minute
                tvEndTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Configurar Horario de Bloqueo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                if (selectedStartHour != -1 && selectedEndHour != -1) {
                    val startTime = String.format("%02d:%02d", selectedStartHour, selectedStartMinute)
                    val endTime = String.format("%02d:%02d", selectedEndHour, selectedEndMinute)
                    saveScheduleToDatabase(startTime, endTime)
                } else {
                    Toast.makeText(this, "Por favor, seleccione una hora de inicio y fin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveTimeLimitToDatabase(hours: Float) {
        val limitInMinutes = (hours * 60).toInt()
        var db: SQLiteDatabase? = null
        try {
            db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, limitInMinutes)
            }

            val rowsAffected = db.update(
                SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
                values,
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString())
            )

            if (rowsAffected == 0) {
                val valuesFb = ContentValues().apply {
                    put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
                    put(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL, "Facebook")
                    put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, limitInMinutes)
                    put(SocialDatabaseHelper.KEY_TIEMPO_USADO, 0)
                    put(SocialDatabaseHelper.KEY_ESTADO_ALERTA, "OK")
                }
                db.insert(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, null, valuesFb)

                val valuesIg = ContentValues().apply {
                    put(SocialDatabaseHelper.KEY_ID_USUARIO_FK, userId)
                    put(SocialDatabaseHelper.KEY_RED_SOCIAL_CONTROL, "Instagram")
                    put(SocialDatabaseHelper.KEY_TIEMPO_LIMITE, limitInMinutes)
                    put(SocialDatabaseHelper.KEY_TIEMPO_USADO, 0)
                    put(SocialDatabaseHelper.KEY_ESTADO_ALERTA, "OK")
                }
                db.insert(SocialDatabaseHelper.TABLE_CONTROL_TIEMPO, null, valuesIg)
            }

        } catch (e: SQLiteException) {
            Log.e("ScheduleActivity", "Error de BD al guardar límite de tiempo", e)
            Toast.makeText(this, "Error al guardar el límite de tiempo.", Toast.LENGTH_SHORT).show()
        } finally {
            db?.close()
        }
    }
    private fun loadCurrentTimeLimit() {
        var db: SQLiteDatabase? = null
        var cursor: Cursor? = null
        try {
            db = dbHelper.readableDatabase
            cursor = db.query(
                SocialDatabaseHelper.TABLE_CONTROL_TIEMPO,
                arrayOf(SocialDatabaseHelper.KEY_TIEMPO_LIMITE),
                "${SocialDatabaseHelper.KEY_ID_USUARIO_FK} = ?",
                arrayOf(userId.toString()),
                null, null, null, "1"
            )

            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedLimitHours = prefs.getFloat(KEY_SAVED_LIMIT, 0f)

            if (cursor != null && cursor.moveToFirst()) {
                val limitInMinutes =
                    cursor.getInt(cursor.getColumnIndexOrThrow(SocialDatabaseHelper.KEY_TIEMPO_LIMITE))
                val hours = limitInMinutes / 60f
                val roundedHours = Math.round(hours * 100) / 100.0f
                sliderTiempo?.value = roundedHours
                if (limitInMinutes <= 0) {
                    switchLimiteTiempo?.isChecked = false
                    sliderTiempo?.isEnabled = false
                    tvTiempoSeleccionado?.text = "Sin límite"
                    sliderTiempo?.value = savedLimitHours
                } else {
                    switchLimiteTiempo?.isChecked = true
                    sliderTiempo?.isEnabled = true
                    val hours = limitInMinutes / 60f
                    sliderTiempo?.value = hours
                }
            } else {
                switchLimiteTiempo?.isChecked = false
                sliderTiempo?.isEnabled = false
                tvTiempoSeleccionado?.text = "Sin límite"
            }
        } catch (e: SQLiteException) {
            Log.e("ScheduleActivity", "Error de BD al cargar límite de tiempo", e)
            Toast.makeText(this, "No se pudo cargar el límite de tiempo.", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
            db?.close()
        }
    }
}