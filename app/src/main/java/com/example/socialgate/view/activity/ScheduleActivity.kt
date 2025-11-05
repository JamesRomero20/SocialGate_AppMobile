package com.example.socialgate.view.activity

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialgate.R
import com.example.socialgate.model.SocialDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.socialgate.controller.ScheduleController
import com.example.socialgate.model.ScheduleItem
import com.example.socialgate.model.ScheduleRepository
import com.example.socialgate.model.TimeControlRepository
import com.example.socialgate.view.view_interfaces.ScheduleView
import java.util.Calendar

class ScheduleActivity : AppCompatActivity(), ScheduleView {

    companion object {
        const val PREFS_NAME = "SchedulePrefs"
        const val KEY_SAVED_LIMIT = "saved_limit_hours"
    }

    private lateinit var controller: ScheduleController
    private var userId: Int = -1
    private lateinit var btnEditarHorario: Button
    private lateinit var switchLimiteTiempo: SwitchMaterial
    private lateinit var switchHorario: SwitchMaterial
    private lateinit var tvTiempoSeleccionado: TextView
    private lateinit var sliderTiempo: Slider
    private lateinit var dayTextViews: Map<String, TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        userId = intent.getIntExtra("USER_ID", -1)
        if (userId == -1) {
            Toast.makeText(this, "Error de usuario. Intente de nuevo.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val dbHelper = SocialDatabaseHelper(this)
        val timeControlRepository = TimeControlRepository(dbHelper)
        val scheduleRepository = ScheduleRepository(dbHelper)
        controller = ScheduleController(this, userId, applicationContext, timeControlRepository, scheduleRepository)
        setupViews()
        setupListeners()
        setupBottomNavigation()
        controller.loadInitialData()

    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_horario
    }

    override fun displayTimeLimit(limitInHours: Float, isEnabled: Boolean) {
        switchLimiteTiempo.isChecked = isEnabled
        sliderTiempo.isEnabled = isEnabled
        sliderTiempo.value = (Math.round(limitInHours * 100) / 100.0f)
        if (!isEnabled) {
            tvTiempoSeleccionado.text = "Sin límite"
        }
    }

    override fun displayAcademicSchedule(schedule: ScheduleItem?) {
        if (schedule != null) {
            switchHorario.isChecked = schedule.isActive
            btnEditarHorario.isEnabled = schedule.isActive
            dayTextViews.values.forEach { it.isEnabled = schedule.isActive }
            findViewById<TextView>(R.id.tvHorario).text = "${schedule.startTime} - ${schedule.endTime}"

            dayTextViews.forEach { (dayName, textView) ->
                updateDayView(textView, schedule.days.contains(dayName))
            }
        } else {
            switchHorario.isChecked = false
            btnEditarHorario.isEnabled = false
            dayTextViews.values.forEach { it.isEnabled = false }
            resetAcademicScheduleUI()
        }
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun resetAcademicScheduleUI() {
        findViewById<TextView>(R.id.tvHorario)?.text = "00:00 - 00:00"
        dayTextViews.values.forEach { textView ->
            updateDayView(textView, false)
        }
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
        btnEditarHorario = findViewById(R.id.btnEditarHorario)
    }

    private fun setupListeners() {
        btnEditarHorario.setOnClickListener { showEditScheduleDialog() }
        sliderTiempo.addOnChangeListener { _, value, _ ->
            val horas = value.toInt()
            val minutos = ((value - horas) * 60).toInt()
            tvTiempoSeleccionado.text = if (minutos == 0) "$horas horas" else "$horas horas y $minutos minutos"
        }

        sliderTiempo.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                controller.onTimeLimitSliderStopped(slider.value)
            }
        })

        switchLimiteTiempo.setOnCheckedChangeListener { _, isChecked ->
            controller.onTimeLimitSwitchChanged(isChecked, sliderTiempo.value)
        }

        switchHorario.setOnCheckedChangeListener { _, isChecked ->
            controller.onAcademicScheduleSwitchChanged(isChecked)
            btnEditarHorario.isEnabled = isChecked
            dayTextViews.values.forEach { it.isEnabled = isChecked }
        }

        dayTextViews.forEach { (dayName, textView) ->
            textView.setOnClickListener {
                val isNowSelected = controller.onDayClicked(dayName)
                updateDayView(textView, isNowSelected)
            }
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

    private fun updateDayView(textView: TextView, isSelected: Boolean) {
        if (isSelected) {
            textView.background = ContextCompat.getDrawable(this, R.drawable.day_circle_selected)
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            textView.background = ContextCompat.getDrawable(this, R.drawable.day_circle_unselected)
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun showEditScheduleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_schedule, null)
        val tvStartTime = dialogView.findViewById<TextView>(R.id.tvStartTime)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tvEndTime)
        var startTime = ""
        var endTime = ""

        tvStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                startTime = String.format("%02d:%02d", hourOfDay, minute)
                tvStartTime.text = startTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
        tvEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                endTime = String.format("%02d:%02d", hourOfDay, minute)
                tvEndTime.text = endTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Configurar Horario de Bloqueo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    controller.saveNewSchedule(startTime, endTime)
                } else {
                    Toast.makeText(this, "Por favor, seleccione una hora de inicio y fin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}