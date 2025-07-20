package com.joatoribio.customleaguebeisbol.Fragmentos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.Adaptadores.AdaptadorOrdenTurnos
import com.joatoribio.customleaguebeisbol.Modelo.ModeloLiga
import com.joatoribio.customleaguebeisbol.Modelo.UsuarioParticipante
import com.joatoribio.customleaguebeisbol.R

class FragmentOrdenTurnos : Fragment() {

    private lateinit var mContexto: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var adaptadorOrdenTurnos: AdaptadorOrdenTurnos

    private var ligaActual: ModeloLiga? = null
    private val usuariosParticipantes = mutableListOf<UsuarioParticipante>()
    private var esAdmin = false

    // Views
    private lateinit var tvNombreLiga: TextView
    private lateinit var tvAdministrador: TextView
    private lateinit var tvTotalParticipantes: TextView
    private lateinit var tvEstadoLiga: TextView
    private lateinit var tvContadorParticipantes: TextView
    private lateinit var rvOrdenTurnos: RecyclerView

    // Views para administrador
    private lateinit var panelAdmin: LinearLayout
    private lateinit var btnGuardarOrden: FloatingActionButton
    private lateinit var btnResetearOrden: FloatingActionButton

    // Views para control de draft
    private lateinit var tvEstadoDraft: TextView
    private lateinit var btnIniciarDraft: FloatingActionButton
    private lateinit var btnDetenerDraft: FloatingActionButton
    private lateinit var btnHabilitarLiga: FloatingActionButton

    // NUEVO: Botón para ver progreso del draft
    private lateinit var btnVerProgresoDraft: Button

    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onAttach(context: Context) {
        mContexto = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_orden_turnos, container, false)

        // Inicializar views principales
        tvNombreLiga = view.findViewById(R.id.tvNombreLiga)
        tvAdministrador = view.findViewById(R.id.tvAdministrador)
        tvTotalParticipantes = view.findViewById(R.id.tvTotalParticipantes)
        tvEstadoLiga = view.findViewById(R.id.tvEstadoLiga)
        tvContadorParticipantes = view.findViewById(R.id.tvContadorParticipantes)
        rvOrdenTurnos = view.findViewById(R.id.rvOrdenTurnos)

        // Inicializar views de administrador
        panelAdmin = view.findViewById(R.id.cardViewAdmin)
        btnGuardarOrden = view.findViewById(R.id.btnGuardarOrden)
        btnResetearOrden = view.findViewById(R.id.btnResetearOrden)

        // Inicializar views de control de draft
        tvEstadoDraft = view.findViewById(R.id.tvEstadoDraft)
        btnIniciarDraft = view.findViewById(R.id.btnIniciarDraft)
        btnDetenerDraft = view.findViewById(R.id.btnDetenerDraft)
        btnHabilitarLiga = view.findViewById(R.id.btnHabilitarLiga)

        // NUEVO: Inicializar botón de progreso del draft
        btnVerProgresoDraft = view.findViewById(R.id.btnVerProgresoDraft)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        inicializarAdaptador()
        configurarRecyclerView()
        configurarFuncionesAdmin()
        configurarBotonProgresoDraft()

        cargarLigaDelUsuario()
    }

    private fun configurarBotonProgresoDraft() {
        btnVerProgresoDraft.setOnClickListener {
            Log.d("NAVEGACION", "Navegando a progreso del draft desde OrdenTurnos")
            navegarAProgresoDraft()
        }

        // Inicialmente oculto hasta verificar el estado del draft
        btnVerProgresoDraft.visibility = View.GONE
        Log.d("BOTON_PROGRESO", "Botón de progreso configurado en OrdenTurnos")
    }

    private fun navegarAProgresoDraft() {
        try {
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentProgreso = FragmentProgresoDraft()

            fragmentManager.beginTransaction()
                .replace(R.id.fragmet_LayoutL1, fragmentProgreso)
                .addToBackStack("ProgresoDraft")
                .commitAllowingStateLoss()

            Toast.makeText(mContexto, "Abriendo progreso del draft...", Toast.LENGTH_SHORT).show()
            Log.d("NAVEGACION", "Navegación exitosa a FragmentProgresoDraft desde OrdenTurnos")

        } catch (e: Exception) {
            Log.e("NAVEGACION_ERROR", "Error al navegar a progreso: ${e.message}")
            Toast.makeText(mContexto, "Error al abrir progreso del draft", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarVisibilidadBotonProgreso() {
        try {
            val mostrarBoton = ligaActual?.let { liga ->
                val configuracionDraft = liga.configuracion.configuracionDraft
                configuracionDraft.draftIniciado || configuracionDraft.draftCompletado ||
                        (configuracionDraft.rondaActual > 1 || configuracionDraft.turnoActual > 0)
            } ?: false

            btnVerProgresoDraft.visibility = if (mostrarBoton) View.VISIBLE else View.GONE

            Log.d("BOTON_PROGRESO", "Visibilidad botón en OrdenTurnos: ${if (mostrarBoton) "VISIBLE" else "GONE"}")

        } catch (e: Exception) {
            Log.w("BOTON_PROGRESO", "Error actualizando visibilidad en OrdenTurnos: ${e.message}")
        }
    }

    private fun configurarFuncionesAdmin() {
        panelAdmin.visibility = View.GONE

        btnIniciarDraft.visibility = View.GONE
        btnDetenerDraft.visibility = View.GONE
        btnHabilitarLiga.visibility = View.GONE

        btnGuardarOrden.setOnClickListener {
            guardarOrdenActual()
        }

        btnResetearOrden.setOnClickListener {
            resetearOrdenOriginal()
        }

        btnHabilitarLiga.setOnClickListener {
            habilitarLiga()
        }

        btnIniciarDraft.setOnClickListener {
            mostrarDialogoIniciarDraft()
        }

        btnDetenerDraft.setOnClickListener {
            mostrarDialogoDetenerDraft()
        }
    }

    private fun habilitarLiga() {
        ligaActual?.let { liga ->
            val builder = AlertDialog.Builder(mContexto)
            builder.setTitle("🚀 Habilitar Liga")
            builder.setMessage("¿Estás seguro de que deseas habilitar la liga?\n\nEsto permitirá que los participantes puedan ver los equipos, pero aún no podrán seleccionar lineups hasta que inicies el draft.")

            builder.setPositiveButton("✅ Habilitar") { dialog, _ ->
                dialog.dismiss()
                cambiarEstadoLiga("Disponible")
            }

            builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun mostrarDialogoIniciarDraft() {
        ligaActual?.let { liga ->
            val configuracionDraft = liga.configuracion.configuracionDraft
            val esReanudacion = configuracionDraft.rondaActual > 1 || configuracionDraft.turnoActual > 0

            val builder = AlertDialog.Builder(mContexto)

            if (esReanudacion) {
                builder.setTitle("🔄 Reanudar Draft")
                val mensaje = """
                ¿Deseas reanudar el draft desde donde se quedó?
                
                📊 Estado actual:
                • Ronda: ${configuracionDraft.rondaActual}/4
                • Turno: ${configuracionDraft.turnoActual + 1}/${liga.usuariosParticipantes.size}
                
                Una vez reanudado:
                • Los participantes podrán continuar seleccionando
                • Se mantiene el progreso anterior
                • El orden seguirá siendo el configurado
                
                ¿Continuar?
            """.trimIndent()

                builder.setMessage(mensaje)
                builder.setPositiveButton("🔄 Reanudar Draft") { dialog, _ ->
                    dialog.dismiss()
                    iniciarDraft()
                }
            } else {
                builder.setTitle("🏆 Iniciar Draft")
                val mensaje = """
                ¿Estás listo para iniciar el draft?
                
                Una vez iniciado:
                • Los participantes podrán seleccionar lineups
                • El orden será el configurado actualmente
                • No podrás modificar el orden hasta que termine
                
                ¿Continuar?
            """.trimIndent()

                builder.setMessage(mensaje)
                builder.setPositiveButton("🚀 Iniciar Draft") { dialog, _ ->
                    dialog.dismiss()
                    iniciarDraft()
                }
            }

            builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun mostrarDialogoDetenerDraft() {
        ligaActual?.let { liga ->
            val configuracionDraft = liga.configuracion.configuracionDraft

            val builder = AlertDialog.Builder(mContexto)
            builder.setTitle("⏸️ Pausar Draft")

            val mensaje = """
            ¿Estás seguro de que deseas pausar el draft?
            
            📊 Estado actual:
            • Ronda: ${configuracionDraft.rondaActual}/4
            • Turno: ${configuracionDraft.turnoActual + 1}/${liga.usuariosParticipantes.size}
            
            ⚠️ Al pausar:
            • Los participantes no podrán seleccionar temporalmente
            • El progreso se guardará automáticamente
            • Podrás reanudarlo más tarde desde el mismo punto
            
            ¿Continuar?
        """.trimIndent()

            builder.setMessage(mensaje)

            builder.setPositiveButton("⏸️ Pausar") { dialog, _ ->
                dialog.dismiss()
                detenerDraft()
            }

            builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun cambiarEstadoLiga(nuevoEstado: String) {
        ligaActual?.let { liga ->
            val database = FirebaseDatabase.getInstance()
            val ligaRef = database.getReference("Ligas").child(liga.id)

            ligaRef.child("estado").setValue(nuevoEstado)
                .addOnSuccessListener {
                    Toast.makeText(mContexto, "Liga ${nuevoEstado.lowercase()}", Toast.LENGTH_SHORT).show()
                    ligaActual = liga.copy(estado = nuevoEstado)
                    actualizarUISegunEstado()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(mContexto, "Error al cambiar estado: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun iniciarDraft() {
        val database = FirebaseDatabase.getInstance()
        val configuracionRef = database.getReference("Ligas/${ligaActual?.id}/configuracion/configuracionDraft")

        ligaActual?.let { liga ->
            // Obtener el primer usuario según el orden de turnos
            val primerUsuario = if (liga.configuracion.configuracionDraft.ordenTurnos.isNotEmpty()) {
                liga.configuracion.configuracionDraft.ordenTurnos[0]
            } else if (liga.usuariosParticipantes.isNotEmpty()) {
                liga.usuariosParticipantes[0].uid
            } else {
                Log.e("DRAFT", "No hay usuarios para iniciar el draft")
                return
            }

            Log.d("DRAFT", "Iniciando draft con primer usuario: $primerUsuario")

            val updates = mapOf(
                "draftIniciado" to true,
                "fechaInicio" to System.currentTimeMillis(),
                "usuarioEnTurno" to primerUsuario  // IMPORTANTE: Establecer el primer usuario
            )

            configuracionRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(mContexto, "¡Draft iniciado! 🎯", Toast.LENGTH_SHORT).show()
                    Log.d("DRAFT", "✅ Draft iniciado exitosamente")
                    Log.d("DRAFT", "   - Primer usuario en turno: $primerUsuario")

                    // Notificar a todos los participantes (si tienes esta función implementada)
                    // enviarNotificacionInicioLiga(liga.nombre)
                }
                .addOnFailureListener { error ->
                    Toast.makeText(mContexto, "Error al iniciar draft: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("DRAFT", "Error iniciando draft: ${error.message}")
                }
        }
    }

    private fun detenerDraft() {
        ligaActual?.let { liga ->
            val database = FirebaseDatabase.getInstance()
            val ligaRef = database.getReference("Ligas").child(liga.id)

            val updates = mapOf(
                "configuracion/configuracionDraft/draftIniciado" to false,
                "configuracion/configuracionDraft/fechaDetencion" to System.currentTimeMillis(),
                "estado" to "Disponible"
            )

            ligaRef.updateChildren(updates)
                .addOnSuccessListener {
                    val configuracionDraft = liga.configuracion.configuracionDraft
                    val mensaje = "⏹️ Draft pausado en Ronda ${configuracionDraft.rondaActual}, Turno ${configuracionDraft.turnoActual + 1}\n\nPodrás reanudarlo más tarde desde donde se quedó."

                    Toast.makeText(mContexto, mensaje, Toast.LENGTH_LONG).show()
                    cargarLigaDelUsuario()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(mContexto, "Error al detener draft: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun inicializarAdaptador() {
        adaptadorOrdenTurnos = AdaptadorOrdenTurnos(usuariosParticipantes, firebaseAuth.uid ?: "", esAdmin)
    }

    private fun configurarRecyclerView() {
        rvOrdenTurnos.apply {
            layoutManager = LinearLayoutManager(mContexto)
            adapter = adaptadorOrdenTurnos
        }

        configurarDragAndDrop()
    }

    private fun configurarDragAndDrop() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (!esAdmin) return false

                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                if (fromPosition < usuariosParticipantes.size && toPosition < usuariosParticipantes.size) {
                    val usuario = usuariosParticipantes.removeAt(fromPosition)
                    usuariosParticipantes.add(toPosition, usuario)
                    adaptadorOrdenTurnos.notifyItemMoved(fromPosition, toPosition)
                    actualizarNumerosOrden()
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No implementar swipe
            }

            override fun isLongPressDragEnabled(): Boolean {
                return esAdmin && puedeModificarOrden()
            }
        }

        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper?.attachToRecyclerView(rvOrdenTurnos)
    }

    private fun puedeModificarOrden(): Boolean {
        return ligaActual?.configuracion?.configuracionDraft?.draftIniciado != true
    }

    private fun cargarLigaDelUsuario() {
        val database = FirebaseDatabase.getInstance()
        val ligasRef = database.getReference("Ligas")

        Log.d("ORDEN_TURNOS", "Buscando liga del usuario: ${firebaseAuth.uid}")

        ligasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ORDEN_TURNOS", "Datos de ligas recibidos: ${snapshot.childrenCount} ligas encontradas")

                usuariosParticipantes.clear()
                adaptadorOrdenTurnos.notifyDataSetChanged()

                val ligaEncontrada = snapshot.children
                    .mapNotNull { it.getValue(ModeloLiga::class.java) }
                    .firstOrNull { liga ->
                        Log.d("ORDEN_TURNOS", "Revisando liga: ${liga.nombreLiga}, usuarios permitidos: ${liga.usuariosPermitidos.size}")
                        liga.usuariosPermitidos.contains(firebaseAuth.uid)
                    }

                ligaEncontrada?.let { liga ->
                    Log.d("ORDEN_TURNOS", "Liga encontrada: ${liga.nombreLiga}")
                    ligaActual = liga
                    mostrarInformacionLiga(liga)
                    cargarUsuariosParticipantes(liga)
                } ?: run {
                    Log.d("ORDEN_TURNOS", "No se encontró liga para el usuario")
                    mostrarMensajeSinLiga()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ORDEN_TURNOS", "Error al cargar liga: ${error.message}")
                Toast.makeText(mContexto, "Error al cargar información de la liga", Toast.LENGTH_SHORT).show()
                mostrarMensajeSinLiga()
            }
        })
    }

    private fun mostrarInformacionLiga(liga: ModeloLiga) {
        Log.d("ORDEN_TURNOS", "Mostrando información de liga: ${liga.nombreLiga}")

        esAdmin = liga.adminUid == firebaseAuth.uid
        Log.d("ORDEN_TURNOS", "Usuario es admin: $esAdmin")

        actualizarUISegunEstado()

        tvNombreLiga.text = "🏆 ${liga.nombreLiga}"
        tvTotalParticipantes.text = "👥 ${liga.usuariosParticipantes.size}"
        tvEstadoLiga.text = "✅ ${liga.estado}"

        when (liga.estado) {
            "Disponible" -> {
                tvEstadoLiga.setTextColor(mContexto.getColor(android.R.color.holo_green_dark))
            }
            "EnProgreso" -> {
                tvEstadoLiga.setTextColor(mContexto.getColor(android.R.color.holo_orange_dark))
            }
            "Configurando" -> {
                tvEstadoLiga.setTextColor(mContexto.getColor(android.R.color.holo_blue_dark))
            }
            "Finalizada" -> {
                tvEstadoLiga.setTextColor(mContexto.getColor(android.R.color.holo_red_dark))
            }
        }

        buscarIdGamingAdministrador(liga.adminUid) { idGaming ->
            tvAdministrador.text = "👑 $idGaming${if (esAdmin) " (Tú)" else ""}"
        }

        inicializarAdaptador()
        rvOrdenTurnos.adapter = adaptadorOrdenTurnos
    }

    private fun actualizarUISegunEstado() {
        ligaActual?.let { liga ->
            val configuracionDraft = liga.configuracion.configuracionDraft

            if (esAdmin) {
                panelAdmin.visibility = View.VISIBLE

                when {
                    liga.estado == "Configurando" -> {
                        btnHabilitarLiga.visibility = View.VISIBLE
                        btnIniciarDraft.visibility = View.GONE
                        btnDetenerDraft.visibility = View.GONE
                        tvEstadoDraft.text = "⚙️ Liga en configuración - Presiona 'Habilitar Liga' cuando esté lista"
                    }
                    liga.estado == "Disponible" && !configuracionDraft.draftIniciado -> {
                        btnHabilitarLiga.visibility = View.GONE
                        btnIniciarDraft.visibility = View.VISIBLE
                        btnDetenerDraft.visibility = View.GONE

                        val esReanudacion = configuracionDraft.rondaActual > 1 || configuracionDraft.turnoActual > 0

                        if (esReanudacion) {
                            tvEstadoDraft.text = "⏸️ Draft pausado en R${configuracionDraft.rondaActual}, T${configuracionDraft.turnoActual + 1} - Puedes reanudarlo"
                            btnIniciarDraft.contentDescription = "Reanudar Draft"
                        } else {
                            tvEstadoDraft.text = "🚀 Liga lista - Puedes iniciar el draft cuando quieras"
                            btnIniciarDraft.contentDescription = "Iniciar Draft"
                        }
                    }
                    configuracionDraft.draftIniciado && !configuracionDraft.draftCompletado -> {
                        btnHabilitarLiga.visibility = View.GONE
                        btnIniciarDraft.visibility = View.GONE
                        btnDetenerDraft.visibility = View.VISIBLE

                        val totalParticipantes = liga.usuariosParticipantes.size
                        tvEstadoDraft.text = "🏆 Draft activo - R${configuracionDraft.rondaActual}/4, T${configuracionDraft.turnoActual + 1}/$totalParticipantes"
                    }
                    configuracionDraft.draftCompletado -> {
                        btnHabilitarLiga.visibility = View.GONE
                        btnIniciarDraft.visibility = View.GONE
                        btnDetenerDraft.visibility = View.GONE
                        tvEstadoDraft.text = "✅ Draft completado - Todas las rondas finalizadas"
                    }
                }
            } else {
                panelAdmin.visibility = View.GONE
                btnHabilitarLiga.visibility = View.GONE
                btnIniciarDraft.visibility = View.GONE
                btnDetenerDraft.visibility = View.GONE

                when {
                    liga.estado == "Configurando" -> {
                        tvEstadoDraft.text = "⏳ El administrador está configurando la liga"
                    }
                    liga.estado == "Disponible" && !configuracionDraft.draftIniciado -> {
                        val esReanudacion = configuracionDraft.rondaActual > 1 || configuracionDraft.turnoActual > 0

                        if (esReanudacion) {
                            tvEstadoDraft.text = "⏸️ Draft pausado en R${configuracionDraft.rondaActual}, T${configuracionDraft.turnoActual + 1} - Esperando reanudación"
                        } else {
                            tvEstadoDraft.text = "🚀 Esperando que el administrador inicie el draft"
                        }
                    }
                    configuracionDraft.draftIniciado && !configuracionDraft.draftCompletado -> {
                        val totalParticipantes = liga.usuariosParticipantes.size
                        tvEstadoDraft.text = "🏆 Draft activo (R${configuracionDraft.rondaActual}/4, T${configuracionDraft.turnoActual + 1}/$totalParticipantes) - ¡Puedes seleccionar lineups!"
                    }
                    configuracionDraft.draftCompletado -> {
                        tvEstadoDraft.text = "✅ Draft completado - Revisa tu equipo"
                    }
                }
            }

            actualizarVisibilidadBotonProgreso()
        }
    }

    private fun buscarIdGamingAdministrador(adminUid: String, callback: (String) -> Unit) {
        val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios")

        usuariosRef.child(adminUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val idGaming = snapshot.child("idGaming").value?.toString()
                val nombre = snapshot.child("nombres").value?.toString()
                val email = snapshot.child("email").value?.toString()

                val identificador = when {
                    !idGaming.isNullOrEmpty() && idGaming != "null" -> idGaming
                    !nombre.isNullOrEmpty() && nombre != "null" -> nombre
                    !email.isNullOrEmpty() && email != "null" -> email.substringBefore("@")
                    else -> "Admin desconocido"
                }

                callback(identificador)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Admin desconocido")
            }
        })
    }

    private fun cargarUsuariosParticipantes(liga: ModeloLiga) {
        Log.d("ORDEN_TURNOS", "Cargando ${liga.usuariosParticipantes.size} usuarios participantes")

        usuariosParticipantes.clear()

        val adminEstaIncluido = liga.usuariosParticipantes.any { it.uid == liga.adminUid }

        val usuariosCompletos = if (!adminEstaIncluido) {
            Log.d("ORDEN_TURNOS", "Administrador no está en participantes, agregándolo...")
            val usuariosConAdmin = liga.usuariosParticipantes.toMutableList()

            val adminParticipante = UsuarioParticipante(
                uid = liga.adminUid,
                nombre = "",
                email = "",
                idGaming = "",
                urlImagenPerfil = "",
                esAdmin = true,
                fechaUnion = System.currentTimeMillis(),
                activo = true,
                rol = "Administrador"
            )

            usuariosConAdmin.add(adminParticipante)
            usuariosConAdmin
        } else {
            liga.usuariosParticipantes
        }

        if (liga.configuracion.configuracionDraft.ordenTurnos.isNotEmpty()) {
            Log.d("ORDEN_TURNOS", "Usando orden de turnos existente")
            cargarUsuariosEnOrden(liga, usuariosCompletos)
        } else {
            Log.d("ORDEN_TURNOS", "No hay orden configurado, usando orden de participación")
            enriquecerUsuariosConIdGaming(usuariosCompletos) { usuariosEnriquecidos ->
                usuariosParticipantes.clear()
                usuariosParticipantes.addAll(usuariosEnriquecidos)
                adaptadorOrdenTurnos.notifyDataSetChanged()
                actualizarContadorParticipantes()
                Log.d("ORDEN_TURNOS", "Usuarios cargados: ${usuariosParticipantes.size}")
            }
        }
    }

    private fun cargarUsuariosEnOrden(liga: ModeloLiga, usuariosCompletos: List<UsuarioParticipante>) {
        val ordenTurnos = liga.configuracion.configuracionDraft.ordenTurnos
        val mapaUsuarios = usuariosCompletos.associateBy { it.uid }
        val usuariosOrdenados = mutableListOf<UsuarioParticipante>()

        for (uid in ordenTurnos) {
            mapaUsuarios[uid]?.let { usuario ->
                usuariosOrdenados.add(usuario)
            }
        }

        for (usuario in usuariosCompletos) {
            if (!ordenTurnos.contains(usuario.uid)) {
                usuariosOrdenados.add(usuario)
                Log.d("ORDEN_TURNOS", "Usuario ${usuario.uid} agregado al final del orden")
            }
        }

        enriquecerUsuariosConIdGaming(usuariosOrdenados) { usuariosEnriquecidos ->
            usuariosParticipantes.clear()
            usuariosParticipantes.addAll(usuariosEnriquecidos)
            adaptadorOrdenTurnos.notifyDataSetChanged()
            actualizarContadorParticipantes()
        }
    }

    private fun enriquecerUsuariosConIdGaming(
        usuarios: List<UsuarioParticipante>,
        callback: (List<UsuarioParticipante>) -> Unit
    ) {
        if (usuarios.isEmpty()) {
            callback(emptyList())
            return
        }

        val usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios")
        val usuariosEnriquecidos = mutableListOf<UsuarioParticipante>()
        var contadorCompletados = 0

        Log.d("ORDEN_TURNOS", "Enriqueciendo ${usuarios.size} usuarios con ID Gaming")

        for (usuario in usuarios) {
            usuariosRef.child(usuario.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val idGaming = snapshot.child("idGaming").value?.toString()
                    val nombre = snapshot.child("nombres").value?.toString()
                    val email = snapshot.child("email").value?.toString()

                    val identificadorMostrar = when {
                        !idGaming.isNullOrEmpty() && idGaming != "null" -> idGaming
                        !nombre.isNullOrEmpty() && nombre != "null" -> nombre
                        !email.isNullOrEmpty() && email != "null" -> email.substringBefore("@")
                        else -> "Usuario${usuario.uid.take(6)}"
                    }

                    val usuarioEnriquecido = UsuarioParticipante(
                        uid = usuario.uid,
                        nombre = identificadorMostrar,
                        email = usuario.email,
                        idGaming = idGaming ?: "",
                        urlImagenPerfil = usuario.urlImagenPerfil,
                        esAdmin = usuario.esAdmin,
                        fechaUnion = usuario.fechaUnion,
                        activo = usuario.activo,
                        rol = if (usuario.esAdmin) "Administrador" else "Participante"
                    )

                    usuariosEnriquecidos.add(usuarioEnriquecido)
                    contadorCompletados++

                    Log.d("ORDEN_TURNOS", "Usuario enriquecido: ${identificadorMostrar} ($contadorCompletados/${usuarios.size})")

                    if (contadorCompletados == usuarios.size) {
                        callback(usuariosEnriquecidos)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ORDEN_TURNOS", "Error al obtener datos del usuario ${usuario.uid}: ${error.message}")

                    val usuarioBasico = UsuarioParticipante(
                        uid = usuario.uid,
                        nombre = "Usuario${usuario.uid.take(6)}",
                        email = usuario.email,
                        idGaming = "",
                        urlImagenPerfil = usuario.urlImagenPerfil,
                        esAdmin = usuario.esAdmin,
                        fechaUnion = usuario.fechaUnion,
                        activo = usuario.activo,
                        rol = if (usuario.esAdmin) "Administrador" else "Participante"
                    )

                    usuariosEnriquecidos.add(usuarioBasico)
                    contadorCompletados++

                    if (contadorCompletados == usuarios.size) {
                        callback(usuariosEnriquecidos)
                    }
                }
            })
        }
    }

    private fun actualizarContadorParticipantes() {
        val total = usuariosParticipantes.size
        val activos = usuariosParticipantes.count { it.activo }

        tvContadorParticipantes.text = "$activos/$total"
    }

    private fun mostrarMensajeSinLiga() {
        tvNombreLiga.text = "🏆 Sin Liga Asignada"
        tvAdministrador.text = "👑 No perteneces a ninguna liga activa"
        tvTotalParticipantes.text = "👥 0"
        tvEstadoLiga.text = "❌ Sin asignar"
        tvContadorParticipantes.text = "0/0"
        tvEstadoDraft.text = "❌ No hay liga disponible"

        usuariosParticipantes.clear()
        adaptadorOrdenTurnos.notifyDataSetChanged()

        // Ocultar botón de progreso cuando no hay liga
        btnVerProgresoDraft.visibility = View.GONE
    }

    fun refrescarDatos() {
        usuariosParticipantes.clear()
        adaptadorOrdenTurnos.notifyDataSetChanged()
        cargarLigaDelUsuario()
    }

    override fun onResume() {
        super.onResume()
        refrescarDatos()
    }

    private fun guardarOrdenActual() {
        if (!esAdmin) {
            Toast.makeText(mContexto, "Solo el administrador puede guardar cambios", Toast.LENGTH_SHORT).show()
            return
        }

        if (!puedeModificarOrden()) {
            Toast.makeText(mContexto, "No se puede modificar el orden mientras el draft está activo", Toast.LENGTH_LONG).show()
            return
        }

        ligaActual?.let { liga ->
            val database = FirebaseDatabase.getInstance()
            val ligaRef = database.getReference("Ligas").child(liga.id)

            val nuevosUids = usuariosParticipantes.map { it.uid }

            val updates = mapOf(
                "configuracion/configuracionDraft/ordenTurnos" to nuevosUids,
                "configuracion/configuracionDraft/configuradoPorAdmin" to true,
                "configuracion/configuracionDraft/fechaConfiguracion" to System.currentTimeMillis()
            )

            ligaRef.updateChildren(updates)
                .addOnSuccessListener {
                    actualizarUsuariosParticipantesEnFirebase()
                }
                .addOnFailureListener { error ->
                    Log.e("ORDEN_TURNOS", "Error al guardar orden: ${error.message}")
                    Toast.makeText(mContexto, "Error al guardar orden: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun actualizarUsuariosParticipantesEnFirebase() {
        ligaActual?.let { liga ->
            val database = FirebaseDatabase.getInstance()
            val ligaRef = database.getReference("Ligas").child(liga.id)

            val usuariosActualizados = usuariosParticipantes.map { usuario ->
                mapOf(
                    "uid" to usuario.uid,
                    "nombre" to usuario.nombre,
                    "email" to usuario.email,
                    "idGaming" to usuario.idGaming,
                    "urlImagenPerfil" to usuario.urlImagenPerfil,
                    "esAdmin" to usuario.esAdmin,
                    "fechaUnion" to usuario.fechaUnion,
                    "activo" to usuario.activo,
                    "rol" to usuario.rol
                )
            }

            val updates = mapOf(
                "usuariosParticipantes" to usuariosActualizados
            )

            ligaRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(mContexto, "✅ Orden de draft guardado exitosamente", Toast.LENGTH_SHORT).show()
                    Log.d("ORDEN_TURNOS", "Orden guardado: ${usuariosParticipantes.map { "${it.nombre}${if(it.esAdmin) " (Admin)" else ""}" }}")

                    actualizarUISegunEstado()
                }
                .addOnFailureListener { error ->
                    Log.e("ORDEN_TURNOS", "Error al actualizar usuarios: ${error.message}")
                    Toast.makeText(mContexto, "Error al actualizar participantes", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun resetearOrdenOriginal() {
        if (!esAdmin) {
            Toast.makeText(mContexto, "Solo el administrador puede resetear el orden", Toast.LENGTH_SHORT).show()
            return
        }

        if (!puedeModificarOrden()) {
            Toast.makeText(mContexto, "No se puede modificar el orden mientras el draft está activo", Toast.LENGTH_LONG).show()
            return
        }

        ligaActual?.let { liga ->
            cargarUsuariosParticipantes(liga)
            Toast.makeText(mContexto, "🔄 Orden reseteado al original", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarNumerosOrden() {
        // El adaptador manejará automáticamente la numeración
        adaptadorOrdenTurnos.notifyDataSetChanged()
    }

    fun cambiarEstadoUsuario(posicion: Int) {
        if (!esAdmin || posicion >= usuariosParticipantes.size) return

        val usuario = usuariosParticipantes[posicion]

        val usuarioActualizado = usuario.copy(activo = !usuario.activo)
        usuariosParticipantes[posicion] = usuarioActualizado
        adaptadorOrdenTurnos.notifyItemChanged(posicion)

        actualizarContadorParticipantes()

        val estado = if (usuarioActualizado.activo) "activado" else "desactivado"
        val mensaje = if (usuario.uid == firebaseAuth.uid) {
            "Te has $estado para el draft"
        } else {
            "${getIdentificadorMostrar(usuario)} $estado"
        }

        Toast.makeText(mContexto, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun getIdentificadorMostrar(usuario: UsuarioParticipante): String {
        return when {
            usuario.idGaming.isNotEmpty() && usuario.idGaming != "null" -> usuario.idGaming
            usuario.nombre.isNotEmpty() && usuario.nombre != "null" -> usuario.nombre
            else -> "Usuario${usuario.uid.take(6)}"
        }
    }
}