package com.example.gymhub.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gymhub.R

class WorkoutHistoryArrayAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<WorkOutItem>
) : ArrayAdapter<WorkOutItem>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val item = items[position]

        val tvName = view.findViewById<TextView>(R.id.tvWorkoutName)
        val tvLevel = view.findViewById<TextView>(R.id.tvWorkoutLevel)
        val tvNumEj = view.findViewById<TextView>(R.id.tvWorkoutNumEj)
        val tvDate = view.findViewById<TextView>(R.id.tvWorkoutDate)
        val tvEstimated = view.findViewById<TextView>(R.id.tvEstimatedTime)
        val tvTotal = view.findViewById<TextView>(R.id.tvWorkoutTotal)
        val tvProgress = view.findViewById<TextView>(R.id.tvWorkoutProgress)

        tvName.text = "Nombre: ${item.name}"
        tvLevel.text = "Nivel: ${item.level}"
        tvNumEj.text = "Ejercicios: ${item.numEj}"
        tvDate.text = "Fecha: ${item.date}"
        tvEstimated.text = "Tiempo previsto: ${item.estimatedTime}"
        tvTotal.text = "Tiempo total: ${item.time}"
        tvProgress.text = "% completado: ${item.completionProgress}"

        return view
    }
}

