package com.example.gymhub

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.content.Intent

class ExerciseActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var adapter: ExerciseItemAdapter
    private lateinit var exercises: MutableList<ExerciseItem>
    private lateinit var btnAddExercise: Button
    private lateinit var btnBack: Button
    private lateinit var spinnerWorkoutSelector: Spinner

    private var workoutId: String? = null
    private var isTrainer: Boolean = false
    private var workouts: MutableList<Pair<String, String>> = mutableListOf() // id, nombre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        firestore = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.listViewExercises)
        btnAddExercise = findViewById(R.id.buttonAddExercise)
        btnBack = findViewById(R.id.buttonReturnExercise)
        spinnerWorkoutSelector = findViewById(R.id.spinnerWorkoutSelector)

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
            onDeleteClicked = { deleteExercise(it) },
            onSeriesClicked = { showSeries(it) }
        )
        listView.adapter = adapter

        // Mostrar/ocultar botón de añadir según rol
        btnAddExercise.visibility = if (isTrainer) View.VISIBLE else View.GONE
        btnAddExercise.setOnClickListener { showAddDialog() }

        // Botón volver
        btnBack.setOnClickListener { finish() }

        // Cargar workouts en el spinner
        loadWorkouts()
    }

    // 🔹 Cargar workouts en el spinner
    private fun loadWorkouts() {
        firestore.collection("workouts")
            .get()
            .addOnSuccessListener { result ->
                workouts.clear()
                for (doc in result) {
                    workouts.add(doc.id to (doc.getString("workoutName") ?: "(sin nombre)"))
                }

                val adapterSpinner = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    workouts.map { it.second }
                )
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerWorkoutSelector.adapter = adapterSpinner

                // Seleccionar el workout actual si lo hay
                workoutId?.let { id ->
                    val index = workouts.indexOfFirst { it.first == id }
                    if (index >= 0) spinnerWorkoutSelector.setSelection(index)
                }

                spinnerWorkoutSelector.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: android.view.View?,
                            position: Int,
                            id: Long
                        ) {
                            workoutId = workouts[position].first
                            loadExercises()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
            }
    }

    // 🔹 Cargar ejercicios de un workout
    private fun loadExercises() {
        val id = workoutId ?: return
        val workoutRef = firestore.collection("workouts").document(id)

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
                        workoutRef = doc.getDocumentReference("workoutId")
                    )
                    exercises.add(exercise)
                }
                adapter.notifyDataSetChanged()
                if (exercises.isEmpty()) {
                    Toast.makeText(this, "No hay ejercicios en este workout", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar ejercicios: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // 🔹 Crear ejercicio
    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exercise_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etExerciseDescription)
        val etRest = dialogView.findViewById<EditText>(R.id.etExerciseRest)
        val spinnerWorkoutDialog = dialogView.findViewById<Spinner>(R.id.spinnerWorkoutDialog)

        val adapterSpinner = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            workouts.map { it.second }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkoutDialog.adapter = adapterSpinner

        AlertDialog.Builder(this)
            .setTitle("Nuevo ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val rest = etRest.text.toString().toIntOrNull() ?: 0
                val workoutIdSelected = workouts[spinnerWorkoutDialog.selectedItemPosition].first
                val workoutRef = firestore.collection("workouts").document(workoutIdSelected)

                val newExercise = hashMapOf(
                    "name" to name,
                    "description" to description,
                    "rest" to rest,
                    "workoutId" to workoutRef
                )

                firestore.collection("exercises")
                    .add(newExercise)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ejercicio añadido", Toast.LENGTH_SHORT).show()
                        loadExercises()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // 🔹 Eliminar ejercicio
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

    // 🔹 Mostrar series de un ejercicio
    private fun showSeries(exercise: ExerciseItem) {
        val intent = Intent(this, SeriesActivity::class.java)
        intent.putExtra("exerciseId", exercise.id) // 👈 importante
        startActivity(intent)
    }


    // 🔹 Editar ejercicio
    private fun editExercise(exercise: ExerciseItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exercise_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etExerciseDescription)
        val etRest = dialogView.findViewById<EditText>(R.id.etExerciseRest)
        val spinnerWorkoutDialog = dialogView.findViewById<Spinner>(R.id.spinnerWorkoutDialog)

        etName.setText(exercise.name)
        etDescription.setText(exercise.description ?: "")
        etRest.setText(exercise.rest.toString())

        val adapterSpinner = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            workouts.map { it.second }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkoutDialog.adapter = adapterSpinner

        exercise.workoutRef?.id?.let { id ->
            val index = workouts.indexOfFirst { it.first == id }
            if (index >= 0) spinnerWorkoutDialog.setSelection(index)
        }

        AlertDialog.Builder(this)
            .setTitle("Editar ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val updates = mapOf(
                    "name" to etName.text.toString().trim(),
                    "description" to etDescription.text.toString().trim(),
                    "rest" to (etRest.text.toString().toIntOrNull() ?: 0),
                    "workoutId" to firestore.collection("workouts")
                        .document(workouts[spinnerWorkoutDialog.selectedItemPosition].first)
                )
                firestore.collection("exercises").document(exercise.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ejercicio actualizado", Toast.LENGTH_SHORT).show()
                        loadExercises()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

