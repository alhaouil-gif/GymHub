package com.example.gymhub

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView

class SeriesItemAdapter(
    private val context: Context,
    private val resource: Int,
    private val items: MutableList<SeriesItem>,
    private val onEditClicked: (SeriesItem) -> Unit,
    private val onDeleteClicked: (SeriesItem) -> Unit
) : ArrayAdapter<SeriesItem>(context, resource, items) {

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
        private val tvName: TextView = view.findViewById(R.id.tvSeriesName)
        private val tvReps: TextView = view.findViewById(R.id.tvSeriesReps)
        private val tvDuration: TextView = view.findViewById(R.id.tvSeriesDuration)
        private val btnEdit: Button = view.findViewById(R.id.buttonEditSeries)
        private val btnDelete: Button = view.findViewById(R.id.buttonDeleteSeries)

        fun bind(item: SeriesItem) {
            tvName.text = item.name
            tvReps.text = "Reps: ${item.reps}"
            tvDuration.text = "Tiempo: ${item.time} s"

            val isTrainer = SesionUsuario.userAuthority.equals("Entrenador", ignoreCase = true)
            btnEdit.visibility = if (isTrainer) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isTrainer) View.VISIBLE else View.GONE

            btnEdit.setOnClickListener { onEditClicked(item) }
            btnDelete.setOnClickListener { onDeleteClicked(item) }
        }
    }
}
