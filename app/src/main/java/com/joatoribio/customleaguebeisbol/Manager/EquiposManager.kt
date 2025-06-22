package com.joatoribio.customleaguebeisbol.Manager

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object EquiposManager {

    private val database = FirebaseDatabase.getInstance()
    private val equiposGlobalesRef = database.getReference("EquiposGlobales")

    /**
     * Inicializa los equipos globales si no existen
     * Solo se ejecuta una vez por toda la aplicación
     */
    fun inicializarEquiposGlobales(onComplete: (Boolean) -> Unit) {
        Log.d("EQUIPOS_MANAGER", "Verificando si existen equipos globales...")

        equiposGlobalesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount >= 30) {
                    Log.d("EQUIPOS_MANAGER", "Equipos globales ya existen: ${snapshot.childrenCount}")
                    onComplete(true)
                } else {
                    Log.d("EQUIPOS_MANAGER", "Creando equipos globales...")
                    crearEquiposGlobales(onComplete)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EQUIPOS_MANAGER", "Error verificando equipos: ${error.message}")
                onComplete(false)
            }
        })
    }

    /**
     * Crea todos los 30 equipos globales una sola vez
     */
    private fun crearEquiposGlobales(onComplete: (Boolean) -> Unit) {
        val todosLosEquipos = obtenerTodosLosEquipos()
        var equiposCreados = 0
        val totalEquipos = todosLosEquipos.size

        todosLosEquipos.forEach { (equipoId, datosEquipo) ->
            equiposGlobalesRef.child(equipoId)
                .setValue(datosEquipo)
                .addOnSuccessListener {
                    equiposCreados++
                    Log.d("EQUIPOS_MANAGER", "Equipo $equipoId creado ($equiposCreados/$totalEquipos)")

                    if (equiposCreados == totalEquipos) {
                        Log.d("EQUIPOS_MANAGER", "Todos los equipos globales creados exitosamente")
                        onComplete(true)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("EQUIPOS_MANAGER", "Error creando equipo $equipoId: ${error.message}")
                    onComplete(false)
                }
        }
    }

    /**
     * Actualiza el rating de un jugador específico
     * Útil para mantenimiento futuro
     */
    fun actualizarRatingJugador(
        equipo: String,
        categoria: String,
        posicion: String,
        nuevoRating: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val path = "$equipo/jugadores/$categoria/$posicion/rating"

        equiposGlobalesRef.child(path)
            .setValue(nuevoRating)
            .addOnSuccessListener {
                Log.d("EQUIPOS_MANAGER", "Rating actualizado: $equipo/$categoria/$posicion = $nuevoRating")
                onComplete(true)
            }
            .addOnFailureListener { error ->
                Log.e("EQUIPOS_MANAGER", "Error actualizando rating: ${error.message}")
                onComplete(false)
            }
    }

    /**
     * Obtiene un equipo específico
     */
    fun obtenerEquipo(equipoId: String, callback: (Map<String, Any>?) -> Unit) {
        equiposGlobalesRef.child(equipoId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    @Suppress("UNCHECKED_CAST")
                    val equipo = snapshot.value as? Map<String, Any>
                    callback(equipo)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EQUIPOS_MANAGER", "Error obteniendo equipo $equipoId: ${error.message}")
                    callback(null)
                }
            })
    }

    /**
     * Define todos los 30 equipos con sus datos completos
     * Puedes expandir esto con todos los equipos
     */
    private fun obtenerTodosLosEquipos(): Map<String, Map<String, Any>> {
        return mapOf(
            "Diamondbacks" to mapOf(
                "nombre" to "Arizona Diamondbacks",
                "ciudad" to "Phoenix",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "G.MORENO", "rating" to 82),
                        "1B" to mapOf("nombre" to "C.WALKER", "rating" to 85),
                        "2B" to mapOf("nombre" to "K.MARTE", "rating" to 88),
                        "3B" to mapOf("nombre" to "E.SUAREZ", "rating" to 79),
                        "SS" to mapOf("nombre" to "G.PERDOMO", "rating" to 76)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "L.GURRIEL", "rating" to 80),
                        "CF" to mapOf("nombre" to "A.THOMAS", "rating" to 83),
                        "RF" to mapOf("nombre" to "C.CARROLL", "rating" to 87),
                        "DH" to mapOf("nombre" to "J.BELL", "rating" to 77)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "Z.GALLEN", "rating" to 89),
                        "SP2" to mapOf("nombre" to "M.KELLY", "rating" to 84),
                        "SP3" to mapOf("nombre" to "E.RODRIGUEZ", "rating" to 82),
                        "SP4" to mapOf("nombre" to "B.PFAADT", "rating" to 78),
                        "SP5" to mapOf("nombre" to "R.NELSON", "rating" to 75)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "K.GINKEL", "rating" to 80),
                        "RP2" to mapOf("nombre" to "R.THOMPSON", "rating" to 76),
                        "RP3" to mapOf("nombre" to "J.MANTIPLY", "rating" to 74),
                        "RP4" to mapOf("nombre" to "S.MCGOUGH", "rating" to 72),
                        "RP5" to mapOf("nombre" to "B.WALSTON", "rating" to 70),
                        "RP6" to mapOf("nombre" to "T.SCOTT", "rating" to 68),
                        "RP7" to mapOf("nombre" to "L.FRÍAS", "rating" to 66),
                        "RP8" to mapOf("nombre" to "P.SEWALD", "rating" to 83)
                    )
                )
            ),

            "Braves" to mapOf(
                "nombre" to "Atlanta Braves",
                "ciudad" to "Atlanta",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "S.MURPHY", "rating" to 86),
                        "1B" to mapOf("nombre" to "M.OLSON", "rating" to 89),
                        "2B" to mapOf("nombre" to "O.ALBIES", "rating" to 85),
                        "3B" to mapOf("nombre" to "A.RILEY", "rating" to 87),
                        "SS" to mapOf("nombre" to "O.ARCIA", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.SOLER", "rating" to 81),
                        "CF" to mapOf("nombre" to "M.HARRIS", "rating" to 84),
                        "RF" to mapOf("nombre" to "R.ACUÑA", "rating" to 95),
                        "DH" to mapOf("nombre" to "M.OZUNA", "rating" to 83)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.STRIDER", "rating" to 92),
                        "SP2" to mapOf("nombre" to "C.SALE", "rating" to 88),
                        "SP3" to mapOf("nombre" to "C.MORTON", "rating" to 85),
                        "SP4" to mapOf("nombre" to "R.LOPEZ", "rating" to 82),
                        "SP5" to mapOf("nombre" to "B.ELDER", "rating" to 76)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "J.JIMENEZ", "rating" to 82),
                        "RP2" to mapOf("nombre" to "P.MINTER", "rating" to 80),
                        "RP3" to mapOf("nombre" to "A.IGLESIAS", "rating" to 78),
                        "RP4" to mapOf("nombre" to "J.LOPEZ", "rating" to 76),
                        "RP5" to mapOf("nombre" to "D.LEE", "rating" to 74),
                        "RP6" to mapOf("nombre" to "G.TONNON", "rating" to 72),
                        "RP7" to mapOf("nombre" to "S.JOHNSON", "rating" to 70),
                        "RP8" to mapOf("nombre" to "R.IGLESIAS", "rating" to 84)
                    )
                )
            ),

            "Orioles" to mapOf(
                "nombre" to "Baltimore Orioles",
                "ciudad" to "Baltimore",
                "liga" to "AL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "A.RUTSCHMAN", "rating" to 85),
                        "1B" to mapOf("nombre" to "R.O'HEARN", "rating" to 78),
                        "2B" to mapOf("nombre" to "J.HOLLIDAY", "rating" to 73),
                        "3B" to mapOf("nombre" to "J.WESTBURG", "rating" to 80),
                        "SS" to mapOf("nombre" to "G.HENDERSON", "rating" to 88)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "T.O'NEILL", "rating" to 78),
                        "CF" to mapOf("nombre" to "C.MULLINS", "rating" to 82),
                        "RF" to mapOf("nombre" to "H.KJERSTAD", "rating" to 72),
                        "DH" to mapOf("nombre" to "G.SANCHEZ", "rating" to 77)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "Z.EFLIN", "rating" to 80),
                        "SP2" to mapOf("nombre" to "K.GIBSON", "rating" to 72),
                        "SP3" to mapOf("nombre" to "D.KREMER", "rating" to 71),
                        "SP4" to mapOf("nombre" to "T.SUGANO", "rating" to 68),
                        "SP5" to mapOf("nombre" to "C.POVICH", "rating" to 67)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "Y.CANO", "rating" to 74),
                        "RP2" to mapOf("nombre" to "K.AKIN", "rating" to 74),
                        "RP3" to mapOf("nombre" to "A.KITTREDGE", "rating" to 72),
                        "RP4" to mapOf("nombre" to "S.DOMINGUEZ", "rating" to 72),
                        "RP5" to mapOf("nombre" to "B.BAKER", "rating" to 71),
                        "RP6" to mapOf("nombre" to "G.SOTO", "rating" to 70),
                        "RP7" to mapOf("nombre" to "M.BOWMAN", "rating" to 66),
                        "RP8" to mapOf("nombre" to "F.BAUTISTA", "rating" to 80)
                    )
                )
            ),

            "RedSox" to mapOf(
                "nombre" to "Boston Red Sox",
                "ciudad" to "Boston",
                "liga" to "AL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "D.JANSEN", "rating" to 74),
                        "1B" to mapOf("nombre" to "T.CASAS", "rating" to 79),
                        "2B" to mapOf("nombre" to "G.DEVERS", "rating" to 85),
                        "3B" to mapOf("nombre" to "R.DEVERS", "rating" to 87),
                        "SS" to mapOf("nombre" to "T.STORY", "rating" to 76)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "M.YOSHIDA", "rating" to 81),
                        "CF" to mapOf("nombre" to "J.DURAN", "rating" to 83),
                        "RF" to mapOf("nombre" to "W.ABREU", "rating" to 77),
                        "DH" to mapOf("nombre" to "R.O'NEILL", "rating" to 79)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "B.BELLO", "rating" to 84),
                        "SP2" to mapOf("nombre" to "T.HOUCK", "rating" to 82),
                        "SP3" to mapOf("nombre" to "K.CRAWFORD", "rating" to 78),
                        "SP4" to mapOf("nombre" to "C.PIVETTA", "rating" to 76),
                        "SP5" to mapOf("nombre" to "G.WHITLOCK", "rating" to 80)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "K.JANSEN", "rating" to 82),
                        "RP2" to mapOf("nombre" to "J.MARTIN", "rating" to 78),
                        "RP3" to mapOf("nombre" to "C.BERNARDINO", "rating" to 74),
                        "RP4" to mapOf("nombre" to "Z.KELLY", "rating" to 72),
                        "RP5" to mapOf("nombre" to "B.WINCKOWSKI", "rating" to 70),
                        "RP6" to mapOf("nombre" to "R.BRASIER", "rating" to 68),
                        "RP7" to mapOf("nombre" to "I.HAMILTON", "rating" to 66),
                        "RP8" to mapOf("nombre" to "L.HENDRIKS", "rating" to 85)
                    )
                )
            )

            // TODO: Agregar los otros 26 equipos...
            // "WhiteSox" to mapOf(...),
            // "Cubs" to mapOf(...),
            // etc.
        )
    }
}