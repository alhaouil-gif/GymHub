package com.example.gymhub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class TrainerActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etLevel: EditText
    private lateinit var etUrl: EditText
    private lateinit var etDescription: EditText
    private lateinit var etEstimatedTime: EditText
    private lateinit var etDate: EditText
    private lateinit var etNumEj: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnPlay: Button
    private lateinit var btnEjercicios: Button

    private lateinit var firestore: FirebaseFirestore
    private var workoutId: String? = null
    private var isTrainer: Boolean = false
    private var mode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trainer)

        firestore = FirebaseFirestore.getInstance()

        //   Referencias a vistas
        etName = findViewById(R.id.etWorkoutName)
        etLevel = findViewById(R.id.etWorkoutLevel)
        etUrl = findViewById(R.id.etWorkoutUrl)
        etDescription = findViewById(R.id.etWorkoutDescription)
        etEstimatedTime = findViewById(R.id.etWorkoutEstimatedTime)
        etDate = findViewById(R.id.etWorkoutDate)
        etNumEj = findViewById(R.id.etWorkoutNumEj)
        btnSave = findViewById(R.id.btnSaveWorkout)
        btnDelete = findViewById(R.id.btnDeleteWorkout)
        btnPlay = findViewById(R.id.btnPlayVideo)
        btnEjercicios = findViewById(R.id.btnEjercicios)
        val btnReturn: Button = findViewById(R.id.btnReturn)
        btnReturn.setOnClickListener { finish() }

        //   Recuperar datos del Intent
        workoutId = intent.getStringExtra("workoutId")
        isTrainer = intent.getBooleanExtra("isTrainer", false)
        mode = intent.getStringExtra("mode")
        val hidePlayButton = intent.getBooleanExtra("hidePlayButton", false)

        //  Ocultar btnPlay si corresponde
        if (hidePlayButton) btnPlay.visibility = View.GONE

        //   Rellenar campos con datos del Intent
        etName.setText(intent.getStringExtra("workoutName") ?: "")

        val levelValue = intent.getLongExtra("level", 0)
        etLevel.setText(if (levelValue != 0L) levelValue.toString() else "")

        etUrl.setText(intent.getStringExtra("videoURL") ?: "")
        etDescription.setText(intent.getStringExtra("description") ?: "")
        etEstimatedTime.setText(intent.getStringExtra("estimatedTime") ?: "")
        etDate.setText(intent.getStringExtra("date") ?: "")

        val numEjValue = intent.getIntExtra("numEj", 0)
        etNumEj.setText(if (numEjValue != 0) numEjValue.toString() else "")

        //  Mostrar/ocultar botones según rol
        if (isTrainer) {
            btnSave.visibility = View.VISIBLE
            btnDelete.visibility = if (mode == "edit") View.VISIBLE else View.GONE
        } else {
            btnSave.visibility = View.GONE
            btnDelete.visibility = View.GONE

            //   Bloquear campos
            val editTexts = listOf(etName, etLevel, etUrl, etDescription, etEstimatedTime, etDate, etNumEj)
            for (et in editTexts) {
                et.isFocusable = false
                et.isFocusableInTouchMode = false
                et.isClickable = false
                et.isCursorVisible = false
                et.keyListener = null
                et.setTextColor(resources.getColor(android.R.color.black))
                et.setBackgroundColor(resources.getColor(android.R.color.transparent))
            }
        }

        //  Guardar cambios
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newLevel = etLevel.text.toString().toLongOrNull() ?: 0
            val newUrl = etUrl.text.toString().trim()
            val newDesc = etDescription.text.toString().trim()
            val newTime = etEstimatedTime.text.toString().trim()
            val newDate = etDate.text.toString().trim()
            val newNumEj = etNumEj.text.toString().toIntOrNull() ?: 0

            if (newName.isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = mapOf(
                "workoutName" to newName,
                "level" to newLevel,
                "video" to newUrl,
                "description" to newDesc,
                "estimatedTime" to newTime,
                "date" to newDate,
                "numEj" to newNumEj
            )

            if (mode == "edit" && workoutId != null) {
                firestore.collection("workouts").document(workoutId!!)
                    .update(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Workout actualizado correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar workout", Toast.LENGTH_SHORT).show()
                    }
            } else {
                firestore.collection("workouts")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Workout creado correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al crear workout", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        //   Eliminar workout
        btnDelete.setOnClickListener {
            workoutId?.let {
                firestore.collection("workouts").document(it)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Workout eliminado correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar workout", Toast.LENGTH_SHORT).show()
                    }
            } ?: Toast.makeText(this, "No se encontró el workout para eliminar", Toast.LENGTH_SHORT).show()
        }

        //   Reproducir video
        btnPlay.setOnClickListener {
            val url = etUrl.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(this, "Introduce una URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar URL
            val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "http://$url"
            } else {
                url
            }

            if (!URLUtil.isValidUrl(fixedUrl)) {
                Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Intent seguro
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No hay aplicación para abrir esta URL", Toast.LENGTH_SHORT).show()
            }
        }

        //   Mostrar ejercicios
        btnEjercicios.setOnClickListener {
            Toast.makeText(this, "Mostrar ejercicios del workout (pendiente)", Toast.LENGTH_SHORT).show()
        }
    }
}
