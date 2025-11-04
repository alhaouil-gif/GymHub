package com.example.gymhub.model

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.gymhub.R

class WorkoutItemArrayAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<WorkOutItem>
) : ArrayAdapter<WorkOutItem>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
        val item = items[position]

        val tvName = view.findViewById<TextView>(R.id.tvWorkoutName)
        val tvLevel = view.findViewById<TextView>(R.id.tvWorkoutLevel)
        val tvEstimatedTime = view.findViewById<TextView>(R.id.tvEstimatedTime)
        val tvDate = view.findViewById<TextView>(R.id.tvWorkoutDate)
        val tvNumEj = view.findViewById<TextView>(R.id.tvWorkoutNumEj)
        val tvDescription = view.findViewById<TextView>(R.id.tvWorkoutDescription)
        val tvVideoInfo = view.findViewById<TextView?>(R.id.tvVideoInfo)

        tvName.text = "🏋️ ${item.name}"
        tvLevel.text = "Nivel: ${item.level}"
        tvEstimatedTime.text = "Tiempo estimado: ${item.estimatedTime.ifEmpty { "N/D" }}"
        tvDate.text = "Fecha: ${item.date.ifEmpty { "N/D" }}"
        tvNumEj.text = "Nº Ejercicios: ${item.numEj}"
        tvDescription.text = "Descripción: ${item.description ?: "Sin descripción"}"

        tvVideoInfo?.text = if (item.videoExists) "🎬 Video disponible" else "Sin video"

        return view
    }
}
