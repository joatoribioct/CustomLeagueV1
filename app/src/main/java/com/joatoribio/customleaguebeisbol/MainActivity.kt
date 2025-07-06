package com.joatoribio.customleaguebeisbol

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.joatoribio.customleaguebeisbol.Fragmentos.FragmentCuenta
import com.joatoribio.customleaguebeisbol.Fragmentos.FragmentInicio
import com.joatoribio.customleaguebeisbol.Fragmentos.FragmentMiEquipo
import com.joatoribio.customleaguebeisbol.Fragmentos.FragmentOrdenTurnos
import com.joatoribio.customleaguebeisbol.Ligas.CrearLiga
import com.joatoribio.customleaguebeisbol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    // 🆕 NUEVA VARIABLE para reutilizar FragmentInicio
    private var fragmentInicio: FragmentInicio? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        comprobarSesion()

        //primer fragment en iniciar
        verFragmentInicio()

        //evento en cada boton del bottomNV
        binding.bottomNv.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.item_Inicio -> {
                    verFragmentInicio()
                    true
                }
                R.id.item_OrdenTurnos -> {
                    verFragmentOrdenTurnos()
                    true
                }
                R.id.item_Mi_eqquipo -> {
                    verFragmentMiEquipo()
                    true
                }
                R.id.item_Cuenta -> {
                    verFragmentCuenta()
                    true
                }
                else -> {
                    false
                }
            }
        }

        binding.fab.setOnClickListener {
            comprobarAdminOparticipante()
        }
    }

    private fun comprobarAdminOparticipante() {
        val uid = firebaseAuth.currentUser?.uid
        val database = FirebaseDatabase.getInstance().getReference("Usuarios")

        database.child(uid!!).get().addOnSuccessListener { snapshot ->
            val rol = snapshot.child("rol").value.toString()
            if (rol == "admin") {
                startActivity(Intent(this, CrearLiga::class.java))
            } else {
                Toast.makeText(this, "No eres admin", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al obtener rol", Toast.LENGTH_SHORT).show()
        }
    }

    private fun comprobarSesion() {
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, OpcionesLogin::class.java))
            finishAffinity()
        }
    }

    /**
     * ✏️ MODIFICADO: Reutilizar FragmentInicio para preservar temporizador
     */
    private fun verFragmentInicio() {
        binding.tituloRL.text = "Inicio"

        // Buscar fragmento existente primero
        val existingFragment = supportFragmentManager.findFragmentByTag("FragmentInicio")

        if (existingFragment is FragmentInicio && existingFragment.isAdded) {
            // Fragment ya existe, no crear uno nuevo
            Log.d("NAVIGATION", "♻️ Reutilizando FragmentInicio existente")
            fragmentInicio = existingFragment
            return
        }

        // Solo crear si no existe
        Log.d("NAVIGATION", "🆕 Creando nuevo FragmentInicio")
        fragmentInicio = FragmentInicio()

        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.fragmetLayoutL1.id, fragmentInicio!!, "FragmentInicio")
        fragmentTransition.commit()
    }

    /**
     * 📋 MANTENER exactamente como estaba
     */
    private fun verFragmentOrdenTurnos() {
        binding.tituloRL.text = "Turnos"
        val fragment = FragmentOrdenTurnos()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.fragmetLayoutL1.id, fragment, "FragmentOrdenTurnos")
        fragmentTransition.commit()
    }

    /**
     * 📋 MANTENER exactamente como estaba
     */
    private fun verFragmentMiEquipo() {
        binding.tituloRL.text = "Mi Equipo"
        val fragment = FragmentMiEquipo()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.fragmetLayoutL1.id, fragment, "FragmentMiEquipo")
        fragmentTransition.commit()
    }

    /**
     * 📋 MANTENER exactamente como estaba
     */
    private fun verFragmentCuenta() {
        binding.tituloRL.text = "Cuenta"
        val fragment = FragmentCuenta()
        val fragmentTransition = supportFragmentManager.beginTransaction()
        fragmentTransition.replace(binding.fragmetLayoutL1.id, fragment, "FragmentCuenta")
        fragmentTransition.commit()
    }
}