package com.joatoribio.customleaguebeisbol.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.Modelo.ModeloDraftSelecionado
import com.joatoribio.customleaguebeisbol.R

class DraftsSelecionadoAdaptador(
    private val drafts: List<ModeloDraftSelecionado>
) : RecyclerView.Adapter<DraftsSelecionadoAdaptador.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.tvTipo)
        val contenido: TextView = itemView.findViewById(R.id.tvJugadores)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DraftsSelecionadoAdaptador.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft_seleccionado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: DraftsSelecionadoAdaptador.ViewHolder, position: Int) {
        val draft = drafts[position]
        holder.titulo.text = draft.tipo
        holder.contenido.text = draft.jugadores.entries.joinToString("\n") {
            val nombre = it.value["nombre"]?.toString() ?: "Nombre no disponible"
            "${it.key}: $nombre"
        }

    }

    override fun getItemCount(): Int {
        return drafts.size
    }
}