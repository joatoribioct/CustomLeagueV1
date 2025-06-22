package com.joatoribio.customleaguebeisbol.Modelo

// Modelo principal de Liga
data class ModeloLiga(
    val id: String = "",
    val adminUid: String = "",
    val adminNombre: String = "",
    val nombreLiga: String = "",
    val numeroParticipantes: String = "",
    val estado: String = "",
    val tiempo: String = "",
    val imgLiga: String = "",
    val usuariosPermitidos: List<String> = emptyList(),
    val usuariosParticipantes: List<UsuarioParticipante> = emptyList(),
    val mapaUsuariosInfo: Map<String, Map<String, Any>> = emptyMap(), // NUEVO: Mapa de información de usuarios
    val fechaCreacion: Long = 0,
    val configuracion: ConfiguracionLiga = ConfiguracionLiga()
)

// Configuración general de la liga
data class ConfiguracionLiga(
    val configuracionDraft: ConfiguracionDraft = ConfiguracionDraft(),
    val configuracionJuego: ConfiguracionJuego = ConfiguracionJuego()
)

// Configuración específica del draft
data class ConfiguracionDraft(
    val ordenTurnos: List<String> = emptyList(),
    val draftIniciado: Boolean = false,
    val draftCompletado: Boolean = false,
    val rondaActual: Int = 1,
    val turnoActual: Int = 0,
    val tiempoLimiteSeleccion: Int = 120, // en segundos
    val configuradoPorAdmin: Boolean = false,
    val fechaConfiguracion: Long = 0,
    val fechaInicio: Long = 0,
    val fechaFinalizacion: Long = 0,
    val fechaDetencion: Long = 0
) {
    // Constructor vacío para Firebase
    constructor() : this(
        emptyList(), false, false, 1, 0, 120, false, 0, 0, 0, 0
    )
}

// Configuración de juego (para futuras funcionalidades)
data class ConfiguracionJuego(
    val temporadaActiva: Boolean = false,
    val juegosPorTemporada: Int = 162,
    val playoffsHabilitados: Boolean = true,
    val equiposPorDivision: Int = 5
)

// Modelo de usuario participante
data class UsuarioParticipante(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val idGaming: String = "",
    val urlImagenPerfil: String = "",
    val esAdmin: Boolean = false,
    val fechaUnion: Long = 0,
    val activo: Boolean = true,
    val rol: String = "Participante"
) {
    /**
     * Obtiene el identificador a mostrar, priorizando ID Gaming
     */
    fun getIdentificadorMostrar(): String {
        return when {
            idGaming.isNotEmpty() && idGaming != "null" -> idGaming
            nombre.isNotEmpty() && nombre != "null" -> nombre
            email.isNotEmpty() && email != "null" -> email.substringBefore("@")
            else -> "Usuario${uid.take(6)}"
        }
    }
}

// Estados posibles de la liga
object EstadosLiga {
    const val CONFIGURANDO = "Configurando"
    const val DISPONIBLE = "Disponible"
    const val EN_PROGRESO = "EnProgreso"
    const val FINALIZADA = "Finalizada"
    const val SUSPENDIDA = "Suspendida"
}

// Estados del draft
object EstadosDraft {
    const val NO_CONFIGURADO = "NoConfigurado"
    const val CONFIGURADO = "Configurado"
    const val INICIADO = "Iniciado"
    const val EN_PROGRESO = "EnProgreso"
    const val COMPLETADO = "Completado"
    const val DETENIDO = "Detenido"
}

// Extensiones para facilitar el manejo de estados
fun ModeloLiga.puedeIniciarDraft(): Boolean {
    return estado == EstadosLiga.DISPONIBLE &&
            configuracion.configuracionDraft.configuradoPorAdmin &&
            !configuracion.configuracionDraft.draftIniciado
}

fun ModeloLiga.esDraftActivo(): Boolean {
    return configuracion.configuracionDraft.draftIniciado &&
            !configuracion.configuracionDraft.draftCompletado
}

fun ModeloLiga.puedeModificarOrden(): Boolean {
    return !configuracion.configuracionDraft.draftIniciado
}

fun ModeloLiga.getEstadoDraft(): String {
    return when {
        configuracion.configuracionDraft.draftCompletado -> EstadosDraft.COMPLETADO
        configuracion.configuracionDraft.draftIniciado -> EstadosDraft.INICIADO
        configuracion.configuracionDraft.configuradoPorAdmin -> EstadosDraft.CONFIGURADO
        else -> EstadosDraft.NO_CONFIGURADO
    }
}