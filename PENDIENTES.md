# Pendientes de cierre del sistema — 26/10/2026

Estado compartido de backend y frontend. Los cinco bloques del playbook están implementados localmente; el sistema completo conserva los siguientes pendientes.

## Pendientes confirmados

- [ ] **Publicación coordinada:** publicar backend, frontend y migraciones pendientes hasta V24. Respaldar la base existente antes de migrar; el consolidado se usa únicamente en una base vacía. Subir código a GitHub no confirma un despliegue saludable ni una migración aplicada.
- [ ] **Prueba completa desde navegador:** recorrer matrícula, los tres PDF, validación documental, pago de prueba de S/ 350, confirmación, boleta y consultas por cada rol. Validar los medios de pago habilitados sin realizar cobros reales.
- [ ] **Comunicados por correo:** envío real, registro de resultados y reintentos sin duplicados.
- [ ] **Justificaciones de asistencia:** registrar y consultar motivos médicos/institucionales con documentos adjuntos privados y permisos apropiados.
- [ ] **Tutoría integral:** tablero de notas, ausentismo y riesgo, con acceso limitado a la sección asignada al tutor. La vista actual muestra incidencias y asistencia de hoy.
- [ ] **Importaciones adicionales:** padrón y archivos biométricos. La importación de calificaciones CSV/XLSX ya está implementada.
- [ ] **Gestión completa de horarios:** editar, mover y retirar bloques respetando conflictos de sección/docente y la política de conservación del historial.
- [ ] **Excepciones institucionales de edad:** autorización administrativa con motivo y trazabilidad; sin aprobación documental humana obligatoria.
- [ ] **PDF institucional:** confirmar si firma digital y QR forman parte del requisito final; implementarlos con credenciales institucionales si corresponde. La libreta actual es informativa.
- [ ] **Cierre técnico:** verificar CI y dependencias, reducir el bundle inicial (587.24 kB frente al presupuesto de 500 kB) y medir cargas grandes, incluida una planilla de 500 filas. Si se usan varias réplicas, compartir los límites de peticiones entre instancias.

## Implementación ya comprobada

- [x] Rutas y navegación Etapa 9, portal familiar con control de custodia, suplencias y aprobación institucional con protección de concurrencia.
- [x] IA pedagógica Gemini con generación real, revisión docente y procedencia IA; sin identificadores del menor en el contexto académico enviado.
- [x] Consulta/programación de horarios, sesiones de refuerzo, derivación B/C y pase de lista.
- [x] Plantilla e importación CSV/XLSX de calificaciones, prevalidación y confirmación transaccional.
- [x] Libreta PDF con datos reales, paginación y autorización de acceso.
- [x] Migraciones V1–V24, SQL consolidado y DBML V24 equivalentes: 39 tablas, 340 atributos y 252 restricciones.

## Evidencia y límites

Backend: 224 casos cubiertos por la suite integral y la reverificación selectiva del único fallo de aserción corregido; cero omitidos. La primera ejecución no fue totalmente exitosa. La reverificación de dos casos terminó con `BUILD SUCCESS`.

Frontend: 42 pruebas aprobadas y build correcto, con la advertencia de tamaño indicada. PostgreSQL/Supabase se comprobó en esquemas temporales eliminados al finalizar; no se migró `public`. Gemini se verificó con contexto sintético. Esto no sustituye el recorrido completo desde navegador ni confirma todos los proveedores externos.

La memoria y continuidad detalladas se conservan en la bóveda Obsidian indicada por el propietario, notas `Tareas-Pendientes` y `Checkpoint-2026-10-26-Cierre-Playbook-Funcional`.
