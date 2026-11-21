-- Datos base y administrador inicial para la instancia de práctica.
-- La cuenta inicial requiere un hash BCrypt externo en app.seed_password_hash.
INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('DIRECCION', 'Dirección General', 'Acceso estratégico, reportería y configuración institucional'),
    ('SECRETARIA', 'Secretaría Académica', 'Gestión de matrículas, vacantes, expedientes y caja'),
    ('DOCENTE', 'Plana Docente', 'Registro de notas CNEB, asistencia a refuerzo y comunicados'),
    ('AUXILIAR', 'Auxiliar de Educación', 'Pase de lista diario, importación biométrica e incidencias'),
    ('TUTOR', 'Tutor de Aula', 'Seguimiento formativo, conciliación de ausencias y citaciones'),
    ('APODERADO', 'Padre de Familia / Tutor', 'Acceso al portal para pago de pensiones y boletas')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO niveles (codigo, nombre) VALUES
    ('INICIAL', 'Educación Inicial'),
    ('PRIMARIA', 'Educación Primaria'),
    ('SECUNDARIA', 'Educación Secundaria')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO conceptos_cobro (codigo, nombre, tipo_concepto, monto_sugerido) VALUES
    ('MATRICULA_ANUAL', 'Derecho de Matrícula y Admisión Anual', 'MATRICULA', 350.00),
    ('PENSION_MENSUAL', 'Pensión Escolar de Enseñanza (Cuotas 1 a 10)', 'PENSION', 380.00),
    ('CONSTANCIA_ESTUDIOS', 'Emisión de Constancia de Estudios', 'CERTIFICADO', 25.00)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO usuarios (username, email, password_hash, activo)
SELECT 'fq94289@gmail.com', 'fq94289@gmail.com', current_setting('app.seed_password_hash', true), TRUE WHERE current_setting('app.seed_password_hash', true) ~ '^\[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'
ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'DIRECCION'
WHERE u.email = 'fq94289@gmail.com'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;
