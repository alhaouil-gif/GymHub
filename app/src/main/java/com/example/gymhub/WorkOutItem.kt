package com.example.gymhub.model

data class WorkOutItem(
    val id: String = "",
    val name: String,
    val level: Long,
    val time: String,
    val estimatedTime: String,
    val date: String,
    val completionProgress: String,
    val videoURL: String,
    val numEj: Int,
    val description: String? = null
)
