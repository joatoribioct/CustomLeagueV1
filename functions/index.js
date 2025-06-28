// functions/index.js - ACTUALIZADO para Firebase Functions v2
const {onValueUpdated, onValueCreated} = require('firebase-functions/v2/database');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {initializeApp} = require('firebase-admin/app');
const {getDatabase} = require('firebase-admin/database');

// Inicializar Firebase Admin
initializeApp();

/**
 * FUNCIÓN 1: Iniciar temporizador cuando cambia el turno
 * Se ejecuta automáticamente cuando se actualiza el draft
 */
exports.iniciarTemporizadorDraft = onValueUpdated(
  {
    ref: '/Ligas/{ligaId}/configuracion/configuracionDraft',
    region: 'us-central1'
  },
  async (event) => {
    const ligaId = event.params.ligaId;
    const antes = event.data.before.val();
    const despues = event.data.after.val();

    console.log(`Liga ${ligaId}: Cambio detectado en configuración draft`);

    // Solo proceder si el draft está activo y cambió el turno
    if (!despues || !despues.draftIniciado || despues.draftCompletado) {
      console.log(`Liga ${ligaId}: Draft no activo, saltando temporizador`);
      return null;
    }

    // Verificar si cambió el turno o la ronda
    const cambioTurno = !antes || antes.turnoActual !== despues.turnoActual;
    const cambioRonda = !antes || antes.rondaActual !== despues.rondaActual;

    if (!cambioTurno && !cambioRonda) {
      console.log(`Liga ${ligaId}: No hubo cambio de turno, saltando`);
      return null;
    }

    console.log(`Liga ${ligaId}: Iniciando temporizador para R${despues.rondaActual}-T${despues.turnoActual}`);

    // Crear el temporizador
    const tiempoLimite = despues.tiempoLimiteSeleccion || 180; // 3 minutos por defecto
    const timestampInicio = Date.now();
    const timestampVencimiento = timestampInicio + (tiempoLimite * 1000);

    // Guardar información del temporizador
    const db = getDatabase();
    const temporizadorRef = db.ref(`TemporizadoresDraft/${ligaId}`);

    await temporizadorRef.set({
      ligaId: ligaId,
      ronda: despues.rondaActual,
      turno: despues.turnoActual,
      usuarioEnTurno: despues.ordenTurnos[despues.turnoActual] || '',
      timestampInicio: timestampInicio,
      timestampVencimiento: timestampVencimiento,
      tiempoLimiteSegundos: tiempoLimite,
      activo: true,
      createdAt: Date.now()
    });

    console.log(`Liga ${ligaId}: Temporizador creado, vence en ${tiempoLimite} segundos`);
    return null;
  }
);

/**
 * FUNCIÓN 2: Función programada que verifica temporizadores vencidos cada minuto
 */
exports.verificarTemporizadoresVencidos = onSchedule(
  {
    schedule: 'every 1 minutes',
    region: 'us-central1'
  },
  async (event) => {
    console.log('🕐 Verificando temporizadores vencidos...');

    const ahora = Date.now();
    const db = getDatabase();
    const temporizadoresRef = db.ref('TemporizadoresDraft');

    try {
      const snapshot = await temporizadoresRef.once('value');

      if (!snapshot.exists()) {
        console.log('No hay temporizadores activos');
        return null;
      }

      const temporizadores = snapshot.val();
      const promesasProcesamiento = [];

      for (const ligaId in temporizadores) {
        const temporizador = temporizadores[ligaId];

        // Verificar si el temporizador está vencido
        if (temporizador.activo && ahora >= temporizador.timestampVencimiento) {
          console.log(`⏰ Temporizador vencido para liga ${ligaId}, R${temporizador.ronda}-T${temporizador.turno}`);
          promesasProcesamiento.push(procesarTemporizadorVencido(ligaId, temporizador));
        }
      }

      if (promesasProcesamiento.length > 0) {
        await Promise.all(promesasProcesamiento);
        console.log(`✅ Procesados ${promesasProcesamiento.length} temporizadores vencidos`);
      }

      return null;
    } catch (error) {
      console.error('Error verificando temporizadores:', error);
      return null;
    }
  }
);

/**
 * FUNCIÓN 3: Procesar temporizador vencido (selección automática)
 */
async function procesarTemporizadorVencido(ligaId, temporizador) {
  try {
    console.log(`🔄 Procesando temporizador vencido para liga ${ligaId}`);

    // Verificar que el draft sigue activo y en el mismo turno
    const db = getDatabase();
    const ligaRef = db.ref(`Ligas/${ligaId}/configuracion/configuracionDraft`);
    const ligaSnapshot = await ligaRef.once('value');

    if (!ligaSnapshot.exists()) {
      console.log(`Liga ${ligaId} no encontrada`);
      return;
    }

    const configDraft = ligaSnapshot.val();

    // Verificar que el turno no haya cambiado
    if (configDraft.rondaActual !== temporizador.ronda ||
        configDraft.turnoActual !== temporizador.turno) {
      console.log(`Liga ${ligaId}: El turno ya cambió, cancelando procesamiento automático`);
      await desactivarTemporizador(ligaId);
      return;
    }

    // Verificar que el draft sigue activo
    if (!configDraft.draftIniciado || configDraft.draftCompletado) {
      console.log(`Liga ${ligaId}: Draft ya no está activo`);
      await desactivarTemporizador(ligaId);
      return;
    }

    // Realizar selección automática
    await realizarSeleccionAutomatica(ligaId, temporizador, configDraft);

    // Desactivar temporizador
    await desactivarTemporizador(ligaId);

    console.log(`✅ Liga ${ligaId}: Selección automática completada`);

  } catch (error) {
    console.error(`Error procesando temporizador vencido para liga ${ligaId}:`, error);
  }
}

/**
 * FUNCIÓN 4: Realizar selección automática
 */
async function realizarSeleccionAutomatica(ligaId, temporizador, configDraft) {
  const usuarioId = temporizador.usuarioEnTurno;

  console.log(`🤖 Realizando selección automática para usuario ${usuarioId} en liga ${ligaId}`);

  // Buscar lineup disponible
  const lineupDisponible = await buscarLineupDisponible(ligaId, usuarioId);

  if (!lineupDisponible) {
    console.log(`No se encontró lineup disponible para ${usuarioId}, avanzando turno`);
    await avanzarTurno(ligaId, configDraft);
    return;
  }

  // Guardar selección automática
  await guardarSeleccionAutomatica(ligaId, usuarioId, lineupDisponible, temporizador);

  // Avanzar turno
  await avanzarTurno(ligaId, configDraft);

  console.log(`🎯 Selección automática: ${lineupDisponible.tipo} de ${lineupDisponible.equipoId} para ${usuarioId}`);
}

/**
 * FUNCIÓN 5: Buscar lineup disponible para selección automática
 */
async function buscarLineupDisponible(ligaId, usuarioId) {
  try {
    // Obtener lineups ya seleccionados
    const db = getDatabase();
    const lineupsRef = db.ref('LineupsSeleccionados');
    const lineupsSnapshot = await lineupsRef.once('value');
    const lineupsSeleccionados = lineupsSnapshot.val() || {};

    // Obtener selecciones del usuario
    const seleccionesUsuario = Object.values(lineupsSeleccionados)
      .filter(lineup => lineup.usuarioId === usuarioId)
      .map(lineup => lineup.tipoLineup);

    // Tipos de lineup en orden de prioridad
    const tiposLineup = ['infield', 'outfield', 'pitchers', 'relief'];
    const equipos = [
      'Diamondbacks', 'Braves', 'Orioles', 'RedSox', 'WhiteSox', 'Cubs',
      'Reds', 'Guardians', 'Rockies', 'Tigers', 'Astros', 'Royals',
      'Angels', 'Dodgers', 'Marlins', 'Brewers', 'Twins', 'Yankees',
      'Mets', 'Athletics', 'Phillies', 'Pirates', 'Padres', 'Giants',
      'Mariners', 'Cardinals', 'Rays', 'Rangers', 'BlueJays', 'Nationals'
    ];

    // Buscar primer lineup disponible
    for (const tipo of tiposLineup) {
      // Verificar si ya seleccionó este tipo
      if (seleccionesUsuario.includes(tipo)) continue;

      for (const equipo of equipos) {
        const claveLineup = `${equipo}_${tipo}`;

        // Verificar si está disponible
        if (!lineupsSeleccionados[claveLineup]) {
          return {
            tipo: tipo,
            equipoId: equipo,
            jugadores: generarJugadoresAutomaticos(tipo)
          };
        }
      }
    }

    return null;
  } catch (error) {
    console.error('Error buscando lineup disponible:', error);
    return null;
  }
}

/**
 * FUNCIÓN 6: Generar jugadores automáticos para selección
 */
function generarJugadoresAutomaticos(tipo) {
  const jugadores = {};

  switch (tipo) {
    case 'infield':
      jugadores['C'] = { nombre: 'Auto Catcher', rating: 75 };
      jugadores['1B'] = { nombre: 'Auto 1B', rating: 80 };
      jugadores['2B'] = { nombre: 'Auto 2B', rating: 78 };
      jugadores['3B'] = { nombre: 'Auto 3B', rating: 82 };
      jugadores['SS'] = { nombre: 'Auto SS', rating: 85 };
      break;
    case 'outfield':
      jugadores['LF'] = { nombre: 'Auto LF', rating: 79 };
      jugadores['CF'] = { nombre: 'Auto CF', rating: 83 };
      jugadores['RF'] = { nombre: 'Auto RF', rating: 77 };
      jugadores['DH'] = { nombre: 'Auto DH', rating: 81 };
      break;
    case 'pitchers':
      for (let i = 1; i <= 5; i++) {
        jugadores[`SP${i}`] = { nombre: `Auto SP${i}`, rating: 85 - (i * 2) };
      }
      break;
    case 'relief':
      for (let i = 1; i <= 8; i++) {
        jugadores[`RP${i}`] = { nombre: `Auto RP${i}`, rating: 80 - (i * 2) };
      }
      break;
  }

  return jugadores;
}

/**
 * FUNCIÓN 7: Guardar selección automática
 */
async function guardarSeleccionAutomatica(ligaId, usuarioId, lineup, temporizador) {
  // Obtener información del usuario
  const db = getDatabase();
  const usuarioRef = db.ref(`Usuarios/${usuarioId}`);
  const usuarioSnapshot = await usuarioRef.once('value');
  const usuario = usuarioSnapshot.val() || {};

  const lineupSeleccionado = {
    equipoId: lineup.equipoId,
    tipoLineup: lineup.tipo,
    usuarioId: usuarioId,
    nombreUsuario: usuario.nombres || 'Usuario desconocido',
    idGamingUsuario: usuario.idGaming || 'Player_Auto',
    fechaSeleccion: Date.now(),
    jugadores: lineup.jugadores,
    ronda: temporizador.ronda,
    turno: temporizador.turno,
    seleccionAutomatica: true // Marcar como selección automática
  };

  const claveLineup = `${lineup.equipoId}_${lineup.tipo}`;
  await db.ref(`LineupsSeleccionados/${claveLineup}`).set(lineupSeleccionado);

  console.log(`💾 Selección automática guardada: ${claveLineup} para ${usuarioId}`);
}

/**
 * FUNCIÓN 8: Avanzar turno
 */
async function avanzarTurno(ligaId, configDraft) {
  const totalParticipantes = configDraft.ordenTurnos.length;
  let nuevoTurno = configDraft.turnoActual + 1;
  let nuevaRonda = configDraft.rondaActual;

  // Si completamos todos los turnos de la ronda
  if (nuevoTurno >= totalParticipantes) {
    nuevoTurno = 0;
    nuevaRonda += 1;
  }

  // Verificar si el draft se completó
  const draftCompletado = nuevaRonda > 4;

  const updates = {
    turnoActual: nuevoTurno,
    rondaActual: nuevaRonda,
    draftCompletado: draftCompletado
  };

  if (draftCompletado) {
    updates.draftIniciado = false;
    updates.fechaFinalizacion = Date.now();
  }

  const db = getDatabase();
  await db.ref(`Ligas/${ligaId}/configuracion/configuracionDraft`).update(updates);

  console.log(`➡️ Liga ${ligaId}: Turno avanzado a R${nuevaRonda}-T${nuevoTurno}`);
}

/**
 * FUNCIÓN 9: Desactivar temporizador
 */
async function desactivarTemporizador(ligaId) {
  const db = getDatabase();
  await db.ref(`TemporizadoresDraft/${ligaId}`).update({
    activo: false,
    desactivadoEn: Date.now()
  });
}

/**
 * FUNCIÓN 10: Cancelar temporizador manualmente (cuando usuario selecciona)
 */
exports.cancelarTemporizador = onValueCreated(
  {
    ref: '/LineupsSeleccionados/{lineupId}',
    region: 'us-central1'
  },
  async (event) => {
    const lineupData = event.data.val();
    const usuarioId = lineupData.usuarioId;

    console.log(`🛑 Usuario ${usuarioId} seleccionó lineup, buscando su liga para cancelar temporizador`);

    // Buscar liga del usuario
    const db = getDatabase();
    const ligasRef = db.ref('Ligas');
    const ligasSnapshot = await ligasRef.once('value');

    if (!ligasSnapshot.exists()) return null;

    const ligas = ligasSnapshot.val();
    let ligaId = null;

    // Encontrar liga donde participa el usuario
    for (const id in ligas) {
      const liga = ligas[id];
      if (liga.usuariosPermitidos && liga.usuariosPermitidos.includes(usuarioId)) {
        ligaId = id;
        break;
      }
    }

    if (!ligaId) {
      console.log(`No se encontró liga para usuario ${usuarioId}`);
      return null;
    }

    // Desactivar temporizador de esa liga
    console.log(`🛑 Cancelando temporizador de liga ${ligaId} para usuario ${usuarioId}`);
    await desactivarTemporizador(ligaId);

    return null;
  }
);