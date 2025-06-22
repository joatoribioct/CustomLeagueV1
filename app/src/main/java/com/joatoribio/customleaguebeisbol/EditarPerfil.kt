package com.joatoribio.customleaguebeisbol

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.hbb20.CountryCodePicker
import com.joatoribio.customleaguebeisbol.databinding.ActivityEditarPerfilBinding
import java.util.*

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var imagenUri: Uri? = null

    // Variables para almacenar datos actuales
    private var nombreActual = ""
    private var idGamingActual = ""
    private var fechaNacActual = ""
    private var telefonoActual = ""
    private var codigoTelefonoActual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        leerInfo()

        // Listeners para los botones
        binding.fabCambiarImg.setOnClickListener {
            seleccionarImagen()
        }

        binding.btnSugerencias.setOnClickListener {
            mostrarSugerenciasIdGaming()
        }

        binding.btnActualizar.setOnClickListener {
            validarInfo()
        }

        // Configurar el selector de código de país
        binding.selectorCod.setOnCountryChangeListener {
            codigoTelefonoActual = binding.selectorCod.selectedCountryCode
        }
    }

    private fun leerInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Leer todos los datos actuales
                    nombreActual = "${snapshot.child("nombres").value}"
                    idGamingActual = "${snapshot.child("idGaming").value}"
                    fechaNacActual = "${snapshot.child("fecha_nac").value}"
                    telefonoActual = "${snapshot.child("telefono").value}"
                    codigoTelefonoActual = "${snapshot.child("codigoTelefono").value}"
                    val imagen = "${snapshot.child("urlImagenPerfil").value}"

                    // Llenar los campos con la información actual
                    binding.etNombres.setText(if (nombreActual == "null") "" else nombreActual)

                    // Campo ID Gaming - generar uno si no existe
                    val idGamingMostrar = if (idGamingActual == "null" || idGamingActual.isEmpty()) {
                        val nuevoId = generarIdGamingPorDefecto()
                        idGamingActual = nuevoId
                        // Guardar el nuevo ID en Firebase
                        ref.child("${firebaseAuth.uid}").child("idGaming").setValue(nuevoId)
                        nuevoId
                    } else {
                        idGamingActual
                    }
                    binding.etIdGaming.setText(idGamingMostrar)

                    binding.etFechaNacimiento.setText(if (fechaNacActual == "null") "" else fechaNacActual)
                    binding.etTelefono.setText(if (telefonoActual == "null") "" else telefonoActual)

                    // Configurar código de país
                    if (codigoTelefonoActual != "null" && codigoTelefonoActual.isNotEmpty()) {
                        binding.selectorCod.setCountryForPhoneCode(codigoTelefonoActual.toIntOrNull() ?: 1)
                    }

                    // Cargar imagen de perfil
                    try {
                        Glide.with(this@EditarPerfil)
                            .load(imagen)
                            .placeholder(R.drawable.img_perfil)
                            .into(binding.imgPerfil)
                    } catch (e: Exception) {
                        Toast.makeText(this@EditarPerfil, "Error al cargar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditarPerfil, "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        resultadoImagenARL.launch(intent)
    }

    private val resultadoImagenARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            imagenUri = data?.data

            // Mostrar la imagen seleccionada
            try {
                Glide.with(this)
                    .load(imagenUri)
                    .placeholder(R.drawable.img_perfil)
                    .into(binding.imgPerfil)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al mostrar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validarInfo() {
        val nombres = binding.etNombres.text.toString().trim()
        val idGaming = binding.etIdGaming.text.toString().trim()
        val fechaNac = binding.etFechaNacimiento.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()

        // Validar ID Gaming
        if (idGaming.isEmpty()) {
            binding.etIdGaming.error = "El ID Gaming es obligatorio"
            binding.etIdGaming.requestFocus()
            return
        }

        if (idGaming.length < 3) {
            binding.etIdGaming.error = "Mínimo 3 caracteres"
            binding.etIdGaming.requestFocus()
            return
        }

        if (idGaming.length > 20) {
            binding.etIdGaming.error = "Máximo 20 caracteres"
            binding.etIdGaming.requestFocus()
            return
        }

        if (!idGaming.matches("[a-zA-Z0-9]+".toRegex())) {
            binding.etIdGaming.error = "Solo letras y números"
            binding.etIdGaming.requestFocus()
            return
        }

        // Verificar si el ID Gaming cambió y si es único
        if (idGaming != idGamingActual) {
            verificarIdGamingUnico(idGaming) { esUnico ->
                if (esUnico) {
                    procederConActualizacion(nombres, idGaming, fechaNac, telefono)
                } else {
                    binding.etIdGaming.error = "Este ID Gaming ya está en uso"
                    binding.etIdGaming.requestFocus()
                }
            }
        } else {
            // Si no cambió el ID Gaming, proceder directamente
            procederConActualizacion(nombres, idGaming, fechaNac, telefono)
        }
    }

    private fun procederConActualizacion(nombres: String, idGaming: String, fechaNac: String, telefono: String) {
        if (imagenUri == null) {
            // Solo actualizar datos sin imagen
            actualizarSinImagen(nombres, idGaming, fechaNac, telefono)
        } else {
            // Subir imagen y actualizar datos
            subirImagenYActualizar(nombres, idGaming, fechaNac, telefono)
        }
    }

    private fun verificarIdGamingUnico(idGaming: String, callback: (Boolean) -> Unit) {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios")

        usuariosRef.orderByChild("idGaming").equalTo(idGaming)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var esUnico = true

                    for (usuarioSnapshot in snapshot.children) {
                        val uid = usuarioSnapshot.child("uid").value.toString()
                        if (uid != firebaseAuth.uid) {
                            esUnico = false
                            break
                        }
                    }

                    callback(esUnico)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(true) // En caso de error, permitir la actualización
                }
            })
    }

    private fun actualizarSinImagen(nombres: String, idGaming: String, fechaNac: String, telefono: String) {
        progressDialog.setMessage("Actualizando información...")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = nombres
        hashMap["idGaming"] = idGaming
        hashMap["fecha_nac"] = fechaNac
        hashMap["telefono"] = telefono
        hashMap["codigoTelefono"] = binding.selectorCod.selectedCountryCode

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}").updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Información actualizada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun subirImagenYActualizar(nombres: String, idGaming: String, fechaNac: String, telefono: String) {
        progressDialog.setMessage("Subiendo imagen...")
        progressDialog.show()

        val nombreArchivo = "imagen_perfil_${firebaseAuth.uid}"
        val storageRef = FirebaseStorage.getInstance().getReference("ImagenesPerfil/$nombreArchivo")

        storageRef.putFile(imagenUri!!)
            .addOnSuccessListener { task ->
                task.storage.downloadUrl.addOnSuccessListener { uri ->
                    val urlImagen = uri.toString()

                    val hashMap = HashMap<String, Any>()
                    hashMap["nombres"] = nombres
                    hashMap["idGaming"] = idGaming
                    hashMap["fecha_nac"] = fechaNac
                    hashMap["telefono"] = telefono
                    hashMap["codigoTelefono"] = binding.selectorCod.selectedCountryCode
                    hashMap["urlImagenPerfil"] = urlImagen

                    val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
                    ref.child("${firebaseAuth.uid}").updateChildren(hashMap)
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarSugerenciasIdGaming() {
        val sugerencias = generarSugerenciasIdGaming()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("💡 Sugerencias de ID Gaming")
        builder.setMessage("Selecciona una de estas opciones o úsalas como inspiración:")

        val opciones = sugerencias.toTypedArray()
        builder.setItems(opciones) { dialog, which ->
            binding.etIdGaming.setText(opciones[which])
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun generarIdGamingPorDefecto(): String {
        val email = firebaseAuth.currentUser?.email ?: ""

        return when {
            nombreActual.isNotEmpty() && nombreActual != "null" -> {
                val nombreLimpio = nombreActual.replace(" ", "").take(8)
                "${nombreLimpio}${(1000..9999).random()}"
            }
            email.isNotEmpty() -> {
                val emailPrefix = email.substringBefore("@").take(8)
                "${emailPrefix}${(1000..9999).random()}"
            }
            else -> {
                "Player${(10000..99999).random()}"
            }
        }
    }

    private fun generarSugerenciasIdGaming(): List<String> {
        val sugerencias = mutableListOf<String>()
        val email = firebaseAuth.currentUser?.email ?: ""

        // Sugerencias basadas en nombre
        if (nombreActual.isNotEmpty() && nombreActual != "null") {
            val nombreLimpio = nombreActual.replace(" ", "").take(10)
            sugerencias.add("${nombreLimpio}${(100..999).random()}")
            sugerencias.add("${nombreLimpio}Pro")
            sugerencias.add("${nombreLimpio}${(10..99).random()}")
        }

        // Sugerencias basadas en email
        if (email.isNotEmpty()) {
            val emailPrefix = email.substringBefore("@").take(10)
            sugerencias.add("${emailPrefix}${(100..999).random()}")
            sugerencias.add("${emailPrefix}Player")
        }

        // Sugerencias genéricas
        sugerencias.add("Gamer${(1000..9999).random()}")
        sugerencias.add("Pro${(100..999).random()}")
        sugerencias.add("Legend${(100..999).random()}")
        sugerencias.add("MVP${(1000..9999).random()}")

        return sugerencias.take(6) // Devolver solo las primeras 6 sugerencias
    }
}