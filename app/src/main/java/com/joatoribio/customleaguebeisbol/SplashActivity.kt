package com.joatoribio.customleaguebeisbol // Asegúrate que el paquete sea el correcto

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
// Importa tu ViewBinding
import com.joatoribio.customleaguebeisbol.MainActivity
import com.joatoribio.customleaguebeisbol.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen") // Puedes usar esta anotación si es una splash screen simple
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // Puedes definir un tiempo mínimo de espera si deseas que el splash se muestre
    // incluso si los datos cargan muy rápido.
    private val MIN_SPLASH_TIME_MS = 2000L // 2 segundos
    private var dataLoaded = false
    private var minTimeElapsed = false
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        startTime = System.currentTimeMillis()

        // Inicia la carga de datos
        loadInitialData()

        // Inicia un temporizador para el tiempo mínimo de splash
        Handler(Looper.getMainLooper()).postDelayed({
            minTimeElapsed = true
            navigateToNextScreenIfReady()
        }, MIN_SPLASH_TIME_MS)
    }

    private fun loadInitialData() {
        // Aquí es donde cargas los datos que necesita tu FragmentInicio
        // Ejemplo: Cargar datos del usuario actual o alguna configuración global
        // Este es solo un EJEMPLO, adapta la referencia y los datos que necesitas.

        if (firebaseAuth.currentUser != null) {
            val userId = firebaseAuth.currentUser!!.uid
            val userRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Procesa los datos que necesitas.
                    // Por ejemplo, podrías guardar estos datos en SharedPreferences,
                    // en un Singleton, o pasarlos a la siguiente Activity mediante Intent extras.
                    // Lo importante es que sepas que los datos están listos.

                    Log.d("SplashActivity", "Datos iniciales cargados correctamente.")
                    dataLoaded = true
                    navigateToNextScreenIfReady()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SplashActivity", "Error al cargar datos iniciales: ${error.message}")
                    // Decide cómo manejar el error.
                    // Podrías mostrar un mensaje y cerrar la app, o intentar de nuevo,
                    // o navegar a la pantalla principal con un estado de error.
                    dataLoaded = true // Considera esto como "terminado" para no bloquear el splash
                    navigateToNextScreenIfReady() // O navega a una pantalla de error
                }
            })
        } else {
            // No hay usuario logueado, decide qué hacer.
            // Podrías ir directamente a la pantalla de login/registro
            // o a la pantalla principal si tu app permite acceso anónimo.
            Log.d("SplashActivity", "No hay usuario logueado.")
            dataLoaded = true // Considera esto como "terminado"
            navigateToNextScreenIfReady()
        }

        // Si tienes MÁS datos que cargar de diferentes nodos, puedes encadenar las llamadas
        // o usar coroutines con `async/await` para cargarlos en paralelo y esperar a que todos terminen.
    }

    private fun navigateToNextScreenIfReady() {
        // Solo navega si los datos están cargados Y el tiempo mínimo ha pasado
        if (dataLoaded && minTimeElapsed) {
            // Decide a qué Activity ir.
            // Si tienes una MainActivity que gestiona tus fragmentos:
            val intent = Intent(this, MainActivity::class.java)
            // Puedes pasar datos cargados aquí si es necesario
            // intent.putExtra("USER_DATA_KEY", loadedUserData)
            startActivity(intent)
            finish() // Finaliza SplashActivity para que no se pueda volver a ella
        }
    }
}