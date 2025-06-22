package com.joatoribio.customleaguebeisbol.Ligas

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.joatoribio.customleaguebeisbol.Adaptadores.AdaptadorOrdenDraft
import com.joatoribio.customleaguebeisbol.Constantes
import com.joatoribio.customleaguebeisbol.Manager.EquiposManager
import com.joatoribio.customleaguebeisbol.Modelo.ModeloLiga
import com.joatoribio.customleaguebeisbol.Modelo.UsuarioParticipante
import com.joatoribio.customleaguebeisbol.databinding.ActivityCrearLigaBinding

class CrearLiga : AppCompatActivity() {

    private lateinit var binding: ActivityCrearLigaBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var adaptadorOrdenDraft: AdaptadorOrdenDraft

    private var ligaExistente: ModeloLiga? = null
    private var tieneLigaActiva = false

    private var imagenUri: Uri? = null
    private val usuariosParticipantes = mutableListOf<UsuarioParticipante>()
    private var nombreUsuarioAdmin = ""
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearLigaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere")
        progressDialog.setCanceledOnTouchOutside(false)

        configurarAdaptador()
        configurarRecyclerView()
        configurarDragAndDrop()
        cargarUsuariosDisponibles()

        binding.agregarImg.setOnClickListener {
            mostrarOpcionesImg()
        }

        binding.btnCrearLiga.setOnClickListener {
            Log.d("CREAR_LIGA", "=== BOTÓN CREAR LIGA PRESIONADO ===")
            Toast.makeText(this, "Botón presionado - iniciando validación", Toast.LENGTH_SHORT).show()

            try {
                validarDatos()
            } catch (e: Exception) {
                Log.e("CREAR_LIGA", "Error al validar datos: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Cambiar funcionalidad de botones
        binding.btnSeleccionarTodos.text = "Aleatorio"
        binding.btnSeleccionarTodos.setOnClickListener {
            mezclarOrdenAleatorio()
        }

        binding.btnDeseleccionarTodos.text = "Resetear"
        binding.btnDeseleccionarTodos.setOnClickListener {
            resetearOrdenOriginal()
        }
    }

    private fun configurarAdaptador() {
        Log.d("CREAR_LIGA", "Configurando adaptador")
        adaptadorOrdenDraft = AdaptadorOrdenDraft(usuariosParticipantes, firebaseAuth.uid ?: "")
        Log.d("CREAR_LIGA", "Adaptador creado exitosamente")
    }

    private fun configurarRecyclerView() {
        Log.d("CREAR_LIGA", "Configurando RecyclerView")
        binding.rvUsuariosDisponibles.apply {
            layoutManager = LinearLayoutManager(this@CrearLiga)
            adapter = adaptadorOrdenDraft
        }
        Log.d("CREAR_LIGA", "RecyclerView configurado exitosamente")
    }

    /**
     * Configura el drag & drop para reordenar usuarios
     */
    private fun configurarDragAndDrop() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                if (fromPosition < usuariosParticipantes.size && toPosition < usuariosParticipantes.size) {
                    val usuario = usuariosParticipantes.removeAt(fromPosition)
                    usuariosParticipantes.add(toPosition, usuario)
                    adaptadorOrdenDraft.notifyItemMoved(fromPosition, toPosition)
                    actualizarNumerosOrden()
                }

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No implementar swipe
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }
        }

        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper?.attachToRecyclerView(binding.rvUsuariosDisponibles)
    }

    private fun cargarUsuariosDisponibles() {
        Log.d("CREAR_LIGA", "Iniciando carga de usuarios")
        val database = FirebaseDatabase.getInstance()
        val usuariosRef = database.getReference("Usuarios")

        usuariosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("CREAR_LIGA", "Datos recibidos de Firebase: ${snapshot.childrenCount} usuarios")
                usuariosParticipantes.clear()

                for (usuarioSnapshot in snapshot.children) {
                    val uid = usuarioSnapshot.child("uid").value.toString()
                    val nombre = usuarioSnapshot.child("nombres").value.toString()
                    val email = usuarioSnapshot.child("email").value.toString()
                    val idGaming = usuarioSnapshot.child("idGaming").value?.toString() ?: ""
                    val urlImagen = usuarioSnapshot.child("urlImagenPerfil").value?.toString() ?: ""

                    Log.d("CREAR_LIGA", "Usuario cargado: $nombre (ID: $uid)")

                    // Guardar nombre del admin
                    if (uid == firebaseAuth.uid) {
                        nombreUsuarioAdmin = nombre
                        Log.d("CREAR_LIGA", "Admin identificado: $nombreUsuarioAdmin")
                    }

                    // Crear ID Gaming si no existe
                    val idGamingFinal = if (idGaming.isNullOrEmpty() || idGaming == "null") {
                        generarIdGamingPorDefecto(nombre, email)
                    } else {
                        idGaming
                    }

                    val usuario = UsuarioParticipante(
                        uid = uid,
                        nombre = idGamingFinal,
                        email = email,
                        idGaming = idGamingFinal,
                        urlImagenPerfil = urlImagen,
                        esAdmin = uid == firebaseAuth.uid,
                        fechaUnion = System.currentTimeMillis(),
                        activo = true,
                        rol = if (uid == firebaseAuth.uid) "Administrador" else "Participante"
                    )

                    usuariosParticipantes.add(usuario)
                }

                Log.d("CREAR_LIGA", "Total usuarios cargados: ${usuariosParticipantes.size}")

                // Ordenar: admin primero, luego alfabéticamente por ID Gaming
                usuariosParticipantes.sortWith(
                    compareBy<UsuarioParticipante> { !it.esAdmin }
                        .thenBy { it.idGaming.lowercase() }
                )

                // Notificar cambios al adaptador solo si está inicializado
                if (::adaptadorOrdenDraft.isInitialized) {
                    adaptadorOrdenDraft.notifyDataSetChanged()
                    Log.d("CREAR_LIGA", "Adaptador notificado de cambios")
                } else {
                    Log.w("CREAR_LIGA", "Adaptador no inicializado, reconfigurado...")
                    configurarAdaptador()
                    configurarRecyclerView()
                }

                actualizarEstadisticas()
                actualizarNumerosOrden()

                Log.d("CREAR_LIGA", "Lista de usuarios actualizada en UI")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CREAR_LIGA", "Error al cargar usuarios: ${error.message}")
                Toast.makeText(this@CrearLiga, "Error al cargar usuarios: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Genera un ID Gaming por defecto
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
     * Actualiza los números de orden después del drag & drop
     */
    private fun actualizarNumerosOrden() {
        usuariosParticipantes.forEachIndexed { index, usuario ->
            // El adaptador manejará automáticamente la numeración
        }
        adaptadorOrdenDraft.notifyDataSetChanged()
        actualizarContadorSeleccionados()
    }

    /**
     * Mezcla el orden de usuarios de forma aleatoria
     */
    private fun mezclarOrdenAleatorio() {
        if (usuariosParticipantes.size <= 1) return

        // Mantener al admin en primera posición si está presente
        val adminIndex = usuariosParticipantes.indexOfFirst { it.esAdmin }
        val admin = if (adminIndex >= 0) usuariosParticipantes.removeAt(adminIndex) else null

        // Mezclar el resto de usuarios
        usuariosParticipantes.shuffle()

        // Volver a insertar al admin al principio si existía
        admin?.let { usuariosParticipantes.add(0, it) }

        adaptadorOrdenDraft.notifyDataSetChanged()
        actualizarNumerosOrden()

        Toast.makeText(this, "🎲 Orden aleatorio aplicado", Toast.LENGTH_SHORT).show()
    }

    /**
     * Resetea al orden original (admin primero, luego alfabético)
     */
    private fun resetearOrdenOriginal() {
        usuariosParticipantes.sortWith(
            compareBy<UsuarioParticipante> { !it.esAdmin }
                .thenBy { it.idGaming.lowercase() }
        )

        adaptadorOrdenDraft.notifyDataSetChanged()
        actualizarNumerosOrden()

        Toast.makeText(this, "🔄 Orden original restaurado", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarContadorSeleccionados() {
        val total = usuariosParticipantes.size
        binding.tvContadorSeleccionados.text = "$total participantes en el draft"
    }

    private fun actualizarEstadisticas() {
        val total = usuariosParticipantes.size
        binding.tvEstadisticasUsuarios.text = "Total de usuarios registrados: $total"
        actualizarContadorSeleccionados()

        // VERIFICAR ESTADO DEL BOTÓN
        Log.d("CREAR_LIGA", "Actualizando estadísticas - Total usuarios: $total")
        Log.d("CREAR_LIGA", "Estado del botón después de actualizar: habilitado=${binding.btnCrearLiga.isEnabled}, visible=${binding.btnCrearLiga.visibility == View.VISIBLE}")

        // HABILITAR EL BOTÓN SI HAY USUARIOS
        if (total >= 1) {
            binding.btnCrearLiga.isEnabled = true
            binding.btnCrearLiga.alpha = 1.0f
            Log.d("CREAR_LIGA", "Botón habilitado - hay $total usuarios")
        } else {
            binding.btnCrearLiga.isEnabled = false
            binding.btnCrearLiga.alpha = 0.5f
            Log.d("CREAR_LIGA", "Botón deshabilitado - solo hay $total usuarios")
        }
    }

    private var nombreLiga = ""

    private fun validarDatos() {
        Log.d("CREAR_LIGA", "=== INICIANDO VALIDACIÓN ===")
        Toast.makeText(this, "Verificando datos...", Toast.LENGTH_SHORT).show()

        nombreLiga = binding.etLiga.text.toString().trim()

        Log.d("CREAR_LIGA", "Nombre liga: '$nombreLiga'")
        Log.d("CREAR_LIGA", "Usuarios participantes: ${usuariosParticipantes.size}")

        if (nombreLiga.isEmpty()) {
            Log.e("CREAR_LIGA", "Nombre de liga vacío")
            binding.etLiga.error = "Ingrese el nombre de la liga"
            binding.etLiga.requestFocus()
            Toast.makeText(this, "Por favor ingresa el nombre de la liga", Toast.LENGTH_SHORT).show()
            return
        }

        if (usuariosParticipantes.size < 1) {
            Log.e("CREAR_LIGA", "Muy pocos usuarios: ${usuariosParticipantes.size}")
            Toast.makeText(this, "No hay usuarios cargados. Total: ${usuariosParticipantes.size}", Toast.LENGTH_LONG).show()
            return
        }

        // NUEVO: Verificar si ya tiene una liga activa
        verificarLigaExistente()
    }

    // 3. NUEVO MÉTODO: Verificar si el admin ya tiene una liga
    private fun verificarLigaExistente() {
        Log.d("CREAR_LIGA", "Verificando si el administrador ya tiene una liga activa...")
        progressDialog.setMessage("Verificando ligas existentes...")
        progressDialog.show()

        val database = FirebaseDatabase.getInstance()
        val ligasRef = database.getReference("Ligas")

        ligasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var ligaEncontrada: ModeloLiga? = null

                    // Buscar liga donde el usuario actual es admin
                    for (ligaSnapshot in snapshot.children) {
                        val liga = ligaSnapshot.getValue(ModeloLiga::class.java)

                        if (liga != null && liga.adminUid == firebaseAuth.uid) {
                            ligaEncontrada = liga
                            break
                        }
                    }

                    progressDialog.dismiss()

                    if (ligaEncontrada != null) {
                        // Ya tiene una liga, verificar su estado
                        ligaExistente = ligaEncontrada
                        tieneLigaActiva = true
                        evaluarEstadoLigaExistente(ligaEncontrada)
                    } else {
                        // No tiene liga, puede crear sin problemas
                        tieneLigaActiva = false
                        Log.d("CREAR_LIGA", "No hay liga existente, procediendo con creación normal")
                        mostrarDialogoConfirmacion()
                    }

                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("CREAR_LIGA", "Error verificando liga existente: ${e.message}")
                    Toast.makeText(this@CrearLiga, "Error al verificar ligas existentes: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                Log.e("CREAR_LIGA", "Error en consulta de ligas: ${error.message}")
                Toast.makeText(this@CrearLiga, "Error al consultar ligas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 4. NUEVO MÉTODO: Evaluar el estado de la liga existente
    private fun evaluarEstadoLigaExistente(liga: ModeloLiga) {
        Log.d("CREAR_LIGA", "Liga existente encontrada: ${liga.nombreLiga}")
        Log.d("CREAR_LIGA", "Estado: ${liga.estado}")
        Log.d("CREAR_LIGA", "Draft iniciado: ${liga.configuracion.configuracionDraft.draftIniciado}")
        Log.d("CREAR_LIGA", "Draft completado: ${liga.configuracion.configuracionDraft.draftCompletado}")

        val configuracionDraft = liga.configuracion.configuracionDraft

        when {
            // Liga completamente terminada
            configuracionDraft.draftCompletado -> {
                mostrarDialogoLigaCompletada(liga)
            }
            // Liga con draft activo
            configuracionDraft.draftIniciado && !configuracionDraft.draftCompletado -> {
                mostrarDialogoDraftActivo(liga)
            }
            // Liga configurada pero sin iniciar draft
            liga.estado in listOf("Configurando", "Disponible") -> {
                mostrarDialogoLigaNoTerminada(liga)
            }
            // Otros estados
            else -> {
                mostrarDialogoLigaExistente(liga)
            }
        }
    }

    // 5. NUEVO MÉTODO: Diálogo para liga completada
    private fun mostrarDialogoLigaCompletada(liga: ModeloLiga) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🏆 Liga Anterior Completada")

        val mensaje = """
            Ya tienes una liga completada:
            
            📝 Liga: ${liga.nombreLiga}
            👥 Participantes: ${liga.usuariosParticipantes.size}
            ✅ Estado: Draft completado
            
            ¿Deseas crear una nueva liga?
            Esto reemplazará tu liga anterior.
        """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("🆕 Crear Nueva Liga") { dialog, _ ->
            dialog.dismiss()
            mostrarDialogoConfirmacionReemplazo(liga, "completada")
        }

        builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // 6. NUEVO MÉTODO: Diálogo para draft activo
    private fun mostrarDialogoDraftActivo(liga: ModeloLiga) {
        val configuracionDraft = liga.configuracion.configuracionDraft

        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Draft en Progreso")

        val mensaje = """
            Ya tienes una liga con draft activo:
            
            📝 Liga: ${liga.nombreLiga}
            👥 Participantes: ${liga.usuariosParticipantes.size}
            🏆 Ronda: ${configuracionDraft.rondaActual}/4
            📊 Progreso: En turno ${configuracionDraft.turnoActual + 1}
            
            ⚠️ ADVERTENCIA:
            Si creas una nueva liga, se PERDERÁ todo el progreso del draft actual y todas las selecciones realizadas.
            
            ¿Estás seguro de que deseas continuar?
        """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("🗑️ Sí, Reemplazar Liga") { dialog, _ ->
            dialog.dismiss()
            mostrarDialogoConfirmacionReemplazo(liga, "activa")
        }

        builder.setNegativeButton("❌ No, Mantener Liga Actual") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // 7. NUEVO MÉTODO: Diálogo para liga no terminada
    private fun mostrarDialogoLigaNoTerminada(liga: ModeloLiga) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚙️ Liga en Configuración")

        val mensaje = """
            Ya tienes una liga en configuración:
            
            📝 Liga: ${liga.nombreLiga}
            👥 Participantes: ${liga.usuariosParticipantes.size}
            📊 Estado: ${liga.estado}
            
            Puedes:
            • Continuar configurando la liga existente
            • Crear una nueva liga (reemplazará la actual)
            
            ¿Qué deseas hacer?
        """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("🆕 Crear Nueva Liga") { dialog, _ ->
            dialog.dismiss()
            mostrarDialogoConfirmacionReemplazo(liga, "configuración")
        }

        builder.setNegativeButton("📋 Mantener Liga Actual") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // 8. NUEVO MÉTODO: Diálogo genérico para liga existente
    private fun mostrarDialogoLigaExistente(liga: ModeloLiga) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚠️ Liga Existente")

        val mensaje = """
            Ya tienes una liga creada:
            
            📝 Liga: ${liga.nombreLiga}
            👥 Participantes: ${liga.usuariosParticipantes.size}
            📊 Estado: ${liga.estado}
            
            ¿Deseas reemplazarla con una nueva liga?
        """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("🔄 Reemplazar Liga") { dialog, _ ->
            dialog.dismiss()
            mostrarDialogoConfirmacionReemplazo(liga, "existente")
        }

        builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // 9. NUEVO MÉTODO: Confirmación final de reemplazo
    private fun mostrarDialogoConfirmacionReemplazo(ligaAnterior: ModeloLiga, tipoLiga: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🚨 Confirmación Final")

        val advertencia = when (tipoLiga) {
            "activa" -> "se PERDERÁ todo el progreso del draft y las selecciones"
            "configuración" -> "se perderá la configuración actual"
            "completada" -> "se archivará la liga completada"
            else -> "se eliminará la liga anterior"
        }

        val mensaje = """
            ⚠️ CONFIRMACIÓN FINAL ⚠️
            
            Estás a punto de crear:
            📝 Nueva Liga: "$nombreLiga"
            👥 Con ${usuariosParticipantes.size} participantes
            
            Esto significa que:
            🗑️ Liga anterior "${ligaAnterior.nombreLiga}" será eliminada
            ⚠️ $advertencia
            
            ¿Estás 100% seguro de continuar?
        """.trimIndent()

        builder.setMessage(mensaje)

        builder.setPositiveButton("✅ SÍ, CONFIRMAR") { dialog, _ ->
            dialog.dismiss()
            eliminarLigaAnteriorYCrearNueva(ligaAnterior)
        }

        builder.setNegativeButton("❌ NO, CANCELAR") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    // 10. NUEVO MÉTODO: Eliminar liga anterior y crear nueva
    private fun eliminarLigaAnteriorYCrearNueva(ligaAnterior: ModeloLiga) {
        Log.d("CREAR_LIGA", "Eliminando liga anterior: ${ligaAnterior.nombreLiga}")

        progressDialog.setMessage("Eliminando liga anterior...")
        progressDialog.show()

        val database = FirebaseDatabase.getInstance()
        val ligaAnteriorRef = database.getReference("Ligas").child(ligaAnterior.id)

        // Eliminar liga anterior
        ligaAnteriorRef.removeValue()
            .addOnSuccessListener {
                Log.d("CREAR_LIGA", "Liga anterior eliminada exitosamente")

                // También eliminar lineups seleccionados de la liga anterior si existían
                eliminarLineupsLigaAnterior(ligaAnterior) {
                    // Después de limpiar todo, crear la nueva liga
                    Toast.makeText(this, "Liga anterior eliminada. Creando nueva liga...", Toast.LENGTH_SHORT).show()
                    crearLiga()
                }
            }
            .addOnFailureListener { error ->
                progressDialog.dismiss()
                Log.e("CREAR_LIGA", "Error eliminando liga anterior: ${error.message}")
                Toast.makeText(this, "Error al eliminar liga anterior: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 11. NUEVO MÉTODO: Eliminar lineups de liga anterior
    private fun eliminarLineupsLigaAnterior(ligaAnterior: ModeloLiga, onComplete: () -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val lineupsRef = database.getReference("LineupsSeleccionados")

        // Buscar y eliminar lineups de participantes de la liga anterior
        lineupsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uidsLigaAnterior = ligaAnterior.usuariosParticipantes.map { it.uid }
                val lineupsAEliminar = mutableListOf<String>()

                for (lineupSnapshot in snapshot.children) {
                    val usuarioId = lineupSnapshot.child("usuarioId").value as? String
                    if (usuarioId in uidsLigaAnterior) {
                        lineupsAEliminar.add(lineupSnapshot.key ?: "")
                    }
                }

                // Eliminar lineups encontrados
                if (lineupsAEliminar.isNotEmpty()) {
                    Log.d("CREAR_LIGA", "Eliminando ${lineupsAEliminar.size} lineups de liga anterior")
                    var eliminados = 0

                    for (lineupKey in lineupsAEliminar) {
                        if (lineupKey.isNotEmpty()) {
                            lineupsRef.child(lineupKey).removeValue()
                                .addOnCompleteListener {
                                    eliminados++
                                    if (eliminados == lineupsAEliminar.size) {
                                        Log.d("CREAR_LIGA", "Todos los lineups eliminados")
                                        onComplete()
                                    }
                                }
                        }
                    }
                } else {
                    Log.d("CREAR_LIGA", "No hay lineups que eliminar")
                    onComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("CREAR_LIGA", "Error buscando lineups a eliminar: ${error.message}")
                // Continuar con la creación aunque no se puedan eliminar los lineups
                onComplete()
            }
        })
    }

    private fun mostrarDialogoConfirmacion() {
        Log.d("CREAR_LIGA", "Mostrando diálogo de confirmación")

        val ordenActual = usuariosParticipantes.mapIndexed { index, usuario ->
            "${index + 1}. ${usuario.idGaming}${if (usuario.esAdmin) " (Admin)" else ""}"
        }.joinToString("\n")

        // Mensaje diferente si tiene liga existente
        val mensajeAdicional = if (tieneLigaActiva) {
            "\n⚠️ NOTA: Esta será tu nueva liga (reemplazará cualquier liga anterior).\n"
        } else {
            ""
        }

        val mensaje = """
            ¿Crear liga con la siguiente configuración?
            
            📝 Nombre: $nombreLiga
            👥 Participantes: ${usuariosParticipantes.size} usuarios
            👑 Administrador: $nombreUsuarioAdmin
            
            📋 Orden de Draft:
            $ordenActual
            $mensajeAdicional
            Este será el orden definitivo para el draft.
        """.trimIndent()

        Log.d("CREAR_LIGA", "Mensaje del diálogo: $mensaje")

        try {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("🏆 Confirmar Creación de Liga")
            builder.setMessage(mensaje)

            builder.setPositiveButton("✅ Crear Liga") { dialog, _ ->
                Log.d("CREAR_LIGA", "Usuario confirmó creación")
                dialog.dismiss()
                crearLiga()
            }

            builder.setNegativeButton("❌ Cancelar") { dialog, _ ->
                Log.d("CREAR_LIGA", "Usuario canceló creación")
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
            Log.d("CREAR_LIGA", "Diálogo mostrado exitosamente")

        } catch (e: Exception) {
            Log.e("CREAR_LIGA", "Error al mostrar diálogo: ${e.message}")
            Toast.makeText(this, "Error al mostrar confirmación: ${e.message}", Toast.LENGTH_SHORT).show()
            crearLiga()
        }
    }

    private fun crearLiga() {
        progressDialog.setMessage("Creando Liga...")
        progressDialog.show()

        // NUEVO: Primero asegurar que existen los equipos globales
        EquiposManager.inicializarEquiposGlobales { exito ->
            if (exito) {
                Log.d("CREAR_LIGA", "Equipos globales verificados/creados exitosamente")
                procederConCreacionLiga()
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Error al inicializar equipos. Intenta nuevamente.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * NUEVO: Procede con la creación de la liga una vez que los equipos están listos
     */
    private fun procederConCreacionLiga() {
        val tiempo = "${Constantes.obtenerTiempoDis()}"
        val ref = FirebaseDatabase.getInstance().getReference("Ligas")
        val keyId = ref.push().key

        // Crear el orden de turnos basado en la posición actual
        val ordenTurnos = usuariosParticipantes.map { it.uid }
        val usuariosPermitidos = usuariosParticipantes.map { it.uid }

        val configuracionDraft = mapOf(
            "ordenTurnos" to ordenTurnos,
            "draftIniciado" to false,
            "draftCompletado" to false,
            "rondaActual" to 1,
            "turnoActual" to 0,
            "tiempoLimiteSeleccion" to 120,
            "configuradoPorAdmin" to true
        )

        val configuracion = mapOf(
            "configuracionDraft" to configuracionDraft
        )

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "${keyId}"
        hashMap["adminUid"] = "${firebaseAuth.uid}"
        hashMap["adminNombre"] = nombreUsuarioAdmin
        hashMap["nombreLiga"] = "${nombreLiga}"
        hashMap["numeroParticipantes"] = "${usuariosParticipantes.size}"
        hashMap["estado"] = "Configurando"
        hashMap["tiempo"] = "${tiempo}"
        hashMap["imgLiga"] = ""
        hashMap["usuariosPermitidos"] = usuariosPermitidos
        hashMap["usuariosParticipantes"] = usuariosParticipantes.map { usuario ->
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
        hashMap["configuracion"] = configuracion
        hashMap["fechaCreacion"] = System.currentTimeMillis()
        // NUEVO: Solo referenciar que usa equipos globales
        hashMap["usaEquiposGlobales"] = true

        ref.child(keyId!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Liga '$nombreLiga' creada exitosamente con ${usuariosParticipantes.size} participantes",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "No se pudo crear la liga debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Mantener código de manejo de imágenes...
    private fun mostrarOpcionesImg() {
        val popupMenu = PopupMenu(this, binding.agregarImg)
        popupMenu.menu.add(Menu.NONE,1,1,"Camara")
        popupMenu.menu.add(Menu.NONE,2,2,"Galeria")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA))
                } else {
                    concederPermisoCamara.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    imagenGaleria()
                } else {
                    concederPermisoGaleria.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            true
        }
    }

    private val concederPermisoGaleria = registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
        if (esConcedido) {
            imagenGaleria()
        } else {
            Toast.makeText(this, "Permisos no concedidos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleria_Arl.launch(intent)
    }

    private val resultadoGaleria_Arl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val data = resultado.data
            imagenUri = data!!.data
        } else {
            Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    private val concederPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        var concedidoTodos = true
        for (permiso in resultado) {
            for (seConcede in resultado.values) {
                concedidoTodos = concedidoTodos && seConcede
            }
            if (concedidoTodos) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                imagenCamara()
            } else {
                Toast.makeText(this, "Permisos no concedidos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun imagenCamara() {
        val contenValues = ContentValues()
        contenValues.put(MediaStore.Images.Media.TITLE, "Titulo_imagen")
        contenValues.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")

        imagenUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contenValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imagenUri)
        resultadoCamara_Arl.launch(intent)
    }

    private val resultadoCamara_Arl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            // Imagen capturada exitosamente
        } else {
            Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
        }
    }
}