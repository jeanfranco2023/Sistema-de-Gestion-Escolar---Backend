ALTER TABLE historial_estados_incidencia
    ADD COLUMN resultado_verificado VARCHAR(300),
    ADD COLUMN participantes VARCHAR(120),
    ADD COLUMN comunicacion_apoderado VARCHAR(130);

ALTER TABLE historial_estados_incidencia
    ADD CONSTRAINT chk_historial_cierre_resultado_verificado
        CHECK (
            estado_anterior IS NULL
            OR estado_nuevo <> 'CERRADA'
            OR resultado_verificado IS NOT NULL
        ) NOT VALID,
    ADD CONSTRAINT chk_historial_cierre_resultado_no_vacio
        CHECK (
            resultado_verificado IS NULL
            OR length(btrim(resultado_verificado)) > 0
        ) NOT VALID,
    ADD CONSTRAINT chk_historial_cierre_participantes_no_vacios
        CHECK (
            participantes IS NULL
            OR length(btrim(participantes)) > 0
        ) NOT VALID,
    ADD CONSTRAINT chk_historial_cierre_comunicacion_no_vacia
        CHECK (
            comunicacion_apoderado IS NULL
            OR length(btrim(comunicacion_apoderado)) > 0
        ) NOT VALID;
