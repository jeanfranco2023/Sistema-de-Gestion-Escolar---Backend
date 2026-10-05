import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

/** Verifica DDL en PostgreSQL real dentro de una transacción que siempre revierte. */
class VerificarEsquema {
  static void ejecutar(Connection c, String sql) throws SQLException {
    try (Statement s = c.createStatement()) { s.execute(sql); }
  }
  static String leer(Path p) throws Exception { return Files.readString(p, StandardCharsets.UTF_8); }
  static Map<String, String> entorno(Path root) throws Exception {
    var config = new HashMap<String, String>(System.getenv());
    for (String line : Files.readAllLines(root.resolve(".env"))) {
      line = line.strip();
      if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
      int pos = line.indexOf('=');
      String key = line.substring(0, pos).strip();
      String value = line.substring(pos + 1).strip();
      if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
          || (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1, value.length() - 1);
      config.putIfAbsent(key, value);
    }
    return config;
  }
  static List<String> columnas(Connection c, String schema) throws SQLException {
    var result = new ArrayList<String>();
    try (PreparedStatement s = c.prepareStatement("SELECT table_name,column_name,data_type,"
        + "COALESCE(character_maximum_length::text,''),is_nullable,COALESCE(numeric_precision::text,''),"
        + "COALESCE(numeric_scale::text,'') FROM information_schema.columns WHERE table_schema=? "
        + "ORDER BY table_name,ordinal_position")) {
      s.setString(1, schema);
      try (ResultSet rs = s.executeQuery()) {
        while (rs.next()) {
          var values = new ArrayList<String>();
          for (int i = 1; i <= 7; i++) values.add(rs.getString(i));
          result.add(String.join("\t", values));
        }
      }
    }
    return result;
  }
  static void rechazar(Connection c, String sql, String estado) throws SQLException {
    Savepoint sp = c.setSavepoint();
    try {
      ejecutar(c, sql);
      throw new IllegalStateException("Se aceptó un valor que debía rechazarse");
    } catch (SQLException e) {
      c.rollback(sp);
      if (!estado.equals(e.getSQLState())) throw e;
    } finally { c.releaseSavepoint(sp); }
  }
  static List<String> restricciones(Connection c, String schema) throws SQLException {
    var result = new ArrayList<String>();
    try (PreparedStatement s = c.prepareStatement("SELECT t.relname,k.conname,k.contype,pg_get_constraintdef(k.oid) "
        + "FROM pg_constraint k JOIN pg_class t ON t.oid=k.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace "
        + "WHERE n.nspname=? ORDER BY t.relname,k.conname")) {
      s.setString(1, schema);
      try (ResultSet rs = s.executeQuery()) {
        while (rs.next()) result.add(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3)
            + "\t" + rs.getString(4).replace(schema + ".", ""));
      }
    }
    return result;
  }
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    var config = entorno(root);
    String url = "jdbc:postgresql://" + config.get("SUPABASE_DB_HOST") + ":"
        + config.getOrDefault("SUPABASE_DB_PORT", "5432") + "/postgres?sslmode="
        + config.getOrDefault("SUPABASE_DB_SSLMODE", "require") + "&connectTimeout=15&socketTimeout=60";
    String prefix = "codex_bd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    var props = new Properties();
    props.setProperty("user", config.getOrDefault("SUPABASE_DB_USER", "postgres"));
    props.setProperty("password", config.getOrDefault("SUPABASE_DB_PASSWORD", ""));
    Path db = root.resolve("src/main/resources/db");
    try (Connection c = DriverManager.getConnection(url, props)) {
      c.setAutoCommit(false);
      if (args.length == 3) {
        String schema = args[2];
        if (!schema.matches("codex_it_v18_[a-f0-9]{10}")) throw new IllegalArgumentException("Esquema de prueba inválido");
        if (args[1].equals("preparar")) ejecutar(c, "CREATE SCHEMA " + schema);
        else if (args[1].equals("limpiar")) ejecutar(c, "DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        else throw new IllegalArgumentException("Modo inválido");
        c.commit();
        System.out.println(args[1].equals("preparar") ? "ESQUEMA_DE_PRUEBA_CREADO" : "ESQUEMA_DE_PRUEBA_ELIMINADO");
        return;
      }
      try {
        String incremental = prefix + "_m", consolidated = prefix + "_c";
        ejecutar(c, "CREATE SCHEMA " + incremental);
        ejecutar(c, "SET LOCAL search_path TO " + incremental + ", public, extensions");
        List<Path> migrations;
        try (var files = Files.list(db.resolve("migration"))) {
          migrations = files.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
              .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1))))
              .toList();
        }
        for (Path migration : migrations) {
          String sql = leer(migration).replaceAll("\\bpublic\\b", incremental);
          if (migration.getFileName().toString().startsWith("V15__")) {
            ejecutar(c, "INSERT INTO estudiantes(tipo_documento,numero_documento,nombres,apellido_paterno,apellido_materno,fecha_nacimiento,genero)"
                + " VALUES ('DNI','88888771',repeat('N',21),'Paterno','Materno','2015-01-01','M')");
            rechazar(c, sql, "P0001");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT char_length(nombres) FROM estudiantes WHERE numero_documento='88888771'")) {
              if (!rs.next() || rs.getInt(1) != 21) throw new IllegalStateException("La comprobación truncó los datos");
            }
            ejecutar(c, "DELETE FROM estudiantes WHERE numero_documento='88888771'");
          }
          ejecutar(c, sql);
          if (migration.getFileName().toString().startsWith("V18__")) ejecutar(c, sql);
        }
        System.out.println("MIGRACIONES_V1_V" + migrations.size() + "_OK; PREFLIGHT_SIN_TRUNCADO_OK");
        List<String> a = columnas(c, incremental);
        List<String> restriccionesA = restricciones(c, incremental);
        ejecutar(c, "CREATE SCHEMA " + consolidated);
        ejecutar(c, "SET LOCAL search_path TO " + consolidated + ", public, extensions");
        ejecutar(c, leer(db.resolve("schema_completo_colegio_shuji.sql")).replaceAll("\\bpublic\\b", consolidated));
        List<String> b = columnas(c, consolidated);
        if (!a.equals(b)) throw new IllegalStateException("El consolidado difiere de las migraciones");
        List<String> restriccionesB = restricciones(c, consolidated);
        if (!restriccionesA.equals(restriccionesB)) {
          Files.write(root.resolve("target/restricciones-comparacion-migraciones.tsv"), restriccionesA, StandardCharsets.UTF_8);
          Files.write(root.resolve("target/restricciones-comparacion-consolidado.tsv"), restriccionesB, StandardCharsets.UTF_8);
          throw new IllegalStateException("Diferencia en PK, FK, UNIQUE o CHECK; consultar metadatos de comparación");
        }
        Files.write(root.resolve("target/atributos_bd_v18.tsv"), b, StandardCharsets.UTF_8);
        Files.write(root.resolve("target/restricciones_bd_v18.tsv"), restriccionesB, StandardCharsets.UTF_8);
        ejecutar(c, "INSERT INTO estudiantes(tipo_documento,numero_documento,nombres_estudiante,apellido_paterno_estudiante,apellido_materno_estudiante,fecha_nacimiento,genero)"
            + " VALUES ('DNI','88888771',repeat('N',20),repeat('P',20),repeat('M',20),'2015-01-01','M')");
        rechazar(c, "UPDATE estudiantes SET nombres_estudiante=repeat('N',21) WHERE numero_documento='88888771'", "22001");
        rechazar(c, "UPDATE estudiantes SET apellido_paterno_estudiante=repeat('P',21) WHERE numero_documento='88888771'", "22001");
        rechazar(c, "UPDATE estudiantes SET apellido_materno_estudiante=repeat('M',21) WHERE numero_documento='88888771'", "22001");
        ejecutar(c, "INSERT INTO matriculas(anio_lectivo_id,estudiante_id,seccion_id,observaciones) "
            + "SELECT s.anio_lectivo_id,e.id,s.id,repeat('O',500) FROM secciones s CROSS JOIN estudiantes e "
            + "WHERE e.numero_documento='88888771' LIMIT 1");
        rechazar(c, "UPDATE matriculas SET observaciones=repeat('O',501)", "23514");
        ejecutar(c, "INSERT INTO usuarios(nombre_usuario,correo_electronico,usuario_password,fecha_actualizacion) VALUES"
            + " ('verificacion_v18',repeat('x',50),repeat('x',50),'2000-01-01')");
        rechazar(c, "UPDATE usuarios SET correo_electronico=repeat('x',51) WHERE nombre_usuario='verificacion_v18'", "22001");
        rechazar(c, "INSERT INTO estudiantes(tipo_documento,numero_documento,nombres_estudiante,apellido_paterno_estudiante,apellido_materno_estudiante,fecha_nacimiento,genero)"
            + " VALUES ('DNI','1234567','Nombre','Paterno','Materno','2015-01-01','M')", "23514");
        ejecutar(c, "UPDATE usuarios SET activo=false WHERE nombre_usuario='verificacion_v18'");
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
            "SELECT fecha_actualizacion > '2000-01-02'::timestamptz FROM usuarios WHERE nombre_usuario='verificacion_v18'")) {
          if (!rs.next() || !rs.getBoolean(1)) throw new IllegalStateException("Trigger de fecha incorrecto");
        }
        System.out.println("CONSOLIDADO_EQUIVALENTE_OK; LIMITES_20_21_50_51_Y_500_501_OK; DNI_8_DIGITOS_OK; TRIGGER_FECHA_OK; ATRIBUTOS="
            + b.size() + "; RESTRICCIONES=" + restriccionesB.size());
      } catch (SQLException e) {
        System.err.println("ERROR_SQL_ESTADO=" + e.getSQLState() + "; " + e.getMessage().replaceAll("(?i)(password|contraseña)=[^ ,;]+", "$1=REDACTADO"));
        throw e;
      } finally {
        c.rollback();
        System.out.println("ROLLBACK_COMPLETO; NINGUN_CAMBIO_PERSISTENTE_EN_SUPABASE");
      }
    }
  }
}
