package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joatoribio.customleaguebeisbol.Modelo.UsuarioParticipante
import com.joatoribio.customleaguebeisbol.R

class AdaptadorOrdenDraft(
    private val usuarios: List<UsuarioParticipante>,
    private val uidUsuarioActual: String
) : RecyclerView.Adapter<AdaptadorOrdenDraft.HolderOrdenDraft>() {

    inner class HolderOrdenDraft(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: CardView = itemView as CardView
        val tvPosicion: TextView = itemView.findViewById(R.id.tvPosicion)
        val tvIdGaming: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvRol: TextView = itemView.findViewById(R.id.tvEstadoUsuario)
        val ivPerfil: ImageView = itemView.findViewById(R.id.ivIndicadorUsuario)
        val ivDragHandle: ImageView = itemView.findViewById(R.id.ivMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderOrdenDraft {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_orden_turno, parent, false)
        return HolderOrdenDraft(view)
    }

    override fun onBindViewHolder(holder: HolderOrdenDraft, position: Int) {
        val usuario = usuarios[position]

        with(holder) {
            // Mostrar posición en el draft
            tvPosicion.text = "${position + 1}"

            // Mostrar ID Gaming
            val identificador = usuario.getIdentificadorMostrar()
            tvIdGaming.text = identificador

            // Indicador visual si es el usuario actual (admin)
            if (usuario.uid == uidUsuarioActual) {
                tvIdGaming.text = "$identificador (Tú - Admin)"
                tvIdGaming.setTextColor(Color.parseColor("#FF9800")) // Orange
                tvIdGaming.textSize = 16f
                tvIdGaming.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tvIdGaming.setTextColor(Color.BLACK)
                tvIdGaming.textSize = 14f
                tvIdGaming.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Mostrar rol
            tvRol.text = usuario.rol
            when (usuario.rol) {
                "Administrador" -> {
                    tvRol.setTextColor(Color.parseColor("#FF9800")) // Orange
                }
                else -> {
                    tvRol.setTextColor(Color.parseColor("#4CAF50")) // Green
                }
            }

            // Configurar el fondo dependiendo de la posición usando colores directos
            when (position) {
                0 -> {
                    // Primer lugar - dorado
                    root.setCardBackgroundColor(Color.parseColor("#FFD700"))
                    tvPosicion.setTextColor(Color.WHITE)
                }
                1 -> {
                    // Segundo lugar - plateado
                    root.setCardBackgroundColor(Color.parseColor("#C0C0C0"))
                    tvPosicion.setTextColor(Color.WHITE)
                }
                2 -> {
                    // Tercer lugar - bronce
                    root.setCardBackgroundColor(Color.parseColor("#CD7F32"))
                    tvPosicion.setTextColor(Color.WHITE)
                }
                else -> {
                    // Resto de posiciones
                    root.setCardBackgroundColor(Color.WHITE)
                    tvPosicion.setTextColor(Color.BLACK)
                }
            }

            // Mostrar handle de drag usando un icono del sistema
            ivDragHandle.visibility = View.VISIBLE
            ivDragHandle.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            ivDragHandle.setColorFilter(Color.GRAY)

            // Cargar imagen de perfil si existe
            try {
                if (usuario.urlImagenPerfil.isNotEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(usuario.urlImagenPerfil)
                        .placeholder(R.drawable.img_perfil)
                        .circleCrop()
                        .into(ivPerfil)
                } else {
                    ivPerfil.setImageResource(R.drawable.img_perfil)
                }
            } catch (e: Exception) {
                ivPerfil.setImageResource(R.drawable.img_perfil)
            }
        }
    }

    override fun getItemCount(): Int = usuarios.size
}