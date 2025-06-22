package com.joatoribio.customleaguebeisbol.Draft

import android.os.CountDownTimer
import android.util.Log

/**
 * Temporizador global que persiste entre fragmentos
 */
object TemporizadorGlobal {

    private var temporizador: CountDownTimer? = null
    private var tiempoRestanteSegundos = 180
    private var temporizadorActivo = false
    private var usuarioActual = ""
    private var ligaActual = ""

    // NUEVO: Variables para detectar cambios de turno
    private var rondaActual = -1
    private var turnoActual = -1

    // Callbacks para notificar a la UI
    private var onTickCallback: ((Int) -> Unit)? = null
    private var onFinishCallback: (() -> Unit)? = null

    /**
     * Inicia el temporizador SOLO si es un usuario diferente
     */
    fun iniciarTemporizador(
        usuarioId: String,
        ligaId: String,
        ronda: Int,           // NUEVO: Parámetro de ronda
        turno: Int,           // NUEVO: Parámetro de turno
        onTick: (tiempoRestante: Int) -> Unit,
        onFinish: () -> Unit
    ) {
        Log.d("TEMPORIZADOR_GLOBAL", "🎯 SOLICITUD DE TEMPORIZADOR")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Usuario: $usuarioId")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Liga: $ligaId")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Ronda: $ronda, Turno: $turno")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Estado actual: Usuario=$usuarioActual, Ronda=$rondaActual, Turno=$turnoActual")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Temporizador activo: $temporizadorActivo")

        // CLAVE: Reiniciar si cambió el turno (ronda o posición), NO solo el usuario
        val esMismoTurno = (usuarioActual == usuarioId &&
                ligaActual == ligaId &&
                rondaActual == ronda &&
                turnoActual == turno)

        if (temporizadorActivo && esMismoTurno) {
            Log.d("TEMPORIZADOR_GLOBAL", "✅ MISMO TURNO - Continuando temporizador")
            Log.d("TEMPORIZADOR_GLOBAL", "   - Tiempo restante: $tiempoRestanteSegundos segundos")
            onTickCallback = onTick
            onFinishCallback = onFinish
            onTick(tiempoRestanteSegundos)
            return
        }

        // NUEVO TURNO - Reiniciar temporizador completo
        Log.d("TEMPORIZADOR_GLOBAL", "🆕 NUEVO TURNO DETECTADO - Reiniciando temporizador a 3:00")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Turno anterior: Usuario=$usuarioActual, R$rondaActual-T$turnoActual")
        Log.d("TEMPORIZADOR_GLOBAL", "   - Turno nuevo: Usuario=$usuarioId, R$ronda-T$turno")

        detenerTemporizador()

        // Configurar para nuevo turno
        usuarioActual = usuarioId
        ligaActual = ligaId
        rondaActual = ronda
        turnoActual = turno
        tiempoRestanteSegundos = 180 // SIEMPRE 3 minutos para nuevo turno
        onTickCallback = onTick
        onFinishCallback = onFinish
        temporizadorActivo = true

        temporizador = object : CountDownTimer((tiempoRestanteSegundos * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!temporizadorActivo) {
                    cancel()
                    return
                }

                tiempoRestanteSegundos = (millisUntilFinished / 1000).toInt()
                onTickCallback?.invoke(tiempoRestanteSegundos)

                // Log cada 30 segundos
                if (tiempoRestanteSegundos % 30 == 0) {
                    Log.d("TEMPORIZADOR_GLOBAL", "⏰ R$rondaActual-T$turnoActual ($usuarioActual): $tiempoRestanteSegundos seg")
                }
            }

            override fun onFinish() {
                Log.d("TEMPORIZADOR_GLOBAL", "🏁 TEMPORIZADOR TERMINADO - R$rondaActual-T$turnoActual ($usuarioActual)")
                temporizadorActivo = false
                onFinishCallback?.invoke()
                limpiar()
            }
        }.start()

        Log.d("TEMPORIZADOR_GLOBAL", "🚀 TEMPORIZADOR INICIADO - 3:00 para R$ronda-T$turno ($usuarioId)")
    }

    /**
     * Detiene el temporizador (cuando usuario confirma selección)
     */
    fun detenerTemporizador() {
        Log.d("TEMPORIZADOR_GLOBAL", "Deteniendo temporizador para usuario: $usuarioActual")
        temporizadorActivo = false
        temporizador?.cancel()
        limpiar()
    }

    /**
     * Registra callbacks para UI (NO reinicia temporizador)
     */
    fun registrarCallbacks(
        onTick: (tiempoRestante: Int) -> Unit,
        onFinish: () -> Unit
    ) {
        Log.d("TEMPORIZADOR_GLOBAL", "Registrando callbacks para UI")
        onTickCallback = onTick
        onFinishCallback = onFinish

        // Si el temporizador está activo, notificar el tiempo actual
        if (temporizadorActivo) {
            onTick(tiempoRestanteSegundos)
            Log.d("TEMPORIZADOR_GLOBAL", "Notificando tiempo actual: $tiempoRestanteSegundos segundos")
        }
    }

    /**
     * Desregistra callbacks (cuando fragmento se destruye)
     */
    fun desregistrarCallbacks() {
        Log.d("TEMPORIZADOR_GLOBAL", "Desregistrando callbacks UI")
        onTickCallback = null
        onFinishCallback = null
        // NO detener el temporizador aquí
    }

    /**
     * Verifica si el temporizador está activo para un usuario específico
     */
    fun estaActivoPara(usuarioId: String, ligaId: String, ronda: Int, turno: Int): Boolean {
        val activo = (temporizadorActivo &&
                usuarioActual == usuarioId &&
                ligaActual == ligaId &&
                rondaActual == ronda &&
                turnoActual == turno)
        Log.d("TEMPORIZADOR_GLOBAL", "¿Activo para $usuarioId R$ronda-T$turno? $activo")
        return activo
    }

    /**
     * Obtiene el tiempo restante actual
     */
    fun getTiempoRestante(): Int {
        return if (temporizadorActivo) tiempoRestanteSegundos else 0
    }

    /**
     * Limpia las referencias
     */
    private fun limpiar() {
        temporizador = null
        usuarioActual = ""
        ligaActual = ""
        rondaActual = -1
        turnoActual = -1
        onTickCallback = null
        onFinishCallback = null
    }

    /**
     * Verifica si hay un temporizador activo
     */
    fun estaActivo(): Boolean {
        return temporizadorActivo
    }
}