package com.example.gymhub

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var listViewExercises: ListView
    private lateinit var adapter: ExerciseItemAdapter
    private val exercises = mutableListOf<ExerciseItem>()

    private lateinit var btnAddExercise: Button
    private lateinit var spinnerWorkouts: Spinner

    private val workoutsMap = mutableMapOf<String, String>() // workoutName -> workoutId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        firestore = FirebaseFirestore.getInstance()
        listViewExercises = findViewById(R.id.listViewExercises)
        btnAddExercise = findViewById(R.id.buttonAddExercise)
        spinnerWorkouts = findViewById(R.id.spinnerWorkoutSelector)

        val isTrainer = SesionUsuario.userAuthority == "Entrenador"

        // 🔹 Ocultar botón "Añadir" si no es entrenador
        btnAddExercise.visibility = if (isTrainer) View.VISIBLE else View.GONE

        // 🔹 Configurar adaptador
        adapter = ExerciseItemAdapter(
            this,
            R.layout.exercise_item,
            exercises,
            onEditClicked = { showEditDialog(it) },
            onDeleteClicked = { deleteExercise(it) },
            onSeriesClicked = { openSeries(it) }
        )
        listViewExercises.adapter = adapter

        // 🔹 Cargar workouts (para spinner)
        loadWorkouts()

        // 🔹 Cargar ejercicios iniciales
        loadExercises()

        // 🔹 Crear nuevo ejercicio
        btnAddExercise.setOnClickListener { showCreateDialog() }
    }

    // ========================
    // 🔹 Cargar ejercicios
    // ========================
    private fun loadExercises() {
        firestore.collection("exercises")
            .get()
            .addOnSuccessListener { snapshot ->
                exercises.clear()
                for (doc in snapshot.documents) {
                    val exercise = ExerciseItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "(sin nombre)",
                        description = doc.getString("description"),
                        rest = doc.getLong("rest") ?: 0L,
                        workoutPath = doc.getString("workoutId")
                    )
                    exercises.add(exercise)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar ejercicios", Toast.LENGTH_SHORT).show()
            }
    }

    // ========================
    // 🔹 Cargar workouts (para spinner)
    // ========================
    private fun loadWorkouts() {
        firestore.collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                workoutsMap.clear()
                val workoutNames = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("workoutName") ?: continue
                    workoutsMap[name] = doc.id
                    workoutNames.add(name)
                }

                val adapterSpinner = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    workoutNames
                )
                spinnerWorkouts.adapter = adapterSpinner
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar workouts", Toast.LENGTH_SHORT).show()
            }
    }

    // ========================
    // 🔹 Crear ejercicio
    // ========================
    private fun showCreateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exercise_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etExerciseDesc)
        val etRest = dialogView.findViewById<EditText>(R.id.etExerciseRest)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerWorkoutDialog)

        // Copiar workouts cargados al spinner del diálogo
        val adapterSpinner = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            workoutsMap.keys.toList()
        )
        spinner.adapter = adapterSpinner

        AlertDialog.Builder(this)
            .setTitle("Nuevo ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                val rest = etRest.text.toString().toLongOrNull() ?: 0L
                val selectedWorkout = spinner.selectedItem?.toString()

                if (name.isEmpty() || selectedWorkout == null) {
                    Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val workoutId = workoutsMap[selectedWorkout]!!
                val workoutPath = "/workouts/$workoutId"

                val data = hashMapOf(
                    "name" to name,
                    "description" to desc,
                    "rest" to rest,
                    "workoutId" to workoutPath
                )

                firestore.collection("exercises").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ejercicio creado", Toast.LENGTH_SHORT).show()
                        loadExercises()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al crear ejercicio", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ========================
    // 🔹 Editar ejercicio
    // ========================
    private fun showEditDialog(item: ExerciseItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exercise_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etExerciseName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etExerciseDesc)
        val etRest = dialogView.findViewById<EditText>(R.id.etExerciseRest)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerWorkoutDialog)

        etName.setText(item.name)
        etDesc.setText(item.description)
        etRest.setText(item.rest.toString())

        // Spinner de workouts
        val workoutNames = workoutsMap.keys.toList()
        val adapterSpinner = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            workoutNames
        )
        spinner.adapter = adapterSpinner

        val currentWorkoutName = workoutsMap.entries.find { it.value == item.getWorkoutIdFromPath() }?.key
        val position = workoutNames.indexOf(currentWorkoutName)
        if (position >= 0) spinner.setSelection(position)

        AlertDialog.Builder(this)
            .setTitle("Editar ejercicio")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val updatedData = hashMapOf(
                    "name" to etName.text.toString().trim(),
                    "description" to etDesc.text.toString().trim(),
                    "rest" to (etRest.text.toString().toLongOrNull() ?: 0L),
                    "workoutId" to "/workouts/${workoutsMap[spinner.selectedItem.toString()]}"
                )

                firestore.collection("exercises").document(item.id)
                    .update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ejercicio actualizado", Toast.LENGTH_SHORT).show()
                        loadExercises()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ========================
    // 🔹 Eliminar ejercicio
    // ========================
    private fun deleteExercise(item: ExerciseItem) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar ejercicio")
            .setMessage("¿Seguro que quieres eliminar '${item.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                firestore.collection("exercises").document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ejercicio eliminado", Toast.LENGTH_SHORT).show()
                        loadExercises()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ========================
    // 🔹 Abrir pantalla de Series (pendiente)
    // ========================
    private fun openSeries(item: ExerciseItem) {
        val intent = Intent(this, SeriesActivity::class.java)
        intent.putExtra("exerciseId", item.id)
        intent.putExtra("exerciseName", item.name)
        startActivity(intent)
    }
}
