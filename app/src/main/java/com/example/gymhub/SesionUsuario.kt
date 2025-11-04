package com.example.gymhub

/**
 * Clase Singleton que gestiona la sesión del usuario actualmente logueado.
 * Solo existe una instancia de UserSesion en toda la aplicación.
 */
object SesionUsuario {

    // Datos  del usuario
    var userName: String? = null
    var userLastName: String? = null
    var userMail: String? = null
    var userLogin: String? = null
    var userPassword: String? = null


    var userAuthority: String? = null
    var birthDate: String? = null
    var userLevel: Long = 0L

    var language: String = "es"
    var darkMode: Boolean = false


    fun clearSession() {
        userName = null
        userLastName = null
        userMail = null
        userLogin = null
        userPassword = null
        userAuthority = null
        birthDate = null
        userLevel = 0L

        language = "es"
        darkMode = false}

    /**
     * Comprueba logueado actualmente.
     */
    fun isLoggedIn(): Boolean {
        return !userLogin.isNullOrEmpty() && !userPassword.isNullOrEmpty()
    }

}
