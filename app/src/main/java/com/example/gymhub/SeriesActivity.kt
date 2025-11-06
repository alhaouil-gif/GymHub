package com.example.gymhub

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SeriesActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var seriesAdapter: SeriesItemAdapter
    private lateinit var series: MutableList<SeriesItem>
    private lateinit var btnAddSeries: Button

    private var exerciseId: String? = null
    private var isTrainer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        firestore = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.listViewSeries)
        btnAddSeries = findViewById(R.id.buttonAddSeries)
        val btnReturn = findViewById<Button>(R.id.buttonReturnSeries) // 🔹 nuevo

        exerciseId = intent.getStringExtra("exerciseId")
        isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)

        if (exerciseId == null) {
            Toast.makeText(this, "No se recibió el ejercicio", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        series = mutableListOf()
        seriesAdapter = SeriesItemAdapter(
            this,
            R.layout.series_item,
            series,
            onEditClicked = { editSeries(it) },
            onDeleteClicked = { deleteSeries(it) }
        )
        listView.adapter = seriesAdapter

        // Mostrar botón de añadir solo a entrenadores
        btnAddSeries.visibility = if (isTrainer) View.VISIBLE else View.GONE
        btnAddSeries.setOnClickListener { showAddSeriesDialog() }

        // 🔹 Acción del botón volver
        btnReturn.setOnClickListener { finish() }

        loadSeries()
    }


    private fun loadSeries() {
        val id = exerciseId ?: return
        val exerciseRef = firestore.collection("exercises").document(id)

        firestore.collection("sets")
            .whereEqualTo("exerciseId", exerciseRef)
            .get()
            .addOnSuccessListener { result ->
                series.clear()
                for (doc in result) {
                    val serie = SeriesItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "(sin nombre)",
                        reps = (doc.getLong("reps") ?: 0L).toInt(),
                        time = (doc.getLong("time") ?: 0L).toInt(),
                        exerciseId = doc.getDocumentReference("exerciseId")
                    )
                    series.add(serie)
                }
                seriesAdapter.notifyDataSetChanged()

                if (series.isEmpty()) {
                    Toast.makeText(this, "No hay series para este ejercicio", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar series: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddSeriesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_series_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etSeriesName)
        val etReps = dialogView.findViewById<EditText>(R.id.etSeriesReps)
        val etDuration = dialogView.findViewById<EditText>(R.id.etSeriesDuration)
        val spinnerExerciseDialog = dialogView.findViewById<Spinner>(R.id.spinnerExerciseDialog)

        // Cargar ejercicios en el spinner
        firestore.collection("exercises")
            .get()
            .addOnSuccessListener { result ->
                val exercisesList = result.documents.map { it.id to (it.getString("name") ?: "(sin nombre)") }
                val adapterSpinner = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    exercisesList.map { it.second }
                )
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerExerciseDialog.adapter = adapterSpinner

                AlertDialog.Builder(this)
                    .setTitle("Nueva serie")
                    .setView(dialogView)
                    .setPositiveButton("Guardar") { _, _ ->
                        val name = etName.text.toString().trim()
                        val reps = etReps.text.toString().toIntOrNull() ?: 0
                        val duration = etDuration.text.toString().toIntOrNull() ?: 0
                        val selectedExerciseId = exercisesList[spinnerExerciseDialog.selectedItemPosition].first
                        saveSeries(name, reps, duration, selectedExerciseId)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun saveSeries(name: String, reps: Int, duration: Int, exerciseId: String) {
        val exerciseRef = firestore.collection("exercises").document(exerciseId)
        val newSeries = hashMapOf(
            "name" to name,
            "reps" to reps,
            "time" to duration,
            "exerciseId" to exerciseRef
        )

        firestore.collection("sets")
            .add(newSeries)
            .addOnSuccessListener {
                Toast.makeText(this, "Serie añadida", Toast.LENGTH_SHORT).show()
                loadSeries()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editSeries(seriesItem: SeriesItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_series_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etSeriesName)
        val etReps = dialogView.findViewById<EditText>(R.id.etSeriesReps)
        val etDuration = dialogView.findViewById<EditText>(R.id.etSeriesDuration)
        val spinnerExerciseDialog = dialogView.findViewById<Spinner>(R.id.spinnerExerciseDialog)

        etName.setText(seriesItem.name)
        etReps.setText(seriesItem.reps.toString())
        etDuration.setText(seriesItem.time.toString())

        firestore.collection("exercises")
            .get()
            .addOnSuccessListener { result ->
                val exercisesList = result.documents.map { it.id to (it.getString("name") ?: "(sin nombre)") }
                val adapterSpinner = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    exercisesList.map { it.second }
                )
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerExerciseDialog.adapter = adapterSpinner

                // Seleccionar el ejercicio actual
                seriesItem.exerciseId?.id?.let { id ->
                    val index = exercisesList.indexOfFirst { it.first == id }
                    if (index >= 0) spinnerExerciseDialog.setSelection(index)
                }

                AlertDialog.Builder(this)
                    .setTitle("Editar serie")
                    .setView(dialogView)
                    .setPositiveButton("Guardar") { _, _ ->
                        val updates = mapOf(
                            "name" to etName.text.toString().trim(),
                            "reps" to (etReps.text.toString().toIntOrNull() ?: 0),
                            "time" to (etDuration.text.toString().toIntOrNull() ?: 0),
                            "exerciseId" to firestore.collection("exercises")
                                .document(exercisesList[spinnerExerciseDialog.selectedItemPosition].first)
                        )
                        firestore.collection("sets").document(seriesItem.id)
                            .update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Serie actualizada", Toast.LENGTH_SHORT).show()
                                loadSeries()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun deleteSeries(seriesItem: SeriesItem) {
        firestore.collection("sets").document(seriesItem.id)
            .delete()
            .addOnSuccessListener {
                series.remove(seriesItem)
                seriesAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Serie eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
