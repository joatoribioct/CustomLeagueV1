package com.joatoribio.customleaguebeisbol.Fragmentos

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.Adaptadores.AdaptadorEquipos
import com.joatoribio.customleaguebeisbol.Adaptadores.LineupsAdapter
import com.joatoribio.customleaguebeisbol.Constantes
import com.joatoribio.customleaguebeisbol.Draft.ControladorDraft
import com.joatoribio.customleaguebeisbol.Draft.EstadoDraft
import com.joatoribio.customleaguebeisbol.Draft.TemporizadorGlobal
import com.joatoribio.customleaguebeisbol.Manager.EquiposManager
import com.joatoribio.customleaguebeisbol.Modelo.LineupViewModel
import com.joatoribio.customleaguebeisbol.Modelo.ModeloEquipos
import com.joatoribio.customleaguebeisbol.Modelo.ModeloLiga
import com.joatoribio.customleaguebeisbol.OnLineupClickListener
import com.joatoribio.customleaguebeisbol.R
import com.joatoribio.customleaguebeisbol.RvListennerEquipos
import com.joatoribio.customleaguebeisbol.databinding.FragmentInicioBinding

// Modelos de datos
data class Equipo(
    val nombre: String = "",
    val jugadores: Map<String, Map<String, Map<String, Any>>> = emptyMap()
)

data class LineupSeleccionado(
    val equipoId: String = "",
    val tipoLineup: String = "",
    val usuarioId: String = "",
    val nombreUsuario: String = "",
    val idGamingUsuario: String = "",
    val fechaSeleccion: Long = 0,
    val jugadores: Map<String, Map<String, Any>> = emptyMap(),
    val ronda: Int = 1,
    val turno: Int = 0
)

class FragmentInicio : Fragment() {

    private lateinit var binding: FragmentInicioBinding
    private lateinit var mContexto: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private val lineupViewModel: LineupViewModel by activityViewModels()
    private var lineupsSeleccionados = mutableListOf<LineupSeleccionado>()
    private var nombreUsuarioActual = ""
    private var idGamingUsuarioActual = ""
    private var viewModelSincronizado = false

    // NUEVO: Variables para controlar el draft por turnos
    private var ligaActual: ModeloLiga? = null
    private var controladorDraft: ControladorDraft? = null
    private var estadoDraftActual: EstadoDraft = EstadoDraft()
    private var esAdmin = false
    private var lineupsListener: ValueEventListener? = null
    private var ligasListener: ValueEventListener? = null

    private var temporizadorInicializado = false
    private var esVisiblePorPrimeraVez = true
    private var ultimaNavegacion = 0L

    // Variables para el temporizador - ACTUALIZADO
    private var tiempoRestanteSegundos = 180 // Solo para UI local
    private val TIEMPO_TOTAL_SEGUNDOS = 180
    private var ultimoUsuarioEnTurno = ""
    private var verificacionEnProceso = false // NUEVO: Evitar verificaciones simultáneas
    private var ultimaVerificacion = 0L // NUEVO: Timestamp de última verificación

    private var temporizadorUI: CountDownTimer? = null
    private var tiempoTotalSegundos = 180 // 3 minutos = 180 segundos

    override fun onAttach(context: Context) {
        mContexto = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInicioBinding.inflate(LayoutInflater.from(mContexto), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        leerInfoDeUsuario()
        cargarEquipos()
        escucharLineupsSeleccionados()
        // REMOVIDO: configurarBotonProgresoDraft()
    }

    // REMOVIDOS: Todos los métodos relacionados con el botón de progreso
    // - configurarBotonProgresoDraft()
    // - navegarAProgresoDraft()
    // - actualizarVisibilidadBotonProgreso()

    private fun leerInfoDeUsuario() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userUid = snapshot.child("uid").value.toString()
                    nombreUsuarioActual = snapshot.child("nombres").value.toString()
                    idGamingUsuarioActual = snapshot.child("idGaming").value.toString()

                    if (idGamingUsuarioActual == "null" || idGamingUsuarioActual.isEmpty()) {
                        idGamingUsuarioActual = generarIdGamingPorDefecto(nombreUsuarioActual, snapshot.child("email").value.toString())
                        ref.child("${firebaseAuth.uid}").child("idGaming").setValue(idGamingUsuarioActual)
                    }

                    buscarLigasDisponibles()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(mContexto, "Error al cargar usuario: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun generarIdGamingPorDefecto(nombres: String, email: String): String {
        return when {
            nombres != "null" && nombres.isNotEmpty() -> {
                val nombreLimpio = nombres.replace(" ", "").take(8)
                "${nombreLimpio}${(1000..9999).random()}"
            }
            email != "null" && email.isNotEmpty() -> {
                val emailPrefix = email.substringBefore("@").take(8)
                "${emailPrefix}${(1000..9999).random()}"
            }
            else -> {
                "Player${(10000..99999).random()}"
            }
        }
    }

    private fun buscarLigasDisponibles() {
        val ligasRef = FirebaseDatabase.getInstance().getReference("Ligas")

        ligasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(ligasSnapshot: DataSnapshot) {
                var ligaEncontrada = false

                run busqueda@{
                    for (ligaSnapshot in ligasSnapshot.children) {
                        val liga = ligaSnapshot.getValue(ModeloLiga::class.java)

                        liga?.let {
                            if (it.usuariosPermitidos.contains(firebaseAuth.uid)) {
                                ligaActual = it
                                esAdmin = it.adminUid == firebaseAuth.uid

                                // NUEVO: Inicializar controlador de draft
                                inicializarControladorDraft(it.id)

                                mostrarInformacionLiga(it)
                                ligaEncontrada = true
                                return@busqueda
                            }
                        }
                    }
                }

                if (!ligaEncontrada) {
                    buscarCualquierLiga()
                }
            }

            override fun onCancelled(ligaError: DatabaseError) {
                Toast.makeText(mContexto, "Error al buscar liga: ${ligaError.message}", Toast.LENGTH_SHORT).show()
                binding.tvNombreReal.text = "Liga de Béisbol"
            }
        })
    }

    /**
     * NUEVO: Inicializa el controlador de draft
     */
    private fun inicializarControladorDraft(ligaId: String) {
        Log.d("DRAFT_FRAGMENT", "Inicializando controlador de draft para liga: $ligaId")

        controladorDraft = ControladorDraft(ligaId) { nuevoEstado ->
            val estadoAnterior = estadoDraftActual
            estadoDraftActual = nuevoEstado

            // NUEVO: Detectar cambio de usuario en turno
            val usuarioActualEnTurno = if (nuevoEstado.ordenTurnos.isNotEmpty() &&
                nuevoEstado.turnoActual < nuevoEstado.ordenTurnos.size) {
                nuevoEstado.ordenTurnos[nuevoEstado.turnoActual]
            } else ""

            Log.d("DRAFT_FRAGMENT", "Estado del draft actualizado: $nuevoEstado")
            Log.d("DRAFT_FRAGMENT", "Usuario en turno: $usuarioActualEnTurno")
            Log.d("DRAFT_FRAGMENT", "Usuario anterior: $ultimoUsuarioEnTurno")

            // NUEVO: Solo procesar si realmente cambió el usuario
            if (usuarioActualEnTurno != ultimoUsuarioEnTurno && usuarioActualEnTurno.isNotEmpty()) {
                Log.d("DRAFT_FRAGMENT", "🔄 CAMBIO DE USUARIO DETECTADO")
                ultimoUsuarioEnTurno = usuarioActualEnTurno

                // NUEVO: Evitar verificaciones múltiples con delay
                val tiempoActual = System.currentTimeMillis()
                if (tiempoActual - ultimaVerificacion > 2000) { // Mínimo 2 segundos entre verificaciones
                    ultimaVerificacion = tiempoActual

                    // Delay para asegurar sincronización
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        actualizarUISegunEstadoDraft()

                        // NUEVO: Solo verificación adicional si soy el nuevo usuario Y no hay verificación en proceso
                        val miUsuario = firebaseAuth.uid ?: ""
                        if (usuarioActualEnTurno == miUsuario && !verificacionEnProceso) {
                            Log.d("DRAFT_FRAGMENT", "🎯 SOY EL NUEVO USUARIO EN TURNO - Verificación ÚNICA")
                            verificacionEnProceso = true

                            // Verificar después de un pequeño delay adicional
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                verificarYIniciarTemporizador()
                                verificacionEnProceso = false
                            }, 500)
                        }
                    }, 1000)
                } else {
                    Log.d("DRAFT_FRAGMENT", "⏭️ Verificación reciente ignorada (anti-spam)")
                    // Solo actualizar UI sin verificación adicional
                    actualizarUISegunEstadoDraft()
                }
            } else {
                // Sin cambio de usuario, solo actualizar UI
                actualizarUISegunEstadoDraft()
            }
        }

        controladorDraft?.iniciarEscuchaDraft()
    }

    /**
     * 🆕 CORREGIDO: Reconectar sin usar método privado
     */
    private fun reconectarTemporizador() {
        try {
            val puedeSeleccionar = controladorDraft?.puedeSeleccionar(firebaseAuth.uid ?: "") ?: false

            Log.d("TIMER_RECONNECT", "🔗 Reconectando temporizador - Puede seleccionar: $puedeSeleccionar")

            if (puedeSeleccionar && estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {

                val usuarioId = firebaseAuth.uid ?: ""
                val ligaId = ligaActual?.id ?: ""
                val rondaActual = estadoDraftActual.rondaActual
                val turnoActual = estadoDraftActual.turnoActual

                // Verificar si ya hay un temporizador activo para este turno
                if (TemporizadorGlobal.estaActivoPara(usuarioId, ligaId, rondaActual, turnoActual)) {
                    Log.d("TIMER_RECONNECT", "⚡ Temporizador ya activo, solo registrando callbacks")

                    // Solo registrar callbacks
                    TemporizadorGlobal.registrarCallbacks(
                        onTick = { tiempoRestante ->
                            if (isAdded && ::binding.isInitialized) {
                                actualizarTemporizadorUI(tiempoRestante)
                            }
                        },
                        onFinish = {
                            if (isAdded) {
                                mostrarMensajeSeleccionAutomatica()
                                detenerTemporizadorUI()
                            }
                        }
                    )

                } else {
                    Log.d("TIMER_RECONNECT", "🔍 Verificando estado del servidor...")

                    // Usar el método público para verificar el servidor
                    TemporizadorGlobal.verificarEstadoServidor(ligaId) { activo, tiempoRestante ->
                        if (activo && tiempoRestante > 0) {
                            Log.d("TIMER_RECONNECT", "✅ Servidor activo con ${tiempoRestante}s restantes")

                            // Registrar callbacks
                            TemporizadorGlobal.registrarCallbacks(
                                onTick = { tiempo ->
                                    if (isAdded && ::binding.isInitialized) {
                                        actualizarTemporizadorUI(tiempo)
                                    }
                                },
                                onFinish = {
                                    if (isAdded) {
                                        mostrarMensajeSeleccionAutomatica()
                                        detenerTemporizadorUI()
                                    }
                                }
                            )
                        } else {
                            Log.d("TIMER_RECONNECT", "❌ No hay temporizador activo en servidor")
                            ocultarTemporizador()
                        }
                    }
                }

                // Mostrar UI del temporizador
                if (::binding.isInitialized) {
                    binding.layoutTemporizador.visibility = View.VISIBLE
                }

                Log.d("TIMER_RECONNECT", "✅ Proceso de reconexión completado")

            } else {
                Log.d("TIMER_RECONNECT", "❌ No es mi turno, ocultando temporizador")
                ocultarTemporizador()
            }

        } catch (e: Exception) {
            Log.e("TIMER_RECONNECT", "Error reconectando temporizador: ${e.message}")
        }
    }


    /**
     * CORREGIDO: Actualiza la UI según el estado del draft mostrando ID Gaming
     */
    private fun actualizarUISegunEstadoDraft() {
        val puedeSeleccionar = controladorDraft?.puedeSeleccionar(firebaseAuth.uid ?: "") ?: false

        Log.d("DEBUG_TEMPORIZADOR", "📊 Actualización UI - Puede seleccionar: $puedeSeleccionar")

        try {
            if (::binding.isInitialized) {
                // CORREGIDO: Obtener ID Gaming del usuario actual en turno
                val usuarioActual = controladorDraft?.obtenerInfoUsuarioActual() ?: "Esperando..."

                Log.d("UI_UPDATE", "Usuario actual en turno (ID Gaming): $usuarioActual")

                try {
                    // MOSTRAR ID GAMING en lugar del UID
                    binding.tvUsuarioActual.text = if (usuarioActual == "Esperando...") {
                        "Esperando inicio..."
                    } else {
                        "Turno: $usuarioActual"
                    }

                    actualizarEstadoVisual(puedeSeleccionar)

                    // Manejar temporizador
                    if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {
                        binding.cardEstadoDraft.visibility = View.VISIBLE

                        if (puedeSeleccionar) {
                            // Solo verificar si no hay verificación en proceso
                            if (!verificacionEnProceso) {
                                Log.d("DEBUG_TEMPORIZADOR", "🎯 ES MI TURNO - Verificación normal")
                                verificarYIniciarTemporizador()
                            } else {
                                Log.d("DEBUG_TEMPORIZADOR", "⏳ Verificación en proceso - Saltando")
                            }
                        } else {
                            Log.d("DEBUG_TEMPORIZADOR", "⏸️ NO es mi turno - Ocultando")
                            ocultarTemporizador()
                        }
                    } else {
                        binding.cardEstadoDraft.visibility = View.GONE
                        detenerTemporizadorGlobal()
                    }

                } catch (e: Exception) {
                    Log.w("DRAFT_FRAGMENT", "Error layout: ${e.message}")
                    actualizarUIFallback(puedeSeleccionar)
                }
            }
        } catch (e: Exception) {
            Log.w("DRAFT_FRAGMENT", "Error general: ${e.message}")
        }
        // REMOVIDO: actualizarVisibilidadBotonProgreso()
    }

// SIMPLIFICAR iniciarTemporizadorGlobal() (ya no necesita toda la lógica):
    /**
     * CORREGIDO: Iniciar temporizador con conteo regresivo visual
     */
    private fun iniciarTemporizadorGlobal() {
        val usuarioId = firebaseAuth.uid ?: ""
        val ligaId = ligaActual?.id ?: ""
        val rondaActual = estadoDraftActual.rondaActual
        val turnoActual = estadoDraftActual.turnoActual

        Log.d("TEMPORIZADOR", "🚀 Iniciando temporizador para mi turno")

        if (::binding.isInitialized) {
            binding.layoutTemporizador.visibility = View.VISIBLE

            // Configurar valores iniciales
            binding.progressTemporizador.max = tiempoTotalSegundos
            binding.progressTemporizador.progress = tiempoTotalSegundos
        }

        // Registrar con el servidor (para sincronización)
        TemporizadorGlobal.iniciarTemporizador(
            usuarioId = usuarioId,
            ligaId = ligaId,
            ronda = rondaActual,
            turno = turnoActual,
            onTick = { tiempoRestante ->
                // Este callback puede ser ignorado porque usamos nuestro temporizador local
            },
            onFinish = {
                if (isAdded) {
                    mostrarMensajeSeleccionAutomatica()
                    detenerTemporizadorUI()
                }
            }
        )

        // NUEVO: Iniciar temporizador visual local
        iniciarTemporizadorUI()
    }

    /**
     * NUEVO: Temporizador visual independiente que SÍ funciona
     */
    private fun iniciarTemporizadorUI() {
        // Detener temporizador anterior si existe
        temporizadorUI?.cancel()

        Log.d("TEMPORIZADOR_UI", "🎬 Iniciando temporizador visual de ${tiempoTotalSegundos} segundos")

        temporizadorUI = object : CountDownTimer((tiempoTotalSegundos * 1000).toLong(), 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val segundosRestantes = (millisUntilFinished / 1000).toInt()

                if (isAdded && ::binding.isInitialized) {
                    actualizarTemporizadorUI(segundosRestantes)
                }
            }

            override fun onFinish() {
                Log.d("TEMPORIZADOR_UI", "⏰ Temporizador UI terminado")
                if (isAdded) {
                    // Mostrar tiempo agotado
                    actualizarTemporizadorUI(0)
                    // El callback del servidor se encargará de la selección automática
                }
            }
        }

        temporizadorUI?.start()
    }


// MEJORAR verificarYIniciarTemporizador() con más inteligencia:

    /**
     * ACTUALIZADO: Verificar y iniciar temporizador si es mi turno
     */
    private fun verificarYIniciarTemporizador() {
        val usuarioId = firebaseAuth.uid ?: ""
        val ligaId = ligaActual?.id ?: ""
        val puedeSeleccionar = controladorDraft?.puedeSeleccionar(usuarioId) ?: false

        Log.d("DEBUG_TEMPORIZADOR", "🔍 Verificando temporizador - Puede seleccionar: $puedeSeleccionar")

        if (puedeSeleccionar) {
            // Es mi turno - iniciar temporizador
            if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {
                Log.d("DEBUG_TEMPORIZADOR", "🎯 ES MI TURNO - Iniciando temporizador")
                iniciarTemporizadorGlobal()
            }
        } else {
            // No es mi turno - ocultar temporizador
            Log.d("DEBUG_TEMPORIZADOR", "⏸️ NO es mi turno - Ocultando temporizador")
            ocultarTemporizador()
        }
    }

    /**
     * ACTUALIZADO: Detener temporizador completamente
     */
    private fun detenerTemporizadorGlobal() {
        Log.d("TEMPORIZADOR", "🛑 Deteniendo temporizador global")
        detenerTemporizadorUI()
        TemporizadorGlobal.detenerTemporizador()
    }


    /**
     * NUEVO: Oculta el temporizador sin detenerlo (cuando no es mi turno)
     */
    private fun ocultarTemporizador() {
        try {
            detenerTemporizadorUI()
            TemporizadorGlobal.desregistrarCallbacks()
        } catch (e: Exception) {
            Log.w("TEMPORIZADOR", "Error ocultando temporizador: ${e.message}")
        }
    }


    /**
     * ACTUALIZADO: Método fallback sin información de progreso
     */
    private fun actualizarUIFallback(puedeSeleccionar: Boolean) {
        try {
            // Intentar usar el nuevo layout primero
            binding.cardEstadoDraft.visibility = if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) View.VISIBLE else View.GONE

            if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {
                // Actualizar estado visual
                actualizarEstadoVisual(puedeSeleccionar)
            }

        } catch (e: Exception) {
            Log.w("DRAFT_FRAGMENT", "Error con nuevo layout, usando layout antiguo: ${e.message}")

            // Fallback: usar el layout antiguo
            try {
                val cardEstadoDraft = binding.root.findViewById<View>(R.id.cardEstadoDraft)
                val tvEstadoDraft = binding.root.findViewById<TextView>(R.id.tvEstadoDraft)

                if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {
                    cardEstadoDraft?.visibility = View.VISIBLE

                    // Estado del draft
                    tvEstadoDraft?.text = if (puedeSeleccionar) "🎯 ¡ES TU TURNO!" else "⏳ Draft Activo"
                    tvEstadoDraft?.setTextColor(if (puedeSeleccionar) Color.parseColor("#FF6D00") else Color.parseColor("#2E7D32"))

                    // Color de fondo según si es el turno del usuario
                    val cardView = cardEstadoDraft as? MaterialCardView
                    cardView?.setCardBackgroundColor(
                        if (puedeSeleccionar) Color.parseColor("#FFF3E0") else Color.parseColor("#E8F5E8")
                    )

                } else {
                    cardEstadoDraft?.visibility = View.GONE
                }

            } catch (e2: Exception) {
                Log.w("DRAFT_FRAGMENT", "Layout antiguo tampoco funciona: ${e2.message}")
                // Último fallback: solo mostrar Toast cuando es el turno del usuario
                if (puedeSeleccionar && estadoDraftActual.draftIniciado) {
                    val miUsuarioId = firebaseAuth.uid ?: ""
                    val infoTurno = controladorDraft?.obtenerInfoTurnoConPosicion(miUsuarioId) ?: "Es tu turno"
                    Toast.makeText(mContexto, "🎯 $infoTurno", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    /**
     * ACTUALIZADO: Actualiza el estado visual del draft mostrando posición en cola
     */
    private fun actualizarEstadoVisual(esMiTurno: Boolean) {
        try {
            if (esMiTurno) {
                binding.tvEstadoDraft.text = "¡TU TURNO!"
                binding.tvEstadoDraft.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_my_turn))
                binding.indicadorEstado.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.status_my_turn)
                )
            } else if (estadoDraftActual.draftIniciado && !estadoDraftActual.draftCompletado) {
                // NUEVO: Usar el controlador para obtener posición en cola
                val miUsuarioId = firebaseAuth.uid ?: ""
                val posicionEnCola = controladorDraft?.obtenerPosicionEnCola(miUsuarioId) ?: -1

                val textoEspera = when {
                    posicionEnCola == 1 -> "Falta 1 para tu turno"
                    posicionEnCola > 1 -> "Faltan $posicionEnCola para tu turno"
                    else -> "Activo" // Fallback
                }

                binding.tvEstadoDraft.text = textoEspera
                binding.tvEstadoDraft.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_active))
                binding.indicadorEstado.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.status_active)
                )

                Log.d("ESTADO_VISUAL", "Usuario $miUsuarioId - Posición en cola: $posicionEnCola")
            } else {
                binding.tvEstadoDraft.text = "Esperando"
                binding.tvEstadoDraft.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_waiting))
                binding.indicadorEstado.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.status_waiting)
                )
            }
        } catch (e: Exception) {
            Log.w("DRAFT_FRAGMENT", "Error actualizando estado visual: ${e.message}")
        }
    }

    /**
     * NUEVO: Mostrar mensaje cuando se realiza selección automática
     */
    private fun mostrarMensajeSeleccionAutomatica() {
        try {
            Log.d("SELECCION_AUTO", "⚡ Mostrando mensaje de selección automática")

            if (isAdded && ::binding.isInitialized) {
                // Cambiar UI para mostrar que se está seleccionando automáticamente
                binding.tvTiempoRestante.text = "0:00"
                binding.progressTemporizador.progress = 0

                // Mostrar mensaje
                Toast.makeText(
                    requireContext(),
                    "⏱️ Tiempo agotado! El sistema está seleccionando automáticamente el mejor lineup disponible...",
                    Toast.LENGTH_LONG
                ).show()

                // Mostrar notificación en la UI
                mostrarNotificacionSeleccionAutomatica()
            }

        } catch (e: Exception) {
            Log.w("SELECCION_AUTO", "Error mostrando mensaje: ${e.message}")
        }
    }

    /**
     * NUEVO: Mostrar notificación visual de selección automática
     */
    private fun mostrarNotificacionSeleccionAutomatica() {
        try {
            if (::binding.isInitialized) {
                // Crear un TextView temporal para la notificación
                val notificacion = TextView(requireContext()).apply {
                    text = "🤖 Selección automática en progreso..."
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#FF9800"))
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    gravity = Gravity.CENTER
                }

                // Agregar al layout principal temporalmente
                val parentLayout = binding.root as? ViewGroup
                parentLayout?.addView(notificacion)

                // Remover después de 3 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        parentLayout?.removeView(notificacion)
                    } catch (e: Exception) {
                        Log.w("SELECCION_AUTO", "Error removiendo notificación: ${e.message}")
                    }
                }, 3000)
            }
        } catch (e: Exception) {
            Log.w("SELECCION_AUTO", "Error mostrando notificación: ${e.message}")
        }
    }

    /**
     * NUEVO: Calcula cuántos usuarios faltan para el turno del usuario actual
     */
    private fun calcularPosicionEnCola(): Int {
        try {
            val miUsuarioId = firebaseAuth.uid ?: ""
            val ordenTurnos = estadoDraftActual.ordenTurnos
            val turnoActual = estadoDraftActual.turnoActual

            if (ordenTurnos.isEmpty() || miUsuarioId.isEmpty()) {
                Log.w("POSICION_COLA", "Datos insuficientes para calcular posición")
                return -1
            }

            // Encontrar mi posición en el orden de turnos
            val miPosicion = ordenTurnos.indexOf(miUsuarioId)
            if (miPosicion == -1) {
                Log.w("POSICION_COLA", "Usuario no encontrado en orden de turnos")
                return -1
            }

            // Calcular cuántos turnos faltan
            val totalUsuarios = ordenTurnos.size
            var turnosRestantes = 0

            // Si el turno actual es menor que mi posición, es en esta ronda
            if (turnoActual < miPosicion) {
                turnosRestantes = miPosicion - turnoActual
            } else {
                // Si el turno actual es mayor o igual, es en la siguiente ronda
                // Calcular turnos hasta el final de la ronda actual + mi posición en la siguiente
                turnosRestantes = (totalUsuarios - turnoActual) + miPosicion
            }

            Log.d("POSICION_COLA", "Mi usuario: $miUsuarioId")
            Log.d("POSICION_COLA", "Mi posición: $miPosicion")
            Log.d("POSICION_COLA", "Turno actual: $turnoActual")
            Log.d("POSICION_COLA", "Turnos restantes: $turnosRestantes")

            return turnosRestantes

        } catch (e: Exception) {
            Log.e("POSICION_COLA", "Error calculando posición en cola: ${e.message}")
            return -1
        }
    }

    /**
     * ACTUALIZADO: Actualizar UI del temporizador con animaciones
     */
    private fun actualizarTemporizadorUI(tiempoRestanteSegundos: Int) {
        try {
            if (!::binding.isInitialized) {
                return
            }

            val minutos = tiempoRestanteSegundos / 60
            val segundos = tiempoRestanteSegundos % 60

            // Actualizar texto del tiempo
            binding.tvTiempoRestante.text = String.format("%d:%02d", minutos, segundos)

            // Actualizar barra de progreso
            binding.progressTemporizador.progress = tiempoRestanteSegundos

            // Cambiar colores según el tiempo restante
            val (colorTexto, colorProgreso) = when {
                tiempoRestanteSegundos <= 30 -> Pair(R.color.red_urgent, R.color.red_urgent)
                tiempoRestanteSegundos <= 60 -> Pair(R.color.orange_warning, R.color.orange_warning)
                else -> Pair(R.color.blue_normal, R.color.blue_normal)
            }

            // Aplicar colores
            binding.tvTiempoRestante.setTextColor(ContextCompat.getColor(requireContext(), colorTexto))
            binding.progressTemporizador.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), colorProgreso)
            )

            // Animación de alerta cuando quedan pocos segundos
            if (tiempoRestanteSegundos <= 30 && tiempoRestanteSegundos > 0) {
                if (tiempoRestanteSegundos % 2 == 0) { // Parpadear cada 2 segundos
                    animarAlerta()
                }
            }

            Log.d("TEMPORIZADOR_UI", "⏰ UI actualizada: ${minutos}:${segundos.toString().padStart(2, '0')}")

        } catch (e: Exception) {
            Log.w("TEMPORIZADOR_UI", "Error actualizando UI temporizador: ${e.message}")
        }
    }

    /**
     * NUEVO: Animación de alerta para los últimos segundos
     */
    private fun animarAlerta() {
        try {
            if (::binding.isInitialized) {
                val animation = ObjectAnimator.ofFloat(binding.tvTiempoRestante, "alpha", 1.0f, 0.3f, 1.0f)
                animation.duration = 500
                animation.start()

                // Vibración suave (si está disponible)
                val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.w("TEMPORIZADOR_UI", "Error en animación de alerta: ${e.message}")
        }
    }

    /**
     * NUEVO: Detener temporizador UI
     */
    private fun detenerTemporizadorUI() {
        Log.d("TEMPORIZADOR_UI", "🛑 Deteniendo temporizador UI")
        temporizadorUI?.cancel()
        temporizadorUI = null

        try {
            if (::binding.isInitialized) {
                binding.layoutTemporizador.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.w("TEMPORIZADOR_UI", "Error ocultando temporizador UI: ${e.message}")
        }
    }


    /**
     * NUEVO: Busca un lineup disponible para selección automática
     */
    private fun buscarLineupDisponible(): Triple<String, Map<String, Map<String, Any>>, String>? {
        try {
            // Lista de tipos de lineup en orden de prioridad
            val tiposLineup = listOf("infield", "outfield", "pitchers", "relief")

            for (equipoId in Constantes.equiposNombres) {
                val equipoKey = equipoId.replace(" ", "")

                for (tipo in tiposLineup) {
                    // Verificar si ya seleccioné este tipo
                    if (yaSeleccioneTipoLineup(tipo)) continue

                    // Verificar si este lineup específico ya está tomado
                    if (estaLineupSeleccionado(equipoKey, tipo)) continue

                    // Obtener jugadores del equipo
                    val jugadores = obtenerJugadoresEquipo(equipoKey, tipo)
                    if (jugadores.isNotEmpty()) {
                        Log.d("SELECCION_AUTO", "Lineup disponible encontrado: $equipoKey - $tipo")
                        return Triple(tipo, jugadores, equipoKey)
                    }
                }
            }

            return null
        } catch (e: Exception) {
            Log.e("SELECCION_AUTO", "Error buscando lineup: ${e.message}")
            return null
        }
    }

    /**
     * NUEVO: Obtiene jugadores de un equipo específico (simulado para selección automática)
     */
    private fun obtenerJugadoresEquipo(equipoId: String, tipo: String): Map<String, Map<String, Any>> {
        // Esta es una implementación simplificada para la selección automática
        // Los datos reales se obtienen cuando el usuario selecciona manualmente
        return when (tipo) {
            "infield" -> mapOf(
                "C" to mapOf("nombre" to "Auto Catcher", "rating" to 75),
                "1B" to mapOf("nombre" to "Auto 1B", "rating" to 80),
                "2B" to mapOf("nombre" to "Auto 2B", "rating" to 78),
                "3B" to mapOf("nombre" to "Auto 3B", "rating" to 82),
                "SS" to mapOf("nombre" to "Auto SS", "rating" to 85)
            )
            "outfield" -> mapOf(
                "LF" to mapOf("nombre" to "Auto LF", "rating" to 79),
                "CF" to mapOf("nombre" to "Auto CF", "rating" to 83),
                "RF" to mapOf("nombre" to "Auto RF", "rating" to 77),
                "DH" to mapOf("nombre" to "Auto DH", "rating" to 81)
            )
            "pitchers" -> mapOf(
                "SP1" to mapOf("nombre" to "Auto SP1", "rating" to 82),
                "SP2" to mapOf("nombre" to "Auto SP2", "rating" to 78),
                "SP3" to mapOf("nombre" to "Auto SP3", "rating" to 76),
                "SP4" to mapOf("nombre" to "Auto SP4", "rating" to 74),
                "SP5" to mapOf("nombre" to "Auto SP5", "rating" to 72)
            )
            "relief" -> mapOf(
                "RP1" to mapOf("nombre" to "Auto RP1", "rating" to 75),
                "RP2" to mapOf("nombre" to "Auto RP2", "rating" to 73),
                "RP3" to mapOf("nombre" to "Auto RP3", "rating" to 71),
                "RP4" to mapOf("nombre" to "Auto RP4", "rating" to 69),
                "RP5" to mapOf("nombre" to "Auto RP5", "rating" to 67),
                "RP6" to mapOf("nombre" to "Auto RP6", "rating" to 65),
                "RP7" to mapOf("nombre" to "Auto RP7", "rating" to 63),
                "RP8" to mapOf("nombre" to "Auto RP8", "rating" to 80)
            )
            else -> emptyMap()
        }
    }

    /**
     * NUEVO: Muestra información del turno actual
     */
    private fun mostrarInfoTurno(info: String, esMiTurno: Boolean) {
        val mensaje = if (esMiTurno) {
            "🎯 ¡ES TU TURNO!\n$info"
        } else {
            "⏳ $info"
        }

        // Solo mostrar Toast cuando realmente es el turno del usuario
        if (esMiTurno && estadoDraftActual.draftIniciado) {
            Toast.makeText(mContexto, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarInformacionLiga(liga: ModeloLiga) {
        binding.tvNombreReal.text = liga.nombreLiga
        Log.d("LIGA_INFO", "Liga autorizada encontrada: ${liga.nombreLiga} con ${liga.usuariosParticipantes.size} participantes")
    }

    private fun buscarCualquierLiga() {
        val ligasRef = FirebaseDatabase.getInstance().getReference("Ligas")

        ligasRef.limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(ligasSnapshot: DataSnapshot) {
                if (ligasSnapshot.exists()) {
                    val liga = ligasSnapshot.children.first()
                    val nombreLiga = liga.child("nombreLiga").value.toString()

                    binding.tvNombreReal.text = nombreLiga
                    Log.d("LIGA_INFO", "Liga encontrada (cualquiera): $nombreLiga")
                } else {
                    binding.tvNombreReal.text = "Liga de Béisbol Custom"
                    Log.d("LIGA_INFO", "No se encontraron ligas, usando texto por defecto")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvNombreReal.text = "Liga de Béisbol"
                Log.e("LIGA_ERROR", "Error al buscar cualquier liga: ${error.message}")
            }
        })
    }

    private fun cargarEquipos() {
        val equiposArrayList = ArrayList<ModeloEquipos>()
        for (i in 0 until Constantes.equiposNombres.size) {
            val modeloEquipos = ModeloEquipos(Constantes.equiposNombres[i], Constantes.equiposIconos[i])
            equiposArrayList.add(modeloEquipos)
        }

        val adaptadorEquipos = AdaptadorEquipos(mContexto, equiposArrayList, object :
            RvListennerEquipos {
            override fun onEquipoClick(modeloEquipos: ModeloEquipos) {
                cargarLineupsEquipo(modeloEquipos.equioosId)
            }
        })

        binding.equiposTodosRV.adapter = adaptadorEquipos
    }

    private fun escucharLineupsSeleccionados() {
        val database = FirebaseDatabase.getInstance()
        val lineupsSeleccionadosRef = database.getReference("LineupsSeleccionados")

        lineupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // NUEVO: Verificar si el fragmento sigue conectado antes de procesar
                if (!isAdded || activity == null) {
                    Log.d("LINEUPS_SELECTED", "Fragmento no conectado, ignorando cambios de Firebase")
                    return
                }

                lineupsSeleccionados.clear()

                for (lineupSnapshot in snapshot.children) {
                    val lineup = lineupSnapshot.getValue(LineupSeleccionado::class.java)
                    lineup?.let {
                        lineupsSeleccionados.add(it)
                        Log.d("LINEUPS_SELECTED", "Lineup seleccionado: ${it.equipoId} - ${it.tipoLineup} por ${it.idGamingUsuario}")
                    }
                }

                sincronizarViewModelConFirebase()
                refrescarLineupsEnUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LINEUPS_SELECTED", "Error al escuchar lineups seleccionados: ${error.message}")
            }
        }
        lineupsSeleccionadosRef.addValueEventListener(lineupsListener!!)
    }

    private fun refrescarLineupsEnUI() {
        Log.d("LINEUPS_REFRESH", "Refrescando UI con ${lineupsSeleccionados.size} lineups seleccionados")
    }

    private fun sincronizarViewModelConFirebase() {
        // NUEVO: Verificar si el fragmento sigue conectado
        if (!isAdded || activity == null) {
            Log.d("SYNC_VIEWMODEL", "Fragmento no conectado, cancelando sincronización")
            return
        }

        if (viewModelSincronizado) return

        val misSelecciones = lineupsSeleccionados.filter {
            it.usuarioId == firebaseAuth.uid
        }

        if (misSelecciones.isNotEmpty()) {
            Log.d("SYNC_VIEWMODEL", "Sincronizando ${misSelecciones.size} lineups del usuario")

            try {
                lineupViewModel.limpiarLineups()

                for (lineup in misSelecciones) {
                    val agregado = lineupViewModel.agregarLineup(lineup.tipoLineup, lineup.jugadores)
                    if (agregado) {
                        Log.d("SYNC_VIEWMODEL", "Lineup ${lineup.tipoLineup} agregado al ViewModel")
                    } else {
                        Log.w("SYNC_VIEWMODEL", "No se pudo agregar lineup ${lineup.tipoLineup}")
                    }
                }

                viewModelSincronizado = true
                Log.d("SYNC_VIEWMODEL", "Sincronización completada. Total en ViewModel: ${lineupViewModel.getCantidadLineups()}")
            } catch (e: Exception) {
                Log.e("SYNC_VIEWMODEL", "Error durante sincronización: ${e.message}")
            }
        }
    }

    private fun estaLineupSeleccionado(equipoId: String, tipoLineup: String): Boolean {
        return lineupsSeleccionados.any {
            it.equipoId == equipoId && it.tipoLineup == tipoLineup
        }
    }

    private fun obtenerUsuarioQueSelecciono(equipoId: String, tipoLineup: String): String? {
        val lineup = lineupsSeleccionados.find {
            it.equipoId == equipoId && it.tipoLineup == tipoLineup
        }

        return when {
            !lineup?.idGamingUsuario.isNullOrEmpty() -> lineup?.idGamingUsuario
            !lineup?.nombreUsuario.isNullOrEmpty() -> lineup?.nombreUsuario
            else -> "Usuario desconocido"
        }
    }

    private fun esMiSeleccion(equipoId: String, tipoLineup: String): Boolean {
        return lineupsSeleccionados.any {
            it.equipoId == equipoId && it.tipoLineup == tipoLineup && it.usuarioId == firebaseAuth.uid
        }
    }

    private fun cargarLineupsEquipo(nombreEquipo: String) {
        val equipoKey = nombreEquipo.replace(" ", "")
        Log.d("LINEUPS", "Cargando lineup del equipo: $equipoKey")

        // NUEVO: Usar EquiposManager para obtener datos globales
        EquiposManager.obtenerEquipo(equipoKey) { equipo ->
            if (equipo != null) {
                Log.d("LINEUPS", "Datos del equipo encontrados: ${equipo["nombre"]}")

                @Suppress("UNCHECKED_CAST")
                val jugadores = equipo["jugadores"] as? Map<String, Map<String, Map<String, Any>>> ?: emptyMap()

                val equipoData = Equipo(
                    nombre = equipo["nombre"] as? String ?: nombreEquipo,
                    jugadores = jugadores
                )

                mostrarLineups(equipoData, equipoKey)
            } else {
                Log.e("LINEUPS", "Equipo no encontrado: $equipoKey")
                Toast.makeText(mContexto, "Equipo no encontrado: $nombreEquipo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerNombreTipo(tipo: String): String {
        return when(tipo) {
            "infield" -> "Infield"
            "outfield" -> "Outfield"
            "pitchers" -> "Pitchers"
            "relief" -> "Relief"
            else -> tipo.capitalize()
        }
    }

    private fun mostrarLineups(equipo: Equipo, equipoId: String) {
        val lineups = listOf(
            Triple("infield", equipo.jugadores["infield"] ?: emptyMap(), estaLineupSeleccionado(equipoId, "infield")),
            Triple("outfield", equipo.jugadores["outfield"] ?: emptyMap(), estaLineupSeleccionado(equipoId, "outfield")),
            Triple("pitchers", equipo.jugadores["pitchers"] ?: emptyMap(), estaLineupSeleccionado(equipoId, "pitchers")),
            Triple("relief", equipo.jugadores["relief"] ?: emptyMap(), estaLineupSeleccionado(equipoId, "relief"))
        )

        val adaptador = LineupsAdapter(lineups, equipoId, object : OnLineupClickListener {
            override fun onlineupClick(tipo: String, jugadores: Map<String, Map<String, Any>>) {

                // NUEVO: Verificar si es el turno del usuario
                val puedeSeleccionar = controladorDraft?.puedeSeleccionar(firebaseAuth.uid ?: "") ?: false

                if (!puedeSeleccionar) {
                    mostrarMensajeNoEsTurno()
                    return
                }

                Log.d("LINEUP_CLICK", "Lineup clickeado: tipo=$tipo, equipoId=$equipoId")

                try {
                    // Verificar si ya seleccionó este tipo de lineup en alguna ronda anterior
                    if (yaSeleccioneTipoLineup(tipo)) {
                        val nombreTipo = obtenerNombreTipo(tipo)
                        Toast.makeText(
                            mContexto,
                            "Ya has seleccionado un lineup de $nombreTipo en una ronda anterior.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    if (estaLineupSeleccionado(equipoId, tipo)) {
                        val usuarioQueSelecciono = obtenerUsuarioQueSelecciono(equipoId, tipo)
                        Toast.makeText(
                            mContexto,
                            "Este lineup ya ha sido seleccionado por $usuarioQueSelecciono",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    Log.d("LINEUP_CLICK", "Mostrando diálogo de confirmación para: $tipo")
                    mostrarDialogoConfirmacionDraft(equipoId, tipo, jugadores)

                } catch (e: Exception) {
                    Log.e("LINEUP_ERROR", "Error al procesar click en lineup $tipo: ${e.message}")
                    Toast.makeText(mContexto, "Error al procesar lineup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        binding.rvLinups.adapter = adaptador
    }

    /**
     * NUEVO: Verifica si el usuario ya seleccionó este tipo de lineup
     */
    private fun yaSeleccioneTipoLineup(tipo: String): Boolean {
        return lineupsSeleccionados.any {
            it.usuarioId == firebaseAuth.uid && it.tipoLineup == tipo
        }
    }

    /**
     * NUEVO: Muestra mensaje cuando no es el turno del usuario
     */
    private fun mostrarMensajeNoEsTurno() {
        val infoTurno = controladorDraft?.obtenerInfoTurno() ?: "Draft no activo"

        val mensaje = if (!estadoDraftActual.draftIniciado) {
            "⏳ El draft no ha comenzado aún.\n\nEspera a que el administrador lo inicie."
        } else if (estadoDraftActual.draftCompletado) {
            "✅ El draft ha terminado.\n\nRevisa tu equipo en 'Mi Equipo'."
        } else {
            "⏳ No es tu turno\n\n$infoTurno\n\nEspera tu turno para seleccionar."
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(mContexto, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
        builder.setTitle("🚫 Selección Bloqueada")
        builder.setMessage(mensaje)

        builder.setPositiveButton("Entendido") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    /**
     * CORREGIDO: Diálogo de confirmación específico para el draft
     */
    private fun mostrarDialogoConfirmacionDraft(equipoId: String, tipo: String, jugadores: Map<String, Map<String, Any>>) {
        val nombreTipo = obtenerNombreTipo(tipo)

        // CORREGIDO: Obtener información del turno sin callback
        val miUsuarioId = firebaseAuth.uid ?: ""
        val infoTurno = controladorDraft?.obtenerInfoTurnoConPosicion(miUsuarioId) ?: "Draft no activo"

        val builder = androidx.appcompat.app.AlertDialog.Builder(mContexto, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert)
        builder.setTitle("🎯 Confirmar Selección de Draft")

        val mensaje = """
        ¿Confirmas tu selección de $nombreTipo?
        
        $infoTurno
        
        ⚠️ Una vez confirmado:
        • Este lineup no estará disponible para otros
        • Tu turno pasará al siguiente participante
        • No podrás cambiar esta selección
    """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("✅ Confirmar Selección") { dialog, _ ->
            dialog.dismiss()
            confirmarSeleccionDraft(equipoId, tipo, jugadores)
        }

        builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            textSize = 16f
            setPadding(16, 8, 16, 8)
        }

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            textSize = 16f
            setPadding(16, 8, 16, 8)
        }
    }
    private fun confirmarSeleccionDraft(equipoId: String, tipo: String, jugadores: Map<String, Map<String, Any>>) {
        Log.d("TEMPORIZADOR", "Usuario confirmó selección - DETENIENDO temporizador")

        // Detener temporizador (porque usuario ya eligió)
        detenerTemporizadorGlobal()

        // Procesar selección
        val puedeAgregar = lineupViewModel.agregarLineup(tipo, jugadores)

        if (puedeAgregar) {
            guardarLineupSeleccionadoDraft(equipoId, tipo, jugadores)

            val nombreTipo = obtenerNombreTipo(tipo)
            Toast.makeText(mContexto, "✅ $nombreTipo seleccionado!", Toast.LENGTH_LONG).show()

            // Avanzar turno (siguiente usuario tendrá temporizador nuevo)
            controladorDraft?.avanzarTurno()

            navegarAFragmentMiEquipo(tipo, jugadores)
        }
    }
    /**
     * NUEVO: Guarda el lineup con información del draft
     */
    private fun guardarLineupSeleccionadoDraft(equipoId: String, tipo: String, jugadores: Map<String, Map<String, Any>>) {
        val database = FirebaseDatabase.getInstance()
        val lineupsRef = database.getReference("LineupsSeleccionados")

        val lineupSeleccionado = LineupSeleccionado(
            equipoId = equipoId,
            tipoLineup = tipo,
            usuarioId = firebaseAuth.uid ?: "",
            nombreUsuario = nombreUsuarioActual,
            idGamingUsuario = idGamingUsuarioActual,
            fechaSeleccion = System.currentTimeMillis(),
            jugadores = jugadores,
            ronda = estadoDraftActual.rondaActual,
            turno = estadoDraftActual.turnoActual
        )

        val claveLineup = "${equipoId}_${tipo}"

        lineupsRef.child(claveLineup).setValue(lineupSeleccionado)
            .addOnSuccessListener {
                Log.d("LINEUP_GUARDADO", "Lineup guardado exitosamente: $claveLineup para usuario $idGamingUsuarioActual en ronda ${estadoDraftActual.rondaActual}")
            }
            .addOnFailureListener { error ->
                Log.e("LINEUP_ERROR", "Error al guardar lineup: ${error.message}")
                Toast.makeText(mContexto, "Error al guardar lineup: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navegarAFragmentMiEquipo(tipo: String, jugadores: Map<String, Map<String, Any>>) {
        try {
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentExistente = fragmentManager.findFragmentById(R.id.fragmet_LayoutL1)

            if (fragmentExistente !is FragmentMiEquipo) {
                val bundle = Bundle().apply {
                    putString("tipo", tipo)
                    putSerializable("jugadores", HashMap(jugadores))
                }

                val fragmentMiEquipo = FragmentMiEquipo()
                fragmentMiEquipo.arguments = bundle

                fragmentManager.beginTransaction()
                    .replace(R.id.fragmet_LayoutL1, fragmentMiEquipo)
                    .addToBackStack("MiEquipo")
                    .commitAllowingStateLoss()
            }
        } catch (e: Exception) {
            Log.e("NAVEGACION_ERROR", "Error al navegar a Mi Equipo: ${e.message}")
        }
    }

    fun forzarSincronizacion() {
        viewModelSincronizado = false
        sincronizarViewModelConFirebase()
    }

    /**
     * ✏️ MODIFICADO: onDestroyView mejorado
     */
    override fun onDestroyView() {
        super.onDestroyView()

        Log.d("FRAGMENT_LIFECYCLE", "💀 FragmentInicio - onDestroyView")

        // Solo limpiar recursos locales, NO detener temporizador global
        detenerTemporizadorUI()
        TemporizadorGlobal.desregistrarCallbacks()

        // Limpiar listeners de Firebase
        lineupsListener?.let {
            FirebaseDatabase.getInstance().getReference("LineupsSeleccionados").removeEventListener(it)
        }
        ligasListener?.let {
            FirebaseDatabase.getInstance().getReference("Ligas").removeEventListener(it)
        }

        controladorDraft = null

        // IMPORTANTE: NO resetear temporizadorInicializado aquí
        // Se resetea solo cuando el usuario hace una selección

        Log.d("FRAGMENT_LIFECYCLE", "FragmentInicio destruido, listeners limpiados")
    }

    /**
     * ✏️ MODIFICADO: onResume mejorado
     */
    override fun onResume() {
        super.onResume()
        Log.d("FRAGMENT_LIFECYCLE", "📱 FragmentInicio - onResume")

        // NUEVO: Solo reconectar el temporizador si ya estaba inicializado
        if (temporizadorInicializado && !esVisiblePorPrimeraVez) {
            Log.d("FRAGMENT_LIFECYCLE", "🔄 Reconectando temporizador existente")

            // Pequeño delay para asegurar que la UI esté lista
            Handler(Looper.getMainLooper()).postDelayed({
                reconectarTemporizador()
            }, 200)

        } else {
            Log.d("FRAGMENT_LIFECYCLE", "⏳ Primera vez visible o temporizador no inicializado")
            esVisiblePorPrimeraVez = false
        }
    }
    /**
     * ✏️ MODIFICADO: onPause mejorado
     */
    override fun onPause() {
        super.onPause()
        Log.d("FRAGMENT_LIFECYCLE", "⏸️ FragmentInicio - onPause")

        // IMPORTANTE: Solo desregistrar callbacks, NO detener el temporizador
        TemporizadorGlobal.desregistrarCallbacks()

        // Detener temporizador UI local (visual)
        temporizadorUI?.cancel()
    }

    /**
     * 🆕 NUEVO: Agregar onHiddenChanged para manejar show/hide
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.d("FRAGMENT_LIFECYCLE", "👁️ FragmentInicio - Hidden: $hidden")

        val tiempoActual = System.currentTimeMillis()

        // Evitar navegaciones muy rápidas
        if (tiempoActual - ultimaNavegacion < 500) {
            Log.d("FRAGMENT_LIFECYCLE", "⏭️ Navegación muy rápida, ignorando")
            return
        }
        ultimaNavegacion = tiempoActual

        if (!hidden) {
            // Fragment se vuelve visible
            Log.d("FRAGMENT_LIFECYCLE", "👀 Fragment visible - Reconectando temporizador")

            // Pequeño delay para asegurar transición
            Handler(Looper.getMainLooper()).postDelayed({
                reconectarTemporizador()
            }, 300)

        } else {
            // Fragment se oculta
            Log.d("FRAGMENT_LIFECYCLE", "🙈 Fragment oculto - Desregistrando callbacks")
            TemporizadorGlobal.desregistrarCallbacks()
            temporizadorUI?.cancel()
        }
    }

}