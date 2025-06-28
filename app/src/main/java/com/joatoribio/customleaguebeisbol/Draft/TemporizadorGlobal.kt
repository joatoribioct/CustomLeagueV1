package com.joatoribio.customleaguebeisbol.Draft

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ACTUALIZADO: Temporizador que se sincroniza con el servidor
 * El temporizador del servidor es la fuente de verdad
 */
object TemporizadorGlobal {

    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var onTickCallback: ((Int) -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null

    // Información del temporizador actual
    private var usuarioActualId: String? = null
    private var ligaActualId: String? = null
    private var rondaActual: Int = 0
    private var turnoActual: Int = 0

    // Listener del servidor
    private var serverTimerListener: ValueEventListener? = null

    /**
     * NUEVO: Iniciar temporizador sincronizado con servidor
     */
    fun iniciarTemporizador(
        usuarioId: String,
        ligaId: String,
        ronda: Int,
        turno: Int,
        onTick: (Int) -> Unit,
        onFinish: () -> Unit
    ) {
        Log.d("TEMPORIZADOR_GLOBAL", "🚀 Iniciando temporizador sincronizado con servidor")
        Log.d("TEMPORIZADOR_GLOBAL", "Usuario: $usuarioId, Liga: $ligaId, R$ronda-T$turno")

        // Detener temporizador anterior si existe
        detenerTemporizador()

        // Guardar información
        usuarioActualId = usuarioId
        ligaActualId = ligaId
        rondaActual = ronda
        turnoActual = turno
        onTickCallback = onTick
        onFinishCallback = onFinish

        // Inicializar handler
        handler = Handler(Looper.getMainLooper())

        // Sincronizar con el temporizador del servidor
        sincronizarConServidor(ligaId)
    }

    /**
     * NUEVO: Sincronizar con el temporizador del servidor
     */
    private fun sincronizarConServidor(ligaId: String) {
        val temporizadorRef = FirebaseDatabase.getInstance()
            .getReference("TemporizadoresDraft")
            .child(ligaId)

        serverTimerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d("TEMPORIZADOR_GLOBAL", "❌ No hay temporizador del servidor")
                    onFinishCallback?.invoke()
                    return
                }

                val serverTimer = snapshot.value as? Map<String, Any> ?: return
                val activo = serverTimer["activo"] as? Boolean ?: false

                if (!activo) {
                    Log.d("TEMPORIZADOR_GLOBAL", "⏹️ Temporizador del servidor desactivado")
                    detenerTemporizador()
                    return
                }

                val timestampVencimiento = (serverTimer["timestampVencimiento"] as? Long) ?: 0L
                val rondaServidor = (serverTimer["ronda"] as? Long)?.toInt() ?: 0
                val turnoServidor = (serverTimer["turno"] as? Long)?.toInt() ?: 0

                // Verificar que corresponde al turno actual
                if (rondaServidor != rondaActual || turnoServidor != turnoActual) {
                    Log.d("TEMPORIZADOR_GLOBAL", "🔄 Turno cambió en servidor, deteniendo temporizador local")
                    detenerTemporizador()
                    return
                }

                val tiempoRestanteMs = timestampVencimiento - System.currentTimeMillis()
                val tiempoRestanteSegundos = (tiempoRestanteMs / 1000).toInt()

                Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo restante del servidor: ${tiempoRestanteSegundos}s")

                if (tiempoRestanteSegundos <= 0) {
                    Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo agotado según servidor")
                    onFinishCallback?.invoke()
                    detenerTemporizador()
                } else {
                    // Iniciar cuenta regresiva local sincronizada
                    iniciarCuentaRegresivaLocal(tiempoRestanteSegundos)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TEMPORIZADOR_GLOBAL", "Error sincronizando con servidor: ${error.message}")
            }
        }

        temporizadorRef.addValueEventListener(serverTimerListener!!)
    }

    /**
     * NUEVO: Iniciar cuenta regresiva local sincronizada con servidor
     */
    private fun iniciarCuentaRegresivaLocal(tiempoInicialSegundos: Int) {
        // Detener runnable anterior si existe
        runnable?.let { handler?.removeCallbacks(it) }

        var tiempoRestante = tiempoInicialSegundos

        runnable = object : Runnable {
            override fun run() {
                if (tiempoRestante <= 0) {
                    Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo agotado en cliente")
                    onFinishCallback?.invoke()
                    detenerTemporizador()
                    return
                }

                // Actualizar UI
                onTickCallback?.invoke(tiempoRestante)

                // Programar siguiente tick
                tiempoRestante--
                handler?.postDelayed(this, 1000)
            }
        }

        // Iniciar inmediatamente
        runnable?.let { handler?.post(it) }
    }

    /**
     * Verificar si el temporizador está activo para un turno específico
     */
    fun estaActivoPara(usuarioId: String, ligaId: String, ronda: Int, turno: Int): Boolean {
        return usuarioActualId == usuarioId &&
                ligaActualId == ligaId &&
                rondaActual == ronda &&
                turnoActual == turno &&
                handler != null
    }

    /**
     * Registrar callbacks sin iniciar temporizador (para reconexión)
     */
    fun registrarCallbacks(
        onTick: (Int) -> Unit,
        onFinish: () -> Unit
    ) {
        onTickCallback = onTick
        onFinishCallback = onFinish

        // Si hay un temporizador activo, reconectar con el servidor
        ligaActualId?.let { ligaId ->
            sincronizarConServidor(ligaId)
        }
    }

    /**
     * Desregistrar callbacks (cuando el fragmento se oculta)
     */
    fun desregistrarCallbacks() {
        onTickCallback = null
        onFinishCallback = null

        // No detener el temporizador, solo las callbacks
        Log.d("TEMPORIZADOR_GLOBAL", "📱 Callbacks desregistrados")
    }

    /**
     * Detener temporizador completamente
     */
    fun detenerTemporizador() {
        Log.d("TEMPORIZADOR_GLOBAL", "🛑 Deteniendo temporizador global")

        // Detener runnable local
        runnable?.let { handler?.removeCallbacks(it) }
        runnable = null
        handler = null

        // Detener listener del servidor
        serverTimerListener?.let { listener ->
            ligaActualId?.let { ligaId ->
                FirebaseDatabase.getInstance()
                    .getReference("TemporizadoresDraft")
                    .child(ligaId)
                    .removeEventListener(listener)
            }
        }
        serverTimerListener = null

        // Limpiar callbacks
        onTickCallback = null
        onFinishCallback = null

        // Limpiar información
        usuarioActualId = null
        ligaActualId = null
        rondaActual = 0
        turnoActual = 0
    }

    /**
     * NUEVO: Verificar estado del temporizador del servidor
     */
    fun verificarEstadoServidor(ligaId: String, callback: (Boolean, Int) -> Unit) {
        val temporizadorRef = FirebaseDatabase.getInstance()
            .getReference("TemporizadoresDraft")
            .child(ligaId)

        temporizadorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    callback(false, 0)
                    return
                }

                val serverTimer = snapshot.value as? Map<String, Any> ?: return
                val activo = serverTimer["activo"] as? Boolean ?: false
                val timestampVencimiento = (serverTimer["timestampVencimiento"] as? Long) ?: 0L

                val tiempoRestanteMs = timestampVencimiento - System.currentTimeMillis()
                val tiempoRestanteSegundos = (tiempoRestanteMs / 1000).toInt()

                callback(activo, tiempoRestanteSegundos)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TEMPORIZADOR_GLOBAL", "Error verificando estado servidor: ${error.message}")
                callback(false, 0)
            }
        })
    }
}