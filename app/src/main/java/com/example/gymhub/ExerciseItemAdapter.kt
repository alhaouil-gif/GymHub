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
    private val onDeleteClicked: (ExerciseItem) -> Unit
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
            holder = view.tag as ViewHolder
        }

        holder.bind(items[position])
        return view
    }

    private inner class ViewHolder(view: View) {
        private val tvName: TextView = view.findViewById(R.id.tvExerciseName)
        private val tvDescription: TextView = view.findViewById(R.id.tvExerciseDescription)
        private val tvRest: TextView = view.findViewById(R.id.tvExerciseRest)
        private val tvWorkout: TextView = view.findViewById(R.id.tvExerciseWorkout)
        private val btnEdit: Button = view.findViewById(R.id.buttonEditExercise)
        private val btnDelete: Button = view.findViewById(R.id.buttonDeleteExercise)

        fun bind(item: ExerciseItem) {
            tvName.text = item.name
            tvDescription.text = item.description ?: "Sin descripción"
            tvRest.text = "Descanso: ${item.rest} s"

            // 🔹 Usamos la referencia DocumentReference directamente
            item.workoutRef?.get()
                ?.addOnSuccessListener { doc ->
                    tvWorkout.text = "Workout: ${doc.getString("name") ?: "(sin nombre)"}"
                }
                ?.addOnFailureListener {
                    tvWorkout.text = "Workout: (error al cargar)"
                } ?: run {
                tvWorkout.text = "Workout: (sin asignar)"
            }

            val isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)
            btnEdit.visibility = if (isTrainer) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isTrainer) View.VISIBLE else View.GONE

            btnEdit.setOnClickListener { onEditClicked(item) }
            btnDelete.setOnClickListener { onDeleteClicked(item) }
        }
    }
}
