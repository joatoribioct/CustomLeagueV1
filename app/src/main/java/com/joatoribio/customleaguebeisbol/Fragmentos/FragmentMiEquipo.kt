package com.joatoribio.customleaguebeisbol.Fragmentos

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.joatoribio.customleaguebeisbol.Adaptadores.MiEquipoAdapter
import com.joatoribio.customleaguebeisbol.Adaptadores.OnMiEquipoClickListener
import com.joatoribio.customleaguebeisbol.Modelo.LineupViewModel
import com.joatoribio.customleaguebeisbol.Modelo.ModeloDraftSelecionado
import com.joatoribio.customleaguebeisbol.databinding.FragmentMiEquipoBinding

class FragmentMiEquipo : Fragment() {

    private lateinit var binding: FragmentMiEquipoBinding
    private lateinit var mContexto: Context
    private val lineupViewModel: LineupViewModel by activityViewModels()
    private lateinit var firebaseAuth: FirebaseAuth

    // Adaptador para mostrar los lineups
    private lateinit var miEquipoAdapter: MiEquipoAdapter

    // Lista de lineups seleccionados
    private val listaLineups = mutableListOf<ModeloDraftSelecionado>()

    override fun onAttach(context: Context) {
        mContexto = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMiEquipoBinding.inflate(LayoutInflater.from(mContexto), container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        // Inicializar adaptador
        inicializarAdaptador()

        // Configurar RecyclerView
        configurarRecyclerView()

        // Configurar el observador del ViewModel
        setupViewModelObserver()

        // Actualizar UI inicial
        actualizarEstadisticas(0)
        mostrarMensajeVacioSiNecesario()
    }

    /**
     * Configurar observador del ViewModel
     */
    private fun setupViewModelObserver() {
        lineupViewModel.lineups.observe(viewLifecycleOwner) { lineups ->
            Log.d("MI_EQUIPO", "ViewModel actualizado. Total lineups: ${lineups.size}")

            // Actualizar la UI con los nuevos datos
            actualizarUIConLineups(lineups)
        }
    }

    /**
     * Inicializa el adaptador
     */
    private fun inicializarAdaptador() {
        miEquipoAdapter = MiEquipoAdapter(listaLineups, object : OnMiEquipoClickListener {
            override fun onEliminarLineup(tipo: String, position: Int) {
                mostrarDialogoEliminarLineup(tipo, position)
            }

            override fun onVerDetallesLineup(tipo: String, jugadores: Map<String, Map<String, Any>>) {
                // Aquí puedes implementar una acción para ver detalles
                val nombreTipo = obtenerNombreTipo(tipo)
                Toast.makeText(mContexto, "Viendo detalles de $nombreTipo", Toast.LENGTH_SHORT).show()
                Log.d("MI_EQUIPO", "Ver detalles de $tipo con ${jugadores.size} jugadores")
            }
        })
    }

    /**
     * Configura el RecyclerView
     */
    private fun configurarRecyclerView() {
        binding.recyclerRenglones.apply {
            layoutManager = LinearLayoutManager(mContexto)
            adapter = miEquipoAdapter
            isNestedScrollingEnabled = true
        }
    }

    /**
     * Actualiza la UI con los lineups del ViewModel
     */
    private fun actualizarUIConLineups(lineups: List<ModeloDraftSelecionado>) {
        // Ordenar lineups por tipo
        val lineupsOrdenados = lineups.sortedBy { obtenerOrdenTipo(it.tipo) }

        // Actualizar adaptador
        miEquipoAdapter.actualizarLineups(lineupsOrdenados)

        // Actualizar estadísticas
        actualizarEstadisticas(lineups.size)

        // Mostrar/ocultar mensaje vacío
        mostrarMensajeVacioSiNecesario()

        // Log para depuración
        Log.d("MI_EQUIPO", "UI actualizada con ${lineups.size} lineups")
    }

    /**
     * Actualiza las estadísticas en el header
     */
    private fun actualizarEstadisticas(cantidad: Int) {
        val estadisticas = when (cantidad) {
            0 -> "Sin lineups seleccionados"
            1 -> "1 lineup seleccionado"
            4 -> "$cantidad lineups completos ✅"
            else -> "$cantidad lineups seleccionados"
        }
        binding.tvEstadisticas.text = estadisticas
    }

    /**
     * Muestra u oculta el mensaje cuando no hay lineups
     */
    private fun mostrarMensajeVacioSiNecesario() {
        val tieneLineups = lineupViewModel.getCantidadLineups() > 0

        binding.recyclerRenglones.visibility = if (tieneLineups) View.VISIBLE else View.GONE
        binding.cardMensajeVacio.visibility = if (tieneLineups) View.GONE else View.VISIBLE
    }

    /**
     * Muestra diálogo de confirmación para eliminar lineup
     */
    private fun mostrarDialogoEliminarLineup(tipo: String, position: Int) {
        val nombreTipo = obtenerNombreTipo(tipo)

        val builder = AlertDialog.Builder(mContexto, com.google.android.material.R.style.MaterialAlertDialog_Material3)
        builder.setTitle("🗑️ Eliminar Lineup")
        builder.setMessage("¿Estás seguro de que deseas eliminar el lineup de $nombreTipo?\n\nEsto liberará el lineup para que otros usuarios puedan seleccionarlo.")

        builder.setPositiveButton("✅ Sí, eliminar") { dialog, _ ->
            dialog.dismiss()
            eliminarLineup(tipo, position)
        }

        builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    /**
     * Elimina un lineup del equipo y de Firebase
     */
    private fun eliminarLineup(tipo: String, position: Int) {
        // Eliminar del ViewModel local
        lineupViewModel.eliminarLineup(tipo)

        // Eliminar de Firebase
        eliminarLineupDeFirebase(tipo)

        // Mostrar mensaje de confirmación
        val nombreTipo = obtenerNombreTipo(tipo)
        Toast.makeText(mContexto, "Lineup de $nombreTipo eliminado", Toast.LENGTH_SHORT).show()

        Log.d("MI_EQUIPO", "Lineup $tipo eliminado")
    }

    /**
     * Elimina el lineup de Firebase para liberarlo
     */
    private fun eliminarLineupDeFirebase(tipo: String) {
        val database = FirebaseDatabase.getInstance()
        val lineupsRef = database.getReference("LineupsSeleccionados")

        lineupsRef.orderByChild("usuarioId").equalTo(firebaseAuth.uid)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    for (lineupSnapshot in snapshot.children) {
                        val lineupTipo = lineupSnapshot.child("tipoLineup").value.toString()
                        if (lineupTipo == tipo) {
                            lineupSnapshot.ref.removeValue()
                            Log.d("MI_EQUIPO", "Lineup $tipo eliminado de Firebase")
                            break
                        }
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Toast.makeText(mContexto, "Error al eliminar lineup: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Obtiene el nombre legible del tipo de lineup
     */
    private fun obtenerNombreTipo(tipo: String): String {
        return when(tipo) {
            "infield" -> "Infield"
            "outfield" -> "Outfield"
            "pitchers" -> "Pitchers"
            "relief" -> "Relief"
            else -> tipo.capitalize()
        }
    }

    /**
     * Obtiene el orden de prioridad para mostrar los tipos de lineup
     */
    private fun obtenerOrdenTipo(tipo: String): Int {
        return when (tipo) {
            "infield" -> 1
            "outfield" -> 2
            "pitchers" -> 3
            "relief" -> 4
            else -> 5
        }
    }

    /**
     * Método público para forzar actualización (útil para depuración)
     */
    fun actualizarDesdeViewModel() {
        val lineups = lineupViewModel.getAllLineups()
        actualizarUIConLineups(lineups)
    }

    /**
     * Método para verificar el estado del ViewModel (útil para depuración)
     */
    fun verificarEstadoViewModel() {
        val cantidad = lineupViewModel.getCantidadLineups()
        val tipos = lineupViewModel.getTiposSeleccionados()
        val tieneLineups = lineupViewModel.tieneLineups()

        Log.d("MI_EQUIPO_DEBUG", "Cantidad de lineups: $cantidad")
        Log.d("MI_EQUIPO_DEBUG", "Tipos seleccionados: $tipos")
        Log.d("MI_EQUIPO_DEBUG", "Tiene lineups: $tieneLineups")
        Log.d("MI_EQUIPO_DEBUG", "Items en adaptador: ${listaLineups.size}")
    }

    /**
     * Método para obtener estadísticas del equipo
     */
    fun obtenerEstadisticasEquipo(): String {
        val lineups = lineupViewModel.getAllLineups()
        val totalJugadores = lineups.sumOf { it.jugadores.size }
        val lineupsCompletos = if (lineups.size == 4) "✅ Completo" else "⏳ Incompleto"

        return "Lineups: ${lineups.size}/4 | Jugadores: $totalJugadores | Estado: $lineupsCompletos"
    }

    /**
     * Procesa argumentos del Bundle (para mantener compatibilidad con código existente)
     */
    private fun procesarArgumentos() {
        arguments?.let { bundle ->
            val tipo = bundle.getString("tipo")
            val jugadores = bundle.getSerializable("jugadores") as? HashMap<String, Map<String, Any>>

            if (tipo != null && jugadores != null) {
                Log.d("MI_EQUIPO", "Procesando argumentos: $tipo con ${jugadores.size} jugadores")

                // Verificar si este lineup ya está en el ViewModel
                if (!lineupViewModel.existeLineup(tipo)) {
                    // Agregar al ViewModel si no existe
                    val agregado = lineupViewModel.agregarLineup(tipo, jugadores)
                    if (agregado) {
                        Log.d("MI_EQUIPO", "Lineup $tipo agregado desde argumentos")
                    }
                } else {
                    Log.d("MI_EQUIPO", "Lineup $tipo ya existe, no se agrega desde argumentos")
                }

                // Limpiar argumentos para evitar duplicados en futuras navegaciones
                arguments?.clear()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar estado al reanudar el fragmento
        verificarEstadoViewModel()

        // Procesar argumentos si existen
        procesarArgumentos()

        // Forzar actualización para asegurar sincronización
        if (lineupViewModel.tieneLineups()) {
            actualizarDesdeViewModel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("MI_EQUIPO", "FragmentMiEquipo destruido")
    }
}