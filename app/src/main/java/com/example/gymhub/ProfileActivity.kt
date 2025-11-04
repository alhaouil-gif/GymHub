package com.example.gymhub

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var db: FirebaseFirestore

    private lateinit var editLogin: EditText
    private lateinit var editMail: EditText
    private lateinit var editLevel: EditText
    private lateinit var editAuthority: EditText
    private lateinit var editBirthDate: EditText
    private lateinit var switchTheme: SwitchMaterial
    private lateinit var languageDropdown: AutoCompleteTextView
    private lateinit var buttonSave: Button
    private lateinit var buttonReturn: Button

    private var selectedLangCode = "es"

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        loadLocale()
        db = FirebaseFirestore.getInstance()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //  Inicializar vistas
        editLogin = findViewById(R.id.texteditLogin)
        editMail = findViewById(R.id.texteditMail)
        editLevel = findViewById(R.id.texteditLevel)
        editAuthority = findViewById(R.id.texteditAuthority)
        editBirthDate = findViewById(R.id.texteditBirthDate)
        switchTheme = findViewById(R.id.switchTheme)
        languageDropdown = findViewById(R.id.languageDropdown)
        buttonSave = findViewById(R.id.buttonSave)
        buttonReturn = findViewById(R.id.buttonReturn)

        //  Cargar datos
        loadProfileData()

        //  Configurar idiomas
        val languages = listOf("Español", "Euskera")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages)
        languageDropdown.setAdapter(adapter)

        val currentLang = prefs.getString("My_Lang", "es")
        selectedLangCode = currentLang ?: "es"
        languageDropdown.setText(if (selectedLangCode == "es") "Español" else "Euskera", false)

        languageDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedLangCode = if (position == 0) "es" else "eu"
        }

        //  Configurar tema
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchTheme.isChecked = isDarkMode
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        //  Botón Guardar
        buttonSave.setOnClickListener {
            saveProfileData()
            saveLanguage(selectedLangCode)
            saveTheme(switchTheme.isChecked)
            updateFirestoreProfile()

            // Actualiza sesión activa
            SesionUsuario.userLogin = editLogin.text.toString()
            SesionUsuario.userMail = editMail.text.toString()
            SesionUsuario.userLevel = editLevel.text.toString().toLongOrNull() ?: 0L
            SesionUsuario.userAuthority = editAuthority.text.toString()
            SesionUsuario.birthDate = editBirthDate.text.toString()
            SesionUsuario.language = selectedLangCode
            SesionUsuario.darkMode = switchTheme.isChecked

            setLocale(selectedLangCode)
            recreate()
        }

        //  Botón Volver
        buttonReturn.setOnClickListener { finish() }
    }

    //  Guardar idioma
    private fun saveLanguage(langCode: String) {
        prefs.edit().putString("My_Lang", langCode).apply()
    }

    //  Guardar tema
    private fun saveTheme(isDarkMode: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    //  Cargar idioma
    private fun loadLocale() {
        val lang = getSharedPreferences("Settings", MODE_PRIVATE).getString("My_Lang", "es") ?: "es"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    private fun setLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    //  Guardar localmente
    private fun saveProfileData() {
        prefs.edit().apply {
            putString("login", editLogin.text.toString())
            putString("mail", editMail.text.toString())
            putString("level", editLevel.text.toString())
            putString("authority", editAuthority.text.toString())
            putString("birthdate", editBirthDate.text.toString())
            putString("My_Lang", selectedLangCode)
            putBoolean("dark_mode", switchTheme.isChecked)
            apply()
        }
    }

    //  Cargar
    private fun loadProfileData() {
        val userLogin = SesionUsuario.userLogin ?: return

        db.collection("users")
            .whereEqualTo("login", userLogin)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    val userData = doc.data ?: return@addOnSuccessListener

                    editLogin.setText(userData["login"]?.toString() ?: "")
                    editMail.setText(userData["mail"]?.toString() ?: "")
                    editLevel.setText(userData["level"]?.toString() ?: "0")
                    editAuthority.setText(userData["userType"]?.toString() ?: "")
                    editBirthDate.setText(userData["birthDate"]?.toString() ?: "")

                    // ⚙️ Si no existen campos de idioma o tema, aplicar valores por defecto
                    val lang = userData["language"]?.toString() ?: "es"
                    val darkMode = userData["dark_mode"] as? Boolean ?: false

                    selectedLangCode = lang
                    switchTheme.isChecked = darkMode

                    // 🔹 Sincronizar valores por defecto si no existían
                    val updates = mutableMapOf<String, Any>()
                    if (!userData.containsKey("language")) updates["language"] = "es"
                    if (!userData.containsKey("dark_mode")) updates["dark_mode"] = false
                    if (updates.isNotEmpty()) doc.reference.update(updates)

                    saveLanguage(lang)
                    saveTheme(darkMode)
                } else {
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //  Actualizar Firestore
    private fun updateFirestoreProfile() {
        val userLogin = SesionUsuario.userLogin ?: return

        db.collection("users")
            .whereEqualTo("login", userLogin)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val docRef = db.collection("users").document(document.id)
                    val levelValue = editLevel.text.toString().toLongOrNull() ?: 0L
                    val updatedData = mapOf(
                        "login" to editLogin.text.toString(),
                        "mail" to editMail.text.toString(),
                        "level" to levelValue,
                        "userType" to editAuthority.text.toString(),
                        "birthDate" to editBirthDate.text.toString(),
                        "language" to selectedLangCode,
                        "dark_mode" to switchTheme.isChecked
                    )

                    docRef.update(updatedData)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, getString(R.string.save_error), Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
}
