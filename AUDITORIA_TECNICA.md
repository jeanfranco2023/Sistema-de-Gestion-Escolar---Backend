# Auditoría técnica del backend

Fecha: 2026-09-19

## Diagnóstico general

**Aprobado con correcciones aplicadas.** El backend compila con Java 21, conserva las fronteras hexagonales, usa JPQL en la persistencia de aplicación y ejecuta las pruebas unitarias, arquitectónicas y de integración contra el DDL PostgreSQL oficial.

## Correcciones realizadas

- Las mutaciones de años, períodos, matrículas, pagos, calificaciones, asistencia, conciliaciones, incidencias, refuerzos y comunicados se delegan a métodos del dominio. Los servicios quedaron como orquestadores.
- Los 31 POJOs de dominio incluyen `@Builder`, `@NoArgsConstructor` y `@AllArgsConstructor`.
- Se eliminaron imports comodín en servicios y dependencias directas desde `application` hacia `config` o `infrastructure`.
- Se introdujeron puertos para JWT, hashing de contraseñas y contexto de auditoría.
- Los adapters de escritura sincronizan el actor autenticado mediante `AuditContextPort` y actualizan entidades JPA administradas mediante `@MappingTarget`.
- Se corrigió la auditoría de usuarios: el actor proviene del contexto autenticado y no del usuario editado.
- Se reforzaron las transiciones terminales de pagos, el vencimiento inclusivo de sesiones, las justificaciones obligatorias y la consistencia de lotes biométricos.
- Se verificó ausencia de `createNativeQuery`, `nativeQuery = true`, `JdbcTemplate` e imports JDBC en producción.
- Se añadieron guardas automáticas de arquitectura para impedir regresiones de estas reglas.

## Verificación

Comando normal:

```powershell
.\mvnw.cmd test
```

Resultado: `BUILD SUCCESS`; 21 pruebas ejecutadas y 5 pruebas PostgreSQL omitidas por diseño.

Suite completa con PostgreSQL local inicializado con `V1__schema_colegio_shuji.sql`:

```powershell
.\mvnw.cmd -Dshuji.integration=true test
```

Resultado: `BUILD SUCCESS`; 26 pruebas, 0 fallos, 0 errores, 0 omitidas.
