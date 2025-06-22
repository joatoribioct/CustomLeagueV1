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
     * Define todos los 30 equipos con sus datos completos basados en los PDFs
     */
    private fun obtenerTodosLosEquipos(): Map<String, Map<String, Any>> {
        return mapOf(
            // AMERICAN LEAGUE EAST
            "BlueJays" to mapOf(
                "nombre" to "Toronto Blue Jays",
                "ciudad" to "Toronto",
                "liga" to "AL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "A.KIRK", "rating" to 77),
                        "1B" to mapOf("nombre" to "V.GUERRERO", "rating" to 86),
                        "2B" to mapOf("nombre" to "A.GIMENEZ", "rating" to 82),
                        "3B" to mapOf("nombre" to "E.CLEMENT", "rating" to 79),
                        "SS" to mapOf("nombre" to "B.BICHETTE", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "A.SANTANDER", "rating" to 79),
                        "CF" to mapOf("nombre" to "D.VARSHO", "rating" to 80),
                        "RF" to mapOf("nombre" to "G.SPRINGER", "rating" to 79),
                        "DH" to mapOf("nombre" to "W.WAGNER", "rating" to 69)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "J.BERRIOS", "rating" to 78),
                        "SP2" to mapOf("nombre" to "B.FRANCIS", "rating" to 74),
                        "SP3" to mapOf("nombre" to "K.GAUSMAN", "rating" to 78),
                        "SP4" to mapOf("nombre" to "S.TURNBULL", "rating" to 72),
                        "SP5" to mapOf("nombre" to "C.BASSITT", "rating" to 78)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "C.GREEN", "rating" to 74),
                        "RP2" to mapOf("nombre" to "R.BURR", "rating" to 72),
                        "RP3" to mapOf("nombre" to "E.LAUER", "rating" to 62),
                        "RP4" to mapOf("nombre" to "B.LITTLE", "rating" to 66),
                        "RP5" to mapOf("nombre" to "Y.RODRIGUEZ", "rating" to 65),
                        "RP6" to mapOf("nombre" to "J.HOFFMAN", "rating" to 84),
                        "RP7" to mapOf("nombre" to "N.SADLIN", "rating" to 75),
                        "RP8" to mapOf("nombre" to "Y.GARCIA", "rating" to 84)
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
                        "SS" to mapOf("nombre" to "G.HENDERSON", "rating" to 86)
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

            "Rays" to mapOf(
                "nombre" to "Tampa Bay Rays",
                "ciudad" to "St. Petersburg",
                "liga" to "AL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "D.JANSEN", "rating" to 74),
                        "1B" to mapOf("nombre" to "Y.DIAZ", "rating" to 79),
                        "2B" to mapOf("nombre" to "B.LOWE", "rating" to 79),
                        "3B" to mapOf("nombre" to "J.CABALLERO", "rating" to 73),
                        "SS" to mapOf("nombre" to "J.CAMERINO", "rating" to 71)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "C.MOREL", "rating" to 68),
                        "CF" to mapOf("nombre" to "R.PALACIOS", "rating" to 74),
                        "RF" to mapOf("nombre" to "J.LOWE", "rating" to 72),
                        "DH" to mapOf("nombre" to "J.ARANDA", "rating" to 70)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.MCCLANAHAN", "rating" to 80),
                        "SP2" to mapOf("nombre" to "R.PEPIOT", "rating" to 79),
                        "SP3" to mapOf("nombre" to "T.BRADLEY", "rating" to 78),
                        "SP4" to mapOf("nombre" to "S.BAZ", "rating" to 79),
                        "SP5" to mapOf("nombre" to "D.RASMUSSEN", "rating" to 82)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "J.ENGLERT", "rating" to 63),
                        "RP2" to mapOf("nombre" to "P.FAIRBANKS", "rating" to 71),
                        "RP3" to mapOf("nombre" to "M.RODRIGUEZ", "rating" to 72),
                        "RP4" to mapOf("nombre" to "M.MONTGOMERY", "rating" to 65),
                        "RP5" to mapOf("nombre" to "G.CLEAVINGER", "rating" to 71),
                        "RP6" to mapOf("nombre" to "H.BIGGE", "rating" to 63),
                        "RP7" to mapOf("nombre" to "K.KELLY", "rating" to 74),
                        "RP8" to mapOf("nombre" to "E.UCETA", "rating" to 75)
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
                        "C" to mapOf("nombre" to "C.WONG", "rating" to 75),
                        "1B" to mapOf("nombre" to "T.CASAS", "rating" to 74),
                        "2B" to mapOf("nombre" to "A.BREGMAN", "rating" to 81),
                        "3B" to mapOf("nombre" to "R.DEVERS", "rating" to 78),
                        "SS" to mapOf("nombre" to "T.STORY", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.DURAN", "rating" to 84),
                        "CF" to mapOf("nombre" to "C.RAFAELA", "rating" to 78),
                        "RF" to mapOf("nombre" to "W.ABREU", "rating" to 81),
                        "DH" to mapOf("nombre" to "M.YOSHIDA", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "G.CROCHET", "rating" to 83),
                        "SP2" to mapOf("nombre" to "T.HOUCK", "rating" to 73),
                        "SP3" to mapOf("nombre" to "K.CRAWFORD", "rating" to 78),
                        "SP4" to mapOf("nombre" to "B.BELLO", "rating" to 74),
                        "SP5" to mapOf("nombre" to "W.BUEHLER", "rating" to 74)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "G.WHITLOCK", "rating" to 68),
                        "RP2" to mapOf("nombre" to "B.BERNARDINO", "rating" to 69),
                        "RP3" to mapOf("nombre" to "Z.KELLY", "rating" to 67),
                        "RP4" to mapOf("nombre" to "G.WEISSERT", "rating" to 68),
                        "RP5" to mapOf("nombre" to "L.GUERRERO", "rating" to 66),
                        "RP6" to mapOf("nombre" to "A.CHAPMAN", "rating" to 78),
                        "RP7" to mapOf("nombre" to "L.HENDRIKS", "rating" to 72),
                        "RP8" to mapOf("nombre" to "J.SLATEN", "rating" to 72)
                    )
                )
            ),

            "Yankees" to mapOf(
                "nombre" to "New York Yankees",
                "ciudad" to "New York",
                "liga" to "AL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "A.WELLS", "rating" to 78),
                        "1B" to mapOf("nombre" to "P.GOLDSCHMIDT", "rating" to 76),
                        "2B" to mapOf("nombre" to "J.CHISHOLM", "rating" to 82),
                        "3B" to mapOf("nombre" to "O.CABRERA", "rating" to 72),
                        "SS" to mapOf("nombre" to "A.VOLPE", "rating" to 77)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.DOMINGUEZ", "rating" to 71),
                        "CF" to mapOf("nombre" to "C.BELLINGER", "rating" to 84),
                        "RF" to mapOf("nombre" to "A.JUDGE", "rating" to 90),
                        "DH" to mapOf("nombre" to "G.STANTON", "rating" to 75)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "G.COLE", "rating" to 85),
                        "SP2" to mapOf("nombre" to "M.FRIED", "rating" to 80),
                        "SP3" to mapOf("nombre" to "C.SCHMIDT", "rating" to 79),
                        "SP4" to mapOf("nombre" to "R.YARBROUGH", "rating" to 66),
                        "SP5" to mapOf("nombre" to "C.RONDON", "rating" to 81)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "I.HAMILTON", "rating" to 72),
                        "RP2" to mapOf("nombre" to "T.MATZEK", "rating" to 67),
                        "RP3" to mapOf("nombre" to "M.LEITER", "rating" to 74),
                        "RP4" to mapOf("nombre" to "J.LOAISIGA", "rating" to 72),
                        "RP5" to mapOf("nombre" to "S.EFFROSS", "rating" to 68),
                        "RP6" to mapOf("nombre" to "L.WEAVER", "rating" to 81),
                        "RP7" to mapOf("nombre" to "F.CRUZ", "rating" to 75),
                        "RP8" to mapOf("nombre" to "D.WILLIAMS", "rating" to 77)
                    )
                )
            ),

            // AMERICAN LEAGUE CENTRAL
            "Guardians" to mapOf(
                "nombre" to "Cleveland Guardians",
                "ciudad" to "Cleveland",
                "liga" to "AL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "B.NAYLOR", "rating" to 72),
                        "1B" to mapOf("nombre" to "C.SANTANA", "rating" to 79),
                        "2B" to mapOf("nombre" to "D.SCHEEMANN", "rating" to 66),
                        "3B" to mapOf("nombre" to "J.RAMIREZ", "rating" to 89),
                        "SS" to mapOf("nombre" to "G.ARIAS", "rating" to 70)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "S.KWAN", "rating" to 86),
                        "CF" to mapOf("nombre" to "L.THOMAS", "rating" to 78),
                        "RF" to mapOf("nombre" to "J.NOEL", "rating" to 72),
                        "DH" to mapOf("nombre" to "K.MANZARDO", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "T.BIBEE", "rating" to 77),
                        "SP2" to mapOf("nombre" to "L.ORTIZ", "rating" to 69),
                        "SP3" to mapOf("nombre" to "G.WILLIAMS", "rating" to 73),
                        "SP4" to mapOf("nombre" to "B.LIVELY", "rating" to 72),
                        "SP5" to mapOf("nombre" to "S.CECCONI", "rating" to 69)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "J.JUNIS", "rating" to 68),
                        "RP2" to mapOf("nombre" to "T.HERRIN", "rating" to 73),
                        "RP3" to mapOf("nombre" to "P.SEWALD", "rating" to 72),
                        "RP4" to mapOf("nombre" to "M.FESTA", "rating" to 64),
                        "RP5" to mapOf("nombre" to "J.CANTILLO", "rating" to 68),
                        "RP6" to mapOf("nombre" to "C.SMITH", "rating" to 81),
                        "RP7" to mapOf("nombre" to "H.GADDIS", "rating" to 80),
                        "RP8" to mapOf("nombre" to "E.CLASE", "rating" to 82)
                    )
                )
            ),

            "Royals" to mapOf(
                "nombre" to "Kansas City Royals",
                "ciudad" to "Kansas City",
                "liga" to "AL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "S.PEREZ", "rating" to 78),
                        "1B" to mapOf("nombre" to "V.PASQUANTINO", "rating" to 79),
                        "2B" to mapOf("nombre" to "J.INDIA", "rating" to 76),
                        "3B" to mapOf("nombre" to "M.GARCIA", "rating" to 77),
                        "SS" to mapOf("nombre" to "B.WITT", "rating" to 90)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "D.BLANCO", "rating" to 73),
                        "CF" to mapOf("nombre" to "H.RENFROE", "rating" to 72),
                        "RF" to mapOf("nombre" to "M.CANHA", "rating" to 73),
                        "DH" to mapOf("nombre" to "F.FERMIN", "rating" to 79)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "C.RAGANS", "rating" to 82),
                        "SP2" to mapOf("nombre" to "S.LUGO", "rating" to 79),
                        "SP3" to mapOf("nombre" to "M.WACHA", "rating" to 78),
                        "SP4" to mapOf("nombre" to "M.LORENZEN", "rating" to 73),
                        "SP5" to mapOf("nombre" to "K.BUBIC", "rating" to 78)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "C.ESTEVES", "rating" to 72),
                        "RP2" to mapOf("nombre" to "A.ZERPA", "rating" to 68),
                        "RP3" to mapOf("nombre" to "T.CLARKE", "rating" to 66),
                        "RP4" to mapOf("nombre" to "S.LONG", "rating" to 62),
                        "RP5" to mapOf("nombre" to "J.SCHREIBER", "rating" to 66),
                        "RP6" to mapOf("nombre" to "D.LYNCH IV", "rating" to 65),
                        "RP7" to mapOf("nombre" to "H.HARVEY", "rating" to 73),
                        "RP8" to mapOf("nombre" to "L.ERCEG", "rating" to 80)
                    )
                )
            ),

            "Tigers" to mapOf(
                "nombre" to "Detroit Tigers",
                "ciudad" to "Detroit",
                "liga" to "AL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "J.ROGERS", "rating" to 73),
                        "1B" to mapOf("nombre" to "S.TORKELSON", "rating" to 76),
                        "2B" to mapOf("nombre" to "G.TORRES", "rating" to 76),
                        "3B" to mapOf("nombre" to "A.IBAÑEZ", "rating" to 74),
                        "SS" to mapOf("nombre" to "Z.MCKINSTRY", "rating" to 75)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "R.GREENE", "rating" to 79),
                        "CF" to mapOf("nombre" to "M.VIERLING", "rating" to 74),
                        "RF" to mapOf("nombre" to "K.CARPENTER", "rating" to 75),
                        "DH" to mapOf("nombre" to "D.DINGLER", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "T.SKUBAL", "rating" to 87),
                        "SP2" to mapOf("nombre" to "J.FLAHERTY", "rating" to 77),
                        "SP3" to mapOf("nombre" to "R.OLSEN", "rating" to 78),
                        "SP4" to mapOf("nombre" to "A.COBB", "rating" to 74),
                        "SP5" to mapOf("nombre" to "C.MIZE", "rating" to 75)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "B.HUNTER", "rating" to 72),
                        "RP2" to mapOf("nombre" to "J.FOLEY", "rating" to 71),
                        "RP3" to mapOf("nombre" to "B.BRIESKE", "rating" to 68),
                        "RP4" to mapOf("nombre" to "J.BREBBIA", "rating" to 68),
                        "RP5" to mapOf("nombre" to "B.HANIFEE", "rating" to 66),
                        "RP6" to mapOf("nombre" to "T.HOLTON", "rating" to 77),
                        "RP7" to mapOf("nombre" to "W.VEST", "rating" to 73),
                        "RP8" to mapOf("nombre" to "T.KAHNLE", "rating" to 75)
                    )
                )
            ),

            "Twins" to mapOf(
                "nombre" to "Minnesota Twins",
                "ciudad" to "Minneapolis",
                "liga" to "AL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "R.JEFFERS", "rating" to 78),
                        "1B" to mapOf("nombre" to "T.FRANCE", "rating" to 72),
                        "2B" to mapOf("nombre" to "J.BRIDE", "rating" to 71),
                        "3B" to mapOf("nombre" to "R.LEWIS", "rating" to 76),
                        "SS" to mapOf("nombre" to "C.CORREA", "rating" to 84)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "T.LARNACH", "rating" to 72),
                        "CF" to mapOf("nombre" to "B.BUXTON", "rating" to 83),
                        "RF" to mapOf("nombre" to "H.BADER", "rating" to 78),
                        "DH" to mapOf("nombre" to "M.WALLNER", "rating" to 75)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "J.RYAN", "rating" to 74),
                        "SP2" to mapOf("nombre" to "P.LOPEZ", "rating" to 76),
                        "SP3" to mapOf("nombre" to "B.OBER", "rating" to 79),
                        "SP4" to mapOf("nombre" to "D.FESTA", "rating" to 74),
                        "SP5" to mapOf("nombre" to "C.PADDACK", "rating" to 71)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "M.TOMKIN", "rating" to 70),
                        "RP2" to mapOf("nombre" to "D.COULOMBE", "rating" to 71),
                        "RP3" to mapOf("nombre" to "B.STEWART", "rating" to 74),
                        "RP4" to mapOf("nombre" to "J.ALCALA", "rating" to 73),
                        "RP5" to mapOf("nombre" to "J.TOPA", "rating" to 73),
                        "RP6" to mapOf("nombre" to "J.DURAN", "rating" to 78),
                        "RP7" to mapOf("nombre" to "J.ALCALA", "rating" to 68),
                        "RP8" to mapOf("nombre" to "G.JAX", "rating" to 79)
                    )
                )
            ),

            "WhiteSox" to mapOf(
                "nombre" to "Chicago White Sox",
                "ciudad" to "Chicago",
                "liga" to "AL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "E.QUERO", "rating" to 71),
                        "1B" to mapOf("nombre" to "A.VAUGHN", "rating" to 69),
                        "2B" to mapOf("nombre" to "L.SOSA", "rating" to 71),
                        "3B" to mapOf("nombre" to "J.ROJAS", "rating" to 70),
                        "SS" to mapOf("nombre" to "B.BALDWIN", "rating" to 71)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "A.BENINTENDI", "rating" to 73),
                        "CF" to mapOf("nombre" to "M.TAYLOR", "rating" to 74),
                        "RF" to mapOf("nombre" to "M.TAUCHMAN", "rating" to 73),
                        "DH" to mapOf("nombre" to "L.ROBERT", "rating" to 75)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "J.IRIARTE", "rating" to 66),
                        "SP2" to mapOf("nombre" to "J.CANNON", "rating" to 70),
                        "SP3" to mapOf("nombre" to "O.WHITE", "rating" to 69),
                        "SP4" to mapOf("nombre" to "D.MARTIN", "rating" to 66),
                        "SP5" to mapOf("nombre" to "M.PEREZ", "rating" to 65)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "M.VASIL", "rating" to 61),
                        "RP2" to mapOf("nombre" to "W.GONZALEZ", "rating" to 57),
                        "RP3" to mapOf("nombre" to "A.ELLARD", "rating" to 60),
                        "RP4" to mapOf("nombre" to "B.WILSON", "rating" to 69),
                        "RP5" to mapOf("nombre" to "T.GILBERT", "rating" to 59),
                        "RP6" to mapOf("nombre" to "C.GROSSER", "rating" to 65),
                        "RP7" to mapOf("nombre" to "M.CLEVINGER", "rating" to 67),
                        "RP8" to mapOf("nombre" to "K.MURPHY", "rating" to 72)
                    )
                )
            ),

            // AMERICAN LEAGUE WEST
            "Angels" to mapOf(
                "nombre" to "Los Angeles Angels",
                "ciudad" to "Anaheim",
                "liga" to "AL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "L.O'HOPPE", "rating" to 80),
                        "1B" to mapOf("nombre" to "N.SCHANUEL", "rating" to 72),
                        "2B" to mapOf("nombre" to "L.RENGIFO", "rating" to 77),
                        "3B" to mapOf("nombre" to "Y.MONCADA", "rating" to 70),
                        "SS" to mapOf("nombre" to "Z.NETO", "rating" to 82)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "T.WARD", "rating" to 78),
                        "CF" to mapOf("nombre" to "J.ADELL", "rating" to 74),
                        "RF" to mapOf("nombre" to "M.TROUT", "rating" to 88),
                        "DH" to mapOf("nombre" to "J.SOLER", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "Y.KIKUCHI", "rating" to 79),
                        "SP2" to mapOf("nombre" to "J.SORIANO", "rating" to 78),
                        "SP3" to mapOf("nombre" to "T.ANDERSON", "rating" to 68),
                        "SP4" to mapOf("nombre" to "J.KOCHANOWICZ", "rating" to 69),
                        "SP5" to mapOf("nombre" to "C.SILSETH", "rating" to 69)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "R.DETMERS", "rating" to 68),
                        "RP2" to mapOf("nombre" to "J.EDER", "rating" to 60),
                        "RP3" to mapOf("nombre" to "S.BACHMAN", "rating" to 67),
                        "RP4" to mapOf("nombre" to "C.BROGDON", "rating" to 65),
                        "RP5" to mapOf("nombre" to "H.NERIS", "rating" to 71),
                        "RP6" to mapOf("nombre" to "B.JOYCE", "rating" to 69),
                        "RP7" to mapOf("nombre" to "B.BURKE", "rating" to 68),
                        "RP8" to mapOf("nombre" to "K.JANSEN", "rating" to 72)
                    )
                )
            ),

            "Astros" to mapOf(
                "nombre" to "Houston Astros",
                "ciudad" to "Houston",
                "liga" to "AL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "Y.DIAZ", "rating" to 84),
                        "1B" to mapOf("nombre" to "C.WALKER", "rating" to 83),
                        "2B" to mapOf("nombre" to "M.DUBON", "rating" to 74),
                        "3B" to mapOf("nombre" to "I.PAREDES", "rating" to 77),
                        "SS" to mapOf("nombre" to "J.PENA", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.ALTUVE", "rating" to 85),
                        "CF" to mapOf("nombre" to "J.MEYERS", "rating" to 77),
                        "RF" to mapOf("nombre" to "C.MCCORMICK", "rating" to 74),
                        "DH" to mapOf("nombre" to "Y.ALVAREZ", "rating" to 88)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "F.VALDEZ", "rating" to 80),
                        "SP2" to mapOf("nombre" to "H.BROWN", "rating" to 76),
                        "SP3" to mapOf("nombre" to "R.BLANCO", "rating" to 76),
                        "SP4" to mapOf("nombre" to "H.WESNESKI", "rating" to 74),
                        "SP5" to mapOf("nombre" to "L.MCCULLERS", "rating" to 69)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "S.OKERT", "rating" to 61),
                        "RP2" to mapOf("nombre" to "T.SCOTT", "rating" to 68),
                        "RP3" to mapOf("nombre" to "K.ORT", "rating" to 67),
                        "RP4" to mapOf("nombre" to "S.DUBIN", "rating" to 63),
                        "RP5" to mapOf("nombre" to "B.AREU", "rating" to 77),
                        "RP6" to mapOf("nombre" to "B.KING", "rating" to 69),
                        "RP7" to mapOf("nombre" to "B.SOUSA", "rating" to 62),
                        "RP8" to mapOf("nombre" to "J.HADER", "rating" to 84)
                    )
                )
            ),

            "Athletics" to mapOf(
                "nombre" to "Oakland Athletics",
                "ciudad" to "Oakland",
                "liga" to "AL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "S.LANGELIERS", "rating" to 78),
                        "1B" to mapOf("nombre" to "T.SODERSTROM", "rating" to 75),
                        "2B" to mapOf("nombre" to "Z.GELOF", "rating" to 72),
                        "3B" to mapOf("nombre" to "L.URIAS", "rating" to 67),
                        "SS" to mapOf("nombre" to "J.WILSON", "rating" to 75)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "M.ANDUJAR", "rating" to 73),
                        "CF" to mapOf("nombre" to "L.BUTLER", "rating" to 79),
                        "RF" to mapOf("nombre" to "JJ.BLEDAY", "rating" to 76),
                        "DH" to mapOf("nombre" to "B.ROOKER", "rating" to 80)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "L.SEVERINO", "rating" to 75),
                        "SP2" to mapOf("nombre" to "JP.SEARS", "rating" to 74),
                        "SP3" to mapOf("nombre" to "J.SPRINGS", "rating" to 74),
                        "SP4" to mapOf("nombre" to "J.ESTES", "rating" to 73),
                        "SP5" to mapOf("nombre" to "O.BIDO", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "M.SPENCE", "rating" to 63),
                        "RP2" to mapOf("nombre" to "M.OTAÑEZ", "rating" to 70),
                        "RP3" to mapOf("nombre" to "TJ.MCFARLAND", "rating" to 61),
                        "RP4" to mapOf("nombre" to "H.HARRIS", "rating" to 59),
                        "RP5" to mapOf("nombre" to "G.HOLMAN", "rating" to 60),
                        "RP6" to mapOf("nombre" to "J.STERNER", "rating" to 66),
                        "RP7" to mapOf("nombre" to "T.FERGUSON", "rating" to 71),
                        "RP8" to mapOf("nombre" to "M.MILLER", "rating" to 86)
                    )
                )
            ),

            "Mariners" to mapOf(
                "nombre" to "Seattle Mariners",
                "ciudad" to "Seattle",
                "liga" to "AL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "C.RALEIGH", "rating" to 84),
                        "1B" to mapOf("nombre" to "L.RALEY", "rating" to 75),
                        "2B" to mapOf("nombre" to "J.POLANCO", "rating" to 75),
                        "3B" to mapOf("nombre" to "D.MOORE", "rating" to 74),
                        "SS" to mapOf("nombre" to "JP.CRAWFORD", "rating" to 72)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "R.AROZARENA", "rating" to 75),
                        "CF" to mapOf("nombre" to "J.RODRIGUEZ", "rating" to 83),
                        "RF" to mapOf("nombre" to "D.CANZONE", "rating" to 68),
                        "DH" to mapOf("nombre" to "M.GARVER", "rating" to 71)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "L.GILBERT", "rating" to 85),
                        "SP2" to mapOf("nombre" to "G.KIRBY", "rating" to 85),
                        "SP3" to mapOf("nombre" to "L.CASTILLO", "rating" to 83),
                        "SP4" to mapOf("nombre" to "B.WOO", "rating" to 78),
                        "SP5" to mapOf("nombre" to "B.MILLER", "rating" to 78)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "E.BAZARDO", "rating" to 68),
                        "RP2" to mapOf("nombre" to "T.THORNTON", "rating" to 72),
                        "RP3" to mapOf("nombre" to "T.TAYLOR", "rating" to 63),
                        "RP4" to mapOf("nombre" to "C.SNIDER", "rating" to 70),
                        "RP5" to mapOf("nombre" to "T.SAUCEDO", "rating" to 63),
                        "RP6" to mapOf("nombre" to "M.BRASH", "rating" to 74),
                        "RP7" to mapOf("nombre" to "G.SPEIER", "rating" to 74),
                        "RP8" to mapOf("nombre" to "A.MUÑOZ", "rating" to 83)
                    )
                )
            ),

            "Rangers" to mapOf(
                "nombre" to "Texas Rangers",
                "ciudad" to "Arlington",
                "liga" to "AL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "J.HEIM", "rating" to 78),
                        "1B" to mapOf("nombre" to "J.BURGER", "rating" to 79),
                        "2B" to mapOf("nombre" to "M.SEMIEN", "rating" to 82),
                        "3B" to mapOf("nombre" to "J.JUNG", "rating" to 77),
                        "SS" to mapOf("nombre" to "C.SEAGER", "rating" to 86)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "W.LANGFORD", "rating" to 86),
                        "CF" to mapOf("nombre" to "J.SMITH", "rating" to 77),
                        "RF" to mapOf("nombre" to "A.GARCIA", "rating" to 79),
                        "DH" to mapOf("nombre" to "K.HIGASHIOKA", "rating" to 74)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "J.DEGROM", "rating" to 85),
                        "SP2" to mapOf("nombre" to "N.EOVALDI", "rating" to 82),
                        "SP3" to mapOf("nombre" to "K.ROCKER", "rating" to 70),
                        "SP4" to mapOf("nombre" to "J.LEITER", "rating" to 72),
                        "SP5" to mapOf("nombre" to "T.MAHLE", "rating" to 75)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "L.JACKSON", "rating" to 61),
                        "RP2" to mapOf("nombre" to "J.LATZ", "rating" to 58),
                        "RP3" to mapOf("nombre" to "H.MILNER", "rating" to 72),
                        "RP4" to mapOf("nombre" to "S.ARMSTRONG", "rating" to 68),
                        "RP5" to mapOf("nombre" to "C.WINN", "rating" to 67),
                        "RP6" to mapOf("nombre" to "R.GARCIA", "rating" to 71),
                        "RP7" to mapOf("nombre" to "J.WEBB", "rating" to 69),
                        "RP8" to mapOf("nombre" to "C.MARTIN", "rating" to 81)
                    )
                )
            ),

            // NATIONAL LEAGUE EAST
            "Braves" to mapOf(
                "nombre" to "Atlanta Braves",
                "ciudad" to "Atlanta",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "S.MURPHY", "rating" to 81),
                        "1B" to mapOf("nombre" to "M.OLSON", "rating" to 81),
                        "2B" to mapOf("nombre" to "O.ALBIES", "rating" to 80),
                        "3B" to mapOf("nombre" to "A.RILEY", "rating" to 81),
                        "SS" to mapOf("nombre" to "O.ARCIA", "rating" to 71)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "A.VERDUGO", "rating" to 76),
                        "CF" to mapOf("nombre" to "M.HARRIS", "rating" to 83),
                        "RF" to mapOf("nombre" to "R.ACUÑA", "rating" to 87),
                        "DH" to mapOf("nombre" to "M.OZUNA", "rating" to 83)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "C.SALE", "rating" to 87),
                        "SP2" to mapOf("nombre" to "S.STRIDER", "rating" to 84),
                        "SP3" to mapOf("nombre" to "R.LOPEZ", "rating" to 80),
                        "SP4" to mapOf("nombre" to "S.SCHWELLENBACH", "rating" to 80),
                        "SP5" to mapOf("nombre" to "G.HOLMES", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "E.DE LOS SANTOS", "rating" to 67),
                        "RP2" to mapOf("nombre" to "A.BUMMER", "rating" to 68),
                        "RP3" to mapOf("nombre" to "P.JOHNSON", "rating" to 68),
                        "RP4" to mapOf("nombre" to "R.MONTERO", "rating" to 67),
                        "RP5" to mapOf("nombre" to "D.HERNANDEZ", "rating" to 60),
                        "RP6" to mapOf("nombre" to "I.ANDERSON", "rating" to 64),
                        "RP7" to mapOf("nombre" to "D.LEE", "rating" to 72),
                        "RP8" to mapOf("nombre" to "R.IGLESIAS", "rating" to 79)
                    )
                )
            ),

            "Marlins" to mapOf(
                "nombre" to "Miami Marlins",
                "ciudad" to "Miami",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "A.RAMIREZ", "rating" to 69),
                        "1B" to mapOf("nombre" to "K.STOWERS", "rating" to 65),
                        "2B" to mapOf("nombre" to "O.LOPEZ", "rating" to 73),
                        "3B" to mapOf("nombre" to "G.PAULY", "rating" to 68),
                        "SS" to mapOf("nombre" to "X.EDWARDS", "rating" to 72)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.SANCHEZ", "rating" to 74),
                        "CF" to mapOf("nombre" to "D.HILL", "rating" to 72),
                        "RF" to mapOf("nombre" to "C.NORBY", "rating" to 71),
                        "DH" to mapOf("nombre" to "D.MYERS", "rating" to 73)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.ALCANTARA", "rating" to 72),
                        "SP2" to mapOf("nombre" to "E.CABRERA", "rating" to 74),
                        "SP3" to mapOf("nombre" to "R.WEATHERS", "rating" to 72),
                        "SP4" to mapOf("nombre" to "M.MYERS", "rating" to 76),
                        "SP5" to mapOf("nombre" to "V.BELLOZO", "rating" to 69)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "T.PHILLIPS", "rating" to 63),
                        "RP2" to mapOf("nombre" to "G.SORIANO", "rating" to 66),
                        "RP3" to mapOf("nombre" to "R.HENRIQUEZ", "rating" to 63),
                        "RP4" to mapOf("nombre" to "D.CRONIN", "rating" to 63),
                        "RP5" to mapOf("nombre" to "C.FAUCHER", "rating" to 63),
                        "RP6" to mapOf("nombre" to "L.BACHAR", "rating" to 63),
                        "RP7" to mapOf("nombre" to "J.TINOCO", "rating" to 71),
                        "RP8" to mapOf("nombre" to "A.BENDER", "rating" to 72)
                    )
                )
            ),

            "Mets" to mapOf(
                "nombre" to "New York Mets",
                "ciudad" to "New York",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "F.ALVAREZ", "rating" to 78),
                        "1B" to mapOf("nombre" to "P.ALONSO", "rating" to 85),
                        "2B" to mapOf("nombre" to "J.ACUÑA", "rating" to 74),
                        "3B" to mapOf("nombre" to "M.VIENTOS", "rating" to 77),
                        "SS" to mapOf("nombre" to "F.LINDOR", "rating" to 88)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "B.NIMMO", "rating" to 78),
                        "CF" to mapOf("nombre" to "J.SIRI", "rating" to 79),
                        "RF" to mapOf("nombre" to "S.MARTE", "rating" to 82),
                        "DH" to mapOf("nombre" to "J.MCNEIL", "rating" to 78)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "K.SENGA", "rating" to 80),
                        "SP2" to mapOf("nombre" to "G.CANNING", "rating" to 72),
                        "SP3" to mapOf("nombre" to "C.HOLMES", "rating" to 77),
                        "SP4" to mapOf("nombre" to "T.MEGILL", "rating" to 77),
                        "SP5" to mapOf("nombre" to "D.PETERSON", "rating" to 71)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "T.ADCOCK", "rating" to 68),
                        "RP2" to mapOf("nombre" to "M.KRANICK", "rating" to 65),
                        "RP3" to mapOf("nombre" to "R.STANEK", "rating" to 70),
                        "RP4" to mapOf("nombre" to "H.BRAZOBAN", "rating" to 61),
                        "RP5" to mapOf("nombre" to "R.GARRETT", "rating" to 68),
                        "RP6" to mapOf("nombre" to "D.NUÑEZ", "rating" to 76),
                        "RP7" to mapOf("nombre" to "J.BUTTO", "rating" to 74),
                        "RP8" to mapOf("nombre" to "E.DIAZ", "rating" to 80)
                    )
                )
            ),

            "Nationals" to mapOf(
                "nombre" to "Washington Nationals",
                "ciudad" to "Washington",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "K.RUIZ", "rating" to 76),
                        "1B" to mapOf("nombre" to "N.LOWE", "rating" to 78),
                        "2B" to mapOf("nombre" to "L.GARCIA", "rating" to 79),
                        "3B" to mapOf("nombre" to "I.YEPEZ", "rating" to 72),
                        "SS" to mapOf("nombre" to "CJ.ABRAMS", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.WOOD", "rating" to 76),
                        "CF" to mapOf("nombre" to "J.YOUNG", "rating" to 78),
                        "RF" to mapOf("nombre" to "D.CREWS", "rating" to 78),
                        "DH" to mapOf("nombre" to "I.BELL", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "M.GORE", "rating" to 76),
                        "SP2" to mapOf("nombre" to "J.IRVING", "rating" to 76),
                        "SP3" to mapOf("nombre" to "M.PARKER", "rating" to 72),
                        "SP4" to mapOf("nombre" to "M.SOROKA", "rating" to 69),
                        "SP5" to mapOf("nombre" to "T.WILLIAMS", "rating" to 69)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "C.HENRY", "rating" to 59),
                        "RP2" to mapOf("nombre" to "D.LAW", "rating" to 68),
                        "RP3" to mapOf("nombre" to "J.LOPEZ", "rating" to 67),
                        "RP4" to mapOf("nombre" to "J.FERRER", "rating" to 63),
                        "RP5" to mapOf("nombre" to "Z.BRZYKCY", "rating" to 59),
                        "RP6" to mapOf("nombre" to "K.FINNEGAN", "rating" to 69),
                        "RP7" to mapOf("nombre" to "J.RUTLEDGE", "rating" to 66),
                        "RP8" to mapOf("nombre" to "A.CHAFIN", "rating" to 66)
                    )
                )
            ),

            "Phillies" to mapOf(
                "nombre" to "Philadelphia Phillies",
                "ciudad" to "Philadelphia",
                "liga" to "NL",
                "division" to "East",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "JT.REALMUTO", "rating" to 80),
                        "1B" to mapOf("nombre" to "B.HARPER", "rating" to 87),
                        "2B" to mapOf("nombre" to "B.STOTT", "rating" to 82),
                        "3B" to mapOf("nombre" to "A.BOHM", "rating" to 77),
                        "SS" to mapOf("nombre" to "T.TURNER", "rating" to 85)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "M.KEPLER", "rating" to 75),
                        "CF" to mapOf("nombre" to "B.MARSH", "rating" to 76),
                        "RF" to mapOf("nombre" to "N.CASTELLANOS", "rating" to 76),
                        "DH" to mapOf("nombre" to "K.SCHWARBER", "rating" to 81)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "Z.WHEELER", "rating" to 86),
                        "SP2" to mapOf("nombre" to "A.NOLA", "rating" to 82),
                        "SP3" to mapOf("nombre" to "C.SANCHEZ", "rating" to 78),
                        "SP4" to mapOf("nombre" to "R.SUAREZ", "rating" to 76),
                        "SP5" to mapOf("nombre" to "J.LUZARDO", "rating" to 79)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "T.ROSS", "rating" to 68),
                        "RP2" to mapOf("nombre" to "T.ROMANDINI", "rating" to 65),
                        "RP3" to mapOf("nombre" to "T.BANKS", "rating" to 69),
                        "RP4" to mapOf("nombre" to "T.WALKER", "rating" to 66),
                        "RP5" to mapOf("nombre" to "C.HERNANDEZ", "rating" to 66),
                        "RP6" to mapOf("nombre" to "J.ALVARADO", "rating" to 76),
                        "RP7" to mapOf("nombre" to "M.STRAHM", "rating" to 72),
                        "RP8" to mapOf("nombre" to "K.KERKERING", "rating" to 78)
                    )
                )
            ),

            // NATIONAL LEAGUE CENTRAL
            "Brewers" to mapOf(
                "nombre" to "Milwaukee Brewers",
                "ciudad" to "Milwaukee",
                "liga" to "NL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "W.CONTRERAS", "rating" to 85),
                        "1B" to mapOf("nombre" to "R.HOSKINS", "rating" to 75),
                        "2B" to mapOf("nombre" to "B.TURANG", "rating" to 80),
                        "3B" to mapOf("nombre" to "A.MONASTERIOS", "rating" to 65),
                        "SS" to mapOf("nombre" to "J.ORTIZ", "rating" to 77)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "C.YELICH", "rating" to 83),
                        "CF" to mapOf("nombre" to "J.CHOURIO", "rating" to 82),
                        "RF" to mapOf("nombre" to "G.MITCHELL", "rating" to 75),
                        "DH" to mapOf("nombre" to "B.PERKINS", "rating" to 77)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "F.PERALTA", "rating" to 80),
                        "SP2" to mapOf("nombre" to "N.CORTES", "rating" to 76),
                        "SP3" to mapOf("nombre" to "A.CIVALE", "rating" to 75),
                        "SP4" to mapOf("nombre" to "Q.PRIESTER", "rating" to 63),
                        "SP5" to mapOf("nombre" to "J.QUINTANA", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "T.MYERS", "rating" to 70),
                        "RP2" to mapOf("nombre" to "A.ASHBY", "rating" to 72),
                        "RP3" to mapOf("nombre" to "J.KOENING", "rating" to 72),
                        "RP4" to mapOf("nombre" to "A.URIBE", "rating" to 69),
                        "RP5" to mapOf("nombre" to "E.PEGUERO", "rating" to 64),
                        "RP6" to mapOf("nombre" to "B.HUDSON", "rating" to 78),
                        "RP7" to mapOf("nombre" to "T.MEGILL", "rating" to 76),
                        "RP8" to mapOf("nombre" to "J.PAYAMPS", "rating" to 77)
                    )
                )
            ),

            "Cardinals" to mapOf(
                "nombre" to "St. Louis Cardinals",
                "ciudad" to "St. Louis",
                "liga" to "NL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "I.HERRERA", "rating" to 74),
                        "1B" to mapOf("nombre" to "W.CONTRERAS", "rating" to 80),
                        "2B" to mapOf("nombre" to "N.GORMAN", "rating" to 69),
                        "3B" to mapOf("nombre" to "N.ARENADO", "rating" to 80),
                        "SS" to mapOf("nombre" to "M.WINN", "rating" to 79)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "B.DONOVAN", "rating" to 77),
                        "CF" to mapOf("nombre" to "V.SCOTT", "rating" to 70),
                        "RF" to mapOf("nombre" to "L.NOOTBAAR", "rating" to 75),
                        "DH" to mapOf("nombre" to "A.BURLESON", "rating" to 73)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.GRAY", "rating" to 85),
                        "SP2" to mapOf("nombre" to "E.FEDDE", "rating" to 77),
                        "SP3" to mapOf("nombre" to "M.MIKOLAS", "rating" to 72),
                        "SP4" to mapOf("nombre" to "M.LIBERATORE", "rating" to 67),
                        "SP5" to mapOf("nombre" to "T.HENCE", "rating" to 68)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "R.O'BRIEN", "rating" to 61),
                        "RP2" to mapOf("nombre" to "K.LEAHY", "rating" to 65),
                        "RP3" to mapOf("nombre" to "S.MATZ", "rating" to 68),
                        "RP4" to mapOf("nombre" to "Z.THOMPSON", "rating" to 64),
                        "RP5" to mapOf("nombre" to "R.MUNOZ", "rating" to 64),
                        "RP6" to mapOf("nombre" to "J.ROMERO", "rating" to 74),
                        "RP7" to mapOf("nombre" to "R.FERNANDEZ", "rating" to 67),
                        "RP8" to mapOf("nombre" to "R.HELSLEY", "rating" to 80)
                    )
                )
            ),

            "Cubs" to mapOf(
                "nombre" to "Chicago Cubs",
                "ciudad" to "Chicago",
                "liga" to "NL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "C.KELLY", "rating" to 74),
                        "1B" to mapOf("nombre" to "M.BUSH", "rating" to 71),
                        "2B" to mapOf("nombre" to "N.HORNER", "rating" to 82),
                        "3B" to mapOf("nombre" to "J.TURNER", "rating" to 74),
                        "SS" to mapOf("nombre" to "D.SWANSON", "rating" to 82)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "I.HAPP", "rating" to 78),
                        "CF" to mapOf("nombre" to "P.CROW", "rating" to 82),
                        "RF" to mapOf("nombre" to "K.TUCKER", "rating" to 86),
                        "DH" to mapOf("nombre" to "S.SUZUKI", "rating" to 82)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.IMANAGA", "rating" to 81),
                        "SP2" to mapOf("nombre" to "J.STEEL", "rating" to 80),
                        "SP3" to mapOf("nombre" to "J.TAILLON", "rating" to 75),
                        "SP4" to mapOf("nombre" to "M.BOYD", "rating" to 74),
                        "SP5" to mapOf("nombre" to "B.BROWN", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "E.MORGAN", "rating" to 67),
                        "RP2" to mapOf("nombre" to "L.LITTLE", "rating" to 71),
                        "RP3" to mapOf("nombre" to "N.PEARSON", "rating" to 70),
                        "RP4" to mapOf("nombre" to "C.THIELBAR", "rating" to 67),
                        "RP5" to mapOf("nombre" to "T.MILLER", "rating" to 72),
                        "RP6" to mapOf("nombre" to "R.BRASIER", "rating" to 72),
                        "RP7" to mapOf("nombre" to "P.HODGE", "rating" to 73),
                        "RP8" to mapOf("nombre" to "D.SANTANA", "rating" to 74)
                    )
                )
            ),

            "Pirates" to mapOf(
                "nombre" to "Pittsburgh Pirates",
                "ciudad" to "Pittsburgh",
                "liga" to "NL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "J.BART", "rating" to 78),
                        "1B" to mapOf("nombre" to "S.HORWITZ", "rating" to 75),
                        "2B" to mapOf("nombre" to "N.GONZALEZ", "rating" to 75),
                        "3B" to mapOf("nombre" to "K.HAYES", "rating" to 75),
                        "SS" to mapOf("nombre" to "I.FALEFA", "rating" to 76)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "B.REYNOLDS", "rating" to 80),
                        "CF" to mapOf("nombre" to "O.CRUZ", "rating" to 80),
                        "RF" to mapOf("nombre" to "A.MCCUTCHEN", "rating" to 71),
                        "DH" to mapOf("nombre" to "J.SUWINSKI", "rating" to 71)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "P.SKENES", "rating" to 86),
                        "SP2" to mapOf("nombre" to "J.JONES", "rating" to 78),
                        "SP3" to mapOf("nombre" to "M.KELLER", "rating" to 76),
                        "SP4" to mapOf("nombre" to "A.HEANEY", "rating" to 74),
                        "SP5" to mapOf("nombre" to "B.FALTER", "rating" to 68)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "H.STRATTON", "rating" to 69),
                        "RP2" to mapOf("nombre" to "R.BORUCKI", "rating" to 72),
                        "RP3" to mapOf("nombre" to "C.FERGUSON", "rating" to 67),
                        "RP4" to mapOf("nombre" to "T.MAYZA", "rating" to 62),
                        "RP5" to mapOf("nombre" to "K.NICOLAS", "rating" to 61),
                        "RP6" to mapOf("nombre" to "D.BEDNAR", "rating" to 72),
                        "RP7" to mapOf("nombre" to "C.HOLDERMAN", "rating" to 72),
                        "RP8" to mapOf("nombre" to "D.SANTANA", "rating" to 74)
                    )
                )
            ),

            "Reds" to mapOf(
                "nombre" to "Cincinnati Reds",
                "ciudad" to "Cincinnati",
                "liga" to "NL",
                "division" to "Central",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "T.STEPHENSON", "rating" to 76),
                        "1B" to mapOf("nombre" to "C.ENCARNACION", "rating" to 68),
                        "2B" to mapOf("nombre" to "M.MCLAIN", "rating" to 74),
                        "3B" to mapOf("nombre" to "J.CANDELARIO", "rating" to 71),
                        "SS" to mapOf("nombre" to "E.DE LA CRUZ", "rating" to 85)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "S.STEER", "rating" to 78),
                        "CF" to mapOf("nombre" to "TJ.FRIEDL", "rating" to 78),
                        "RF" to mapOf("nombre" to "R.HINDS", "rating" to 72),
                        "DH" to mapOf("nombre" to "J.TREVINO", "rating" to 78)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "H.GREEN", "rating" to 80),
                        "SP2" to mapOf("nombre" to "N.LODOLO", "rating" to 76),
                        "SP3" to mapOf("nombre" to "N.MARTINEZ", "rating" to 76),
                        "SP4" to mapOf("nombre" to "B.SINGER", "rating" to 73),
                        "SP5" to mapOf("nombre" to "A.ABBOTT", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "G.ASHCRAFT", "rating" to 65),
                        "RP2" to mapOf("nombre" to "S.MOLL", "rating" to 72),
                        "RP3" to mapOf("nombre" to "A.DIAZ", "rating" to 71),
                        "RP4" to mapOf("nombre" to "S.BARLOW", "rating" to 70),
                        "RP5" to mapOf("nombre" to "I.GIBAUT", "rating" to 68),
                        "RP6" to mapOf("nombre" to "T.ROGERS", "rating" to 74),
                        "RP7" to mapOf("nombre" to "T.SANTILLAN", "rating" to 72),
                        "RP8" to mapOf("nombre" to "E.PAGAN", "rating" to 74)
                    )
                )
            ),

            // NATIONAL LEAGUE WEST
            "Diamondbacks" to mapOf(
                "nombre" to "Arizona Diamondbacks",
                "ciudad" to "Phoenix",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "G.MORENO", "rating" to 80),
                        "1B" to mapOf("nombre" to "J.NAYLOR", "rating" to 80),
                        "2B" to mapOf("nombre" to "K.MARTE", "rating" to 88),
                        "3B" to mapOf("nombre" to "E.SUAREZ", "rating" to 80),
                        "SS" to mapOf("nombre" to "G.PERDOMO", "rating" to 76)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "L.GURRIEL", "rating" to 79),
                        "CF" to mapOf("nombre" to "J.MCCARTHY", "rating" to 78),
                        "RF" to mapOf("nombre" to "C.CARROLL", "rating" to 83),
                        "DH" to mapOf("nombre" to "A.DEL CASTILLO", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "C.BURNES", "rating" to 83),
                        "SP2" to mapOf("nombre" to "Z.GALLEN", "rating" to 78),
                        "SP3" to mapOf("nombre" to "M.KELLY", "rating" to 76),
                        "SP4" to mapOf("nombre" to "E.RODRIGUEZ", "rating" to 74),
                        "SP5" to mapOf("nombre" to "B.PFAADT", "rating" to 74)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "R.NELSON", "rating" to 68),
                        "RP2" to mapOf("nombre" to "R.THOMPSON", "rating" to 69),
                        "RP3" to mapOf("nombre" to "K.NELSON", "rating" to 68),
                        "RP4" to mapOf("nombre" to "K.GRAVEMAN", "rating" to 66),
                        "RP5" to mapOf("nombre" to "S.MILLER", "rating" to 74),
                        "RP6" to mapOf("nombre" to "J.MARTINEZ", "rating" to 72),
                        "RP7" to mapOf("nombre" to "AJ.PUK", "rating" to 70),
                        "RP8" to mapOf("nombre" to "K.GINKEL", "rating" to 74)
                    )
                )
            ),

            "Dodgers" to mapOf(
                "nombre" to "Los Angeles Dodgers",
                "ciudad" to "Los Angeles",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "W.SMITH", "rating" to 82),
                        "1B" to mapOf("nombre" to "F.FREEMAN", "rating" to 86),
                        "2B" to mapOf("nombre" to "H.KIM", "rating" to 77),
                        "3B" to mapOf("nombre" to "M.ROJAS", "rating" to 79),
                        "SS" to mapOf("nombre" to "M.MUNCY", "rating" to 78)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "T.HERNANDEZ", "rating" to 79),
                        "CF" to mapOf("nombre" to "T.EDMAN", "rating" to 79),
                        "RF" to mapOf("nombre" to "M.BETTS", "rating" to 88),
                        "DH" to mapOf("nombre" to "M.CONFORTO", "rating" to 74)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "S.OHTANI", "rating" to 91),
                        "SP2" to mapOf("nombre" to "T.GLASNOW", "rating" to 88),
                        "SP3" to mapOf("nombre" to "B.SNELL", "rating" to 85),
                        "SP4" to mapOf("nombre" to "Y.YAMAMOTO", "rating" to 82),
                        "SP5" to mapOf("nombre" to "R.SASAKI", "rating" to 79)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "L.GARCIA", "rating" to 69),
                        "RP2" to mapOf("nombre" to "E.PHILLIPS", "rating" to 78),
                        "RP3" to mapOf("nombre" to "A.VESIA", "rating" to 74),
                        "RP4" to mapOf("nombre" to "M.KOPECH", "rating" to 71),
                        "RP5" to mapOf("nombre" to "A.BANDA", "rating" to 70),
                        "RP6" to mapOf("nombre" to "T.SCOTT", "rating" to 80),
                        "RP7" to mapOf("nombre" to "K.YATES", "rating" to 80),
                        "RP8" to mapOf("nombre" to "B.TREINEN", "rating" to 81)
                    )
                )
            ),

            "Giants" to mapOf(
                "nombre" to "San Francisco Giants",
                "ciudad" to "San Francisco",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "P.BAILEY", "rating" to 76),
                        "1B" to mapOf("nombre" to "L.CAPUANO", "rating" to 74),
                        "2B" to mapOf("nombre" to "T.FITZGERALD", "rating" to 75),
                        "3B" to mapOf("nombre" to "M.CAPMAN", "rating" to 82),
                        "SS" to mapOf("nombre" to "W.ADAMES", "rating" to 80)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "H.RAMOS", "rating" to 79),
                        "CF" to mapOf("nombre" to "J.LEE", "rating" to 77),
                        "RF" to mapOf("nombre" to "M.YAZTRESMI", "rating" to 72),
                        "DH" to mapOf("nombre" to "W.FLORES", "rating" to 72)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "L.WEBB", "rating" to 83),
                        "SP2" to mapOf("nombre" to "J.VERLANDER", "rating" to 79),
                        "SP3" to mapOf("nombre" to "J.HICKS", "rating" to 74),
                        "SP4" to mapOf("nombre" to "K.HARRISON", "rating" to 72),
                        "SP5" to mapOf("nombre" to "R.RAY", "rating" to 73)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "T.ROGERS", "rating" to 66),
                        "RP2" to mapOf("nombre" to "E.MILLER", "rating" to 68),
                        "RP3" to mapOf("nombre" to "S.BIVENS", "rating" to 68),
                        "RP4" to mapOf("nombre" to "H.BIRDSONG", "rating" to 73),
                        "RP5" to mapOf("nombre" to "C.DOVAL", "rating" to 73),
                        "RP6" to mapOf("nombre" to "R.RODRIGUEZ", "rating" to 66),
                        "RP7" to mapOf("nombre" to "R.WALKER", "rating" to 81),
                        "RP8" to mapOf("nombre" to "S.HJELLE", "rating" to 66)
                    )
                )
            ),

            "Padres" to mapOf(
                "nombre" to "San Diego Padres",
                "ciudad" to "San Diego",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "L.STALLINGS", "rating" to 72),
                        "1B" to mapOf("nombre" to "M.TOGLIA", "rating" to 69),
                        "2B" to mapOf("nombre" to "J.IGLESIAS", "rating" to 79),
                        "3B" to mapOf("nombre" to "M.MACHADO", "rating" to 82),
                        "SS" to mapOf("nombre" to "X.BOGARTS", "rating" to 79)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "J.HEYWARD", "rating" to 72),
                        "CF" to mapOf("nombre" to "J.MERRILL", "rating" to 86),
                        "RF" to mapOf("nombre" to "F.TATIS", "rating" to 88),
                        "DH" to mapOf("nombre" to "J.CRONENWRTH", "rating" to 75)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "M.KING", "rating" to 76),
                        "SP2" to mapOf("nombre" to "D.CEASE", "rating" to 83),
                        "SP3" to mapOf("nombre" to "Y.DARVISH", "rating" to 80),
                        "SP4" to mapOf("nombre" to "N.PIVETTA", "rating" to 74),
                        "SP5" to mapOf("nombre" to "J.MUSGROVE", "rating" to 74)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "J.HILL", "rating" to 60),
                        "RP2" to mapOf("nombre" to "C.GILBERT", "rating" to 63),
                        "RP3" to mapOf("nombre" to "Y.BIRD", "rating" to 62),
                        "RP4" to mapOf("nombre" to "S.HALVORSEN", "rating" to 60),
                        "RP5" to mapOf("nombre" to "J.SALAGNICK", "rating" to 69),
                        "RP6" to mapOf("nombre" to "T.KINLEY", "rating" to 66),
                        "RP7" to mapOf("nombre" to "A.CHIVILLI", "rating" to 68),
                        "RP8" to mapOf("nombre" to "J.VODNIK", "rating" to 60)
                    )
                )
            ),

            "Rockies" to mapOf(
                "nombre" to "Colorado Rockies",
                "ciudad" to "Denver",
                "liga" to "NL",
                "division" to "West",
                "jugadores" to mapOf(
                    "infield" to mapOf(
                        "C" to mapOf("nombre" to "C.MARQUEZ", "rating" to 72),
                        "1B" to mapOf("nombre" to "R.TELLEZ", "rating" to 69),
                        "2B" to mapOf("nombre" to "T.ESTRADA", "rating" to 74),
                        "3B" to mapOf("nombre" to "R.MCMAHON", "rating" to 75),
                        "SS" to mapOf("nombre" to "E.TOVAR", "rating" to 80)
                    ),
                    "outfield" to mapOf(
                        "LF" to mapOf("nombre" to "N.MARTIN", "rating" to 69),
                        "CF" to mapOf("nombre" to "B.DOYLE", "rating" to 82),
                        "RF" to mapOf("nombre" to "M.MONIAK", "rating" to 71),
                        "DH" to mapOf("nombre" to "K.FARMER", "rating" to 71)
                    ),
                    "pitchers" to mapOf(
                        "SP1" to mapOf("nombre" to "G.MARQUEZ", "rating" to 72),
                        "SP2" to mapOf("nombre" to "R.FELTNER", "rating" to 69),
                        "SP3" to mapOf("nombre" to "K.FREELAND", "rating" to 67),
                        "SP4" to mapOf("nombre" to "C.DOLLANDER", "rating" to 66),
                        "SP5" to mapOf("nombre" to "A.SENZATELA", "rating" to 62)
                    ),
                    "relief" to mapOf(
                        "RP1" to mapOf("nombre" to "B.HOEING", "rating" to 67),
                        "RP2" to mapOf("nombre" to "Y.MATSUI", "rating" to 73),
                        "RP3" to mapOf("nombre" to "R.MARINACCIO", "rating" to 70),
                        "RP4" to mapOf("nombre" to "A.MOREJÓN", "rating" to 68),
                        "RP5" to mapOf("nombre" to "W.PERALTA", "rating" to 68),
                        "RP6" to mapOf("nombre" to "J.ESTRADA", "rating" to 77),
                        "RP7" to mapOf("nombre" to "R.SUAREZ", "rating" to 66),
                        "RP8" to mapOf("nombre" to "J.ADAM", "rating" to 79)
                    )
                )
            )
        )
    }
}