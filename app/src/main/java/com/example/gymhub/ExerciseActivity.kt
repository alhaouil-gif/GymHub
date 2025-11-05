package com.example.gymhub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var adapter: ExerciseItemAdapter
    private lateinit var exercises: MutableList<ExerciseItem>
    private lateinit var btnAddExercise: Button
    private lateinit var btnBack: Button
    private var workoutId: String? = null
    private var isTrainer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        firestore = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.listViewExercises)
        btnAddExercise = findViewById(R.id.buttonAddExercise)
        btnBack = findViewById(R.id.buttonReturnExercise)

        // Recuperar workoutId del intent
        workoutId = intent.getStringExtra("workoutId")
        Log.d("ExerciseActivity", "workoutId recibido: $workoutId")


        isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)

        exercises = mutableListOf()
        adapter = ExerciseItemAdapter(
            this,
            R.layout.exercise_item,
            exercises,
            onEditClicked = { editExercise(it) },
            onDeleteClicked = { deleteExercise(it) }
        )
        listView.adapter = adapter

        // Mostrar/ocultar botón de añadir según rol
        btnAddExercise.visibility = if (isTrainer) View.VISIBLE else View.GONE
        btnAddExercise.setOnClickListener {
            Toast.makeText(this, "Función de añadir pendiente", Toast.LENGTH_SHORT).show()
        }

        // Botón volver funcional
        btnBack.setOnClickListener { finish() }

        // Cargar ejercicios
        loadExercises()
    }

    private fun loadExercises() {
        val id = workoutId ?: return

        // Obtener la referencia del workout
        val workoutRef = firestore.collection("workouts").document(id)

        // Filtrar ejercicios por la referencia
        firestore.collection("exercises")
            .whereEqualTo("workoutId", workoutRef)
            .get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (doc in result) {
                    val exercise = ExerciseItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "(sin nombre)",
                        description = doc.getString("description"),
                        rest = (doc.getLong("rest") ?: 0L).toInt(),
                        workoutRef = doc.getDocumentReference("workoutId") // 🔹 guardamos la referencia
                    )
                    exercises.add(exercise)
                }

                if (exercises.isEmpty()) {
                    Toast.makeText(this, "No hay ejercicios en este workout", Toast.LENGTH_SHORT).show()
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar ejercicios: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editExercise(exercise: ExerciseItem) {
        Toast.makeText(this, "Función de editar pendiente", Toast.LENGTH_SHORT).show()
    }

    private fun deleteExercise(exercise: ExerciseItem) {
        firestore.collection("exercises").document(exercise.id)
            .delete()
            .addOnSuccessListener {
                exercises.remove(exercise)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Ejercicio eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadExercises()
    }
}
