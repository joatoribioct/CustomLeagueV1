package com.joatoribio.customleaguebeisbol.Utils

import android.graphics.Color

object RatingUtils {

    /**
     * Categorías de rating según el sistema de béisbol
     */
    enum class RatingCategory(val emoji: String, val nombre: String, val colorHex: String) {
        DIAMANTE("💎", "Diamante", "#00BCD4"),
        ORO("🥇", "Oro", "#FFD700"),
        PLATA("🥈", "Plata", "#C0C0C0"),
        BRONCE("🥉", "Bronce", "#CD7F32"),
        GRIS("⚫", "Gris", "#808080")
    }

    /**
     * Obtiene la categoría según el rating
     */
    fun getCategoriaRating(rating: Int): RatingCategory {
        return when {
            rating >= 85 -> RatingCategory.DIAMANTE
            rating >= 80 -> RatingCategory.ORO
            rating >= 75 -> RatingCategory.PLATA
            rating >= 70 -> RatingCategory.BRONCE
            else -> RatingCategory.GRIS
        }
    }

    /**
     * Obtiene el color del rating
     */
    fun getRatingColor(rating: Int): Int {
        val categoria = getCategoriaRating(rating)
        return Color.parseColor(categoria.colorHex)
    }

    /**
     * Obtiene el texto con emoji y rating
     */
    fun getRatingConEmoji(rating: Int): String {
        val categoria = getCategoriaRating(rating)
        return "${categoria.emoji} $rating"
    }

    /**
     * Obtiene el nombre de la categoría
     */
    fun getNombreCategoria(rating: Int): String {
        return getCategoriaRating(rating).nombre
    }

    /**
     * Obtiene estadísticas de un grupo de ratings
     */
    fun obtenerEstadisticasRatings(ratings: List<Int>): String {
        if (ratings.isEmpty()) return "Sin datos"

        val promedio = ratings.average().toInt()
        val categoriaPromedio = getCategoriaRating(promedio)
        val maximo = ratings.maxOrNull() ?: 0
        val minimo = ratings.minOrNull() ?: 0

        return "${categoriaPromedio.emoji} $promedio (${minimo}-${maximo})"
    }

    /**
     * Obtiene distribución de categorías
     */
    fun obtenerDistribucionCategorias(ratings: List<Int>): Map<RatingCategory, Int> {
        return ratings.groupBy { getCategoriaRating(it) }
            .mapValues { it.value.size }
    }
}