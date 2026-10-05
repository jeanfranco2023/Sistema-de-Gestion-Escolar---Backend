import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Aplica exclusivamente migraciones Flyway pendientes; no limpia ni reinicializa. */
class ActualizarBaseDatos {
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    var config = VerificarEsquema.entorno(root);
    String url = "jdbc:postgresql://" + config.get("SUPABASE_DB_HOST") + ":"
        + config.getOrDefault("SUPABASE_DB_PORT", "5432")
        + "/postgres?sslmode=require&connectTimeout=15&socketTimeout=180";
    String user = config.get("SUPABASE_DB_USER"), password = config.get("SUPABASE_DB_PASSWORD");
    var flyway = Flyway.configure().dataSource(url, user, password)
        .locations("filesystem:" + root.resolve("src/main/resources/db/migration").toString().replace('\\', '/'))
        .schemas("public").defaultSchema("public").createSchemas(false)
        .baselineOnMigrate(false).cleanDisabled(true).ignoreMigrationPatterns("*:pending").group(true).load();
    var info = flyway.info();
    System.out.println("VERSION_ACTUAL=" + (info.current() == null ? "SIN_HISTORIAL" : info.current().getVersion()));
    for (var migration : info.pending()) System.out.println("PENDIENTE=" + migration.getVersion() + ":" + migration.getDescription());
    flyway.validate();
    if (args[1].equals("inspeccionar")) {
      try (Connection c = DriverManager.getConnection(url, user, password)) {
        consultar(c, "CORREOS", "SELECT table_name, column_name, character_maximum_length FROM information_schema.columns "
            + "WHERE table_schema='public' AND (table_name,column_name) IN "
            + "(('usuarios','correo_electronico'),('apoderados','correo_electronico'),('solicitudes_matricula_publica','correo_apoderado')) ORDER BY table_name");
        consultar(c, "CHECK_DNI", "SELECT t.relname, k.conname, k.convalidated, pg_get_constraintdef(k.oid) FROM pg_constraint k "
            + "JOIN pg_class t ON t.oid=k.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace "
            + "WHERE n.nspname='public' AND k.conname IN ('chk_estudiantes_dni_8_digitos','chk_apoderados_dni_8_digitos') ORDER BY t.relname");
        consultar(c, "FUNCION_RESERVAS", "SELECT position('fecha_expiracion_reserva' in pg_get_functiondef('public.fn_liberar_reservas_expiradas()'::regprocedure)) > 0, "
            + "position('reserva_expira_at' in pg_get_functiondef('public.fn_liberar_reservas_expiradas()'::regprocedure)) > 0");
        consultar(c, "PRECHECK_DATOS", "SELECT "
            + "(SELECT count(*) FROM public.usuarios WHERE char_length(correo_electronico)>50), "
            + "(SELECT count(*) FROM public.apoderados WHERE correo_electronico IS NOT NULL AND char_length(correo_electronico)>50), "
            + "(SELECT count(*) FROM public.solicitudes_matricula_publica WHERE correo_apoderado IS NOT NULL AND char_length(correo_apoderado)>50), "
            + "(SELECT count(*) FROM public.estudiantes WHERE tipo_documento='DNI' AND numero_documento !~ '^[0-9]{8}$'), "
            + "(SELECT count(*) FROM public.apoderados WHERE tipo_documento='DNI' AND numero_documento !~ '^[0-9]{8}$')");
      }
      return;
    }
    if (args[1].equals("verificar")) {
      try (Connection c=DriverManager.getConnection(url,user,password)) {
        Files.write(root.resolve("target/atributos-public-v18.tsv"),VerificarEsquema.columnas(c,"public"),StandardCharsets.UTF_8);
        Files.write(root.resolve("target/restricciones-public-v18.tsv"),VerificarEsquema.restricciones(c,"public"),StandardCharsets.UTF_8);
        System.out.println("INVENTARIO_PUBLIC_GUARDADO");
      }
      return;
    }
    if (!args[1].equals("aplicar")) throw new IllegalArgumentException("Modo desconocido");
    if (info.current() == null) throw new IllegalStateException("Se requiere historial Flyway existente; no se inicializa una base existente automáticamente");
    Path backup = root.resolve("target/respaldo-depuracion-" + System.currentTimeMillis() + ".tsv");
    var removed = new LinkedHashMap<String,String>();
    for (String table : List.of("usuarios", "apoderados", "estudiantes", "matriculas", "pagos_transacciones", "comprobantes_pago")) removed.put(table, "uuid");
    removed.put("roles", "descripcion");
    List<String> saved = new ArrayList<>();
    Map<String,Long> counts = new TreeMap<>();
    try (Connection c = DriverManager.getConnection(url, user, password)) {
      for (var row : removed.entrySet()) {
        try (PreparedStatement s = c.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?)")) {
          s.setString(1,row.getKey()); s.setString(2,row.getValue());
          try (ResultSet rs=s.executeQuery()) { rs.next(); if (!rs.getBoolean(1)) continue; }
        }
        try (Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT id,to_json("+row.getValue()+")::text FROM public."+row.getKey()+" ORDER BY id")) {
          while(rs.next()) saved.add(row.getKey()+"\t"+rs.getLong(1)+"\t"+row.getValue()+"\t"+Objects.toString(rs.getString(2),"null"));
        }
      }
      for (String metadata : Files.readAllLines(root.resolve("target/atributos_bd_v18.tsv"))) {
        String table=metadata.split("\t")[0];
        if (counts.containsKey(table)) continue;
        try (Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT count(*) FROM public."+table)) { rs.next(); counts.put(table,rs.getLong(1)); }
      }
    }
    Files.write(backup,saved,StandardCharsets.UTF_8);
    System.out.println("RESPALDO_ATRIBUTOS="+backup.getFileName()+"; FILAS="+saved.size());
    var result = flyway.migrate();
    System.out.println("MIGRACIONES_APLICADAS="+result.migrationsExecuted);
    flyway.validate();
    try (Connection c=DriverManager.getConnection(url,user,password)) {
      var columns=VerificarEsquema.columnas(c,"public").stream().filter(x->!x.startsWith("flyway_schema_history\t")).toList();
      var constraints=VerificarEsquema.restricciones(c,"public").stream().filter(x->!x.startsWith("flyway_schema_history\t")).toList();
      if (!columns.equals(Files.readAllLines(root.resolve("target/atributos_bd_v18.tsv")))) throw new IllegalStateException("Columnas reales difieren del esquema V18 verificado");
      if (!constraints.equals(Files.readAllLines(root.resolve("target/restricciones_bd_v18.tsv")))) throw new IllegalStateException("Restricciones reales difieren del esquema V18 verificado");
      for (var entry:counts.entrySet()) {
        try (Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT count(*) FROM public."+entry.getKey())) {
          rs.next(); if(rs.getLong(1)!=entry.getValue()) throw new IllegalStateException("Cambió el conteo de registros en "+entry.getKey());
        }
      }
      System.out.println("ESQUEMA_PUBLIC_OK; ATRIBUTOS="+columns.size()+"; RESTRICCIONES="+constraints.size()+"; CONTEOS_CONSERVADOS="+counts.size());
    }
  }

  private static void consultar(Connection c, String etiqueta, String sql) throws SQLException {
    try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
      System.out.println(etiqueta + ":");
      ResultSetMetaData md = rs.getMetaData();
      while (rs.next()) {
        var values = new ArrayList<String>();
        for (int i = 1; i <= md.getColumnCount(); i++) values.add(Objects.toString(rs.getObject(i), "NULL"));
        System.out.println(String.join("\t", values));
      }
    }
  }
}
