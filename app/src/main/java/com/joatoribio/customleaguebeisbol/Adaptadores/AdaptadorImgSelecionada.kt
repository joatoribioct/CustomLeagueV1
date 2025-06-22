package com.joatoribio.customleaguebeisbol.Adaptadores

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.joatoribio.customleaguebeisbol.Modelo.ModeloImgSelecionada
import com.joatoribio.customleaguebeisbol.R
import com.joatoribio.customleaguebeisbol.databinding.ItemImagenesSelecionadasBinding

class AdaptadorImgSelecionada(
    private val context: Context,
    private val imagenesSelecArrayList : ArrayList<ModeloImgSelecionada>
) : Adapter<AdaptadorImgSelecionada.HolderImagenesSelecionada>() {

    private lateinit var binding : ItemImagenesSelecionadasBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagenesSelecionada {
        binding = ItemImagenesSelecionadasBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderImagenesSelecionada(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImagenesSelecionada, position: Int) {
        val modelo = imagenesSelecArrayList[position]

        val imagenUri = modelo.imagenUri

        try {
            Glide.with(context)
                .load(imagenUri)
                .placeholder(R.drawable.item_imagen)
                .into(holder.item_imagen)

        }catch (e: Exception){

        }

        holder.btn_cerrar.setOnClickListener {
            imagenesSelecArrayList.remove(modelo)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return imagenesSelecArrayList.size
    }

    inner class HolderImagenesSelecionada(itemView: View) : ViewHolder(itemView){
        var item_imagen = binding.itemImagen
        var btn_cerrar = binding.cerrarItem
    }
}