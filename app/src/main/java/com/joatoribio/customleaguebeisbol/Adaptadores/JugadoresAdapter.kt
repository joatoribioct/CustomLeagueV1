package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.R
import com.joatoribio.customleaguebeisbol.Utils.RatingUtils

data class JugadorInfo(
    val posicion: String,
    val nombre: String,
    val rating: Int
)

class JugadoresAdapter(
    private val jugadores: List<JugadorInfo>,
    private val estaSeleccionado: Boolean
) : RecyclerView.Adapter<JugadoresAdapter.JugadorViewHolder>() {

    class JugadorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPosicion: TextView = itemView.findViewById(R.id.tv_posicion)
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre)
        val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JugadorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jugador, parent, false)
        return JugadorViewHolder(view)
    }

    override fun onBindViewHolder(holder: JugadorViewHolder, position: Int) {
        val jugador = jugadores[position]

        holder.tvPosicion.text = jugador.posicion
        holder.tvNombre.text = jugador.nombre

        // Usar RatingUtils para mostrar rating con emoji
        holder.tvRating.text = RatingUtils.getRatingConEmoji(jugador.rating)

        // Aplicar estilos diferentes si el lineup está seleccionado
        if (estaSeleccionado) {
            // Lineup seleccionado - colores más tenues
            holder.tvPosicion.apply {
                setTextColor(Color.parseColor("#9E9E9E"))
                alpha = 0.7f
            }
            holder.tvNombre.apply {
                setTextColor(Color.parseColor("#BDBDBD"))
                alpha = 0.7f
            }
            holder.tvRating.apply {
                setTextColor(Color.parseColor("#BDBDBD"))
                alpha = 0.7f
            }
        } else {
            // Lineup disponible - colores normales
            holder.tvPosicion.apply {
                setTextColor(Color.parseColor("#2196F3"))
                alpha = 1.0f
            }
            holder.tvNombre.apply {
                setTextColor(Color.parseColor("#424242"))
                alpha = 1.0f
            }
            holder.tvRating.apply {
                setTextColor(RatingUtils.getRatingColor(jugador.rating))
                alpha = 1.0f
            }
        }
    }

    override fun getItemCount(): Int = jugadores.size
}