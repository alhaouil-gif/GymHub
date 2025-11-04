package com.example.gymhub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.gymhub.R.*
import com.example.gymhub.model.WorkOutItem
import com.example.gymhub.model.WorkoutHistoryArrayAdapter
import com.example.gymhub.model.WorkoutItemArrayAdapter
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var workoutListView: ListView
    private lateinit var spinnerLevelFilter: Spinner
    private lateinit var buttonFilter: Button
    private val workoutItemList = mutableListOf<WorkOutItem>()
    private val allWorkouts = mutableListOf<WorkOutItem>()
    private val availableLevels = mutableListOf<Long>()
    private var adapter: WorkoutItemArrayAdapter? = null
    private var adapterHist: WorkoutHistoryArrayAdapter? = null
    private var showingHistorico = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_workout)

        firestore = FirebaseFirestore.getInstance()

        workoutListView = findViewById(id.listViewWorkouts)
        spinnerLevelFilter = findViewById(id.spinnerLevelFilter)
        buttonFilter = findViewById(id.buttonFilter)

        val buttonProfile: Button = findViewById(id.buttonProfile)
        val buttonCoach: Button = findViewById(id.buttonCoach)
        val buttonHistorico: Button = findViewById(id.buttonHistorico)
        val buttonReturn: Button = findViewById(id.buttonReturn)
        val buttonWorkouts: Button = findViewById(id.buttonWorkouts)
        val labelLevel: TextView = findViewById(id.textViewUserLevel)

        val userLevel = SesionUsuario.userLevel
        val userAuthority = SesionUsuario.userAuthority!!

        labelLevel.text = "Nivel: $userLevel"

        if (userAuthority == "Entrenador") {
            buttonHistorico.visibility = View.GONE
            buttonCoach.visibility = View.VISIBLE
            buttonWorkouts.visibility = View.GONE
        } else {
            buttonHistorico.visibility = View.VISIBLE
            buttonCoach.visibility = View.GONE
            buttonWorkouts.isEnabled = true
        }

        adapter = WorkoutItemArrayAdapter(this, layout.workout_item, workoutItemList)
        workoutListView.adapter = adapter

        if (userAuthority == "Entrenador") {
            loadAllWorkouts()
        } else {
            loadWorkouts(userLevel)
        }

        // --- Cargar niveles dinámicamente ---
        loadAvailableLevels()

        // --- Filtro por nivel ---
        buttonFilter.setOnClickListener {
            val selected = spinnerLevelFilter.selectedItem?.toString()
            applyLevelFilter(selected)
        }

        //  Ir al perfil
        buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        //  Botón entrenador
        buttonCoach.setOnClickListener {
            val intent = Intent(this, TrainerActivity::class.java)
            intent.putExtra("mode", "create")
            intent.putExtra("isTrainer", true)
            intent.putExtra("hidePlayButton", true)
            startActivity(intent)
        }

        // --- Botón histórico ---
        buttonHistorico.setOnClickListener { loadHistorico() }

        buttonWorkouts.setOnClickListener {
            showingHistorico = false
            workoutListView.adapter = adapter
            workoutItemList.clear()
            workoutItemList.addAll(allWorkouts)
            adapter?.notifyDataSetChanged()
        }

        buttonReturn.setOnClickListener { finish() }

        // --- Click en workout ---
        workoutListView.setOnItemClickListener { _, _, position, _ ->
            val selectedWorkout = workoutItemList[position]
            val intent = Intent(this, TrainerActivity::class.java)

            intent.putExtra("workoutId", selectedWorkout.id)
            intent.putExtra("workoutName", selectedWorkout.name)
            intent.putExtra("level", selectedWorkout.level)
            intent.putExtra("numEj", selectedWorkout.numEj)
            intent.putExtra("videoURL", selectedWorkout.videoURL)
            intent.putExtra("description", selectedWorkout.description ?: "")
            intent.putExtra("estimatedTime", selectedWorkout.estimatedTime)
            intent.putExtra("date", selectedWorkout.date)

            if (SesionUsuario.userAuthority == "Entrenador") {
                intent.putExtra("mode", "edit")
                intent.putExtra("isTrainer", true)
            } else {
                intent.putExtra("mode", "view")
                intent.putExtra("isTrainer", false)
            }

            startActivity(intent)
        }
    }

    // --- 🔹 Cargar niveles únicos desde Firestore ---
    private fun loadAvailableLevels() {
        firestore.collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                availableLevels.clear()
                val levelsSet = mutableSetOf<Long>()

                for (doc in snapshot.documents) {
                    val level = doc.getLong("level")
                    if (level != null) levelsSet.add(level)
                }

                val sortedLevels = levelsSet.sorted()
                availableLevels.addAll(sortedLevels)

                val levelOptions = mutableListOf("Todos")
                levelOptions.addAll(sortedLevels.map { it.toString() })

                val spinnerAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    levelOptions
                )
                spinnerLevelFilter.adapter = spinnerAdapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar niveles", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyLevelFilter(selected: String?) {
        if (selected == null || selected == "Todos") {
            workoutItemList.clear()
            workoutItemList.addAll(allWorkouts)
        } else {
            val levelValue = selected.toLongOrNull()
            workoutItemList.clear()
            if (levelValue != null) {
                workoutItemList.addAll(allWorkouts.filter { it.level == levelValue })
            }
        }
        notifyCurrentAdapter()
    }

    private fun notifyCurrentAdapter() {
        if (showingHistorico) adapterHist?.notifyDataSetChanged()
        else adapter?.notifyDataSetChanged()
    }

    // --- Cargar workouts normales o del entrenador ---
    private fun loadWorkouts(maxLevel: Long) {
        firestore.collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                allWorkouts.clear()
                showingHistorico = false

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("workoutName") ?: continue
                    val numEj = doc.getLong("numEj")?.toInt() ?: 0
                    val level = doc.getLong("level") ?: 0L
                    val video = doc.getString("video") ?: ""
                    val description = doc.getString("description") ?: ""
                    val estimatedTime = doc.getString("estimatedTime") ?: ""
                    val date = doc.getString("date") ?: ""

                    allWorkouts.add(
                        WorkOutItem(
                            id, name, level, "", estimatedTime, date,
                            "", video, numEj, description
                        )
                    )
                }

                workoutItemList.clear()
                workoutItemList.addAll(allWorkouts.filter { it.level <= maxLevel })
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar workouts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllWorkouts() {
        firestore.collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                allWorkouts.clear()
                showingHistorico = false

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("workoutName") ?: continue
                    val numEj = doc.getLong("numEj")?.toInt() ?: 0
                    val level = doc.getLong("level") ?: 0L
                    val video = doc.getString("video") ?: ""
                    val description = doc.getString("description") ?: ""
                    val estimatedTime = doc.getString("estimatedTime") ?: ""
                    val date = doc.getString("date") ?: ""

                    allWorkouts.add(
                        WorkOutItem(
                            id, name, level, "", estimatedTime, date,
                            "", video, numEj, description
                        )
                    )
                }

                workoutItemList.clear()
                workoutItemList.addAll(allWorkouts)
                adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar workouts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadHistorico() {
        val userId = SesionUsuario.userLogin
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = firestore.collection("users").document(userId)
        firestore.collection("historicos")
            .whereEqualTo("userId", userRef)
            .get()
            .addOnSuccessListener { snapshot ->
                allWorkouts.clear()
                workoutItemList.clear()
                showingHistorico = true

                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Histórico vacío", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                adapterHist = WorkoutHistoryArrayAdapter(
                    this,
                    R.layout.list_item_workout_history,
                    workoutItemList
                )
                workoutListView.adapter = adapterHist

                for (doc in snapshot.documents) {
                    val name = doc.getString("workoutName") ?: continue
                    val level = doc.getLong("level") ?: 0L
                    val completion = doc.getLong("completionProgress")?.toString() ?: "0"
                    val totalTime = doc.getLong("totalTime")?.toString() ?: ""
                    val estimatedTime = doc.get("estimatedTime")?.toString() ?: ""
                    val workoutRef = doc.getDocumentReference("workoutId")

                    workoutRef?.get()?.addOnSuccessListener { workoutDoc ->
                        if (workoutDoc.exists()) {
                            val id = workoutDoc.id
                            val video = workoutDoc.getString("video") ?: ""
                            val numEj = workoutDoc.getLong("numEj")?.toInt() ?: 0
                            val description = workoutDoc.getString("description") ?: ""

                            val item = WorkOutItem(
                                id, name, level, totalTime, estimatedTime,
                                doc.getString("date") ?: "", completion,
                                video, numEj, description
                            )

                            allWorkouts.add(item)
                            workoutItemList.add(item)
                            adapterHist?.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar histórico", Toast.LENGTH_SHORT).show()
            }
    }
}
