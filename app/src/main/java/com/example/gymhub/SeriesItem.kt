package com.example.gymhub

import com.google.firebase.firestore.DocumentReference

data class SeriesItem(
    var id: String = "",
    var name: String = "",                  // nombre de la serie
    var reps: Int = 0,                      // número de repeticiones
    var time: Int = 0,                      // tiempo en segundos
    var exerciseId: DocumentReference? = null // referencia al ejercicio en Firestore
)
