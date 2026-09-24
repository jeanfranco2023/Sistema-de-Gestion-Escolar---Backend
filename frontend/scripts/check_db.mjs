import pg from 'pg';
const { Client } = pg;

for (const key of ['SUPABASE_DB_HOST', 'SUPABASE_DB_USER', 'SUPABASE_DB_PASSWORD']) {
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

async function inspect() {
  await client.connect();
  console.log('--- DATABASE INSPECTION ---');
  
  const tables = [
    'roles',
    'usuarios',
    'anios_lectivos',
    'periodos_academicos',
    'niveles',
    'grados',
    'secciones',
    'areas_curriculares',
    'competencias',
    'bloques_horarios',
    'docentes', // if exists or usuarios with DOCENTE
    'apoderados',
    'estudiantes',
    'estudiante_apoderados',
    'matriculas',
    'conceptos_cobro',
    'obligaciones_pago',
    'asignaciones_docentes'
  ];

  for (const table of tables) {
    try {
      const res = await client.query(`SELECT COUNT(*) as count FROM ${table}`);
      console.log(`${table}: ${res.rows[0].count} rows`);
    } catch (e) {
      console.log(`${table}: ERROR - ${e.message}`);
    }
  }

  await client.end();
}

inspect().catch(err => {
  console.error('Error:', err.message);
  process.exit(1);
});
