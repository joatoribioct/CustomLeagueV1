package com.joatoribio.customleaguebeisbol.Fragmentos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.Constantes
import com.joatoribio.customleaguebeisbol.EditarPerfil
import com.joatoribio.customleaguebeisbol.OpcionesLogin
import com.joatoribio.customleaguebeisbol.R
import com.joatoribio.customleaguebeisbol.databinding.FragmentCuentaBinding

class FragmentCuenta : Fragment() {

    private lateinit var binding: FragmentCuentaBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mContext: Context
    // aqui se crea un contexto del fragmente acutal ya que no se puede utliziar THIS o appliaccion context en un fragment
    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }
    //funcion que infla la vista del fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //se crea la vista el fragmente actual
        binding = FragmentCuentaBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //se crea la instanaciona de firebase aut, para poder acceder a firebaseAuth.signOut() mas abajo
        firebaseAuth = FirebaseAuth.getInstance()


        leerInfo()

        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(mContext, EditarPerfil::class.java))
        }


        // aqui se indidca que hace el boton cerrar sesion al ser presionado, el cual finaliza el login y envia a opciones lgin
        binding.btnCerrarSesion.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(mContext, OpcionesLogin::class.java))
            activity?.finishAffinity()
        }
    }

    private fun leerInfo() {
        // se trae una referencia a la base de datos de firebase
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")//del usuario que inicio sesion
            .addValueEventListener(object : ValueEventListener {
                // la informacion de la base de datos se estan guardando en variables
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val idGaming = "${snapshot.child("idGaming").value}"  // NUEVO CAMPO
                    val email = "${snapshot.child("email").value}"
                    val fech_nac = "${snapshot.child("fecha_nac").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"
                    var tiempo = "${snapshot.child("tiempo").value}"
                    val telefono = "${snapshot.child("telefono").value}"
                    val codigoTelefono = "${snapshot.child("codigoTelefono").value}"
                    val proveedor = "${snapshot.child("proveedor").value}"

                    val codigto_telefono = codigoTelefono+telefono

                    if (tiempo == "null"){
                        tiempo = "0"
                    }

                    val for_tiempo = Constantes.obtenerFecha(tiempo.toLong())

                    // CAMBIO: Mostrar ID Gaming en lugar del nombre y email
                    val idGamingMostrar = if (idGaming == "null" || idGaming.isEmpty()) {
                        // Si no tiene ID Gaming, generar uno por defecto
                        generarIdGamingPorDefecto(nombres, email)
                    } else {
                        idGaming
                    }

                    //seteamos informacion en la vista - CAMBIOS AQUÍ
                    binding.tvIdGaming.text = idGamingMostrar  // Mostrar ID Gaming prominentemente
                    binding.tvEmail.text = email  // Mantener email oculto pero accesible
                    binding.tvNombres.text = if (nombres == "null" || nombres.isEmpty()) "No especificado" else nombres
                    binding.tvFechaNacimiento.text = fech_nac
                    binding.tvMiembro.text = for_tiempo
                    binding.tvTelfono.text = codigto_telefono

                    // Si el ID Gaming fue generado por defecto, guardarlo en la base de datos
                    if (idGaming == "null" || idGaming.isEmpty()) {
                        actualizarIdGamingEnFirebase(idGamingMostrar)
                    }

                    //seato la imagen de perfil // tenemos que tener instalada la libreria glide
                    try {
                        Glide.with(mContext).load(imagen).placeholder(R.drawable.img_perfil).into(binding.ivPerfil)

                    }catch (e: Exception){
                        Toast.makeText(mContext, "Error al cargar la imagen debido a :${e.message} ", Toast.LENGTH_SHORT).show()
                    }
                    //verificamos si esta registrado con email o con google y si estan verificada
                    if (proveedor == "Email"){
                        val esVerificado = firebaseAuth.currentUser!!.isEmailVerified
                        if (esVerificado){
                            binding.tvEstadoCuenta.text = "Verificado"
                        }else{
                            binding.tvEstadoCuenta.text = "No Verificado"
                        }
                    }else{
                        binding.tvEstadoCuenta.text = "Verificado"
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    /**
     * Genera un ID Gaming por defecto para usuarios existentes que no lo tienen
     */
    private fun generarIdGamingPorDefecto(nombres: String, email: String): String {
        return when {
            nombres != "null" && nombres.isNotEmpty() -> {
                val nombreLimpio = nombres.replace(" ", "").take(8)
                "${nombreLimpio}${(1000..9999).random()}"
            }
            email != "null" && email.isNotEmpty() -> {
                val emailPrefix = email.substringBefore("@").take(8)
                "${emailPrefix}${(1000..9999).random()}"
            }
            else -> {
                "Player${(10000..99999).random()}"
            }
        }
    }

    /**
     * Actualiza el ID Gaming en Firebase para usuarios existentes
     */
    private fun actualizarIdGamingEnFirebase(nuevoIdGaming: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}").child("idGaming").setValue(nuevoIdGaming)
            .addOnSuccessListener {
                // ID Gaming actualizado exitosamente
            }
            .addOnFailureListener { e ->
                Toast.makeText(mContext, "Error al actualizar ID Gaming: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}