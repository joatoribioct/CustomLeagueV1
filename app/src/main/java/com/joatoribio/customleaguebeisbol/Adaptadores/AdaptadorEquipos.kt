package com.joatoribio.customleaguebeisbol.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joatoribio.customleaguebeisbol.Modelo.ModeloEquipos
import com.joatoribio.customleaguebeisbol.RvListennerEquipos
import com.joatoribio.customleaguebeisbol.databinding.ItemEquiposInicioBinding

class AdaptadorEquipos(
    private val context: Context,
    private val listaEquipos: List<ModeloEquipos>,
    private val rvListennerEquipos: RvListennerEquipos
) : RecyclerView.Adapter<AdaptadorEquipos.HolderEquipos>() {

    private lateinit var binding: ItemEquiposInicioBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderEquipos {
        binding = ItemEquiposInicioBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderEquipos(binding.root)
    }

    override fun onBindViewHolder(holder: HolderEquipos, position: Int) {
        val modelEquipos = listaEquipos[position]

        val iconId = modelEquipos.iconId
        val nombreEquipo = modelEquipos.equioosId

        holder.equiposIconosIv.setImageResource(iconId)
        holder.tvEquiposNombres.text = nombreEquipo

        holder.itemView.setOnClickListener {
            rvListennerEquipos.onEquipoClick(modelEquipos)
        }
    }

    override fun getItemCount(): Int {
        return listaEquipos.size
    }

    inner class HolderEquipos(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var equiposIconosIv = binding.equiposIconosIv
        var tvEquiposNombres = binding.tvEquiposNombres
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    rvListennerEquipos.onEquipoClick(listaEquipos[position])
                }
            }
        }

    }

}