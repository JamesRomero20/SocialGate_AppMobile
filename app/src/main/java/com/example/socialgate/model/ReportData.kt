package com.example.socialgate.model

data class ReportData(
    val userName: String = "",
    val userEmail: String = "",
    val totalUsageSeconds: Int = 0,
    val totalDaysWithActivity: Int = 0,
    val facebookUsageSeconds: Int = 0,
    val instagramUsageSeconds: Int = 0,
    val dailyUsageMinutes: FloatArray = FloatArray(7)
) {
    val averageUsageSeconds: Int
        get() = if (totalDaysWithActivity > 0) totalUsageSeconds / totalDaysWithActivity else 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ReportData
        if (userName != other.userName) return false
        if (userEmail != other.userEmail) return false
        if (totalUsageSeconds != other.totalUsageSeconds) return false
        if (totalDaysWithActivity != other.totalDaysWithActivity) return false
        if (facebookUsageSeconds != other.facebookUsageSeconds) return false
        if (instagramUsageSeconds != other.instagramUsageSeconds) return false
        if (!dailyUsageMinutes.contentEquals(other.dailyUsageMinutes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + userEmail.hashCode()
        result = 31 * result + totalUsageSeconds
        result = 31 * result + totalDaysWithActivity
        result = 31 * result + facebookUsageSeconds
        result = 31 * result + instagramUsageSeconds
        result = 31 * result + dailyUsageMinutes.contentHashCode()
        return result
    }
}