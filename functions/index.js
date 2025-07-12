const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { getDatabase } = require('firebase-admin/database');

admin.initializeApp();

async function buscarMejorLineupDisponible(ligaId, usuarioId) {
  try {
    console.log(`🎯 Buscando MEJOR lineup disponible para usuario: ${usuarioId}`);

    // Obtener lineups ya seleccionados
    const { getDatabase } = require('firebase-admin/database');
    const db = getDatabase();
    const lineupsRef = db.ref('LineupsSeleccionados');
    const lineupsSnapshot = await lineupsRef.once('value');
    const lineupsSeleccionados = lineupsSnapshot.val() || {};

    console.log(`📋 Lineups ya seleccionados: ${Object.keys(lineupsSeleccionados).length}`);

    // Obtener selecciones del usuario
    const seleccionesUsuario = Object.values(lineupsSeleccionados)
      .filter(lineup => lineup.usuarioId === usuarioId)
      .map(lineup => lineup.tipoLineup);

    console.log(`👤 Usuario ${usuarioId} ya seleccionó: ${seleccionesUsuario.join(', ')}`);

    // Obtener datos de equipos
    const equiposData = await obtenerDatosEquipos();

    // Tipos de lineup en orden de prioridad
    const tiposLineup = ['infield', 'outfield', 'pitchers', 'relief'];

    // Lista para almacenar todos los lineups disponibles con sus ratings
    let lineupsDisponibles = [];

    // Buscar TODOS los lineups disponibles y calcular sus ratings
    for (const tipo of tiposLineup) {
      // Verificar si ya seleccionó este tipo
      if (seleccionesUsuario.includes(tipo)) {
        console.log(`❌ Usuario ya seleccionó ${tipo}, saltando...`);
        continue;
      }

      for (const [equipoKey, equipoData] of Object.entries(equiposData)) {
        const claveLineup = `${equipoKey}_${tipo}`;

        // Verificar si está disponible
        if (!lineupsSeleccionados[claveLineup]) {
          const jugadores = equipoData.jugadores[tipo];

          if (jugadores && Object.keys(jugadores).length > 0) {
            // Calcular rating promedio del lineup
            const ratingPromedio = calcularRatingPromedio(jugadores);

            lineupsDisponibles.push({
              tipo: tipo,
              equipoId: equipoKey,
              equipoNombre: equipoData.nombre,
              jugadores: jugadores,
              ratingPromedio: ratingPromedio,
              claveLineup: claveLineup
            });

            console.log(`✅ Lineup disponible: ${claveLineup} - Rating: ${ratingPromedio}`);
          }
        } else {
          console.log(`❌ ${claveLineup} ya ocupado por ${lineupsSeleccionados[claveLineup].usuarioId}`);
        }
      }
    }

    if (lineupsDisponibles.length === 0) {
      console.log(`❌ No se encontró ningún lineup disponible para usuario ${usuarioId}`);
      return null;
    }

    // ORDENAR por rating promedio (de mayor a menor) y luego por prioridad de tipo
    lineupsDisponibles.sort((a, b) => {
      // Primero por rating promedio (descendente)
      if (b.ratingPromedio !== a.ratingPromedio) {
        return b.ratingPromedio - a.ratingPromedio;
      }

      // Si tienen mismo rating, por prioridad de tipo
      const prioridadTipo = {
        'infield': 1,
        'outfield': 2,
        'pitchers': 3,
        'relief': 4
      };

      return prioridadTipo[a.tipo] - prioridadTipo[b.tipo];
    });

    // Seleccionar el MEJOR lineup disponible
    const mejorLineup = lineupsDisponibles[0];

    console.log(`🏆 MEJOR lineup encontrado:`);
    console.log(`   - Equipo: ${mejorLineup.equipoNombre} (${mejorLineup.equipoId})`);
    console.log(`   - Tipo: ${mejorLineup.tipo}`);
    console.log(`   - Rating Promedio: ${mejorLineup.ratingPromedio}`);
    console.log(`   - Jugadores: ${Object.keys(mejorLineup.jugadores).length}`);

    return {
      tipo: mejorLineup.tipo,
      equipoId: mejorLineup.equipoId,
      jugadores: mejorLineup.jugadores,
      ratingPromedio: mejorLineup.ratingPromedio,
      equipoNombre: mejorLineup.equipoNombre
    };

  } catch (error) {
    console.error('❌ Error buscando mejor lineup disponible:', error);
    return null;
  }
}

// Función para calcular rating promedio
function calcularRatingPromedio(jugadores) {
  try {
    const ratings = Object.values(jugadores)
      .map(jugador => {
        const rating = jugador.rating;
        return typeof rating === 'number' ? rating :
               typeof rating === 'string' ? parseInt(rating) || 0 : 0;
      })
      .filter(rating => rating > 0);

    if (ratings.length === 0) return 0;

    const suma = ratings.reduce((acc, rating) => acc + rating, 0);
    return Math.round(suma / ratings.length);
  } catch (error) {
    console.error('Error calculando rating promedio:', error);
    return 0;
  }
}

// Función para calcular rating promedio de un conjunto de jugadores
function calcularRatingPromedio(jugadores) {
  try {
    const ratings = Object.values(jugadores)
      .map(jugador => {
        const rating = jugador.rating;
        return typeof rating === 'number' ? rating :
               typeof rating === 'string' ? parseInt(rating) || 0 : 0;
      })
      .filter(rating => rating > 0);

    if (ratings.length === 0) return 0;

    const suma = ratings.reduce((acc, rating) => acc + rating, 0);
    return Math.round(suma / ratings.length);
  } catch (error) {
    console.error('Error calculando rating promedio:', error);
    return 0;
  }
}

// Función para obtener datos de equipos (debes implementar según tu estructura)
async function obtenerDatosEquipos() {
  // Esta función debe retornar los datos de equipos desde tu EquiposManager
  // Puedes almacenarlos en Firebase o incluirlos directamente aquí

  const equipos = {
    "Diamondbacks": {
      "nombre": "Arizona Diamondbacks",
      "jugadores": {
        "infield": {
          "C": {"nombre": "G.MORENO", "rating": 78},
          "1B": {"nombre": "C.WALKER", "rating": 82},
          "2B": {"nombre": "K.MARTE", "rating": 85},
          "3B": {"nombre": "E.SUAREZ", "rating": 81},
          "SS": {"nombre": "G.PERDOMO", "rating": 76}
        },
        "outfield": {
          "LF": {"nombre": "L.GURRIEL", "rating": 79},
          "CF": {"nombre": "A.THOMAS", "rating": 77},
          "RF": {"nombre": "C.CARROLL", "rating": 83},
          "DH": {"nombre": "J.BELL", "rating": 74}
        },
        "pitchers": {
          "SP1": {"nombre": "Z.GALLEN", "rating": 83},
          "SP2": {"nombre": "M.KELLY", "rating": 78},
          "SP3": {"nombre": "E.RODRIGUEZ", "rating": 76},
          "SP4": {"nombre": "B.PFAADT", "rating": 74},
          "SP5": {"nombre": "R.NELSON", "rating": 72}
        },
        "relief": {
          "RP1": {"nombre": "R.NELSON", "rating": 68},
          "RP2": {"nombre": "R.THOMPSON", "rating": 69},
          "RP3": {"nombre": "K.NELSON", "rating": 68},
          "RP4": {"nombre": "K.GRAVEMAN", "rating": 66},
          "RP5": {"nombre": "S.MILLER", "rating": 74},
          "RP6": {"nombre": "J.MARTINEZ", "rating": 72},
          "RP7": {"nombre": "AJ.PUK", "rating": 70},
          "RP8": {"nombre": "K.GINKEL", "rating": 74}
        }
      }
    },

    "Braves": {
          "nombre": "Atlanta Braves",
          "jugadores": {
            "infield": {
              "C": {"nombre": "S.MURPHY", "rating": 85},
              "1B": {"nombre": "M.OLSON", "rating": 86},
              "2B": {"nombre": "O.ALBIES", "rating": 83},
              "3B": {"nombre": "A.RILEY", "rating": 84},
              "SS": {"nombre": "O.ARCIA", "rating": 78}
            },
            "outfield": {
              "LF": {"nombre": "J.SOLER", "rating": 79},
              "CF": {"nombre": "M.HARRIS", "rating": 81},
              "RF": {"nombre": "R.ACUNA", "rating": 92},
              "DH": {"nombre": "M.OZUNA", "rating": 80}
            },
            "pitchers": {
              "SP1": {"nombre": "C.SALE", "rating": 88},
              "SP2": {"nombre": "S.ALCANTARA", "rating": 75},
              "SP3": {"nombre": "C.MORTON", "rating": 77},
              "SP4": {"nombre": "R.LOPEZ", "rating": 76},
              "SP5": {"nombre": "G.ELDER", "rating": 70}
            },
            "relief": {
              "RP1": {"nombre": "A.JIMENEZ", "rating": 76},
              "RP2": {"nombre": "J.BUMMER", "rating": 72},
              "RP3": {"nombre": "D.LEE", "rating": 73},
              "RP4": {"nombre": "G.GALLEGOS", "rating": 74},
              "RP5": {"nombre": "J.HOLMES", "rating": 75},
              "RP6": {"nombre": "P.MINTER", "rating": 71},
              "RP7": {"nombre": "R.IGLESIAS", "rating": 82},
              "RP8": {"nombre": "A.VESTIL", "rating": 68}
            }
          }
        },

        "Orioles": {
          "nombre": "Baltimore Orioles",
          "jugadores": {
            "infield": {
              "C": {"nombre": "A.RUTSCHMAN", "rating": 86},
              "1B": {"nombre": "R.MOUNTCASTLE", "rating": 78},
              "2B": {"nombre": "J.HOLLIDAY", "rating": 80},
              "3B": {"nombre": "J.WESTBURG", "rating": 79},
              "SS": {"nombre": "G.HENDERSON", "rating": 84}
            },
            "outfield": {
              "LF": {"nombre": "C.COWSER", "rating": 77},
              "CF": {"nombre": "C.MULLINS", "rating": 79},
              "RF": {"nombre": "A.SANTANDER", "rating": 82},
              "DH": {"nombre": "R.O'HEARN", "rating": 73}
            },
            "pitchers": {
              "SP1": {"nombre": "C.BURNES", "rating": 89},
              "SP2": {"nombre": "Z.EFLIN", "rating": 78},
              "SP3": {"nombre": "G.RODRIGUEZ", "rating": 83},
              "SP4": {"nombre": "A.SUAREZ", "rating": 75},
              "SP5": {"nombre": "T.WELLS", "rating": 72}
            },
            "relief": {
              "RP1": {"nombre": "Y.CANO", "rating": 74},
              "RP2": {"nombre": "K.AKIN", "rating": 74},
              "RP3": {"nombre": "A.KITTREDGE", "rating": 72},
              "RP4": {"nombre": "S.DOMINGUEZ", "rating": 72},
              "RP5": {"nombre": "B.BAKER", "rating": 71},
              "RP6": {"nombre": "G.SOTO", "rating": 70},
              "RP7": {"nombre": "M.BOWMAN", "rating": 66},
              "RP8": {"nombre": "F.BAUTISTA", "rating": 80}
            }
          }
        },

        "RedSox": {
          "nombre": "Boston Red Sox",
          "jugadores": {
            "infield": {
              "C": {"nombre": "C.WONG", "rating": 75},
              "1B": {"nombre": "T.CASAS", "rating": 81},
              "2B": {"nombre": "G.DEVERS", "rating": 84},
              "3B": {"nombre": "R.DEVERS", "rating": 86},
              "SS": {"nombre": "T.STORY", "rating": 74}
            },
            "outfield": {
              "LF": {"nombre": "J.DURAN", "rating": 82},
              "CF": {"nombre": "C.RAFAELA", "rating": 78},
              "RF": {"nombre": "W.ABREU", "rating": 79},
              "DH": {"nombre": "M.YOSHIDA", "rating": 80}
            },
            "pitchers": {
              "SP1": {"nombre": "T.HOUCK", "rating": 79},
              "SP2": {"nombre": "B.KUTTER", "rating": 76},
              "SP3": {"nombre": "N.PIVETTA", "rating": 74},
              "SP4": {"nombre": "G.WHITLOCK", "rating": 75},
              "SP5": {"nombre": "C.CRAWFORD", "rating": 72}
            },
            "relief": {
              "RP1": {"nombre": "K.JANSEN", "rating": 78},
              "RP2": {"nombre": "J.WINCKOWSKI", "rating": 69},
              "RP3": {"nombre": "G.BERNARDINO", "rating": 71},
              "RP4": {"nombre": "Z.KELLY", "rating": 73},
              "RP5": {"nombre": "B.BERNARDO", "rating": 68},
              "RP6": {"nombre": "C.MARTIN", "rating": 72},
              "RP7": {"nombre": "L.WEISSERT", "rating": 67},
              "RP8": {"nombre": "J.SLATEN", "rating": 70}
            }
          }
        },

        "WhiteSox": {
          "nombre": "Chicago White Sox",
          "jugadores": {
            "infield": {
              "C": {"nombre": "K.LEE", "rating": 73},
              "1B": {"nombre": "A.VAUGHN", "rating": 78},
              "2B": {"nombre": "N.LOPEZ", "rating": 72},
              "3B": {"nombre": "Y.MONCADA", "rating": 75},
              "SS": {"nombre": "P.DEJONG", "rating": 70}
            },
            "outfield": {
              "LF": {"nombre": "E.JIMENEZ", "rating": 76},
              "CF": {"nombre": "L.ROBERT", "rating": 84},
              "RF": {"nombre": "G.SHEETS", "rating": 71},
              "DH": {"nombre": "A.BENINTENDI", "rating": 74}
            },
            "pitchers": {
              "SP1": {"nombre": "G.CROCHET", "rating": 82},
              "SP2": {"nombre": "E.FEDDE", "rating": 76},
              "SP3": {"nombre": "C.FLEXEN", "rating": 72},
              "SP4": {"nombre": "J.THORPE", "rating": 69},
              "SP5": {"nombre": "D.MARTIN", "rating": 68}
            },
            "relief": {
              "RP1": {"nombre": "M.KOPECH", "rating": 78},
              "RP2": {"nombre": "J.CROCHET", "rating": 75},
              "RP3": {"nombre": "S.WILSON", "rating": 72},
              "RP4": {"nombre": "N.NASTRINI", "rating": 67},
              "RP5": {"nombre": "F.GRAVEMAN", "rating": 71},
              "RP6": {"nombre": "T.BANKS", "rating": 69},
              "RP7": {"nombre": "B.SHAW", "rating": 70},
              "RP8": {"nombre": "J.BREBBIA", "rating": 68}
            }
          }
        },

        "Cubs": {
          "nombre": "Chicago Cubs",
          "jugadores": {
            "infield": {
              "C": {"nombre": "M.AMAYA", "rating": 74},
              "1B": {"nombre": "M.BUSCH", "rating": 76},
              "2B": {"nombre": "N.HOERNER", "rating": 79},
              "3B": {"nombre": "I.PAREDES", "rating": 81},
              "SS": {"nombre": "D.SWANSON", "rating": 82}
            },
            "outfield": {
              "LF": {"nombre": "I.HAPP", "rating": 80},
              "CF": {"nombre": "P.CROW-ARMSTRONG", "rating": 75},
              "RF": {"nombre": "S.SUZUKI", "rating": 79},
              "DH": {"nombre": "C.BELLINGER", "rating": 83}
            },
            "pitchers": {
              "SP1": {"nombre": "S.IMANAGA", "rating": 85},
              "SP2": {"nombre": "J.STEELE", "rating": 81},
              "SP3": {"nombre": "J.ASSAD", "rating": 74},
              "SP4": {"nombre": "J.TAILLON", "rating": 76},
              "SP5": {"nombre": "B.WICKS", "rating": 73}
            },
            "relief": {
              "RP1": {"nombre": "P.HODGINS", "rating": 69},
              "RP2": {"nombre": "H.NERIS", "rating": 76},
              "RP3": {"nombre": "N.PEARSON", "rating": 72},
              "RP4": {"nombre": "E.ROBERTS", "rating": 70},
              "RP5": {"nombre": "T.MILLER", "rating": 71},
              "RP6": {"nombre": "J.LITTLE", "rating": 68},
              "RP7": {"nombre": "P.ALZOLAY", "rating": 74},
              "RP8": {"nombre": "A.KING", "rating": 67}
            }
          }
        },

        "Reds": {
          "nombre": "Cincinnati Reds",
          "jugadores": {
            "infield": {
              "C": {"nombre": "T.STEPHENSON", "rating": 78},
              "1B": {"nombre": "S.STEER", "rating": 76},
              "2B": {"nombre": "J.INDIA", "rating": 80},
              "3B": {"nombre": "N.MARTE", "rating": 74},
              "SS": {"nombre": "E.DE LA CRUZ", "rating": 86}
            },
            "outfield": {
              "LF": {"nombre": "J.FRALEY", "rating": 72},
              "CF": {"nombre": "T.FRIEDL", "rating": 77},
              "RF": {"nombre": "N.SENZEL", "rating": 73},
              "DH": {"nombre": "C.ENCARNACION", "rating": 75}
            },
            "pitchers": {
              "SP1": {"nombre": "H.GREENE", "rating": 84},
              "SP2": {"nombre": "N.LODOLO", "rating": 82},
              "SP3": {"nombre": "F.MONTAS", "rating": 74},
              "SP4": {"nombre": "A.ABBOTT", "rating": 72},
              "SP5": {"nombre": "B.WILLIAMSON", "rating": 70}
            },
            "relief": {
              "RP1": {"nombre": "A.DIAZ", "rating": 81},
              "RP2": {"nome": "F.CRUZ", "rating": 76},
              "RP3": {"nombre": "B.FARMER", "rating": 73},
              "RP4": {"nombre": "S.MOLL", "rating": 71},
              "RP5": {"nombre": "Y.SANTILLAN", "rating": 69},
              "RP6": {"nombre": "C.YOUNG", "rating": 68},
              "RP7": {"nombre": "T.PHAM", "rating": 70},
              "RP8": {"nombre": "J.LEGUMINA", "rating": 67}
            }
          }
        },

        "Guardians": {
          "nombre": "Cleveland Guardians",
          "jugadores": {
            "infield": {
              "C": {"nombre": "B.HEDGES", "rating": 72},
              "1B": {"nombre": "J.NAYLOR", "rating": 77},
              "2B": {"nombre": "A.GIMENEZ", "rating": 79},
              "3B": {"nombre": "J.RAMIREZ", "rating": 87},
              "SS": {"nombre": "B.ROCCHIO", "rating": 75}
            },
            "outfield": {
              "LF": {"nombre": "S.KWAN", "rating": 81},
              "CF": {"nombre": "T.FREEMAN", "rating": 78},
              "RF": {"nombre": "W.BRENNAN", "rating": 74},
              "DH": {"nombre": "D.FRY", "rating": 76}
            },
            "pitchers": {
              "SP1": {"nombre": "S.BIEBER", "rating": 86},
              "SP2": {"nome": "T.MCKENZIE", "rating": 78},
              "SP3": {"nombre": "B.LIVELY", "rating": 75},
              "SP4": {"nombre": "C.WILLIAMS", "rating": 73},
              "SP5": {"nombre": "G.CANTILLO", "rating": 71}
            },
            "relief": {
              "RP1": {"nome": "E.CLASE", "rating": 91},
              "RP2": {"nome": "C.FAIRBANKS", "rating": 79},
              "RP3": {"nome": "S.HENTGES", "rating": 74},
              "RP4": {"nome": "T.KARINCHAK", "rating": 76},
              "RP5": {"nome": "A.MORGAN", "rating": 72},
              "RP6": {"nome": "H.GADDIS", "rating": 68},
              "RP7": {"nome": "P.SANDLIN", "rating": 70},
              "RP8": {"nome": "C.SMITH", "rating": 69}
            }
          }
        },


    // ... agregar todos los demás equipos aquí
  };

  return equipos;
}

// Función actualizada para realizar selección automática
async function realizarSeleccionAutomatica(ligaId, temporizador, configDraft) {
  try {
    console.log(`🤖 Iniciando selección automática para usuario: ${temporizador.usuarioEnTurno}`);

    // Buscar el MEJOR lineup disponible
    const mejorLineup = await buscarMejorLineupDisponible(ligaId, temporizador.usuarioEnTurno);

    if (!mejorLineup) {
      console.log(`❌ No se encontró lineup disponible para selección automática`);
      return false;
    }

    console.log(`🎯 Seleccionando automáticamente:`);
    console.log(`   - Usuario: ${temporizador.usuarioEnTurno}`);
    console.log(`   - Equipo: ${mejorLineup.equipoNombre}`);
    console.log(`   - Tipo: ${mejorLineup.tipo}`);
    console.log(`   - Rating: ${mejorLineup.ratingPromedio}`);

    // Crear la selección
    const seleccion = {
      ligaId: ligaId,
      usuarioId: temporizador.usuarioEnTurno,
      equipoId: mejorLineup.equipoId,
      tipoLineup: mejorLineup.tipo,
      jugadores: mejorLineup.jugadores,
      ratingPromedio: mejorLineup.ratingPromedio,
      timestamp: Date.now(),
      ronda: configDraft.rondaActual,
      turno: configDraft.turnoActual,
      seleccionAutomatica: true, // Marcador importante
      motivoAutomatico: 'tiempo_agotado'
    };

    // Guardar la selección
    const db = getDatabase();
    const claveSeleccion = `${mejorLineup.equipoId}_${mejorLineup.tipo}`;

    await db.ref(`LineupsSeleccionados/${claveSeleccion}`).set(seleccion);

    console.log(`✅ Selección automática guardada: ${claveSeleccion}`);

    // Avanzar al siguiente turno
    await avanzarAlSiguienteTurno(ligaId, configDraft);

    return true;

  } catch (error) {
    console.error('❌ Error en selección automática:', error);
    return false;
  }
}

// Función para avanzar al siguiente turno
async function avanzarAlSiguienteTurno(ligaId, configDraft) {
  try {
    const db = getDatabase();
    const ligaRef = db.ref(`Ligas/${ligaId}/configuracion/configuracionDraft`);

    // Obtener información actual
    const participantesSnapshot = await db.ref(`Ligas/${ligaId}/usuariosParticipantes`).once('value');
    const participantes = participantesSnapshot.val() || [];
    const totalParticipantes = participantes.length;

    let nuevaRonda = configDraft.rondaActual;
    let nuevoTurno = configDraft.turnoActual + 1;

    // Si llegamos al final de la ronda, avanzar a la siguiente
    if (nuevoTurno >= totalParticipantes) {
      nuevaRonda++;
      nuevoTurno = 0;
    }

    // Verificar si el draft se completó (4 rondas)
    if (nuevaRonda > 4) {
      await ligaRef.update({
        draftCompletado: true,
        fechaFinalizacion: Date.now()
      });

      console.log(`🏁 Draft completado para liga ${ligaId}`);
      return;
    }

    // Actualizar ronda y turno
    await ligaRef.update({
      rondaActual: nuevaRonda,
      turnoActual: nuevoTurno
    });

    console.log(`➡️ Avanzado a Ronda ${nuevaRonda}, Turno ${nuevoTurno}`);

  } catch (error) {
    console.error('❌ Error avanzando turno:', error);
  }
}

/**
 * NUEVO: Verificar si el usuario hizo una selección manual reciente
 */
async function verificarSeleccionReciente(ligaId, usuarioId, inicioTiempo) {
  try {
    const { getDatabase } = require('firebase-admin/database');
    const db = getDatabase();

    const lineupsRef = db.ref('LineupsSeleccionados');
    const lineupsSnapshot = await lineupsRef.once('value');
    const selecciones = lineupsSnapshot.val() || {};

    // Buscar selecciones del usuario después del inicio del temporizador
    const seleccionesUsuario = Object.values(selecciones).filter(seleccion =>
      seleccion.usuarioId === usuarioId &&
      seleccion.timestamp >= inicioTiempo &&
      !seleccion.seleccionAutomatica // Solo selecciones manuales
    );

    return seleccionesUsuario.length > 0;

  } catch (error) {
    console.error('Error verificando selección reciente:', error);
    return false;
  }
}

/**
 * NUEVO: Función para manejar el temporizador del servidor (cada 10 segundos)
 */
exports.verificarTemporizadores = functions.pubsub.schedule('every 1 minutes').onRun(async (context) => {
  try {
    console.log('🕐 Verificando temporizadores activos...');

    const { getDatabase } = require('firebase-admin/database');
    const db = getDatabase();

    // Obtener todos los temporizadores activos
    const temporizadoresRef = db.ref('TemporizadoresDraft');
    const temporizadoresSnapshot = await temporizadoresRef.once('value');
    const temporizadores = temporizadoresSnapshot.val() || {};

    const ahora = Date.now();

    for (const [ligaId, temporizador] of Object.entries(temporizadores)) {
      if (!temporizador.activo) continue;

      const tiempoTranscurrido = ahora - temporizador.inicioTiempo;
      const tiempoLimite = 3 * 60 * 1000; // 3 minutos en milisegundos

      console.log(`⏰ Liga ${ligaId}: ${Math.floor(tiempoTranscurrido / 1000)}s / ${Math.floor(tiempoLimite / 1000)}s`);

      // Si el tiempo se agotó
      if (tiempoTranscurrido >= tiempoLimite) {
        console.log(`🚨 TIEMPO AGOTADO para liga ${ligaId} - usuario: ${temporizador.usuarioEnTurno}`);

        // ✅ VERIFICAR si el usuario ya hizo su selección durante estos 3 minutos
        const yaHizoSeleccion = await verificarSeleccionReciente(ligaId, temporizador.usuarioEnTurno, temporizador.inicioTiempo);

        if (yaHizoSeleccion) {
          console.log(`✅ Usuario ${temporizador.usuarioEnTurno} ya hizo su selección - no se requiere selección automática`);

          // Detener temporizador ya que el usuario seleccionó a tiempo
          await temporizadoresRef.child(ligaId).update({
            activo: false,
            finalizadoPor: 'seleccion_manual'
          });

        } else {
          console.log(`🤖 Usuario ${temporizador.usuarioEnTurno} NO seleccionó - iniciando selección automática`);

          // Obtener configuración del draft
          const configRef = db.ref(`Ligas/${ligaId}/configuracion/configuracionDraft`);
          const configSnapshot = await configRef.once('value');
          const configDraft = configSnapshot.val();

          if (configDraft) {
            // ✅ Realizar selección automática con el MEJOR lineup disponible
            const seleccionExitosa = await realizarSeleccionAutomatica(ligaId, temporizador, configDraft);

            if (seleccionExitosa) {
              console.log(`✅ Selección automática completada para ${temporizador.usuarioEnTurno}`);

              // Detener temporizador
              await temporizadoresRef.child(ligaId).update({
                activo: false,
                finalizadoPor: 'seleccion_automatica'
              });

            } else {
              console.log(`❌ Error en selección automática para ${temporizador.usuarioEnTurno}`);
            }
          }
        }
      }
    }

    console.log('✅ Verificación de temporizadores completada');

  } catch (error) {
    console.error('❌ Error en temporizador del servidor:', error);
  }
});

/**
 * NUEVO: Función para iniciar temporizador cuando es el turno de un usuario
 */
exports.iniciarTemporizadorTurno = functions.database.ref('/Ligas/{ligaId}/configuracion/configuracionDraft/usuarioEnTurno')
  .onUpdate(async (change, context) => {
    try {
      const ligaId = context.params.ligaId;
      const nuevoUsuario = change.after.val();

      if (!nuevoUsuario) return;

      console.log(`🕐 Iniciando temporizador para usuario: ${nuevoUsuario} en liga: ${ligaId}`);

      const { getDatabase } = require('firebase-admin/database');
      const db = getDatabase();

      // Crear/actualizar temporizador
      await db.ref(`TemporizadoresDraft/${ligaId}`).set({
        usuarioEnTurno: nuevoUsuario,
        inicioTiempo: Date.now(),
        activo: true,
        ligaId: ligaId
      });

      console.log(`✅ Temporizador iniciado para ${nuevoUsuario}`);

    } catch (error) {
      console.error('❌ Error iniciando temporizador:', error);
    }
  });

/**
 * NUEVO: Función para detener temporizador cuando se hace una selección manual
 */
exports.detenerTemporizadorSeleccion = functions.database.ref('/LineupsSeleccionados/{seleccionId}')
  .onCreate(async (snapshot, context) => {
    try {
      const seleccion = snapshot.val();

      // Solo detener si es una selección manual (no automática)
      if (seleccion.seleccionAutomatica) return;

      const ligaId = seleccion.ligaId;
      const usuarioId = seleccion.usuarioId;

      console.log(`🛑 Deteniendo temporizador por selección manual de ${usuarioId} en liga ${ligaId}`);

      const { getDatabase } = require('firebase-admin/database');
      const db = getDatabase();

      // Detener temporizador
      await db.ref(`TemporizadoresDraft/${ligaId}`).update({
        activo: false,
        finalizadoPor: 'seleccion_manual',
        usuarioQueSelecciono: usuarioId
      });

      console.log(`✅ Temporizador detenido - selección manual completada`);

    } catch (error) {
      console.error('❌ Error deteniendo temporizador:', error);
    }
  });