-- El proyecto acordó admitir correos de hasta 100 caracteres en sus tres flujos.
-- Ampliar VARCHAR no trunca ni elimina los valores existentes.
ALTER TABLE public.usuarios
    ALTER COLUMN correo_electronico TYPE VARCHAR(100);

ALTER TABLE public.apoderados
    ALTER COLUMN correo_electronico TYPE VARCHAR(100);

ALTER TABLE public.solicitudes_matricula_publica
    ALTER COLUMN correo_apoderado TYPE VARCHAR(100);
