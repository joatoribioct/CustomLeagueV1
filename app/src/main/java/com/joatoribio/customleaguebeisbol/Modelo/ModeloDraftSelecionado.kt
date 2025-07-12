package com.joatoribio.customleaguebeisbol.Modelo

data class ModeloDraftSelecionado(
    val ligaId: String = "",
    val usuarioId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val tipoLineup: String = "",
    val jugadores: Map<String, Map<String, Any>> = emptyMap(),
    val ratingPromedio: Int = 0,
    val timestamp: Long = 0,
    val ronda: Int = 0,
    val turno: Int = 0,

    // ✅ NUEVOS: Campos para selección automática
    val seleccionAutomatica: Boolean = false,
    val motivoAutomatico: String = "",

    // Campos de compatibilidad hacia atrás
    val tipo: String = tipoLineup,
    val nombreUsuario: String = "",
    val idGamingUsuario: String = "",
    val fechaSeleccion: Long = timestamp
) {
    constructor() : this(
        ligaId = "",
        usuarioId = "",
        equipoId = "",
        equipoNombre = "",
        tipoLineup = "",
        jugadores = emptyMap(),
        ratingPromedio = 0,
        timestamp = 0,
        ronda = 0,
        turno = 0,
        seleccionAutomatica = false,
        motivoAutomatico = ""
    )

    /**
     * Método para verificar si la selección fue automática
     */
    fun esSeleccionAutomatica(): Boolean = seleccionAutomatica

    /**
     * Método para obtener descripción de la selección
     */
    fun obtenerDescripcionSeleccion(): String {
        return if (seleccionAutomatica) {
            "🤖 Selección automática"
        } else {
            "👤 Selección manual"
        }
    }

    /**
     * Método para obtener el motivo de la selección automática
     */
    fun obtenerMotivoAutomatico(): String {
        return when (motivoAutomatico) {
            "tiempo_agotado" -> "Tiempo agotado (3 minutos)"
            "usuario_inactivo" -> "Usuario inactivo"
            "error_conexion" -> "Error de conexión"
            else -> "Selección automática"
        }
    }
}