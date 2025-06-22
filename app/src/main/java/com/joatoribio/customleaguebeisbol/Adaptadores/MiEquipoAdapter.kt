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

        // Convertir los jugadores a una lista de JugadorInfo
        val listaJugadores = convertirJugadoresALista(lineup.jugadores)

        // Configurar el RecyclerView de jugadores
        holder.rvJugadores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = JugadoresAdapter(listaJugadores, false) // false porque están en nuestro equipo
            isNestedScrollingEnabled = false
        }

        // Calcular y mostrar rating promedio con emoji
        val ratingPromedio = calcularRatingPromedio(listaJugadores)
        holder.tvRatingPromedio.text = RatingUtils.getRatingConEmoji(ratingPromedio)

        // Configurar color del rating según su valor
        holder.tvRatingPromedio.setTextColor(RatingUtils.getRatingColor(ratingPromedio))

        // Configurar estado
        holder.tvEstadoSeleccionado.text = "✅ En tu equipo"

        // Configurar card con estilo de "seleccionado"
        holder.cardView.apply {
            setCardBackgroundColor(Color.parseColor("#E8F5E8")) // Verde claro
            cardElevation = 6f
        }

        // Configurar botón de eliminar
        holder.btnEliminar.setOnClickListener {
            listener.onEliminarLineup(lineup.tipo, position)
        }

        // Configurar click en la card (opcional - para ver detalles)
        holder.cardView.setOnClickListener {
            listener.onVerDetallesLineup(lineup.tipo, lineup.jugadores)
        }

        // Efecto visual al tocar
        holder.cardView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.9f
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1.0f
                }
            }
            false
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
     * Calcula el rating promedio de los jugadores
     */
    private fun calcularRatingPromedio(jugadores: List<JugadorInfo>): Int {
        if (jugadores.isEmpty()) return 0
        val suma = jugadores.sumOf { it.rating }
        return suma / jugadores.size
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