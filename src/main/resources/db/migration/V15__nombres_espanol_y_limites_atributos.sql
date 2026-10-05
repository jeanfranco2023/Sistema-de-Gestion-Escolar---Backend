-- V15: nombres españoles en snake_case y límites funcionales de texto.
-- No trunca valores: la comprobación previa aborta si exceden el límite.
-- Las claves primarias, FK, índices y registros existentes se conservan.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios WHERE char_length(username) > 30) THEN
        RAISE EXCEPTION 'V15 cancelada: usuarios.username supera 30 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM roles WHERE char_length(codigo) > 20) THEN
        RAISE EXCEPTION 'V15 cancelada: roles.codigo supera 20 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM apoderados WHERE char_length(nombres) > 20) THEN
        RAISE EXCEPTION 'V15 cancelada: apoderados.nombres supera 20 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM estudiantes WHERE char_length(nombres) > 20) THEN
        RAISE EXCEPTION 'V15 cancelada: estudiantes.nombres supera 20 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM apoderados WHERE char_length(direccion) > 150) THEN
        RAISE EXCEPTION 'V15 cancelada: apoderados.direccion supera 150 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM solicitudes_matricula_publica WHERE char_length(direccion_apoderado) > 150) THEN
        RAISE EXCEPTION 'V15 cancelada: solicitudes_matricula_publica.direccion_apoderado supera 150 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM areas_curriculares WHERE char_length(nombre) > 60) THEN
        RAISE EXCEPTION 'V15 cancelada: areas_curriculares.nombre supera 60 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM conceptos_cobro WHERE char_length(nombre) > 50) THEN
        RAISE EXCEPTION 'V15 cancelada: conceptos_cobro.nombre supera 50 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM aulas WHERE char_length(ubicacion) > 60) THEN
        RAISE EXCEPTION 'V15 cancelada: aulas.ubicacion supera 60 caracteres. No se truncaron datos.';
    END IF;
    IF EXISTS (SELECT 1 FROM solicitudes_matricula_documentos WHERE char_length(mime_type) > 30) THEN
        RAISE EXCEPTION 'V15 cancelada: solicitudes_matricula_documentos.mime_type supera 30 caracteres. No se truncaron datos.';
    END IF;
END
$$;

ALTER TABLE usuarios ALTER COLUMN username TYPE VARCHAR(30);
ALTER TABLE roles ALTER COLUMN codigo TYPE VARCHAR(20);
ALTER TABLE apoderados ALTER COLUMN nombres TYPE VARCHAR(20);
ALTER TABLE estudiantes ALTER COLUMN nombres TYPE VARCHAR(20);
ALTER TABLE apoderados ALTER COLUMN direccion TYPE VARCHAR(150);
ALTER TABLE solicitudes_matricula_publica ALTER COLUMN direccion_apoderado TYPE VARCHAR(150);
ALTER TABLE areas_curriculares ALTER COLUMN nombre TYPE VARCHAR(60);
ALTER TABLE conceptos_cobro ALTER COLUMN nombre TYPE VARCHAR(50);
ALTER TABLE aulas ALTER COLUMN ubicacion TYPE VARCHAR(60);
ALTER TABLE solicitudes_matricula_documentos ALTER COLUMN mime_type TYPE VARCHAR(30);

-- TEXT no reserva espacio fijo. Los límites replican las reglas del formulario
-- para impedir cargas excesivas también si se escribe directamente por SQL.
ALTER TABLE matriculas ADD CONSTRAINT chk_matriculas_observaciones_longitud
    CHECK (char_length(observaciones) <= 500);
ALTER TABLE inscripciones_refuerzo ADD CONSTRAINT chk_refuerzo_observaciones_longitud
    CHECK (char_length(observaciones) <= 500);
ALTER TABLE estudiantes ADD CONSTRAINT chk_estudiantes_salud_longitud
    CHECK (char_length(alergias_condiciones) <= 4000);
ALTER TABLE competencias ADD CONSTRAINT chk_competencias_descripcion_longitud
    CHECK (char_length(descripcion) <= 4000);
ALTER TABLE asistencias_aula ADD CONSTRAINT chk_asistencias_justificacion_longitud
    CHECK (char_length(motivo_justificacion) <= 4000);
ALTER TABLE incidencias_conductuales ADD CONSTRAINT chk_incidencias_descripcion_longitud
    CHECK (char_length(descripcion) <= 10000);
ALTER TABLE calificaciones_cneb ADD CONSTRAINT chk_calificaciones_conclusion_longitud
    CHECK (char_length(conclusion_descriptiva) <= 4000);
ALTER TABLE comunicados_oficiales ADD CONSTRAINT chk_comunicados_contenido_longitud
    CHECK (char_length(contenido) <= 20000);

ALTER TABLE usuarios RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE usuarios RENAME COLUMN updated_at TO fecha_actualizacion;
ALTER TABLE usuarios RENAME COLUMN email TO correo_electronico;
ALTER TABLE usuarios RENAME COLUMN username TO nombre_usuario;
ALTER TABLE usuarios RENAME COLUMN password_hash TO usuario_password;
ALTER TABLE sesiones RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE sesiones RENAME COLUMN token_hash TO huella_token;
ALTER TABLE sesiones RENAME COLUMN expira_at TO fecha_expiracion;
ALTER TABLE sesiones RENAME COLUMN ip_address TO direccion_ip;
ALTER TABLE sesiones RENAME COLUMN user_agent TO agente_usuario;
ALTER TABLE auditoria_cambios RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE apoderados RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE apoderados RENAME COLUMN email TO correo_electronico;
ALTER TABLE apoderados RENAME COLUMN nombres TO nombres_apoderado;
ALTER TABLE apoderados RENAME COLUMN apellido_paterno TO apellido_paterno_apoderado;
ALTER TABLE apoderados RENAME COLUMN apellido_materno TO apellido_materno_apoderado;
ALTER TABLE estudiantes RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE estudiantes RENAME COLUMN nombres TO nombres_estudiante;
ALTER TABLE estudiantes RENAME COLUMN apellido_paterno TO apellido_paterno_estudiante;
ALTER TABLE estudiantes RENAME COLUMN apellido_materno TO apellido_materno_estudiante;
ALTER TABLE matriculas RENAME COLUMN reserva_expira_at TO fecha_expiracion_reserva;
ALTER TABLE lotes_biometrico RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE lotes_biometrico RENAME COLUMN hash_contenido TO huella_contenido;
ALTER TABLE marcas_biometrico_porteria RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE incidencias_conductuales RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE calificaciones_cneb RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE calificaciones_cneb RENAME COLUMN updated_at TO fecha_actualizacion;
ALTER TABLE series_comprobante RENAME COLUMN updated_at TO fecha_actualizacion;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN created_at TO fecha_creacion;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN updated_at TO fecha_actualizacion;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN token_hash TO huella_token;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN pago_expira_at TO fecha_expiracion_pago;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN documentos_emitidos_at TO fecha_emision_documentos;
ALTER TABLE solicitudes_matricula_publica RENAME COLUMN email_apoderado TO correo_apoderado;
ALTER TABLE solicitudes_matricula_documentos RENAME COLUMN actualizado_at TO fecha_actualizacion;
ALTER TABLE solicitudes_matricula_documentos RENAME COLUMN bucket TO contenedor;
ALTER TABLE solicitudes_matricula_documentos RENAME COLUMN object_key TO ruta_objeto;
ALTER TABLE solicitudes_matricula_documentos RENAME COLUMN mime_type TO tipo_contenido;
ALTER TABLE solicitudes_matricula_documentos RENAME COLUMN sha256 TO huella_sha256;
ALTER TABLE roles RENAME COLUMN nombre TO nombre_rol;
ALTER TABLE roles RENAME COLUMN codigo TO codigo_rol;
ALTER TABLE niveles RENAME COLUMN nombre TO nombre_nivel;
ALTER TABLE niveles RENAME COLUMN codigo TO codigo_nivel;
ALTER TABLE grados RENAME COLUMN nombre TO nombre_grado;
ALTER TABLE periodos_academicos RENAME COLUMN nombre TO nombre_periodo;
ALTER TABLE aulas RENAME COLUMN nombre TO nombre_aula;
ALTER TABLE aulas RENAME COLUMN codigo TO codigo_aula;
ALTER TABLE areas_curriculares RENAME COLUMN nombre TO nombre_area;
ALTER TABLE areas_curriculares RENAME COLUMN codigo TO codigo_area;
ALTER TABLE competencias RENAME COLUMN nombre TO nombre_competencia;
ALTER TABLE conceptos_cobro RENAME COLUMN nombre TO nombre_concepto;
ALTER TABLE conceptos_cobro RENAME COLUMN codigo TO codigo_concepto;
ALTER TABLE comunicados_oficiales RENAME COLUMN titulo TO titulo_comunicado;
ALTER TABLE sesiones_refuerzo RENAME COLUMN tema TO tema_refuerzo;
ALTER TABLE sesiones_refuerzo RENAME COLUMN aula_asignada TO codigo_aula;

-- PL/pgSQL contiene referencias textuales que deben actualizarse explícitamente.
ALTER FUNCTION fn_auto_updated_at() RENAME TO fn_actualizar_fecha;
CREATE OR REPLACE FUNCTION fn_actualizar_fecha()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
ALTER TRIGGER trg_usuarios_updated_at ON usuarios RENAME TO trg_usuarios_fecha_actualizacion;
ALTER TRIGGER trg_calificaciones_updated_at ON calificaciones_cneb RENAME TO trg_calificaciones_fecha_actualizacion;
