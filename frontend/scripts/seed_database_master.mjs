import pg from 'pg';
const { Client } = pg;

if (process.env.ALLOW_MASTER_SEED !== '1') {
  throw new Error('Set ALLOW_MASTER_SEED=1 to authorize the master seed.');
}
for (const key of ['SUPABASE_DB_HOST', 'SUPABASE_DB_USER', 'SUPABASE_DB_PASSWORD', 'MASTER_ADMIN_PASSWORD_HASH']) {
  if (!process.env[key]) throw new Error(`Missing required environment variable: ${key}`);
}

const client = new Client({
  host: process.env.SUPABASE_DB_HOST,
  port: Number(process.env.SUPABASE_DB_PORT || 5432),
  database: process.env.SUPABASE_DB_NAME || 'postgres',
  user: process.env.SUPABASE_DB_USER,
  password: process.env.SUPABASE_DB_PASSWORD,
  ssl: { rejectUnauthorized: true }
});

const PASSWORD_HASH = process.env.MASTER_ADMIN_PASSWORD_HASH;

async function seed() {
  console.log('🌱 Iniciando sembrado maestro de la base de datos I.E.P. Shuji Kitamura...');
  await client.connect();

  try {
    await client.query('BEGIN');

    // 1. Año Lectivo 2026
    console.log('📅 1. Registrando Año Lectivo 2026...');
    let anioRes = await client.query('SELECT id FROM anios_lectivos WHERE anio = 2026');
    let anioLectivoId;
    if (anioRes.rows.length === 0) {
      const insAnio = await client.query(`
        INSERT INTO anios_lectivos (anio, fecha_inicio, fecha_fin, abierto)
        VALUES (2026, '2026-03-01', '2026-12-20', true)
        RETURNING id;
      `);
      anioLectivoId = insAnio.rows[0].id;
    } else {
      anioLectivoId = anioRes.rows[0].id;
    }
    console.log(`   -> Año Lectivo 2026 ID: ${anioLectivoId}`);

    // 2. Periodos Académicos 2026 (4 Bimestres Oficiales MINEDU)
    console.log('📆 2. Registrando 4 Bimestres Académicos CNEB...');
    const bimestres = [
      { num: 1, nombre: 'I Bimestre', ini: '2026-03-01', fin: '2026-05-08' },
      { num: 2, nombre: 'II Bimestre', ini: '2026-05-18', fin: '2026-07-24' },
      { num: 3, nombre: 'III Bimestre', ini: '2026-08-10', fin: '2026-10-09' },
      { num: 4, nombre: 'IV Bimestre', ini: '2026-10-19', fin: '2026-12-18' }
    ];
    for (const b of bimestres) {
      await client.query(`
        INSERT INTO periodos_academicos (anio_lectivo_id, numero_periodo, nombre, fecha_inicio, fecha_fin, cerrado)
        VALUES ($1, $2, $3, $4, $5, false)
        ON CONFLICT (anio_lectivo_id, numero_periodo) DO NOTHING;
      `, [anioLectivoId, b.num, b.nombre, b.ini, b.fin]);
    }

    // 3. Grados (Inicial: 3, 4, 5 años | Primaria: 1° a 6° | Secundaria: 1° a 5°)
    console.log('🏫 3. Registrando Grados para Inicial, Primaria y Secundaria...');
    const estructuraGrados = [
      // Nivel 1: INICIAL
      { nivelId: 1, num: 1, nombre: 'Inicial 3 años' },
      { nivelId: 1, num: 2, nombre: 'Inicial 4 años' },
      { nivelId: 1, num: 3, nombre: 'Inicial 5 años' },
      // Nivel 2: PRIMARIA
      { nivelId: 2, num: 1, nombre: '1° de Primaria' },
      { nivelId: 2, num: 2, nombre: '2° de Primaria' },
      { nivelId: 2, num: 3, nombre: '3° de Primaria' },
      { nivelId: 2, num: 4, nombre: '4° de Primaria' },
      { nivelId: 2, num: 5, nombre: '5° de Primaria' },
      { nivelId: 2, num: 6, nombre: '6° de Primaria' },
      // Nivel 3: SECUNDARIA
      { nivelId: 3, num: 1, nombre: '1° de Secundaria' },
      { nivelId: 3, num: 2, nombre: '2° de Secundaria' },
      { nivelId: 3, num: 3, nombre: '3° de Secundaria' },
      { nivelId: 3, num: 4, nombre: '4° de Secundaria' },
      { nivelId: 3, num: 5, nombre: '5° de Secundaria' }
    ];

    const mapaGrados = {};
    for (const g of estructuraGrados) {
      const resG = await client.query(`
        INSERT INTO grados (nivel_id, numero_grado, nombre)
        VALUES ($1, $2, $3)
        ON CONFLICT (nivel_id, numero_grado) DO UPDATE SET nombre = EXCLUDED.nombre
        RETURNING id, nivel_id, numero_grado, nombre;
      `, [g.nivelId, g.num, g.nombre]);
      const clave = `${g.nivelId}_${g.num}`;
      mapaGrados[clave] = resG.rows[0].id;
    }

    // 4. Catálogo de Aulas Físicas y Secciones para el Año Lectivo 2026
    console.log('🏛️ 4.1 Creando Catálogo de Aulas Físicas...');
    const aulasCatalogo = [
      // Inicial
      { codigo: 'AULA_I_01', nombre: 'Pabellón Inicial - Aula 1', ubicacion: 'Pabellón A - Piso 1', capacidad: 30 },
      { codigo: 'AULA_I_02', nombre: 'Pabellón Inicial - Aula 2', ubicacion: 'Pabellón A - Piso 1', capacidad: 30 },
      { codigo: 'AULA_I_03', nombre: 'Pabellón Inicial - Aula 3', ubicacion: 'Pabellón A - Piso 1', capacidad: 30 },
      // Primaria
      { codigo: 'AULA_P_101', nombre: 'Pabellón Primaria - Aula 101', ubicacion: 'Pabellón B - Piso 1', capacidad: 35 },
      { codigo: 'AULA_P_102', nombre: 'Pabellón Primaria - Aula 102', ubicacion: 'Pabellón B - Piso 1', capacidad: 35 },
      { codigo: 'AULA_P_103', nombre: 'Pabellón Primaria - Aula 103', ubicacion: 'Pabellón B - Piso 2', capacidad: 35 },
      { codigo: 'AULA_P_104', nombre: 'Pabellón Primaria - Aula 104', ubicacion: 'Pabellón B - Piso 2', capacidad: 35 },
      { codigo: 'AULA_P_105', nombre: 'Pabellón Primaria - Aula 105', ubicacion: 'Pabellón B - Piso 3', capacidad: 35 },
      { codigo: 'AULA_P_106', nombre: 'Pabellón Primaria - Aula 106', ubicacion: 'Pabellón B - Piso 3', capacidad: 35 },
      // Secundaria
      { codigo: 'AULA_S_201', nombre: 'Pabellón Secundaria - Aula 201', ubicacion: 'Pabellón C - Piso 1', capacidad: 35 },
      { codigo: 'AULA_S_202', nombre: 'Pabellón Secundaria - Aula 202', ubicacion: 'Pabellón C - Piso 1', capacidad: 35 },
      { codigo: 'AULA_S_203', nombre: 'Pabellón Secundaria - Aula 203', ubicacion: 'Pabellón C - Piso 2', capacidad: 35 },
      { codigo: 'AULA_S_204', nombre: 'Pabellón Secundaria - Aula 204', ubicacion: 'Pabellón C - Piso 2', capacidad: 35 },
      { codigo: 'AULA_S_205', nombre: 'Pabellón Secundaria - Aula 205', ubicacion: 'Pabellón C - Piso 3', capacidad: 35 }
    ];

    const mapaAulas = {};
    for (const a of aulasCatalogo) {
      const resAula = await client.query(`
        INSERT INTO aulas (codigo, nombre, ubicacion, capacidad, activa)
        VALUES ($1, $2, $3, $4, true)
        ON CONFLICT (codigo) DO UPDATE SET nombre = EXCLUDED.nombre
        RETURNING id, codigo;
      `, [a.codigo, a.nombre, a.ubicacion, a.capacidad]);
      mapaAulas[a.codigo] = resAula.rows[0].id;
    }

    console.log('🚪 4.2 Creando Secciones para el Año Lectivo 2026 vinculadas a Aulas...');
    const seccionesConfig = [
      // Inicial
      { nivelId: 1, num: 1, aulaCod: 'AULA_I_01' },
      { nivelId: 1, num: 2, aulaCod: 'AULA_I_02' },
      { nivelId: 1, num: 3, aulaCod: 'AULA_I_03' },
      // Primaria
      { nivelId: 2, num: 1, aulaCod: 'AULA_P_101' },
      { nivelId: 2, num: 2, aulaCod: 'AULA_P_102' },
      { nivelId: 2, num: 3, aulaCod: 'AULA_P_103' },
      { nivelId: 2, num: 4, aulaCod: 'AULA_P_104' },
      { nivelId: 2, num: 5, aulaCod: 'AULA_P_105' },
      { nivelId: 2, num: 6, aulaCod: 'AULA_P_106' },
      // Secundaria
      { nivelId: 3, num: 1, aulaCod: 'AULA_S_201' },
      { nivelId: 3, num: 2, aulaCod: 'AULA_S_202' },
      { nivelId: 3, num: 3, aulaCod: 'AULA_S_203' },
      { nivelId: 3, num: 4, aulaCod: 'AULA_S_204' },
      { nivelId: 3, num: 5, aulaCod: 'AULA_S_205' }
    ];

    const mapaSecciones = {};
    for (const sc of seccionesConfig) {
      const gradoId = mapaGrados[`${sc.nivelId}_${sc.num}`];
      const aulaId = mapaAulas[sc.aulaCod];
      const resSec = await client.query(`
        INSERT INTO secciones (anio_lectivo_id, grado_id, nivel_id, letra, cupo_maximo, vacantes_ocupadas, aula_fisica, aula_id)
        VALUES ($1, $2, $3, 'A', 30, 0, $4, $5)
        ON CONFLICT (anio_lectivo_id, grado_id, letra) DO UPDATE SET aula_fisica = EXCLUDED.aula_fisica, aula_id = EXCLUDED.aula_id
        RETURNING id, grado_id, nivel_id, letra;
      `, [anioLectivoId, gradoId, sc.nivelId, sc.aulaCod, aulaId]);
      mapaSecciones[`${sc.nivelId}_${sc.num}`] = resSec.rows[0].id;
    }

    // 5. Áreas Curriculares CNEB
    console.log('📚 5. Creando Áreas Curriculares Oficiales MINEDU...');
    const areasCurriculares = [
      // Primaria (Nivel 2)
      { nivelId: 2, codigo: 'MAT', nombre: 'Matemática' },
      { nivelId: 2, codigo: 'COM', nombre: 'Comunicación' },
      { nivelId: 2, codigo: 'CYT', nombre: 'Ciencia y Tecnología' },
      { nivelId: 2, codigo: 'PS', nombre: 'Personal Social' },
      { nivelId: 2, codigo: 'ING', nombre: 'Inglés' },
      { nivelId: 2, codigo: 'ARTE', nombre: 'Arte y Cultura' },
      { nivelId: 2, codigo: 'EDFIS', nombre: 'Educación Física' },
      { nivelId: 2, codigo: 'REL', nombre: 'Educación Religiosa' },
      // Secundaria (Nivel 3)
      { nivelId: 3, codigo: 'MAT', nombre: 'Matemática' },
      { nivelId: 3, codigo: 'COM', nombre: 'Comunicación' },
      { nivelId: 3, codigo: 'CYT', nombre: 'Ciencia y Tecnología' },
      { nivelId: 3, codigo: 'CCSS', nombre: 'Ciencias Sociales' },
      { nivelId: 3, codigo: 'DPCC', nombre: 'Desarrollo Personal, Ciudadanía y Cívica' },
      { nivelId: 3, codigo: 'ING', nombre: 'Inglés' },
      { nivelId: 3, codigo: 'ARTE', nombre: 'Arte y Cultura' },
      { nivelId: 3, codigo: 'EDFIS', nombre: 'Educación Física' },
      { nivelId: 3, codigo: 'EPT', nombre: 'Educación para el Trabajo' },
      { nivelId: 3, codigo: 'REL', nombre: 'Educación Religiosa' }
    ];

    const mapaAreas = {};
    for (const a of areasCurriculares) {
      const resA = await client.query(`
        INSERT INTO areas_curriculares (nivel_id, codigo, nombre)
        VALUES ($1, $2, $3)
        ON CONFLICT (nivel_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre
        RETURNING id, nivel_id, codigo;
      `, [a.nivelId, a.codigo, a.nombre]);
      mapaAreas[`${a.nivelId}_${a.codigo}`] = resA.rows[0].id;
    }

    // 6. Competencias CNEB para Matemática, Comunicación y Ciencia y Tecnología
    console.log('🎯 6. Creando Competencias CNEB...');
    const competencias = [
      // Secundaria Matemática
      { areaKey: '3_MAT', orden: 1, nombre: 'Resuelve problemas de cantidad' },
      { areaKey: '3_MAT', orden: 2, nombre: 'Resuelve problemas de regularidad, equivalencia y cambio' },
      { areaKey: '3_MAT', orden: 3, nombre: 'Resuelve problemas de forma, movimiento y localización' },
      { areaKey: '3_MAT', orden: 4, nombre: 'Resuelve problemas de gestión de datos e incertidumbre' },
      // Secundaria Comunicación
      { areaKey: '3_COM', orden: 1, nombre: 'Se comunica oralmente en su lengua materna' },
      { areaKey: '3_COM', orden: 2, nombre: 'Lee diversos tipos de textos escritos' },
      { areaKey: '3_COM', orden: 3, nombre: 'Escribe diversos tipos de textos' },
      // Secundaria Ciencia y Tecnología
      { areaKey: '3_CYT', orden: 1, nombre: 'Indaga mediante métodos científicos para construir conocimientos' },
      { areaKey: '3_CYT', orden: 2, nombre: 'Explica el mundo físico basándose en conocimientos científicos' },
      { areaKey: '3_CYT', orden: 3, nombre: 'Diseña y construye soluciones tecnológicas' }
    ];

    for (const comp of competencias) {
      const areaId = mapaAreas[comp.areaKey];
      if (areaId) {
        await client.query(`
          INSERT INTO competencias (area_id, numero_orden, nombre, descripcion)
          VALUES ($1, $2, $3, $4)
          ON CONFLICT DO NOTHING;
        `, [areaId, comp.orden, comp.nombre, `Competencia oficial MINEDU N° ${comp.orden}`]);
      }
    }

    // 7. Bloques Horarios Escolares
    console.log('⏰ 7. Creando Bloques Horarios Escolares...');
    const bloques = [
      { num: 1, ini: '08:00:00', fin: '08:45:00', recreo: false },
      { num: 2, ini: '08:45:00', fin: '09:30:00', recreo: false },
      { num: 3, ini: '09:30:00', fin: '10:15:00', recreo: false },
      { num: 4, ini: '10:15:00', fin: '10:45:00', recreo: true },
      { num: 5, ini: '10:45:00', fin: '11:30:00', recreo: false },
      { num: 6, ini: '11:30:00', fin: '12:15:00', recreo: false },
      { num: 7, ini: '12:15:00', fin: '13:00:00', recreo: false },
      { num: 8, ini: '13:00:00', fin: '13:45:00', recreo: false }
    ];
    for (const blk of bloques) {
      await client.query(`
        INSERT INTO bloques_horarios (numero_bloque, hora_inicio, hora_fin, es_recreo)
        VALUES ($1, $2, $3, $4)
        ON CONFLICT (numero_bloque) DO UPDATE SET hora_inicio = EXCLUDED.hora_inicio, hora_fin = EXCLUDED.hora_fin;
      `, [blk.num, blk.ini, blk.fin, blk.recreo]);
    }

    // 8. Usuarios de Prueba con Roles Docente y Auxiliar
    console.log('👥 8. Creando Usuarios Demo para Docente y Auxiliar...');
    // Usuario Docente: docente.garcia
    let uDocente = await client.query("SELECT id FROM usuarios WHERE username = 'docente.garcia'");
    let docenteId;
    if (uDocente.rows.length === 0) {
      const insU = await client.query(`
        INSERT INTO usuarios (username, email, password_hash, activo)
        VALUES ('docente.garcia', 'docente@shuji.edu.pe', $1, true)
        RETURNING id;
      `, [PASSWORD_HASH]);
      docenteId = insU.rows[0].id;
      // Asignar rol DOCENTE (id: 3)
      await client.query('INSERT INTO usuario_roles (usuario_id, rol_id) VALUES ($1, 3)', [docenteId]);
    } else {
      docenteId = uDocente.rows[0].id;
    }

    // Usuario Auxiliar: auxiliar.torres
    let uAuxiliar = await client.query("SELECT id FROM usuarios WHERE username = 'auxiliar.torres'");
    let auxiliarId;
    if (uAuxiliar.rows.length === 0) {
      const insU = await client.query(`
        INSERT INTO usuarios (username, email, password_hash, activo)
        VALUES ('auxiliar.torres', 'auxiliar@shuji.edu.pe', $1, true)
        RETURNING id;
      `, [PASSWORD_HASH]);
      auxiliarId = insU.rows[0].id;
      // Asignar rol AUXILIAR (id: 4)
      await client.query('INSERT INTO usuario_roles (usuario_id, rol_id) VALUES ($1, 4)', [auxiliarId]);
    } else {
      auxiliarId = uAuxiliar.rows[0].id;
    }
    console.log(`   -> Docente ID: ${docenteId} | Auxiliar ID: ${auxiliarId}`);

    // 9. Asignación Docente Demo (Prof. García en 1° Secundaria Sección A - Matemática)
    console.log('👨‍🏫 9. Asignando Docente a 1° Secundaria Sección A (Matemática)...');
    const seccionSec1A = mapaSecciones['3_1']; // 1° Secundaria A
    const areaMatSec = mapaAreas['3_MAT'];
    let asigId;
    const asigExist = await client.query(`
      SELECT id FROM asignaciones_docentes 
      WHERE seccion_id = $1 AND area_curricular_id = $2 AND anio_lectivo_id = $3
    `, [seccionSec1A, areaMatSec, anioLectivoId]);

    if (asigExist.rows.length === 0) {
      const insAsig = await client.query(`
        INSERT INTO asignaciones_docentes (docente_usuario_id, seccion_id, anio_lectivo_id, nivel_id, area_curricular_id)
        VALUES ($1, $2, $3, 3, $4)
        RETURNING id;
      `, [docenteId, seccionSec1A, anioLectivoId, areaMatSec]);
      asigId = insAsig.rows[0].id;
    } else {
      asigId = asigExist.rows[0].id;
    }
    console.log(`   -> Asignación Docente ID: ${asigId}`);

    // 10. Apoderado y Vinculación para Mateo Quispe (Estudiante ID: 1)
    console.log('👨‍👦 10. Registrando Apoderado para Estudiante Mateo Quispe...');
    let apoderadoRes = await client.query("SELECT id FROM apoderados WHERE numero_documento = '10293847'");
    let apoderadoId;
    if (apoderadoRes.rows.length === 0) {
      const insAp = await client.query(`
        INSERT INTO apoderados (
          tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno,
          celular, email, direccion, ubigeo_inei, validado_reniec, origen_registro
        )
        VALUES ('DNI', '10293847', 'Carlos Alberto', 'Quispe', 'Mamani', '987654321', 'carlos.quispe@gmail.com', 'Av. Arequipa 1234, Lima', '150101', true, 'RENIEC_API')
        RETURNING id;
      `);
      apoderadoId = insAp.rows[0].id;
    } else {
      apoderadoId = apoderadoRes.rows[0].id;
    }

    // Vinculación en estudiante_apoderados
    await client.query(`
      INSERT INTO estudiante_apoderados (estudiante_id, apoderado_id, parentesco, es_responsable_economico, tiene_custodia, permite_recojo)
      VALUES (1, $1, 'PADRE', true, true, true)
      ON CONFLICT (estudiante_id, apoderado_id) DO NOTHING;
    `, [apoderadoId]);
    console.log(`   -> Apoderado ID: ${apoderadoId} vinculado a Estudiante #1`);

    // 11. Matrícula Oficial de Mateo Quispe en 1° de Secundaria - Sección A
    console.log('📝 11. Generando Matrícula Oficial en 1° Secundaria Sección A...');
    const matriculaExist = await client.query(`
      SELECT id FROM matriculas WHERE anio_lectivo_id = $1 AND estudiante_id = 1
    `, [anioLectivoId]);

    let matriculaId;
    if (matriculaExist.rows.length === 0) {
      const insMat = await client.query(`
        INSERT INTO matriculas (anio_lectivo_id, estudiante_id, seccion_id, estado_matricula, observaciones)
        VALUES ($1, 1, $2, 'MATRICULADO', 'Matrícula regular 2026 generada con validación CNEB completa.')
        RETURNING id;
      `, [anioLectivoId, seccionSec1A]);
      matriculaId = insMat.rows[0].id;
    } else {
      matriculaId = matriculaExist.rows[0].id;
    }
    console.log(`   -> Matrícula ID: ${matriculaId}`);

    // 12. Obligaciones de Pago en Tesorería (Matrícula + Pensión Marzo + Pensión Abril)
    console.log('💰 12. Generando Obligaciones de Pago (Tesorería)...');
    // Concepto 1: MATRICULA_ANUAL (cuota 0)
    await client.query(`
      INSERT INTO obligaciones_pago (
        matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion,
        fecha_vencimiento, monto_base, total_pagado, estado
      )
      VALUES ($1, 1, 'MATRICULA', 0, 'Derecho de Matrícula 2026', '2026-03-05', 350.00, 350.00, 'PAGADO_TOTAL')
      ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;
    `, [matriculaId]);

    // Concepto 2: PENSION_MENSUAL Marzo (cuota 1)
    await client.query(`
      INSERT INTO obligaciones_pago (
        matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion,
        fecha_vencimiento, monto_base, total_pagado, estado
      )
      VALUES ($1, 2, 'PENSION', 1, 'Pensión Escolar - Marzo 2026', '2026-03-31', 380.00, 0.00, 'PENDIENTE')
      ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;
    `, [matriculaId]);

    // Concepto 2: PENSION_MENSUAL Abril (cuota 2)
    await client.query(`
      INSERT INTO obligaciones_pago (
        matricula_id, concepto_id, tipo_concepto, numero_cuota, descripcion,
        fecha_vencimiento, monto_base, total_pagado, estado
      )
      VALUES ($1, 2, 'PENSION', 2, 'Pensión Escolar - Abril 2026', '2026-04-30', 380.00, 0.00, 'PENDIENTE')
      ON CONFLICT (matricula_id, concepto_id, numero_cuota) DO NOTHING;
    `, [matriculaId]);

    await client.query('COMMIT');
    console.log('🎉 ¡Sembrado maestro completado con éxito!');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Error durante el sembrado:', err);
    throw err;
  } finally {
    await client.end();
  }
}

seed().catch(err => {
  console.error(err);
  process.exit(1);
});
