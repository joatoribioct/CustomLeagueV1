package com.joatoribio.customleaguebeisbol.Opciones_login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.joatoribio.customleaguebeisbol.MainActivity
import com.joatoribio.customleaguebeisbol.Registrar_email
import com.joatoribio.customleaguebeisbol.databinding.ActivityLoginEmailBinding

class Login_email : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.btnIngresar.setOnClickListener {
            validarInfo()
        }

        binding.txtRegistrar.setOnClickListener {
            startActivity(Intent(this@Login_email, Registrar_email::class.java))
        }
    }

    private var email = ""
    private var password = ""

    private fun validarInfo() {
        email = binding.etEmail.text.toString().trim()
        password = binding.etPassword.text.toString().trim()

        //esta primer if valida que email sea valido es decir que tenga los componentes de email (.com y @)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email invalido"
            binding.etEmail.requestFocus()
        }//segunda condicion valida que el campo correo no este vacio
        else if (email.isEmpty()) {
            binding.etEmail.error = "Ingrese email"
            binding.etEmail.requestFocus()
        }//tercera condicion valida que la contraseña no este vacia
        else if (password.isEmpty()) {
            binding.etPassword.error = "Ingrese password"
            binding.etPassword.requestFocus()
        }
        else {
            loginUsuario()
        }
    }

    private fun loginUsuario() {
        progressDialog.setMessage("Ingresando...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                progressDialog.dismiss()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
                Toast.makeText(
                    this,
                    "Bienvenido(a)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se ha podido iniciar sesion debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
    }
}