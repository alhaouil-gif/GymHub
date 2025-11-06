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

        // Referencias
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

        // Recuperar datos del Intent
        workoutId = intent.getStringExtra("workoutId")
        isTrainer = intent.getBooleanExtra("isTrainer", false)
        mode = intent.getStringExtra("mode")
        val hidePlayButton = intent.getBooleanExtra("hidePlayButton", false)

        if (hidePlayButton) btnPlay.visibility = View.GONE

        // Rellenar datos
        etName.setText(intent.getStringExtra("workoutName") ?: "")

        val levelValue = intent.getLongExtra("level", 0)
        etLevel.setText(if (levelValue != 0L) levelValue.toString() else "")

        etUrl.setText(intent.getStringExtra("videoURL") ?: "")
        etDescription.setText(intent.getStringExtra("description") ?: "")
        etEstimatedTime.setText(intent.getStringExtra("estimatedTime") ?: "")
        etDate.setText(intent.getStringExtra("date") ?: "")

        val numEjValue = intent.getIntExtra("numEj", 0)
        etNumEj.setText(if (numEjValue != 0) numEjValue.toString() else "")

        // Mostrar/ocultar botones
        if (isTrainer) {
            btnSave.visibility = View.VISIBLE
            btnDelete.visibility = if (mode == "edit") View.VISIBLE else View.GONE
        } else {
            btnSave.visibility = View.GONE
            btnDelete.visibility = View.GONE

            listOf(etName, etLevel, etUrl, etDescription, etEstimatedTime, etDate, etNumEj).forEach {
                it.isFocusable = false
                it.isClickable = false
            }
        }

        // Mostrar ejercicios del workout actual ✅
        btnEjercicios.setOnClickListener {
            val selectedWorkoutName = etName.text.toString().trim()

            if (workoutId.isNullOrEmpty()) {
                Toast.makeText(this, "El workout no tiene ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Mantiene sesión activa y pasa workout al siguiente Activity
            val intent = Intent(this, ExerciseActivity::class.java)
            intent.putExtra("workoutId", workoutId)
            startActivity(intent)
        }
    }
}
