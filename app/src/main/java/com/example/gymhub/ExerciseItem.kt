package com.example.gymhub

import com.google.firebase.firestore.DocumentReference

data class ExerciseItem(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var rest: Int = 0,
    var workoutRef: DocumentReference? = null, // referencia al workout en Firestore
    var workoutName: String? = null            // nombre del workout para mostrar en UI
)
