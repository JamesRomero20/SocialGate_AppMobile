package com.example.socialgate.controller

import android.content.Context
import com.example.socialgate.model.ScheduleRepository
import com.example.socialgate.model.SocialDatabaseHelper
import com.example.socialgate.model.TimeControlRepository
import com.example.socialgate.view.activity.ScheduleActivity
import com.example.socialgate.view.view_interfaces.ScheduleView
import androidx.core.content.edit

class ScheduleController(
    private val view: ScheduleView,
    private val userId: Int,
    private val context: Context,
    dbHelper: SocialDatabaseHelper
) {
    private val timeControlRepository = TimeControlRepository(dbHelper)
    private val scheduleRepository = ScheduleRepository(dbHelper)
    private val selectedDays = mutableSetOf<String>()
    fun loadInitialData() {
        val limitInMinutes = timeControlRepository.getTimeLimit(userId)
        val isEnabled = limitInMinutes > 0
        val limitInHours = if (isEnabled) {
            limitInMinutes / 60f
        } else {
            val prefs = context.getSharedPreferences(ScheduleActivity.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getFloat(ScheduleActivity.KEY_SAVED_LIMIT, 0f)
        }
        view.displayTimeLimit(limitInHours, isEnabled)

        val schedule = scheduleRepository.getSchedules(userId)
        selectedDays.clear()
        if (schedule != null) {
            selectedDays.addAll(schedule.days)
        }
        view.displayAcademicSchedule(schedule)
    }

    fun onTimeLimitSwitchChanged(isEnabled: Boolean, currentSliderValue: Float) {
        if (isEnabled) {
            val prefs = context.getSharedPreferences(ScheduleActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val savedLimitHours = prefs.getFloat(ScheduleActivity.KEY_SAVED_LIMIT, 0f)
            timeControlRepository.saveTimeLimit(userId, (savedLimitHours * 60).toInt())
            view.displayTimeLimit(savedLimitHours, true)
            view.showToast("Límite de tiempo activado")
        } else {
            timeControlRepository.saveTimeLimit(userId, 0)
            view.displayTimeLimit(currentSliderValue, false)
            view.showToast("Límite de tiempo desactivado")
        }
    }

    fun onTimeLimitSliderStopped(hours: Float) {
        timeControlRepository.saveTimeLimit(userId, (hours * 60).toInt())
        val prefs = context.getSharedPreferences(ScheduleActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(ScheduleActivity.KEY_SAVED_LIMIT, hours).apply()
        view.showToast("Límite de tiempo guardado")
    }

    fun onAcademicScheduleSwitchChanged(isEnabled: Boolean) {
        scheduleRepository.updateScheduleStatus(userId, isEnabled)
        if (!isEnabled) {
            view.resetAcademicScheduleUI()
            selectedDays.clear()
            view.showToast("Horario académico desactivado")
        } else {
            view.showToast("Horario académico activado")
        }
    }

    fun onDayClicked(dayName: String): Boolean {
        val isCurrentlySelected = selectedDays.contains(dayName)
        if (isCurrentlySelected) {
            selectedDays.remove(dayName)
            return false
        } else {
            selectedDays.add(dayName)
            return true
        }
    }

    fun saveNewSchedule(startTime: String, endTime: String) {
        if (selectedDays.isEmpty()) {
            view.showToast("Por favor, seleccione al menos un día")
            return
        }
        scheduleRepository.saveSchedule(userId, startTime, endTime, selectedDays)
        view.showToast("Horario guardado correctamente")
        loadInitialData()
    }
}