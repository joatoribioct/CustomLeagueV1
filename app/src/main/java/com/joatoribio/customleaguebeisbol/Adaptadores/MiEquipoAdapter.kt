package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.Modelo.ModeloDraftSelecionado
import com.joatoribio.customleaguebeisbol.R
import com.joatoribio.customleaguebeisbol.Utils.RatingCalculator
import com.joatoribio.customleaguebeisbol.Utils.RatingUtils

class MiEquipoAdapter(
    private val lineups: MutableList<ModeloDraftSelecionado>,
    private val listener: OnMiEquipoClickListener
) : RecyclerView.Adapter<MiEquipoAdapter.MiEquipoViewHolder>() {

    class MiEquipoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipoLineup: TextView = itemView.findViewById(R.id.tv_tipo_lineup)
        val rvJugadores: RecyclerView = itemView.findViewById(R.id.rv_jugadores_mi_equipo)
        val tvEstadoSeleccionado: TextView = itemView.findViewById(R.id.tv_estado_seleccionado)
        val tvRatingPromedio: TextView = itemView.findViewById(R.id.tv_rating_promedio)
        val btnEliminar: TextView = itemView.findViewById(R.id.btn_eliminar)
        val cardView: CardView = itemView.findViewById(R.id.card_mi_lineup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiEquipoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mi_equipo_lineup, parent, false)
        return MiEquipoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiEquipoViewHolder, position: Int) {
        val lineup = lineups[position]

        // Configurar el nombre del tipo de lineup
        val nombreTipo = when(lineup.tipo) {
            "infield" -> "⚾ Infield"
            "outfield" -> "🏟️ Outfield"
            "pitchers" -> "🥎 Pitchers"
            "relief" -> "🔥 Relief"
            else -> lineup.tipo.capitalize()
        }

        holder.tvTipoLineup.text = nombreTipo

        // Usar RatingCalculator para convertir jugadores
        val listaJugadores = RatingCalculator.convertirJugadoresALista(lineup.jugadores)

        // Configurar el RecyclerView de jugadores
        holder.rvJugadores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = JugadoresAdapter(listaJugadores, false) // false porque están en nuestro equipo
            isNestedScrollingEnabled = false
        }

        // Calcular y mostrar rating promedio usando RatingCalculator
        val ratingPromedio = RatingCalculator.calcularRatingPromedio(listaJugadores)
        holder.tvRatingPromedio.text = RatingUtils.getRatingConEmoji(ratingPromedio)

        // Configurar color del rating según su valor
        holder.tvRatingPromedio.setTextColor(RatingUtils.getRatingColor(ratingPromedio))

        // Configurar estado
        holder.tvEstadoSeleccionado.text = "✅ Seleccionado"
        holder.tvEstadoSeleccionado.setTextColor(Color.parseColor("#4CAF50"))

        // Configurar botón eliminar
        holder.btnEliminar.setOnClickListener {
            listener.onEliminarLineup(lineup.tipo, position)
        }

        // Configurar click en la tarjeta para ver detalles
        holder.cardView.setOnClickListener {
            listener.onVerDetallesLineup(lineup.tipo, lineup.jugadores)
        }
    }

    override fun getItemCount(): Int = lineups.size

    /**
     * Actualiza la lista de lineups
     */
    fun actualizarLineups(nuevosLineups: List<ModeloDraftSelecionado>) {
        lineups.clear()
        lineups.addAll(nuevosLineups)
        notifyDataSetChanged()
    }

    /**
     * Elimina un lineup de la lista
     */
    fun eliminarLineup(position: Int) {
        if (position >= 0 && position < lineups.size) {
            lineups.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, lineups.size)
        }
    }
}

// Interface para manejar clicks en el adaptador
interface OnMiEquipoClickListener {
    fun onEliminarLineup(tipo: String, position: Int)
    fun onVerDetallesLineup(tipo: String, jugadores: Map<String, Map<String, Any>>)
}