package com.example.gymhub

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
        userText.setText(sharedP.getString("user", ""))
        passwordText.setText(sharedP.getString("password", ""))

        rememberMe.isChecked = !(userText.text.isEmpty() && passwordText.text.isEmpty())

        loginButton.setOnClickListener {
            val inputLogin = userText.text.toString().trim()
            val inputPassword = passwordText.text.toString().trim()

            if (inputLogin.isEmpty() || inputPassword.isEmpty()) {
                Toast.makeText(this, "Introduce usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users")
                .whereEqualTo("login", inputLogin)
                .whereEqualTo("password", inputPassword)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val document = result.documents.first()

                    // Guardar datos en SesionUsuario
                    SesionUsuario.clearSession()
                    SesionUsuario.userLogin = document.getString("login")
                    SesionUsuario.userName = document.getString("name")
                    SesionUsuario.userLastName = document.getString("lastName")
                    SesionUsuario.userMail = document.getString("mail")
                    SesionUsuario.userAuthority = document.getString("userType")
                    SesionUsuario.userPassword = document.getString("password")
                    SesionUsuario.birthDate = document.getString("birthDate")
                    SesionUsuario.userLevel = (document.getLong("level") ?: 0L)
                    SesionUsuario.language = document.getString("language") ?: "es"
                    SesionUsuario.darkMode = document.getBoolean("dark_mode") ?: false

                    applyUserPreferences()

                    if (rememberMe.isChecked) {
                        sharedP.edit()
                            .putString("user", inputLogin)
                            .putString("password", inputPassword)
                            .apply()
                    } else {
                        sharedP.edit().clear().apply()
                    }

                    Toast.makeText(this, "Login correcto", Toast.LENGTH_SHORT).show()
                    Log.d("Login", "Usuario autenticado correctamente")

                    val intent = Intent(this, WorkoutsActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Login", "Error al iniciar sesión", e)
                }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun applyUserPreferences() {
        // Idioma
        val locale = Locale(SesionUsuario.language)
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
}
