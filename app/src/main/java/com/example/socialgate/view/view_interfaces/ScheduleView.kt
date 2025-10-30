package com.example.socialgate.view.view_interfaces

import com.example.socialgate.model.ScheduleItem

interface ScheduleView {
    fun displayTimeLimit(limitInHours: Float, isEnabled: Boolean)
    fun displayAcademicSchedule(schedule: ScheduleItem?)
    fun showToast(message: String)
    fun resetAcademicScheduleUI()
}