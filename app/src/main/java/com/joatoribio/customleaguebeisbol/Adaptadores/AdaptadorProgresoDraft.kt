package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.Fragmentos.PickSeleccionado
import com.joatoribio.customleaguebeisbol.Fragmentos.ProgresoParticipante
import com.joatoribio.customleaguebeisbol.R

class AdaptadorProgresoDraft(
    private val participantes: List<ProgresoParticipante>,
    private val uidUsuarioActual: String
) : RecyclerView.Adapter<AdaptadorProgresoDraft.HolderProgresoDraft>() {

    inner class HolderProgresoDraft(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: CardView = itemView as CardView
        val tvPosicion: TextView = itemView.findViewById(R.id.tvPosicionParticipante)
        val tvNombreParticipante: TextView = itemView.findViewById(R.id.tvNombreParticipante)
        val tvIndicadorAdmin: TextView = itemView.findViewById(R.id.tvIndicadorAdmin)
        val layoutPicks: LinearLayout = itemView.findViewById(R.id.layoutPicksParticipante)
        val tvSinSelecciones: TextView = itemView.findViewById(R.id.tvSinSelecciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderProgresoDraft {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_progreso_participante, parent, false)
        return HolderProgresoDraft(view)
    }

    override fun onBindViewHolder(holder: HolderProgresoDraft, position: Int) {
        val participante = participantes[position]

        with(holder) {
            // Mostrar posición
            tvPosicion.text = "${participante.posicion}"

            // Mostrar nombre con indicador de usuario actual
            if (participante.uid == uidUsuarioActual) {
                tvNombreParticipante.text = "${participante.idGaming} (Tú)"
                tvNombreParticipante.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_my_turn))
                tvNombreParticipante.textSize = 16f
                tvNombreParticipante.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tvNombreParticipante.text = participante.idGaming
                tvNombreParticipante.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                tvNombreParticipante.textSize = 14f
                tvNombreParticipante.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Mostrar indicador de admin
            if (participante.esAdmin) {
                tvIndicadorAdmin.visibility = View.VISIBLE
                tvIndicadorAdmin.text = "👑 Admin"
                tvIndicadorAdmin.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_admin))
            } else {
                tvIndicadorAdmin.visibility = View.GONE
            }

            // Configurar fondo según posición
            when (participante.posicion) {
                1 -> {
                    root.setCardBackgroundColor(Color.parseColor("#FFF3E0")) // Dorado claro
                    tvPosicion.setTextColor(Color.parseColor("#FF8F00"))
                    tvPosicion.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                2 -> {
                    root.setCardBackgroundColor(Color.parseColor("#F5F5F5")) // Plateado claro
                    tvPosicion.setTextColor(Color.parseColor("#616161"))
                    tvPosicion.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                3 -> {
                    root.setCardBackgroundColor(Color.parseColor("#FFF8E1")) // Bronce claro
                    tvPosicion.setTextColor(Color.parseColor("#F57C00"))
                    tvPosicion.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                else -> {
                    root.setCardBackgroundColor(Color.WHITE)
                    tvPosicion.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                    tvPosicion.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }

            // Mostrar picks del participante
            mostrarPicksParticipante(participante.picks, holder)
        }
    }

    private fun mostrarPicksParticipante(picks: List<PickSeleccionado>, holder: HolderProgresoDraft) {
        holder.layoutPicks.removeAllViews()

        if (picks.isEmpty()) {
            holder.tvSinSelecciones.visibility = View.VISIBLE
            return
        }

        holder.tvSinSelecciones.visibility = View.GONE

        picks.forEachIndexed { index, pick ->
            val pickView = crearVistaPickSeleccionado(pick, index + 1, holder)
            holder.layoutPicks.addView(pickView)
        }
    }

    private fun crearVistaPickSeleccionado(
        pick: PickSeleccionado,
        numeroPick: Int,
        holder: HolderProgresoDraft
    ): View {
        val context = holder.itemView.context
        val inflater = LayoutInflater.from(context)
        val pickView = inflater.inflate(R.layout.item_pick_seleccionado, holder.layoutPicks, false)

        val tvRonda = pickView.findViewById<TextView>(R.id.tvRondaPick)
        val tvNumeroPick = pickView.findViewById<TextView>(R.id.tvNumeroPick)
        val tvTipoLineup = pickView.findViewById<TextView>(R.id.tvTipoLineup)
        val tvEquipo = pickView.findViewById<TextView>(R.id.tvEquipoPick)

        // Configurar datos
        tvRonda.text = "R${pick.ronda}"
        tvNumeroPick.text = "Pick $numeroPick"
        tvTipoLineup.text = formatearTipoLineup(pick.tipoLineup)
        tvEquipo.text = formatearNombreEquipo(pick.equipoId)

        // Colorear según la ronda
        val colorRonda = when (pick.ronda) {
            1 -> ContextCompat.getColor(context, R.color.ronda_1)
            2 -> ContextCompat.getColor(context, R.color.ronda_2)
            3 -> ContextCompat.getColor(context, R.color.ronda_3)
            4 -> ContextCompat.getColor(context, R.color.ronda_4)
            else -> ContextCompat.getColor(context, R.color.text_secondary)
        }

        tvRonda.setTextColor(colorRonda)
        tvRonda.setTypeface(null, android.graphics.Typeface.BOLD)

        return pickView
    }

    private fun formatearTipoLineup(tipo: String): String {
        return when (tipo.lowercase()) {
            "infield" -> "Infield"
            "outfield" -> "Outfield"
            "pitchers" -> "Pitchers"
            "relief" -> "Relief"
            else -> tipo.capitalize()
        }
    }

    private fun formatearNombreEquipo(equipoId: String): String {
        return when (equipoId) {
            "RedSox" -> "Red Sox"
            "WhiteSox" -> "White Sox"
            "BlueJays" -> "Blue Jays"
            else -> equipoId
        }
    }

    override fun getItemCount(): Int = participantes.size
}