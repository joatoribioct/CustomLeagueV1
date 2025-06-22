package com.joatoribio.customleaguebeisbol.Fragmentos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.Adaptadores.AdaptadorProgresoDraft
import com.joatoribio.customleaguebeisbol.Draft.ControladorDraft
import com.joatoribio.customleaguebeisbol.Draft.EstadoDraft
import com.joatoribio.customleaguebeisbol.Modelo.ModeloLiga
import com.joatoribio.customleaguebeisbol.R

// Modelo de datos para el progreso de cada participante
data class ProgresoParticipante(
    val posicion: Int,
    val uid: String,
    val idGaming: String,
    val esAdmin: Boolean,
    val picks: MutableList<PickSeleccionado> = mutableListOf()
)

data class PickSeleccionado(
    val ronda: Int,
    val tipoLineup: String,
    val equipoId: String,
    val fechaSeleccion: Long = 0
)

class FragmentProgresoDraft : Fragment() {

    private lateinit var mContexto: Context
    private lateinit var firebaseAuth: FirebaseAuth

    // Views
    private var tvEstadoDraft: TextView? = null
    private var tvEstadisticasDraft: TextView? = null
    private var btnCopiarProgreso: Button? = null
    private var btnCompartirWhatsapp: Button? = null
    private var rvProgresoParticipantes: RecyclerView? = null

    private var ligaActual: ModeloLiga? = null
    private var controladorDraft: ControladorDraft? = null
    private var estadoDraftActual: EstadoDraft = EstadoDraft()
    private var esAdmin = false

    private val participantes = mutableListOf<ProgresoParticipante>()
    private lateinit var adaptadorProgreso: AdaptadorProgresoDraft

    // Listeners para Firebase
    private var lineupsListener: ValueEventListener? = null
    private var ligasListener: ValueEventListener? = null

    override fun onAttach(context: Context) {
        mContexto = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progreso_draft, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        inicializarVistas(view)
        configurarRecyclerView()
        configurarBotones()
        buscarLigaActual()
    }

    private fun inicializarVistas(view: View) {
        tvEstadoDraft = view.findViewById(R.id.tvEstadoDraft)
        tvEstadisticasDraft = view.findViewById(R.id.tvEstadisticasDraft)
        btnCopiarProgreso = view.findViewById(R.id.btnCopiarProgreso)
        btnCompartirWhatsapp = view.findViewById(R.id.btnCompartirWhatsapp)
        rvProgresoParticipantes = view.findViewById(R.id.rvProgresoParticipantes)
    }

    private fun configurarRecyclerView() {
        adaptadorProgreso = AdaptadorProgresoDraft(participantes, firebaseAuth.uid ?: "")
        rvProgresoParticipantes?.apply {
            layoutManager = LinearLayoutManager(mContexto)
            adapter = adaptadorProgreso
        }
    }

    private fun configurarBotones() {
        btnCopiarProgreso?.setOnClickListener {
            copiarProgresoAlPortapapeles()
        }

        btnCompartirWhatsapp?.setOnClickListener {
            compartirPorWhatsApp()
        }

        actualizarVisibilidadBotones()
    }

    private fun buscarLigaActual() {
        val ligasRef = FirebaseDatabase.getInstance().getReference("Ligas")

        ligasListener = object : ValueEventListener {
            override fun onDataChange(ligasSnapshot: DataSnapshot) {
                var ligaEncontrada = false

                run busqueda@{
                    for (ligaSnapshot in ligasSnapshot.children) {
                        val liga = ligaSnapshot.getValue(ModeloLiga::class.java)

                        liga?.let {
                            if (it.usuariosPermitidos.contains(firebaseAuth.uid)) {
                                ligaActual = it
                                esAdmin = it.adminUid == firebaseAuth.uid

                                inicializarControladorDraft(it.id)
                                cargarParticipantes(it)
                                escucharLineupsSeleccionados()
                                actualizarVisibilidadBotones()

                                ligaEncontrada = true
                                return@busqueda
                            }
                        }
                    }
                }

                if (!ligaEncontrada) {
                    mostrarMensajeSinLiga()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PROGRESO_DRAFT", "Error al buscar liga: ${error.message}")
                Toast.makeText(mContexto, "Error al cargar liga: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        ligasRef.addValueEventListener(ligasListener!!)
    }

    private fun inicializarControladorDraft(ligaId: String) {
        controladorDraft = ControladorDraft(ligaId) { nuevoEstado ->
            estadoDraftActual = nuevoEstado
            actualizarEstadoUI()
        }
        controladorDraft?.iniciarEscuchaDraft()
    }

    private fun cargarParticipantes(liga: ModeloLiga) {
        participantes.clear()

        try {
            // Acceso correcto usando la estructura de ConfiguracionLiga
            val ordenTurnos = liga.configuracion.configuracionDraft.ordenTurnos

            Log.d("PROGRESO_DRAFT", "Orden de turnos obtenido: $ordenTurnos")

            // Crear participantes basado en el orden del draft
            if (ordenTurnos.isNotEmpty()) {
                // Usar el orden específico del draft
                ordenTurnos.forEachIndexed { index, uid ->
                    val usuarioParticipante = liga.usuariosParticipantes.find { it.uid == uid }

                    if (usuarioParticipante != null) {
                        val participante = ProgresoParticipante(
                            posicion = index + 1,
                            uid = uid,
                            idGaming = usuarioParticipante.idGaming.ifEmpty {
                                usuarioParticipante.nombre.ifEmpty { "Usuario ${uid.take(8)}" }
                            },
                            esAdmin = usuarioParticipante.esAdmin
                        )
                        participantes.add(participante)
                    }
                }
            } else {
                // Fallback: usar orden de usuariosParticipantes si no hay orden de draft
                Log.d("PROGRESO_DRAFT", "No hay orden de draft, usando orden de participantes")
                liga.usuariosParticipantes.forEachIndexed { index, usuarioParticipante ->
                    val participante = ProgresoParticipante(
                        posicion = index + 1,
                        uid = usuarioParticipante.uid,
                        idGaming = usuarioParticipante.idGaming.ifEmpty {
                            usuarioParticipante.nombre.ifEmpty { "Usuario ${usuarioParticipante.uid.take(8)}" }
                        },
                        esAdmin = usuarioParticipante.esAdmin
                    )
                    participantes.add(participante)
                }
            }

            adaptadorProgreso.notifyDataSetChanged()
            actualizarEstadisticas()

            Log.d("PROGRESO_DRAFT", "Participantes cargados: ${participantes.size}")

        } catch (e: Exception) {
            Log.e("PROGRESO_DRAFT", "Error cargando participantes: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun escucharLineupsSeleccionados() {
        val database = FirebaseDatabase.getInstance()
        val lineupsSeleccionadosRef = database.getReference("LineupsSeleccionados")

        lineupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || activity == null) return

                // Limpiar picks existentes
                participantes.forEach { it.picks.clear() }

                // Procesar lineups seleccionados
                for (lineupSnapshot in snapshot.children) {
                    try {
                        val usuarioId = lineupSnapshot.child("usuarioId").value as? String ?: continue
                        val tipoLineup = lineupSnapshot.child("tipoLineup").value as? String ?: continue
                        val equipoId = lineupSnapshot.child("equipoId").value as? String ?: continue
                        val ronda = (lineupSnapshot.child("ronda").value as? Long)?.toInt() ?: 1
                        val fechaSeleccion = lineupSnapshot.child("fechaSeleccion").value as? Long ?: 0

                        // Encontrar el participante y agregar su pick
                        val participante = participantes.find { it.uid == usuarioId }
                        participante?.let {
                            val pick = PickSeleccionado(
                                ronda = ronda,
                                tipoLineup = tipoLineup,
                                equipoId = equipoId,
                                fechaSeleccion = fechaSeleccion
                            )
                            it.picks.add(pick)
                        }

                        Log.d("PROGRESO_DRAFT", "Pick agregado: R$ronda - $tipoLineup de $equipoId para usuario $usuarioId")

                    } catch (e: Exception) {
                        Log.e("PROGRESO_DRAFT", "Error procesando lineup: ${e.message}")
                    }
                }

                // Ordenar picks por ronda para cada participante
                participantes.forEach { participante ->
                    participante.picks.sortBy { it.ronda }
                }

                adaptadorProgreso.notifyDataSetChanged()
                actualizarEstadisticas()

                Log.d("PROGRESO_DRAFT", "Progreso actualizado con lineups seleccionados")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PROGRESO_DRAFT", "Error al escuchar lineups: ${error.message}")
            }
        }

        lineupsSeleccionadosRef.addValueEventListener(lineupsListener!!)
    }

    private fun actualizarEstadoUI() {
        try {
            val configuracionDraft = ligaActual?.configuracion?.configuracionDraft

            val infoEstado = when {
                configuracionDraft?.draftCompletado == true -> "✅ Draft completado"
                configuracionDraft?.draftIniciado == false -> "⏳ Draft no iniciado"
                configuracionDraft?.draftIniciado == true -> {
                    val usuarioActual = controladorDraft?.obtenerInfoUsuarioActual() ?: "Esperando..."
                    "🔄 Ronda ${configuracionDraft.rondaActual}/4 - Turno: $usuarioActual"
                }
                else -> "⏳ Cargando estado del draft..."
            }

            tvEstadoDraft?.text = infoEstado
        } catch (e: Exception) {
            Log.w("PROGRESO_DRAFT", "Error actualizando estado UI: ${e.message}")
        }
    }

    private fun actualizarEstadisticas() {
        try {
            val totalParticipantes = participantes.size
            val totalPicks = participantes.sumOf { it.picks.size }
            val totalPosiblesPicks = totalParticipantes * 4 // 4 rondas

            tvEstadisticasDraft?.text =
                "📊 Participantes: $totalParticipantes | Selecciones: $totalPicks/$totalPosiblesPicks"
        } catch (e: Exception) {
            Log.w("PROGRESO_DRAFT", "Error actualizando estadísticas: ${e.message}")
        }
    }

    private fun actualizarVisibilidadBotones() {
        try {
            val hayDatos = participantes.isNotEmpty()
            val puedeExportar = esAdmin || hayDatos

            btnCopiarProgreso?.visibility = if (puedeExportar) View.VISIBLE else View.GONE
            btnCompartirWhatsapp?.visibility = if (puedeExportar) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.w("PROGRESO_DRAFT", "Error actualizando visibilidad botones: ${e.message}")
        }
    }

    private fun copiarProgresoAlPortapapeles() {
        try {
            val textoProgreso = generarTextoProgreso()

            val clipboard = mContexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Progreso Draft", textoProgreso)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(mContexto, "📋 Progreso copiado al portapapeles", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("PROGRESO_DRAFT", "Error copiando al portapapeles: ${e.message}")
            Toast.makeText(mContexto, "Error al copiar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirPorWhatsApp() {
        try {
            val textoProgreso = generarTextoProgreso()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textoProgreso)
                setPackage("com.whatsapp")
            }

            // Verificar si WhatsApp está instalado
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback: compartir con cualquier app
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textoProgreso)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir progreso del draft"))
            }

        } catch (e: Exception) {
            Log.e("PROGRESO_DRAFT", "Error compartiendo: ${e.message}")
            Toast.makeText(mContexto, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarTextoProgreso(): String {
        val nombreLiga = ligaActual?.nombreLiga ?: "Liga de Béisbol"
        val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        val builder = StringBuilder()
        builder.append("🏆 PROGRESO DEL DRAFT - $nombreLiga\n")
        builder.append("📅 $fechaActual\n")
        builder.append("👥 ${participantes.size} participantes\n\n")

        // Estado actual del draft usando la configuración correcta
        val configuracionDraft = ligaActual?.configuracion?.configuracionDraft
        val estadoTexto = when {
            configuracionDraft?.draftCompletado == true -> "✅ Draft completado"
            configuracionDraft?.draftIniciado == false -> "⏳ Draft no iniciado"
            configuracionDraft?.draftIniciado == true -> "🔄 En progreso - Ronda ${configuracionDraft.rondaActual}/4"
            else -> "⏳ Estado desconocido"
        }
        builder.append("$estadoTexto\n\n")

        // Progreso de cada participante
        builder.append("📋 SELECCIONES POR PARTICIPANTE:\n")
        builder.append("═══════════════════════════════\n\n")

        participantes.forEach { participante ->
            val adminIndicator = if (participante.esAdmin) " 👑" else ""
            builder.append("${participante.posicion}. ${participante.idGaming}$adminIndicator\n")

            if (participante.picks.isEmpty()) {
                builder.append("   Sin selecciones aún\n")
            } else {
                participante.picks.forEachIndexed { index, pick ->
                    val numeroPickGlobal = index + 1
                    val tipoFormateado = formatearTipoLineup(pick.tipoLineup)
                    val equipoFormateado = formatearNombreEquipo(pick.equipoId)
                    builder.append("   R${pick.ronda} Pick $numeroPickGlobal: $tipoFormateado de $equipoFormateado\n")
                }
            }
            builder.append("\n")
        }

        // Estadísticas finales
        val totalPicks = participantes.sumOf { it.picks.size }
        val totalPosibles = participantes.size * 4
        builder.append("───────────────────────────────\n")
        builder.append("📊 Total selecciones: $totalPicks/$totalPosibles\n")
        builder.append("🎯 Progreso: ${if (totalPosibles > 0) (totalPicks * 100 / totalPosibles) else 0}%\n\n")
        builder.append("🔗 Generado desde Custom League Béisbol")

        return builder.toString()
    }

    private fun formatearTipoLineup(tipo: String): String {
        return when (tipo.lowercase()) {
            "infield" -> "Infield"
            "outfield" -> "Outfield"
            "pitchers" -> "Pitchers"
            "relief" -> "Relief"
            else -> tipo.capitalize()
        }
    }

    private fun formatearNombreEquipo(equipoId: String): String {
        return when (equipoId) {
            "RedSox" -> "Red Sox"
            "WhiteSox" -> "White Sox"
            "BlueJays" -> "Blue Jays"
            else -> equipoId
        }
    }

    private fun mostrarMensajeSinLiga() {
        tvEstadoDraft?.text = "❌ No se encontró una liga activa"
        tvEstadisticasDraft?.text = "Únete a una liga para ver el progreso del draft"
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Limpiar listeners
        lineupsListener?.let {
            FirebaseDatabase.getInstance().getReference("LineupsSeleccionados").removeEventListener(it)
        }

        ligasListener?.let {
            FirebaseDatabase.getInstance().getReference("Ligas").removeEventListener(it)
        }

        controladorDraft = null
    }
}