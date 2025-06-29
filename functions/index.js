// Actualización para buscarLineupDisponible con más logging

async function buscarLineupDisponible(ligaId, usuarioId) {
  try {
    console.log(`🔍 Buscando lineup disponible para usuario: ${usuarioId}`);

    // Obtener lineups ya seleccionados
    const db = getDatabase();
    const lineupsRef = db.ref('LineupsSeleccionados');
    const lineupsSnapshot = await lineupsRef.once('value');
    const lineupsSeleccionados = lineupsSnapshot.val() || {};

    console.log(`📋 Lineups ya seleccionados: ${Object.keys(lineupsSeleccionados).length}`);
    console.log(`📋 Claves existentes: ${Object.keys(lineupsSeleccionados).join(', ')}`);

    // Obtener selecciones del usuario
    const seleccionesUsuario = Object.values(lineupsSeleccionados)
      .filter(lineup => lineup.usuarioId === usuarioId)
      .map(lineup => lineup.tipoLineup);

    console.log(`👤 Usuario ${usuarioId} ya seleccionó: ${seleccionesUsuario.join(', ')}`);

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
      console.log(`🔍 Verificando tipo: ${tipo}`);

      // Verificar si ya seleccionó este tipo
      if (seleccionesUsuario.includes(tipo)) {
        console.log(`❌ Usuario ya seleccionó ${tipo}, saltando...`);
        continue;
      }

      for (const equipo of equipos) {
        const claveLineup = `${equipo}_${tipo}`;

        // Verificar si está disponible
        if (!lineupsSeleccionados[claveLineup]) {
          console.log(`✅ Lineup disponible encontrado: ${claveLineup}`);
          return {
            tipo: tipo,
            equipoId: equipo,
            jugadores: generarJugadoresAutomaticos(tipo)
          };
        } else {
          console.log(`❌ ${claveLineup} ya ocupado por ${lineupsSeleccionados[claveLineup].usuarioId}`);
        }
      }

      console.log(`❌ No hay equipos disponibles para tipo ${tipo}`);
    }

    console.log(`❌ No se encontró ningún lineup disponible para usuario ${usuarioId}`);
    return null;
  } catch (error) {
    console.error('❌ Error buscando lineup disponible:', error);
    return null;
  }
}

// TAMBIÉN actualizar procesarTemporizadorVencido con más logging:

async function procesarTemporizadorVencido(ligaId, temporizador) {
  try {
    console.log(`🔄 === PROCESANDO TEMPORIZADOR VENCIDO ===`);
    console.log(`🔄 Liga: ${ligaId}`);
    console.log(`🔄 Ronda: ${temporizador.ronda}, Turno: ${temporizador.turno}`);
    console.log(`🔄 Usuario: ${temporizador.usuarioEnTurno}`);

    // Verificar que el draft sigue activo y en el mismo turno
    const db = getDatabase();
    const ligaRef = db.ref(`Ligas/${ligaId}/configuracion/configuracionDraft`);
    const ligaSnapshot = await ligaRef.once('value');

    if (!ligaSnapshot.exists()) {
      console.log(`❌ Liga ${ligaId} no encontrada`);
      return;
    }

    const configDraft = ligaSnapshot.val();
    console.log(`📊 Estado actual del draft:`, configDraft);

    // Verificar que el turno no haya cambiado
    if (configDraft.rondaActual !== temporizador.ronda ||
        configDraft.turnoActual !== temporizador.turno) {
      console.log(`❌ Liga ${ligaId}: El turno ya cambió`);
      console.log(`   - Draft: R${configDraft.rondaActual}-T${configDraft.turnoActual}`);
      console.log(`   - Temporizador: R${temporizador.ronda}-T${temporizador.turno}`);
      await desactivarTemporizador(ligaId);
      return;
    }

    // Verificar que el draft sigue activo
    if (!configDraft.draftIniciado || configDraft.draftCompletado) {
      console.log(`❌ Liga ${ligaId}: Draft ya no está activo`);
      console.log(`   - Iniciado: ${configDraft.draftIniciado}`);
      console.log(`   - Completado: ${configDraft.draftCompletado}`);
      await desactivarTemporizador(ligaId);
      return;
    }

    console.log(`✅ Condiciones OK, procediendo con selección automática...`);

    // Realizar selección automática
    await realizarSeleccionAutomatica(ligaId, temporizador, configDraft);

    // Desactivar temporizador
    await desactivarTemporizador(ligaId);

    console.log(`✅ Liga ${ligaId}: Selección automática completada`);

  } catch (error) {
    console.error(`❌ Error procesando temporizador vencido para liga ${ligaId}:`, error);
  }
}