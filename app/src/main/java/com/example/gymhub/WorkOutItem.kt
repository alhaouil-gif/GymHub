package com.example.gymhub.model

data class WorkOutItem(
    val id: String = "",
    val name: String = "",
    val level: Long = 0L,
    val time: String = "",
    val estimatedTime: String = "",
    val date: String = "",
    val completionProgress: String = "",
    val videoURL: String? = null,
    val numEj: Int = 0,
    val description: String? = null
) {
    val videoExists: Boolean
        get() = !videoURL.isNullOrEmpty()
}
