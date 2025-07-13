package com.joatoribio.customleaguebeisbol.Draft

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Temporizador que se sincroniza con el timestamp de vencimiento del servidor
 * Mantiene la cuenta regresiva aunque se cierre la app o cambie de fragment
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
     * Iniciar temporizador sincronizado con servidor
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
     * Sincronizar con el temporizador del servidor usando timestampVencimiento
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

                // IMPORTANTE: Usar timestampVencimiento del servidor
                val timestampVencimiento = (serverTimer["timestampVencimiento"] as? Long) ?: 0L
                val rondaServidor = (serverTimer["ronda"] as? Long)?.toInt() ?: 0
                val turnoServidor = (serverTimer["turno"] as? Long)?.toInt() ?: 0

                // Verificar que corresponde al turno actual
                if (rondaServidor != rondaActual || turnoServidor != turnoActual) {
                    Log.d("TEMPORIZADOR_GLOBAL", "🔄 Turno cambió en servidor, deteniendo temporizador local")
                    detenerTemporizador()
                    return
                }

                // Calcular tiempo restante basado en timestamp absoluto
                val ahora = System.currentTimeMillis()
                val tiempoRestanteMs = timestampVencimiento - ahora
                val tiempoRestanteSegundos = (tiempoRestanteMs / 1000).toInt()

                Log.d("TEMPORIZADOR_GLOBAL", "⏰ Timestamp vencimiento: $timestampVencimiento")
                Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo restante: ${tiempoRestanteSegundos}s")

                if (tiempoRestanteSegundos <= 0) {
                    Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo agotado según servidor")
                    onFinishCallback?.invoke()
                    detenerTemporizador()
                } else {
                    // Iniciar cuenta regresiva local sincronizada con el timestamp
                    iniciarCuentaRegresivaLocal(tiempoRestanteSegundos)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TEMPORIZADOR_GLOBAL", "Error sincronizando con servidor: ${error.message}")
            }
        }

        // Escuchar cambios en tiempo real
        temporizadorRef.addValueEventListener(serverTimerListener!!)
    }

    /**
     * Iniciar cuenta regresiva local basada en el tiempo restante del servidor
     */
    private fun iniciarCuentaRegresivaLocal(segundosRestantes: Int) {
        // Cancelar runnable anterior si existe
        runnable?.let { handler?.removeCallbacks(it) }

        var tiempoRestante = segundosRestantes

        runnable = object : Runnable {
            override fun run() {
                if (tiempoRestante > 0) {
                    // Notificar callback
                    onTickCallback?.invoke(tiempoRestante)

                    // Decrementar y programar siguiente tick
                    tiempoRestante--
                    handler?.postDelayed(this, 1000)
                } else {
                    // Tiempo agotado
                    Log.d("TEMPORIZADOR_GLOBAL", "⏰ Tiempo agotado localmente")
                    onFinishCallback?.invoke()
                    detenerTemporizador()
                }
            }
        }

        // Ejecutar inmediatamente el primer tick
        handler?.post(runnable!!)
    }

    /**
     * Detener temporizador
     */
    fun detenerTemporizador() {
        Log.d("TEMPORIZADOR_GLOBAL", "🛑 Deteniendo temporizador")

        // Cancelar runnable
        runnable?.let { handler?.removeCallbacks(it) }
        runnable = null
        handler = null

        // Remover listener del servidor
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
     * Verificar estado del temporizador del servidor (para reconexión)
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

                val ahora = System.currentTimeMillis()
                val tiempoRestanteMs = timestampVencimiento - ahora
                val tiempoRestanteSegundos = (tiempoRestanteMs / 1000).toInt()

                callback(activo && tiempoRestanteSegundos > 0, tiempoRestanteSegundos)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TEMPORIZADOR_GLOBAL", "Error verificando estado servidor: ${error.message}")
                callback(false, 0)
            }
        })
    }

    /**
     * Desregistrar callbacks sin detener el temporizador
     * (útil cuando el fragment se pausa pero el temporizador debe continuar)
     */
    fun desregistrarCallbacks() {
        onTickCallback = null
        onFinishCallback = null
    }

    /**
     * Re-registrar callbacks cuando el fragment se reanuda
     */
    fun registrarCallbacks(onTick: (Int) -> Unit, onFinish: () -> Unit) {
        onTickCallback = onTick
        onFinishCallback = onFinish
    }
}