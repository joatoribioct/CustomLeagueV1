package com.joatoribio.customleaguebeisbol

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.joatoribio.customleaguebeisbol.Opciones_login.Login_email
import com.joatoribio.customleaguebeisbol.databinding.ActivityOpcionesLoginBinding

class OpcionesLogin : AppCompatActivity() {
    private lateinit var binding: ActivityOpcionesLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient : GoogleSignInClient
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpcionesLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)


        binding.ingresarEmail.setOnClickListener {
            startActivity(Intent(this@OpcionesLogin, Login_email::class.java))
        }

        binding.ingresarGoogle.setOnClickListener {
            googleLogin()
        }
    }

    private fun googleLogin() {
        val googleSingIntent = mGoogleSignInClient.signInIntent
        googleSignInARL.launch(googleSingIntent)
    }

    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ resultado->
        if (resultado.resultCode == RESULT_OK){
            val data = resultado.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val cuenta = task.getResult(ApiException::class.java)
                autenticacionGoogle(cuenta.idToken)
            }catch (e:Exception){
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun autenticacionGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {resultadoAuth->
                if (resultadoAuth.additionalUserInfo!!.isNewUser){
                    llenarInfoBd()
                }else{
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
            .addOnFailureListener {e->
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun llenarInfoBd() {
        progressDialog.setMessage("Guardando Informacion")

        val tiempo = Constantes.obtenerTiempoDis()
        val emailUsuario = firebaseAuth.currentUser!!.email
        val uidUsuario = firebaseAuth.uid
        val nombreUsuario = firebaseAuth.currentUser?.displayName

        // Generar ID Gaming predeterminado basado en el nombre o email
        val idGamingPredeterminado = generarIdGamingPredeterminado(nombreUsuario, emailUsuario)

        val hashMap = HashMap<String, Any>()
        hashMap["nombres"] = "${nombreUsuario}"
        hashMap["idGaming"] = idGamingPredeterminado  // NUEVO CAMPO
        hashMap["codigoTelefono"] = ""
        hashMap["telefono"] = ""
        hashMap["urlImagenPerfil"] = ""
        hashMap["proveedor"] = "Google"
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
     * Genera un ID Gaming predeterminado basado en el nombre del usuario o email
     */
    private fun generarIdGamingPredeterminado(nombre: String?, email: String?): String {
        return when {
            !nombre.isNullOrEmpty() -> {
                // Usar las primeras letras del nombre + número aleatorio
                val nombreLimpio = nombre.replace(" ", "").take(8)
                "${nombreLimpio}${(1000..9999).random()}"
            }
            !email.isNullOrEmpty() -> {
                // Usar la parte antes del @ del email + número aleatorio
                val emailPrefix = email.substringBefore("@").take(8)
                "${emailPrefix}${(1000..9999).random()}"
            }
            else -> {
                // Generar ID completamente aleatorio
                "Player${(10000..99999).random()}"
            }
        }
    }

    private fun comprobarSesion(){
        if (firebaseAuth.currentUser !== null){
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}