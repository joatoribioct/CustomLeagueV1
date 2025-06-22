package com.joatoribio.customleaguebeisbol

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

object Constantes {
    const val equipo_disponible = "Disponible"
    const val equipo_no_disponible = "No disponible"

    val equiposNombres = arrayOf(
        "Diamondbacks",
        "Braves",
        "Orioles",
        "RedSox",
        "WhiteSox",
        "Cubs",
        "Reds",
        "Guardians",
        "Rockies",
        "Tigers",
        "Astros",
        "Royals",
        "Angels",
        "Dodgers",
        "Marlins",
        "Brewers",
        "Twins",
        "Yankees",
        "Mets",
        "Athletics",
        "Phillies",
        "Pirates",
        "Padres",
        "Giants",
        "Mariners",
        "Cardinals",
        "Rays",
        "Rangers",
        "BlueJays",
        "Nationals"
    )

    val equiposIconos = arrayOf(
        R.drawable.logo_arizona,
        R.drawable.logo_atlanta,
        R.drawable.logo_orioles,
        R.drawable.logo_boston,
        R.drawable.logo_ws,
        R.drawable.logo_chicagocubs,
        R.drawable.logo_cintinati,
        R.drawable.logo_cl,
        R.drawable.logo_colorad,
        R.drawable.logo_detroit,
        R.drawable.logo_huston,
        R.drawable.logo_kansacity,
        R.drawable.logo_anaheim,
        R.drawable.logo_dodgers,
        R.drawable.logo_miami,
        R.drawable.logo_milwoki,
        R.drawable.logo_minesota,
        R.drawable.logo_yankees,
        R.drawable.logo_mets,
        R.drawable.logo_okland,
        R.drawable.logo_philis,
        R.drawable.ic_piratas,
        R.drawable.logo_sandiego,
        R.drawable.logo_sanfrancisco,
        R.drawable.logo_seatle,
        R.drawable.logo_cardenales,
        R.drawable.logo_tampa,
        R.drawable.logo_texa,
        R.drawable.toronto,
        R.drawable.logo_nacionales

    )


    val numerosDeEquipos = arrayOf(
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12",
        "13",
        "14",
        "15",
        "16",
        "17",
        "18",
        "19",
        "20",
        "21",
        "22",
        "23",
        "24",
        "25",
        "26",
        "27",
        "28",
        "29",
        "30"
    )


    // funcio devulve el tiempo actual del dispositivo
    fun obtenerTiempoDis() : Long{
        return System.currentTimeMillis()
    }
    // permite obtener mes, dia y año de una fecha
    fun obtenerFecha(tiempo: Long) : String{
        val calendario = Calendar.getInstance(Locale.ENGLISH)
        calendario.timeInMillis = tiempo

        return DateFormat.format("dd/MM/yyyy", calendario).toString()
    }

}