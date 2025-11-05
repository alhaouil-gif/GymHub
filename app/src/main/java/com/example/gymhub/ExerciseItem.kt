package com.example.gymhub

import com.google.firebase.firestore.DocumentReference

data class ExerciseItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var rest: Int = 0,
    var workoutRef: DocumentReference? = null // ⚠️ ahora es referencia
)
