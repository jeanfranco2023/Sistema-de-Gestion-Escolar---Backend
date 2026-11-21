-- Completa el historial de incidencias preexistentes sin inventar transiciones.
-- Si ya hay cambios auditados, toma el estado anterior al primero; si no,
-- registra el estado actual como línea base y lo identifica claramente.
WITH primera_transicion AS (
    SELECT DISTINCT ON (h.incidencia_id)
           h.incidencia_id,
           h.estado_anterior,
           h.fecha_cambio
    FROM historial_estados_incidencia h
    WHERE h.estado_anterior IS NOT NULL
    ORDER BY h.incidencia_id, h.fecha_cambio, h.id
)
INSERT INTO historial_estados_incidencia (
    incidencia_id,
    estado_anterior,
    estado_nuevo,
    usuario_id,
    nombre_usuario_snapshot,
    fecha_cambio,
    observacion_gestion
)
SELECT i.id,
       NULL,
       COALESCE(p.estado_anterior, i.estado),
       i.reportado_por_usuario_id,
       u.nombre_usuario,
       CASE
           WHEN p.fecha_cambio IS NOT NULL
               THEN LEAST(i.fecha_creacion::timestamptz, p.fecha_cambio - INTERVAL '1 microsecond')
           ELSE i.fecha_creacion::timestamptz
       END,
       CASE
           WHEN p.fecha_cambio IS NOT NULL
               THEN 'Estado inicial inferido del primer cambio auditado al habilitar el historial.'
           ELSE 'Estado existente tomado como línea base; no hay transiciones anteriores disponibles.'
       END
FROM incidencias_conductuales i
JOIN usuarios u ON u.id = i.reportado_por_usuario_id
LEFT JOIN primera_transicion p ON p.incidencia_id = i.id
WHERE NOT EXISTS (
    SELECT 1
    FROM historial_estados_incidencia h
    WHERE h.incidencia_id = i.id
      AND h.estado_anterior IS NULL
);
