package com.example.gymhub

import android.os.Bundle
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
    private var workoutName: String? = null
    private var isTrainer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        firestore = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.listViewExercises) // 🔹 Corrige ID al de tu XML
        btnAddExercise = findViewById(R.id.buttonAddExercise) // 🔹 Corrige ID al de tu XML

        // Recuperar datos del intent
        workoutName = intent.getStringExtra("workoutName")
        isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)

        exercises = mutableListOf()

        adapter = ExerciseItemAdapter(
            this,
            R.layout.exercise_item,
            exercises,
            onEditClicked = { exercise -> editExercise(exercise) },
            onDeleteClicked = { exercise -> deleteExercise(exercise) }
        )
        listView.adapter = adapter

        // Mostrar/ocultar botón de añadir según el rol
        btnAddExercise.visibility = if (isTrainer) View.VISIBLE else View.GONE
        btnAddExercise.setOnClickListener {
            Toast.makeText(this, "Función de añadir pendiente", Toast.LENGTH_SHORT).show()
        }

        // Cargar ejercicios desde Firestore
        loadExercises()
    }

    private fun loadExercises() {
        val name = workoutName ?: return

        firestore.collection("exercises")
            .whereEqualTo("workoutName", name)
            .get()
            .addOnSuccessListener { result ->
                exercises.clear()
                for (doc in result) {
                    val exercise = ExerciseItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "(sin nombre)",
                        description = doc.getString("description"),
                        rest = (doc.getLong("rest") ?: 0L).toInt(),
                        workoutPath = doc.getString("workoutName") ?: ""
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
        // 🔹 Ya no intentamos abrir otra Activity
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
