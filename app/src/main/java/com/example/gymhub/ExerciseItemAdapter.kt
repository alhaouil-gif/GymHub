package com.example.gymhub

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseItemAdapter(
    private val context: Context,
    private val resource: Int,
    private val items: MutableList<ExerciseItem>,
    private val onEditClicked: (ExerciseItem) -> Unit,
    private val onDeleteClicked: (ExerciseItem) -> Unit,
    private val onSeriesClicked: (ExerciseItem) -> Unit
) : ArrayAdapter<ExerciseItem>(context, resource, items) {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(resource, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val item = items[position]
        holder.bind(item)

        return view
    }

    private inner class ViewHolder(view: View) {
        private val tvName: TextView = view.findViewById(R.id.tvExerciseName)
        private val tvDescription: TextView = view.findViewById(R.id.tvExerciseDescription)
        private val tvRest: TextView = view.findViewById(R.id.tvExerciseRest)
        private val tvWorkout: TextView = view.findViewById(R.id.tvExerciseWorkout)
        private val btnEdit: Button = view.findViewById(R.id.buttonEditExercise)
        private val btnDelete: Button = view.findViewById(R.id.buttonDeleteExercise)
        private val btnSeries: Button = view.findViewById(R.id.buttonSeries)

        fun bind(item: ExerciseItem) {
            tvName.text = item.name
            tvDescription.text = item.description ?: "Sin descripción"
            tvRest.text = "Descanso: ${item.rest} s"
            tvWorkout.text = "Workout: Cargando..."

            // 🔹 Cargar el nombre del workout desde Firestore
            val workoutId = item.getWorkoutIdFromPath()
            if (!workoutId.isNullOrEmpty()) {
                firestore.collection("workouts").document(workoutId).get()
                    .addOnSuccessListener { doc ->
                        val workoutName = doc.getString("name") ?: workoutId
                        tvWorkout.text = "Workout: $workoutName"
                    }
                    .addOnFailureListener {
                        tvWorkout.text = "Workout: (error al cargar)"
                    }
            } else {
                tvWorkout.text = "Workout: (sin asignar)"
            }

            // 🔹 Controlar visibilidad de botones según rol
            val isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)
            btnEdit.visibility = if (isTrainer) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isTrainer) View.VISIBLE else View.GONE

            // 🔹 Asignar acciones
            btnEdit.setOnClickListener { onEditClicked(item) }
            btnDelete.setOnClickListener { onDeleteClicked(item) }
            btnSeries.setOnClickListener { onSeriesClicked(item) }
        }
    }
}
