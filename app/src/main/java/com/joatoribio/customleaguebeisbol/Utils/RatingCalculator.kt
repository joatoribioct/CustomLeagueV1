package com.joatoribio.customleaguebeisbol.Utils

import android.util.Log
import com.joatoribio.customleaguebeisbol.Adaptadores.JugadorInfo

/**
 * Objeto singleton para calcular ratings de manera consistente en toda la aplicación
 */
object RatingCalculator {

    /**
     * Calcula el rating promedio de una lista de jugadores
     */
    fun calcularRatingPromedio(jugadores: List<JugadorInfo>): Int {
        if (jugadores.isEmpty()) return 0

        val ratingsValidos = jugadores
            .map { it.rating }
            .filter { it > 0 }

        if (ratingsValidos.isEmpty()) return 0

        val suma = ratingsValidos.sum()
        return suma / ratingsValidos.size
    }

    /**
     * Convierte un mapa de jugadores a una lista de JugadorInfo
     * Maneja las inconsistencias en los datos (rating como Number o String)
     */
    fun convertirJugadoresALista(jugadoresMap: Map<String, Map<String, Any>>): List<JugadorInfo> {
        val listaJugadores = mutableListOf<JugadorInfo>()

        for ((posicion, datosJugador) in jugadoresMap) {
            // Obtener nombre, manejando errores tipográficos comunes
            val nombre = when {
                datosJugador.containsKey("nombre") -> datosJugador["nombre"] as? String ?: "Sin nombre"
                datosJugador.containsKey("nome") -> datosJugador["nome"] as? String ?: "Sin nombre" // Error tipográfico común
                else -> "Sin nombre"
            }

            // Obtener rating de manera robusta
            val rating = obtenerRatingDeJugador(datosJugador)

            listaJugadores.add(JugadorInfo(posicion, nombre, rating))

            // Log para debugging
            Log.d("RatingCalculator", "Jugador procesado: $posicion - $nombre - Rating: $rating")
        }

        // Ordenar por posición para consistencia
        return listaJugadores.sortedBy { obtenerOrdenPosicion(it.posicion) }
    }

    /**
     * Extrae el rating de un jugador manejando diferentes tipos de datos
     */
    private fun obtenerRatingDeJugador(datosJugador: Map<String, Any>): Int {
        return when (val ratingValue = datosJugador["rating"]) {
            is Number -> ratingValue.toInt()
            is String -> ratingValue.toIntOrNull() ?: 0
            else -> {
                Log.w("RatingCalculator", "Rating no válido: $ratingValue")
                0
            }
        }
    }

    /**
     * Calcula el rating promedio directamente desde un mapa de jugadores
     */
    fun calcularRatingPromedioDesdeMap(jugadoresMap: Map<String, Map<String, Any>>): Int {
        val listaJugadores = convertirJugadoresALista(jugadoresMap)
        return calcularRatingPromedio(listaJugadores)
    }

    /**
     * Define el orden de las posiciones para ordenamiento consistente
     */
    private fun obtenerOrdenPosicion(posicion: String): Int {
        return when (posicion) {
            // Infield
            "C" -> 1
            "1B" -> 2
            "2B" -> 3
            "3B" -> 4
            "SS" -> 5
            "DH" -> 9

            // Outfield
            "LF" -> 6
            "CF" -> 7
            "RF" -> 8

            // Pitchers
            "SP1" -> 10
            "SP2" -> 11
            "SP3" -> 12
            "SP4" -> 13
            "SP5" -> 14

            // Relief
            "RP1" -> 15
            "RP2" -> 16
            "RP3" -> 17
            "RP4" -> 18
            "RP5" -> 19
            "RP6" -> 20
            "RP7" -> 21
            "RP8" -> 22

            else -> 999
        }
    }

    /**
     * Valida si un lineup tiene datos válidos para calcular rating
     */
    fun validarLineup(jugadoresMap: Map<String, Map<String, Any>>): Boolean {
        if (jugadoresMap.isEmpty()) return false

        val jugadoresConRating = jugadoresMap.values.count { jugador ->
            obtenerRatingDeJugador(jugador) > 0
        }

        return jugadoresConRating > 0
    }
}