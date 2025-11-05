package com.example.gymhub

data class ExerciseItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var rest: Int = 0,
    var workoutPath: String? = null,
    val workoutName: String? = null,
    var createdBy: String? = null
) {

    fun getWorkoutIdFromPath(): String? {
        val path = workoutPath ?: return null
        val parts = path.trim().split("/")
        return if (parts.isNotEmpty()) parts.last().takeIf { it.isNotBlank() } else null
    }
}
