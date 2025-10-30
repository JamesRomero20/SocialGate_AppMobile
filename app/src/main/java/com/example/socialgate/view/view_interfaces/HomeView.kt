package com.example.socialgate.view.view_interfaces

import com.example.socialgate.model.HomeDisplayData

interface HomeView {
    fun displayDashboardData(data: HomeDisplayData)
    fun displayError(message: String)
}