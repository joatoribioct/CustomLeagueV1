package com.joatoribio.customleaguebeisbol

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.joatoribio.customleaguebeisbol.databinding.ActivityRegistrarEmailBinding

class Registrar_email : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrarEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)
        // cuuando se da click en btnRegistrar se llama la funcion validar info
        binding.btnRegistrar.setOnClickListener {
            validarInfo()
        }
    }

    //incializa la variable como string vacias
    private var email = ""
    private var passaword = ""
    private var r_Password = ""

    private fun validarInfo() {
        email = binding.etEmail.text.toString()
            .trim()//le introduce a la variable email lo que el usuario escriba en el etEmail y to.string lo convierte a string y .trim elimina los spacion que dejo el usuario
        passaword = binding.etPassword.text.toString().trim()
        r_Password = binding.etRPassword.text.toString().trim()
        //esta primer if valida que email sea valido es decir que tenga los componentes de email (.com y @)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email invalido"
            binding.etEmail.requestFocus()
        }//segunda condicion valida que el campo correo no este vacio
        else if (email.isEmpty()) {
            binding.etEmail.error = "Ingrese email"
            binding.etEmail.requestFocus()
        }//tercera condicion valida que la contraseña no este vacia
        else if (passaword.isEmpty()) {
            binding.etPassword.error = "Ingrese password"
            binding.etPassword.requestFocus()
        }//cuarta condicion valida que este repitiendo el password en el campo
        else if (r_Password.isEmpty()) {
            binding.etRPassword.error = "Repita el password"
            binding.etRPassword.requestFocus()
        }//cuarta condicion valida que conicidan los dos password
        else if (passaword != r_Password) {
            binding.etRPassword.error = "No coinciden"
            binding.etRPassword.requestFocus()
        }//si no falto nada entonces se llama la funcion registar usuario
        else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando... cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, passaword)
            .addOnSuccessListener {
                llenarInfBd()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registro el usuario debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun llenarInfBd() {
        progressDialog.setMessage("Guardando Informacion")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid

        // Generar ID Gaming predeterminado basado en el email
        val idGamingPredeterminado = generarIdGamingPredeterminado(emailUsuario)

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = ""
        hashMap["idGaming"] = idGamingPredeterminado  // NUEVO CAMPO
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Email"
        hashMap["escribiendo"] = ""
        hashMap["tiempo"] = tiempo
        hashMap["online"] = true
        hashMap["email"] = "${emailUsuario}"
        hashMap["uid"] = "${uidUsuario}"
        hashMap["fecha_nac"] = ""
        hashMap["rol"] = "Participante"

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidUsuario!!).setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se registro el usuario debido a ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Genera un ID Gaming predeterminado basado en el email del usuario
     */
    private fun generarIdGamingPredeterminado(email: String?): String {
        return if (!email.isNullOrEmpty()) {
            // Usar la parte antes del @ del email + número aleatorio
            val emailPrefix = email.substringBefore("@").take(8)
            "${emailPrefix}${(1000..9999).random()}"
        } else {
            // Generar ID completamente aleatorio
            "Player${(10000..99999).random()}"
        }
    }
}