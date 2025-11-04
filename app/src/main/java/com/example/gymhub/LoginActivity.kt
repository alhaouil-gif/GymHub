package com.example.gymhub

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firestore = FirebaseFirestore.getInstance()

        val registerButton: Button = findViewById(R.id.registerButton)
        val rememberMe: CheckBox = findViewById(R.id.rememberMe)
        val userText: EditText = findViewById(R.id.userText)
        val passwordText: EditText = findViewById(R.id.passwordText)
        val sharedP: SharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE)
        val loginButton: Button = findViewById(R.id.loginButton)

        // Recuperar usuario y contraseña guardados
        userText.setText(sharedP.getString("user", intent.getStringExtra("login")))
        passwordText.setText(sharedP.getString("password", intent.getStringExtra("password")))

        rememberMe.isChecked = !(userText.text.isEmpty() && passwordText.text.isEmpty())

        loginButton.setOnClickListener {
            val inputLogin = userText.text.toString().trim()
            val inputPassword = passwordText.text.toString().trim()

            if (inputLogin.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(this, "Introduce usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    var userFound = false
                    for (document in result) {
                        val dbLogin = document.getString("login")
                        val dbPassword = document.getString("password")

                        if (dbLogin?.equals(inputLogin, ignoreCase = true) == true &&
                            dbPassword == inputPassword
                        ) {
                            userFound = true

                            //  Limpiar datos de sesión anteriores
                            SesionUsuario.clearSession()

                            // Guardar datos actuales del usuario
                            SesionUsuario.userLogin = dbLogin
                            SesionUsuario.userName = document.getString("name")
                            SesionUsuario.userAuthority = document.getString("userType")
                            SesionUsuario.birthDate = document.getString("birthDate")
                            SesionUsuario.userLevel = (document.get("level") as? Long) ?: 0L
                            SesionUsuario.userMail = document.getString("mail")
                            SesionUsuario.userLastName = document.getString("lastName")
                            SesionUsuario.userPassword = dbPassword

                            //  Cargar idioma y tema (por defecto si no existen)
                            SesionUsuario.language = document.getString("language") ?: "es"
                            SesionUsuario.darkMode = document.getBoolean("dark_mode") ?: false

                            //  Si no existen en Firestore, los crea
                            val updates = hashMapOf<String, Any>(
                                "language" to SesionUsuario.language,
                                "dark_mode" to SesionUsuario.darkMode
                            )
                            firestore.collection("users").document(document.id).update(updates)

                            //  Aplicar idioma y tema
                            applyUserPreferences()

                            Toast.makeText(this, "Login correcto", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Usuario autenticado correctamente")

                            //  Limpiar preferencias del usuario anterior
                            val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
                            prefs.edit().clear().apply()

                            //  Guardar los datos del usuario actual
                            prefs.edit().apply {
                                putString("login", dbLogin)
                                putString("mail", document.getString("mail"))
                                putString("authority", document.getString("userType"))
                                putString("birthdate", document.getString("birthDate"))
                                putLong("level", SesionUsuario.userLevel) // ✅ mantiene level numérico
                                apply()
                            }

                            goToMenu(rememberMe, sharedP, passwordText, userText)
                            break
                        }
                    }

                    if (!userFound) {
                        Toast.makeText(
                            this,
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error al intentar iniciar sesión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Error al obtener usuarios", e)
                }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    //  tema
    private fun applyUserPreferences() {
        // Idioma
        val langCode = SesionUsuario.language
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Tema
        val mode = if (SesionUsuario.darkMode)
            AppCompatDelegate.MODE_NIGHT_YES
        else
            AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun goToMenu(
        rememberMe: CheckBox,
        sharedP: SharedPreferences,
        passwordText: TextView,
        userText: TextView
    ) {
        val intent = Intent(this, WorkoutsActivity::class.java)
        intent.putExtra("login", userText.text.toString())
        startActivity(intent)

        val editor: SharedPreferences.Editor = sharedP.edit()
        if (rememberMe.isChecked) {
            editor.putString("user", userText.text.toString().trim())
            editor.putString("password", passwordText.text.toString().trim())
        } else {
            editor.remove("user")
            editor.remove("password")
        }
        editor.apply()
    }
}
