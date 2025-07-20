package com.joatoribio.customleaguebeisbol.Draft

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Controlador para manejar el sistema de draft por turnos
 */
class ControladorDraft(
    private val ligaId: String,
    private val onEstadoCambiado: (EstadoDraft) -> Unit
) {

    private val database = FirebaseDatabase.getInstance()
    private val ligaRef = database.getReference("Ligas").child(ligaId)
    private val draftRef = ligaRef.child("configuracion/configuracionDraft")

    private var estadoActual: EstadoDraft = EstadoDraft()
    private var participantes: List<String> = emptyList()
    private var mapaUsuariosInfo: Map<String, Map<String, Any>> = emptyMap()

    /**
     * Escucha cambios en el estado del draft
     */
    fun iniciarEscuchaDraft() {
        Log.d("DRAFT_CONTROLLER", "Iniciando escucha del draft para liga: $ligaId")

        // Escuchar cambios en el draft
        draftRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val draftIniciado = snapshot.child("draftIniciado").value as? Boolean ?: false
                    val draftCompletado = snapshot.child("draftCompletado").value as? Boolean ?: false
                    val rondaActual = (snapshot.child("rondaActual").value as? Long)?.toInt() ?: 1
                    val turnoActual = (snapshot.child("turnoActual").value as? Long)?.toInt() ?: 0
                    val ordenTurnos = snapshot.child("ordenTurnos").children.mapNotNull { it.value as? String }

                    val nuevoEstado = EstadoDraft(
                        draftIniciado = draftIniciado,
                        draftCompletado = draftCompletado,
                        rondaActual = rondaActual,
                        turnoActual = turnoActual,
                        ordenTurnos = ordenTurnos,
                        usuarioActivo = if (ordenTurnos.isNotEmpty() && turnoActual < ordenTurnos.size) {
                            ordenTurnos[turnoActual]
                        } else null
                    )

                    estadoActual = nuevoEstado
                    participantes = ordenTurnos

                    Log.d("DRAFT_CONTROLLER", "Estado actualizado: $nuevoEstado")
                    onEstadoCambiado(nuevoEstado)

                } catch (e: Exception) {
                    Log.e("DRAFT_CONTROLLER", "Error al procesar cambios del draft: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DRAFT_CONTROLLER", "Error al escuchar draft: ${error.message}")
            }
        })

        // Escuchar el mapa de usuarios para obtener ID Gaming
        ligaRef.child("mapaUsuariosInfo").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val mapaTemp = mutableMapOf<String, Map<String, Any>>()
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.key ?: continue
                        @Suppress("UNCHECKED_CAST")
                        val userData = userSnapshot.value as? Map<String, Any> ?: continue
                        mapaTemp[uid] = userData
                    }
                    mapaUsuariosInfo = mapaTemp
                    Log.d("DRAFT_CONTROLLER", "Mapa de usuarios actualizado: ${mapaUsuariosInfo.size} usuarios")
                } catch (e: Exception) {
                    Log.e("DRAFT_CONTROLLER", "Error al cargar mapa de usuarios: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DRAFT_CONTROLLER", "Error al escuchar mapa de usuarios: ${error.message}")
            }
        })

        // NUEVO: También escuchar directamente la información de usuarios desde la liga
        ligaRef.child("usuariosParticipantes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val mapaTemp = mutableMapOf<String, Map<String, Any>>()
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.child("uid").value as? String ?: continue
                        val idGaming = userSnapshot.child("idGaming").value as? String ?: ""
                        val nombre = userSnapshot.child("nombre").value as? String ?: ""
                        val email = userSnapshot.child("email").value as? String ?: ""

                        mapaTemp[uid] = mapOf(
                            "idGaming" to idGaming,
                            "nombre" to nombre,
                            "email" to email
                        )
                    }
                    mapaUsuariosInfo = mapaTemp
                    Log.d("DRAFT_CONTROLLER", "Información de usuarios actualizada desde liga: ${mapaUsuariosInfo.size} usuarios")

                    // Imprimir información de cada usuario para debug
                    mapaUsuariosInfo.forEach { (uid, info) ->
                        Log.d("DRAFT_CONTROLLER", "Usuario $uid: idGaming=${info["idGaming"]}, nombre=${info["nombre"]}")
                    }
                } catch (e: Exception) {
                    Log.e("DRAFT_CONTROLLER", "Error al cargar usuarios de la liga: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DRAFT_CONTROLLER", "Error al escuchar usuarios de la liga: ${error.message}")
            }
        })
    }

    /**
     * Avanza al siguiente turno después de una selección
     */
    fun avanzarTurno() {
        Log.d("DRAFT_CONTROLLER", "Avanzando turno desde: ronda=${estadoActual.rondaActual}, turno=${estadoActual.turnoActual}")

        val siguienteTurno = estadoActual.turnoActual + 1
        val totalParticipantes = participantes.size

        if (siguienteTurno >= totalParticipantes) {
            // Avanzar a la siguiente ronda
            val siguienteRonda = estadoActual.rondaActual + 1

            if (siguienteRonda > 4) {
                // Draft completado
                completarDraft()
            } else {
                // Nueva ronda, empezar desde el primer participante
                actualizarEstadoDraft(siguienteRonda, 0)
            }
        } else {
            // Siguiente participante en la misma ronda
            actualizarEstadoDraft(estadoActual.rondaActual, siguienteTurno)
        }
    }

    /**
     * Actualiza el estado del draft en Firebase
     * IMPORTANTE: Ahora también actualiza usuarioEnTurno para activar el trigger
     */
    private fun actualizarEstadoDraft(ronda: Int, turno: Int) {
        Log.d("DRAFT_CONTROLLER", "Actualizando estado: ronda=$ronda, turno=$turno")

        // Los participantes son una lista de String (UIDs)
        val usuarioEnTurno = if (turno < participantes.size) {
            participantes[turno]  // Ya es un String (UID)
        } else {
            Log.e("DRAFT_CONTROLLER", "Error: turno $turno fuera de rango (${participantes.size} participantes)")
            return
        }

        Log.d("DRAFT_CONTROLLER", "📝 Nuevo usuario en turno: $usuarioEnTurno")

        // IMPORTANTE: Actualizar TODO incluyendo usuarioEnTurno
        val updates = mapOf(
            "rondaActual" to ronda,
            "turnoActual" to turno,
            "usuarioEnTurno" to usuarioEnTurno,  // ESTO ES LO QUE FALTABA
            "ultimaActualizacion" to System.currentTimeMillis()
        )

        draftRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("DRAFT_CONTROLLER", "✅ Estado actualizado exitosamente")
                Log.d("DRAFT_CONTROLLER", "   - Ronda: $ronda")
                Log.d("DRAFT_CONTROLLER", "   - Turno: $turno")
                Log.d("DRAFT_CONTROLLER", "   - Usuario en turno: $usuarioEnTurno")
            }
            .addOnFailureListener { error ->
                Log.e("DRAFT_CONTROLLER", "❌ Error al actualizar estado: ${error.message}")
            }
    }

    /**
     * También corregir la función de completar draft
     */
    private fun completarDraft() {
        Log.d("DRAFT_CONTROLLER", "Completando draft")

        val updates = mapOf(
            "draftCompletado" to true,
            "fechaFinalizacion" to System.currentTimeMillis(),
            "usuarioEnTurno" to ""  // Limpiar usuario en turno al completar
        )

        draftRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("DRAFT_CONTROLLER", "✅ Draft completado exitosamente")
            }
            .addOnFailureListener { error ->
                Log.e("DRAFT_CONTROLLER", "❌ Error completando draft: ${error.message}")
            }
    }

    /**
     * Verifica si un usuario puede seleccionar en este momento
     */
    fun puedeSeleccionar(userId: String): Boolean {
        return estadoActual.draftIniciado &&
                !estadoActual.draftCompletado &&
                estadoActual.usuarioActivo == userId
    }

    /**
     * CORREGIDO: Obtiene el ID Gaming de un usuario con múltiples fallbacks
     */
    fun obtenerIdGaming(uid: String): String {
        Log.d("DRAFT_CONTROLLER", "Obteniendo ID Gaming para UID: $uid")

        val userInfo = mapaUsuariosInfo[uid]
        Log.d("DRAFT_CONTROLLER", "Información del usuario: $userInfo")

        return when {
            userInfo?.get("idGaming") != null &&
                    userInfo.get("idGaming").toString().isNotEmpty() &&
                    userInfo.get("idGaming").toString() != "null" -> {
                val idGaming = userInfo.get("idGaming").toString()
                Log.d("DRAFT_CONTROLLER", "ID Gaming encontrado: $idGaming")
                idGaming
            }
            userInfo?.get("nombre") != null &&
                    userInfo.get("nombre").toString().isNotEmpty() &&
                    userInfo.get("nombre").toString() != "null" -> {
                val nombre = userInfo.get("nombre").toString()
                Log.d("DRAFT_CONTROLLER", "Usando nombre como fallback: $nombre")
                nombre
            }
            else -> {
                val fallback = "Usuario ${uid.take(8)}"
                Log.d("DRAFT_CONTROLLER", "Usando fallback final: $fallback")
                fallback
            }
        }
    }

    /**
     * CORREGIDO: Obtiene información completa del usuario actual (solo ID Gaming)
     */
    fun obtenerInfoUsuarioActual(): String {
        val uid = estadoActual.usuarioActivo
        Log.d("DRAFT_CONTROLLER", "Obteniendo info del usuario actual: $uid")

        return if (uid != null) {
            obtenerIdGaming(uid)
        } else {
            Log.d("DRAFT_CONTROLLER", "No hay usuario activo")
            "Esperando..."
        }
    }

    /**
     * Obtiene información detallada de la posición en cola para un usuario
     */
    fun obtenerPosicionEnCola(usuarioId: String): Int {
        val ordenTurnos = estadoActual.ordenTurnos
        val turnoActual = estadoActual.turnoActual

        if (ordenTurnos.isEmpty() || usuarioId.isEmpty()) {
            return -1
        }

        val miPosicion = ordenTurnos.indexOf(usuarioId)
        if (miPosicion == -1) {
            return -1
        }

        return if (turnoActual < miPosicion) {
            // Está en la misma ronda
            miPosicion - turnoActual
        } else {
            // Está en la siguiente ronda
            (ordenTurnos.size - turnoActual) + miPosicion
        }
    }

    /**
     * CORREGIDO: Obtiene información del turno con posición en cola mostrando ID Gaming
     */
    fun obtenerInfoTurnoConPosicion(usuarioConsultante: String): String {
        val usuarioActualIdGaming = obtenerInfoUsuarioActual()

        return if (usuarioConsultante == estadoActual.ordenTurnos.getOrNull(estadoActual.turnoActual)) {
            // Es el turno del usuario consultante
            "¡Es tu turno!"
        } else {
            // No es su turno, mostrar posición en cola
            val posicion = obtenerPosicionEnCola(usuarioConsultante)
            when {
                posicion == 1 -> "Falta 1 para tu turno"
                posicion > 1 -> "Faltan $posicion para tu turno"
                else -> "Turno de $usuarioActualIdGaming"
            }
        }
    }

    /**
     * CORREGIDO: Obtiene información del turno actual mostrando ID Gaming
     */
    fun obtenerInfoTurno(): String {
        if (!estadoActual.draftIniciado) return "Draft no iniciado"
        if (estadoActual.draftCompletado) return "Draft completado"

        val uidParticipante = estadoActual.usuarioActivo ?: "Desconocido"
        val idGamingParticipante = obtenerIdGaming(uidParticipante)

        return "Ronda ${estadoActual.rondaActual}/4 - Turno de: $idGamingParticipante"
    }

    /**
     * Obtiene información del turno con callback para ID Gaming
     */
    fun obtenerInfoTurnoConIdGaming(callback: (String) -> Unit) {
        val info = obtenerInfoTurno()
        callback(info)
    }

    /**
     * Obtiene el progreso del draft
     */
    fun obtenerProgreso(): Pair<Int, Int> {
        val totalTurnos = participantes.size * 4 // 4 rondas
        val turnosCompletados = (estadoActual.rondaActual - 1) * participantes.size + estadoActual.turnoActual
        return Pair(turnosCompletados, totalTurnos)
    }
}

/**
 * Estado actual del draft
 */
data class EstadoDraft(
    val draftIniciado: Boolean = false,
    val draftCompletado: Boolean = false,
    val rondaActual: Int = 1,
    val turnoActual: Int = 0,
    val ordenTurnos: List<String> = emptyList(),
    val usuarioActivo: String? = null
)