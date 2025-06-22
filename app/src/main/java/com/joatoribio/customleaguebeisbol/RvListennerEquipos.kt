package com.joatoribio.customleaguebeisbol

import com.joatoribio.customleaguebeisbol.Modelo.ModeloEquipos

interface RvListennerEquipos {

    fun onEquipoClick(modeloEquipos: ModeloEquipos)
}