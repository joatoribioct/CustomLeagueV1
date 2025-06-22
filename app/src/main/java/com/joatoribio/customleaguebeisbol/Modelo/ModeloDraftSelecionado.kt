package com.joatoribio.customleaguebeisbol.Modelo

import java.io.Serializable

class ModeloDraftSelecionado(
    val tipo: String,
    val jugadores: Map<String, Map<String, Any>>
) : Serializable