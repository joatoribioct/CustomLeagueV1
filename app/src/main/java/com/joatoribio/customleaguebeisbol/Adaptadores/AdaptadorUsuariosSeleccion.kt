package com.joatoribio.customleaguebeisbol.Adaptadores

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joatoribio.customleaguebeisbol.Modelo.UsuarioParticipante
import com.joatoribio.customleaguebeisbol.R

class AdaptadorUsuariosSeleccion(
    private val usuarios: List<UsuarioParticipante>,
    private val usuariosSeleccionados: MutableSet<String>,
    private val adminUid: String,
    private val listener: OnUsuarioSeleccionListener
) : RecyclerView.Adapter<AdaptadorUsuariosSeleccion.UsuarioViewHolder>() {

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_usuario)
        val ivPerfil: ImageView = itemView.findViewById(R.id.iv_perfil_usuario)
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_usuario)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_email_usuario)
        val tvEstado: TextView = itemView.findViewById(R.id.tv_estado_usuario)
        val checkBoxSeleccion: CheckBox = itemView.findViewById(R.id.checkbox_seleccion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_seleccion, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        val esAdmin = usuario.uid == adminUid

        // Configurar datos del usuario
        holder.tvNombre.text = usuario.nombre
        holder.tvEmail.text = usuario.email

        // Cargar imagen de perfil
        if (usuario.urlImagenPerfil.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(usuario.urlImagenPerfil)
                .placeholder(R.drawable.default_user_avatar)
                .circleCrop()
                .into(holder.ivPerfil)
        } else {
            holder.ivPerfil.setImageResource(R.drawable.default_user_avatar)
        }

        // Configurar estado y selección
        if (esAdmin) {
            holder.tvEstado.text = "👑 Administrador"
            holder.tvEstado.setTextColor(Color.parseColor("#FF9800"))
            holder.checkBoxSeleccion.isChecked = true
            holder.checkBoxSeleccion.isEnabled = false // Admin siempre seleccionado
            usuariosSeleccionados.add(usuario.uid)
        } else {
            holder.tvEstado.text = if (usuario.activo) "🟢 Activo" else "🔴 Inactivo"
            holder.tvEstado.setTextColor(
                if (usuario.activo) Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )
            holder.checkBoxSeleccion.isChecked = usuariosSeleccionados.contains(usuario.uid)
            holder.checkBoxSeleccion.isEnabled = usuario.activo
        }

        // Configurar apariencia de la card según selección
        configurarAparienciaCard(holder, holder.checkBoxSeleccion.isChecked, esAdmin)

        // Configurar listeners
        holder.checkBoxSeleccion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                usuariosSeleccionados.add(usuario.uid)
            } else {
                usuariosSeleccionados.remove(usuario.uid)
            }
            configurarAparienciaCard(holder, isChecked, esAdmin)
            listener.onUsuarioSeleccionado(usuario, isChecked, usuariosSeleccionados.size)
        }

        holder.cardView.setOnClickListener {
            if (!esAdmin && usuario.activo) {
                holder.checkBoxSeleccion.toggle()
            }
        }
    }

    override fun getItemCount(): Int = usuarios.size

    private fun configurarAparienciaCard(holder: UsuarioViewHolder, seleccionado: Boolean, esAdmin: Boolean) {
        when {
            esAdmin -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.cardView.cardElevation = 6f
            }
            seleccionado -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E8"))
                holder.cardView.cardElevation = 4f
            }
            else -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                holder.cardView.cardElevation = 2f
            }
        }
    }

    /**
     * Obtiene la lista de usuarios seleccionados
     */
    fun getUsuariosSeleccionados(): List<UsuarioParticipante> {
        return usuarios.filter { usuariosSeleccionados.contains(it.uid) }
    }

    /**
     * Selecciona todos los usuarios activos
     */
    fun seleccionarTodos() {
        usuarios.forEach { usuario ->
            if (usuario.activo) {
                usuariosSeleccionados.add(usuario.uid)
            }
        }
        notifyDataSetChanged()
        listener.onSeleccionarTodos(usuariosSeleccionados.size)
    }

    /**
     * Deselecciona todos los usuarios (excepto admin)
     */
    fun deseleccionarTodos() {
        usuariosSeleccionados.clear()
        usuariosSeleccionados.add(adminUid) // Mantener admin seleccionado
        notifyDataSetChanged()
        listener.onDeseleccionarTodos(usuariosSeleccionados.size)
    }
}

interface OnUsuarioSeleccionListener {
    fun onUsuarioSeleccionado(usuario: UsuarioParticipante, seleccionado: Boolean, totalSeleccionados: Int)
    fun onSeleccionarTodos(totalSeleccionados: Int)
    fun onDeseleccionarTodos(totalSeleccionados: Int)
}