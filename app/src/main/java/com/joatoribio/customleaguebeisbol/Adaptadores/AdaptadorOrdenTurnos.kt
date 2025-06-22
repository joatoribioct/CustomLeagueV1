package com.joatoribio.customleaguebeisbol.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.Fragmentos.FragmentOrdenTurnos
import com.joatoribio.customleaguebeisbol.Modelo.UsuarioParticipante
import com.joatoribio.customleaguebeisbol.R

class AdaptadorOrdenTurnos(
    private val usuarios: List<UsuarioParticipante>,
    private val uidUsuarioActual: String,
    private val esAdmin: Boolean = false  // NUEVO: Flag para funciones de admin
) : RecyclerView.Adapter<AdaptadorOrdenTurnos.HolderOrdenTurnos>() {

    inner class HolderOrdenTurnos(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: CardView = itemView as CardView
        val tvPosicion: TextView = itemView.findViewById(R.id.tvPosicion)
        val tvNombreUsuario: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvEstadoUsuario: TextView = itemView.findViewById(R.id.tvEstadoUsuario)
        val ivIndicadorUsuario: ImageView = itemView.findViewById(R.id.ivIndicadorUsuario)
        val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderOrdenTurnos {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_orden_turno, parent, false)
        return HolderOrdenTurnos(view)
    }

    override fun onBindViewHolder(holder: HolderOrdenTurnos, position: Int) {
        val usuario = usuarios[position]

        with(holder) {
            // Mostrar posición en el orden (empezando desde 1)
            tvPosicion.text = "${position + 1}"

            // CAMBIO PRINCIPAL: Mostrar ID Gaming en lugar del nombre
            val identificador = usuario.getIdentificadorMostrar()
            tvNombreUsuario.text = identificador

            // Indicador visual si es el usuario actual
            if (usuario.uid == uidUsuarioActual) {
                tvNombreUsuario.text = "$identificador (Tú)"
                tvNombreUsuario.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                tvNombreUsuario.textSize = 16f
                tvNombreUsuario.setTypeface(null, android.graphics.Typeface.BOLD)

                // Agregar icono o indicador visual
                ivIndicadorUsuario.visibility = View.VISIBLE
                ivIndicadorUsuario.setImageResource(R.drawable.ic_person_pin)
            } else {
                tvNombreUsuario.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                tvNombreUsuario.textSize = 14f
                tvNombreUsuario.setTypeface(null, android.graphics.Typeface.NORMAL)
                ivIndicadorUsuario.visibility = View.GONE
            }

            // Mostrar estado del usuario
            when {
                !usuario.activo -> {
                    tvEstadoUsuario.text = "Inactivo"
                    tvEstadoUsuario.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                    root.alpha = 0.6f
                }
                usuario.rol == "Administrador" -> {
                    tvEstadoUsuario.text = "Admin"
                    tvEstadoUsuario.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                    root.alpha = 1.0f
                }
                else -> {
                    tvEstadoUsuario.text = "Activo"
                    tvEstadoUsuario.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                    root.alpha = 1.0f
                }
            }

            // Configurar el fondo dependiendo de la posición
            when (position) {
                0 -> {
                    // Primer lugar - dorado
                    root.setBackgroundResource(R.drawable.bg_primer_lugar)
                    tvPosicion.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                1 -> {
                    // Segundo lugar - plateado
                    root.setBackgroundResource(R.drawable.bg_segundo_lugar)
                    tvPosicion.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                2 -> {
                    // Tercer lugar - bronce
                    root.setBackgroundResource(R.drawable.bg_tercer_lugar)
                    tvPosicion.setTextColor(holder.itemView.context.getColor(android.R.color.white))
                }
                else -> {
                    // Resto de posiciones
                    root.setBackgroundResource(R.drawable.bg_item_normal)
                    tvPosicion.setTextColor(holder.itemView.context.getColor(android.R.color.black))
                }
            }

            // NUEVO: Configurar click en el ícono para admin (activar/desactivar usuario)
            if (esAdmin) {
                ivMenu.visibility = View.VISIBLE

                if (usuario.uid == uidUsuarioActual) {
                    // Para el admin mismo, mostrar ícono de admin
                    ivMenu.setImageResource(R.drawable.ic_admin_setting)
                    ivMenu.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
                } else {
                    // Para otros usuarios, mostrar estado clickeable
                    ivMenu.setImageResource(if (usuario.activo) R.drawable.ic_check_circle else R.drawable.ic_cancel)
                    ivMenu.setColorFilter(
                        if (usuario.activo)
                            holder.itemView.context.getColor(android.R.color.holo_green_dark)
                        else
                            holder.itemView.context.getColor(android.R.color.holo_red_dark)
                    )
                }

                // Click para cambiar estado (solo si no es el admin mismo)
                ivMenu.setOnClickListener {
                    if (usuario.uid != uidUsuarioActual) {
                        // Llamar al fragmento para cambiar estado
                        val fragment = (holder.itemView.context as? androidx.fragment.app.FragmentActivity)
                            ?.supportFragmentManager
                            ?.findFragmentById(R.id.fragmet_LayoutL1) as? FragmentOrdenTurnos

                        fragment?.cambiarEstadoUsuario(position)
                    }
                }
            } else {
                // Para participantes normales, ocultar el ícono de menú
                ivMenu.visibility = View.GONE
            }

            // ELIMINADO: Click listener para mostrar información detallada
            // Ahora no hay funcionalidad de click en las tarjetas
            root.setOnClickListener(null)
            root.isClickable = false
        }
    }

    override fun getItemCount(): Int = usuarios.size
}