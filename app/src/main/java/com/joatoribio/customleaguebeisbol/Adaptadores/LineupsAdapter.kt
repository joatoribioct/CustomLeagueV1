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
import com.joatoribio.customleaguebeisbol.Utils.RatingCalculator
import com.joatoribio.customleaguebeisbol.Utils.RatingUtils

class LineupsAdapter(
    private val lineups: List<Triple<String, Map<String, Map<String, Any>>, Boolean>>,
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

        // Usar RatingCalculator para convertir jugadores
        val listaJugadores = RatingCalculator.convertirJugadoresALista(jugadores)

        // Configurar el RecyclerView de jugadores
        holder.rvJugadores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = JugadoresAdapter(listaJugadores, estaSeleccionado)
            isNestedScrollingEnabled = false
        }

        // Configurar estado y apariencia según si está seleccionado
        if (estaSeleccionado) {
            holder.tvEstado.text = "No disponible"
            holder.tvEstado.setTextColor(Color.parseColor("#F44336"))
            holder.cardView.alpha = 0.5f
            holder.cardView.isClickable = false
            holder.cardView.isEnabled = false
        } else {
            // Calcular y mostrar rating promedio usando RatingCalculator
            val ratingPromedio = RatingCalculator.calcularRatingPromedio(listaJugadores)
            holder.tvEstado.text = RatingUtils.getRatingConEmoji(ratingPromedio)
            holder.tvEstado.setTextColor(RatingUtils.getRatingColor(ratingPromedio))

            holder.cardView.alpha = 1.0f
            holder.cardView.isClickable = true
            holder.cardView.isEnabled = true

            // Configurar click listener solo si no está seleccionado
            holder.cardView.setOnClickListener {
                listener.onlineupClick(tipo, jugadores)
            }
        }
    }

    override fun getItemCount(): Int = lineups.size
}

// Interfaz para el listener (mantener si no existe)
interface OnLineupClickListener {
    fun onlineupClick(tipo: String, jugadores: Map<String, Map<String, Any>>)
}