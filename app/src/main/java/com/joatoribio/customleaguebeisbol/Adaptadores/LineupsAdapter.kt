package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.OnLineupClickListener
import com.joatoribio.customleaguebeisbol.R

class LineupsAdapter(
    private val lineups: List<Triple<String, Map<String, Map<String, Any>>, Boolean>>, // Triple incluye si está seleccionado
    private val equipoId: String,
    private val listener: OnLineupClickListener
) : RecyclerView.Adapter<LineupsAdapter.LineupViewHolder>() {

    class LineupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipoLineup: TextView = itemView.findViewById(R.id.tv_tipo_lineup)
        val rvJugadores: RecyclerView = itemView.findViewById(R.id.rv_jugadores)
        val tvEstado: TextView = itemView.findViewById(R.id.tv_estado)
        val cardView: CardView = itemView.findViewById(R.id.card_lineup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lineup, parent, false)
        return LineupViewHolder(view)
    }

    override fun onBindViewHolder(holder: LineupViewHolder, position: Int) {
        val (tipo, jugadores, estaSeleccionado) = lineups[position]

        // Configurar el nombre del tipo de lineup
        val nombreTipo = when(tipo) {
            "infield" -> "⚾ Infield"
            "outfield" -> "🏟️ Outfield"
            "pitchers" -> "🥎 Pitchers"
            "relief" -> "🔥 Relief"
            else -> tipo.capitalize()
        }

        holder.tvTipoLineup.text = nombreTipo

        // Convertir los jugadores a una lista de JugadorInfo
        val listaJugadores = convertirJugadoresALista(jugadores)

        // Configurar el RecyclerView de jugadores
        holder.rvJugadores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = JugadoresAdapter(listaJugadores, estaSeleccionado)
            isNestedScrollingEnabled = false
        }

        // Configurar el estado visual según si está seleccionado o no
        if (estaSeleccionado) {
            // Lineup ya seleccionado - desactivado
            holder.cardView.apply {
                setCardBackgroundColor(Color.parseColor("#F5F5F5")) // Gris claro
                alpha = 0.6f
                isClickable = false
                isFocusable = false
            }

            holder.tvTipoLineup.setTextColor(Color.parseColor("#757575")) // Gris oscuro

            holder.tvEstado.apply {
                visibility = View.VISIBLE
                text = "🔒 Ya seleccionado"
                setTextColor(Color.parseColor("#F44336")) // Rojo
                textSize = 12f
            }

            // Remover cualquier listener de click
            holder.cardView.setOnClickListener(null)

        } else {
            // Lineup disponible - activado
            holder.cardView.apply {
                setCardBackgroundColor(Color.parseColor("#FFFFFF")) // Blanco
                alpha = 1.0f
                isClickable = true
                isFocusable = true

                // Efecto de elevación para indicar que es clickeable
                cardElevation = 8f
            }

            holder.tvTipoLineup.setTextColor(Color.parseColor("#212121")) // Negro

            holder.tvEstado.apply {
                visibility = View.VISIBLE
                text = "✅ Disponible"
                setTextColor(Color.parseColor("#4CAF50")) // Verde
                textSize = 12f
            }

            // Configurar click listener
            holder.cardView.setOnClickListener {
                listener.onlineupClick(tipo, jugadores)
            }

            // Efecto visual al tocar
            holder.cardView.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.alpha = 0.8f
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        v.alpha = 1.0f
                    }
                }
                false
            }
        }
    }

    override fun getItemCount(): Int = lineups.size

    /**
     * Convierte el Map de jugadores a una lista de JugadorInfo
     */
    private fun convertirJugadoresALista(jugadores: Map<String, Map<String, Any>>): List<JugadorInfo> {
        val listaJugadores = mutableListOf<JugadorInfo>()

        for ((posicion, datosJugador) in jugadores) {
            val nombre = datosJugador["nombre"] as? String ?: "Sin nombre"
            val rating = when (val ratingValue = datosJugador["rating"]) {
                is Number -> ratingValue.toInt()
                is String -> ratingValue.toIntOrNull() ?: 0
                else -> 0
            }

            listaJugadores.add(JugadorInfo(posicion, nombre, rating))
        }

        // Ordenar por posición para un orden consistente
        return listaJugadores.sortedBy { obtenerOrdenPosicion(it.posicion) }
    }

    /**
     * Obtiene un orden numérico para las posiciones para mantener consistencia
     */
    private fun obtenerOrdenPosicion(posicion: String): Int {
        return when (posicion) {
            "C" -> 1
            "1B" -> 2
            "2B" -> 3
            "3B" -> 4
            "SS" -> 5
            "LF" -> 6
            "CF" -> 7
            "RF" -> 8
            "DH" -> 9
            "SP1" -> 10
            "SP2" -> 11
            "SP3" -> 12
            "SP4" -> 13
            "SP5" -> 14
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
}

// Interfaz para el listener (si no la tienes ya)
interface OnLineupClickListener {
    fun onlineupClick(tipo: String, jugadores: Map<String, Map<String, Any>>)
}