package com.example.socialgate.model

data class HomeDisplayData(
    val userName: String,
    val facebookTimeSeconds: Int,
    val instagramTimeSeconds: Int,
    val timeLimitMinutes: Int
) {
    val totalTimeSeconds: Int
        get() = facebookTimeSeconds + instagramTimeSeconds
}