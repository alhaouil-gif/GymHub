package com.example.gymhub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gymhub.R

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var textViewName: TextView
    private lateinit var textViewLevel: TextView
    private lateinit var textViewNumEj: TextView
    private lateinit var textViewVideo: TextView
    private lateinit var buttonStartExercise: Button
    private lateinit var buttonPlayVideo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        textViewName = findViewById(R.id.textViewWorkoutName)
        textViewLevel = findViewById(R.id.textViewWorkoutLevel)
        textViewNumEj = findViewById(R.id.textViewWorkoutNumEj)
        textViewVideo = findViewById(R.id.textViewWorkoutVideo)
         buttonPlayVideo = findViewById(R.id.buttonPlayVideo)

        val workoutName = intent.getStringExtra("workoutName") ?: "Nombre no disponible"
        val level = intent.getIntExtra("level", 0)
        val numEj = intent.getIntExtra("numEj", 0)
        val videoURL = intent.getStringExtra("videoURL") ?: ""

        textViewName.text = workoutName
        textViewLevel.text = "Nivel: $level"
        textViewNumEj.text = "Número de ejercicios: $numEj"
        textViewVideo.text = videoURL



         buttonPlayVideo.setOnClickListener {
            if (videoURL.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoURL))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay video disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
