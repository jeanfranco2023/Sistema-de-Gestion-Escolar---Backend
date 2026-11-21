-- Altas de prueba sintéticas. Ejecutar con psql y cuatro variables de contraseña distintas.
-- Ejemplo: psql ... -v pwd_camila='...' -v pwd_alvaro='...' -v pwd_valeria='...' -v pwd_diego='...'
-- Las cuentas no incluyen DNI ni datos de contacto personales y deben cambiar su clave al ingresar.
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO public.usuarios (
  nombre_usuario, correo_electronico, usuario_password, activo, debe_cambiar_password,
  nombre_completo, especialidad
)
VALUES
  ('camila.rojas', 'camila.rojas@shuji.test', extensions.crypt(:'pwd_camila', extensions.gen_salt('bf', 12)), true, true, 'Camila Rojas Mendoza', 'Matemática'),
  ('alvaro.salazar', 'alvaro.salazar@shuji.test', extensions.crypt(:'pwd_alvaro', extensions.gen_salt('bf', 12)), true, true, 'Álvaro Salazar Quispe', 'Comunicación'),
  ('valeria.huaman', 'valeria.huaman@shuji.test', extensions.crypt(:'pwd_valeria', extensions.gen_salt('bf', 12)), true, true, 'Valeria Huamán Torres', 'Ciencia y Tecnología'),
  ('diego.navarro', 'diego.navarro@shuji.test', extensions.crypt(:'pwd_diego', extensions.gen_salt('bf', 12)), true, true, 'Diego Navarro Paredes', 'Inglés')
ON CONFLICT (nombre_usuario) DO NOTHING;

INSERT INTO public.usuario_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM public.usuarios u
JOIN public.roles r ON r.codigo_rol = 'DOCENTE'
WHERE u.nombre_usuario IN ('camila.rojas', 'alvaro.salazar', 'valeria.huaman', 'diego.navarro')
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

COMMIT;
