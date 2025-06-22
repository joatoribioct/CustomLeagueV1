package com.joatoribio.customleaguebeisbol

interface OnLineupClickListener {
    fun onlineupClick(tipo: String, jugadores: Map<String, Map<String, Any>>)
}