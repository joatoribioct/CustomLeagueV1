package com.joatoribio.customleaguebeisbol

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // Tiempo mínimo de splash para mostrar el logo
    private val MIN_SPLASH_TIME_MS = 2500L // 2.5 segundos
    private var dataLoaded = false
    private var minTimeElapsed = false
    private var startTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        // CORREGIDO: Instalar el splash screen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        startTime = System.currentTimeMillis()

        // CORREGIDO: Configurar el splash screen DESPUÉS de setContentView
        try {
            splashScreen.setKeepOnScreenCondition {
                !dataLoaded || !minTimeElapsed
            }
        } catch (e: Exception) {
            Log.w("SplashActivity", "Error configurando splash screen: ${e.message}")
            // Continuar sin el splash screen nativo si hay error
        }

        // Detectar modo oscuro y ajustar colores si es necesario
        detectarModoOscuro()

        // Inicia la carga de datos
        loadInitialData()

        // Inicia un temporizador para el tiempo mínimo de splash
        Handler(Looper.getMainLooper()).postDelayed({
            minTimeElapsed = true
            navigateToNextScreenIfReady()
        }, MIN_SPLASH_TIME_MS)
    }

    /**
     * Detecta si el dispositivo está en modo oscuro
     */
    private fun detectarModoOscuro() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                // Modo claro - los temas ya están configurados
                Log.d("SplashActivity", "Modo claro detectado")
                // OPCIONAL: Actualizar colores de la vista si es necesario
                updateUIForLightMode()
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                // Modo oscuro - los temas ya están configurados
                Log.d("SplashActivity", "Modo oscuro detectado")
                // OPCIONAL: Actualizar colores de la vista si es necesario
                updateUIForDarkMode()
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                // Modo indefinido - usar configuración por defecto
                Log.d("SplashActivity", "Modo indefinido detectado")
            }
        }
    }

    /**
     * NUEVO: Actualiza la UI para modo claro
     */
    private fun updateUIForLightMode() {
        try {
            // Configurar colores específicos si el layout tiene elementos personalizados
            binding.root.setBackgroundColor(getColor(R.color.white))

            // Si tienes un ProgressBar, configurarlo
            if (::binding.isInitialized) {
                // Configurar cualquier vista específica aquí
            }
        } catch (e: Exception) {
            Log.w("SplashActivity", "Error actualizando UI para modo claro: ${e.message}")
        }
    }

    /**
     * NUEVO: Actualiza la UI para modo oscuro
     */
    private fun updateUIForDarkMode() {
        try {
            // Configurar colores específicos para modo oscuro
            binding.root.setBackgroundColor(getColor(R.color.background_dark))

            // Si tienes un ProgressBar, configurarlo
            if (::binding.isInitialized) {
                // Configurar cualquier vista específica aquí
            }
        } catch (e: Exception) {
            Log.w("SplashActivity", "Error actualizando UI para modo oscuro: ${e.message}")
        }
    }

    private fun loadInitialData() {
        // Carga datos iniciales necesarios para la aplicación
        if (firebaseAuth.currentUser != null) {
            val userId = firebaseAuth.currentUser!!.uid
            val userRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Procesa los datos del usuario
                    val nombres = snapshot.child("nombres").value?.toString()
                    val idGaming = snapshot.child("idGaming").value?.toString()
                    val email = snapshot.child("email").value?.toString()

                    Log.d("SplashActivity", "Datos de usuario cargados:")
                    Log.d("SplashActivity", "- Nombres: ${nombres ?: "No especificado"}")
                    Log.d("SplashActivity", "- ID Gaming: ${idGaming ?: "No especificado"}")
                    Log.d("SplashActivity", "- Email: ${email ?: "No especificado"}")

                    // OPCIONAL: Precargar datos adicionales
                    precargarDatosAdicionales()

                    dataLoaded = true
                    navigateToNextScreenIfReady()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("SplashActivity", "Error al cargar datos iniciales: ${error.message}")

                    // Aún así, permitir continuar (para no bloquear la app)
                    dataLoaded = true
                    navigateToNextScreenIfReady()
                }
            })
        } else {
            // No hay usuario logueado
            Log.d("SplashActivity", "No hay usuario logueado")
            dataLoaded = true
            navigateToNextScreenIfReady()
        }
    }

    /**
     * NUEVO: Precarga datos adicionales si es necesario
     */
    private fun precargarDatosAdicionales() {
        // Aquí puedes precargar datos como:
        // - Configuración de equipos
        // - Datos de ligas
        // - Configuraciones de la app
        // etc.

        // Por ejemplo:
        try {
            // Precargar equipos globales si es necesario
            // EquiposManager.verificarEquiposGlobales()

            Log.d("SplashActivity", "Datos adicionales precargados")
        } catch (e: Exception) {
            Log.w("SplashActivity", "Error precargando datos adicionales: ${e.message}")
        }
    }

    private fun navigateToNextScreenIfReady() {
        // Solo navega si los datos están cargados Y el tiempo mínimo ha pasado
        if (dataLoaded && minTimeElapsed) {
            Log.d("SplashActivity", "Condiciones cumplidas, navegando a la siguiente pantalla")

            // Determinar a qué pantalla ir
            val intent = if (firebaseAuth.currentUser != null) {
                // Usuario logueado, ir a MainActivity
                Intent(this, MainActivity::class.java)
            } else {
                // Usuario no logueado, ir a OpcionesLogin
                Intent(this, OpcionesLogin::class.java)
            }

            // OPCIONAL: Agregar flags para una mejor experiencia
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish() // Finaliza SplashActivity para que no se pueda volver a ella

            // OPCIONAL: Agregar transición suave
            try {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Exception) {
                Log.w("SplashActivity", "Error aplicando transición: ${e.message}")
            }
        } else {
            val tiempoTranscurrido = System.currentTimeMillis() - startTime
            Log.d("SplashActivity", "Esperando condiciones: dataLoaded=$dataLoaded, minTimeElapsed=$minTimeElapsed, tiempo=${tiempoTranscurrido}ms")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SplashActivity", "SplashActivity destruida")
    }
}