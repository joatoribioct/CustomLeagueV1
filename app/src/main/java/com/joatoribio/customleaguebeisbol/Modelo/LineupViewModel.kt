package com.joatoribio.customleaguebeisbol.Modelo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LineupViewModel : ViewModel() {
    // LiveData privado que contiene la lista de lineups
    private val _lineups = MutableLiveData<MutableList<ModeloDraftSelecionado>>(mutableListOf())

    // LiveData público para observar desde los fragmentos
    val lineups: LiveData<MutableList<ModeloDraftSelecionado>> = _lineups

    /**
     * Intenta agregar un lineup, devuelve true si fue exitoso, false si ya existe el tipo
     */
    fun agregarLineup(tipo: String, jugadores: Map<String, Map<String, Any>>): Boolean {
        val listaActual = _lineups.value ?: mutableListOf()

        // Verificar si ya existe este tipo de lineup
        val yaExiste = listaActual.any { it.tipo == tipo }

        if (yaExiste) {
            // Si ya existe este tipo, no permitir agregar y devolver false
            return false
        } else {
            // Si no existe, agregar nuevo lineup
            listaActual.add(ModeloDraftSelecionado(tipo, jugadores))
            // Actualizar el LiveData para notificar a los observadores
            _lineups.value = listaActual
            return true
        }
    }

    fun limpiarLineups() {
        _lineups.value = mutableListOf()
    }

    fun eliminarLineup(tipo: String) {
        val listaActual = _lineups.value ?: mutableListOf()
        listaActual.removeAll { it.tipo == tipo }
        _lineups.value = listaActual
    }

    fun getCantidadLineups(): Int {
        return _lineups.value?.size ?: 0
    }

    fun existeLineup(tipo: String): Boolean {
        return _lineups.value?.any { it.tipo == tipo } ?: false
    }

    /**
     * Obtiene los tipos de lineup ya seleccionados
     */
    fun getTiposSeleccionados(): List<String> {
        return _lineups.value?.map { it.tipo } ?: emptyList()
    }

    // NUEVOS MÉTODOS AGREGADOS - PASO 2
    /**
     * Obtiene todos los lineups como lista inmutable
     */
    fun getAllLineups(): List<ModeloDraftSelecionado> {
        return _lineups.value?.toList() ?: emptyList()
    }

    /**
     * Obtiene un lineup específico por tipo
     */
    fun getLineupPorTipo(tipo: String): ModeloDraftSelecionado? {
        return _lineups.value?.find { it.tipo == tipo }
    }

    /**
     * Verifica si el ViewModel tiene datos
     */
    fun tieneLineups(): Boolean {
        return (_lineups.value?.size ?: 0) > 0
    }
}