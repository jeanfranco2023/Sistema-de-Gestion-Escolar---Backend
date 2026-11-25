-- V63: Llenado al 100% de todas las aulas/secciones 2026 (Inicial, Primaria y Secundaria)
-- con estudiantes y apoderados realistas sin nombres ni apellidos duplicados,
-- y completado integral de asignaciones y horarios de seccion para todos los grados 2026 y 2027.

DO $$
DECLARE
  v_anio_2026_id smallint;
  v_anio_2027_id smallint;
  v_concepto_mat_id smallint;

  -- Catalogo amplio de nombres y apellidos peruanos realistas (sin duplicados)
  v_nombres_m text[] := ARRAY[
    'Alejandro','Alonso','Andres','Angel','Antonio','Arturo','Benjamin','Bruno','Camilo','Carlos',
    'Cesar','Christian','Cristobal','Daniel','Dante','David','Diego','Eduardo','Elias','Emiliano',
    'Enrique','Esteban','Fabian','Felipe','Fernando','Francisco','Gabriel','Gael','Gonzalo','Guillermo',
    'Gustavo','Hector','Hugo','Ian','Ignacio','Ivan','Javier','Joaquin','Jorge','Jose',
    'Juan','Julian','Leonardo','Liam','Lucas','Luis','Manuel','Marco','Martin','Mateo',
    'Matias','Mauricio','Miguel','Nicolas','Oscar','Pablo','Patricio','Pedro','Rafael','Raul',
    'Renato','Ricardo','Roberto','Rodrigo','Samuel','Santiago','Sebastian','Sergio','Thiago','Tomas'
  ];
  v_segundos_m text[] := ARRAY[
    'Adriano','Alberto','Alexander','Alfonso','Armando','Augusto','Caleb','Dario','Edgardo','Edson',
    'Efrain','Eliseo','Ernesto','Ezequiel','Fabricio','Federico','Fidel','Franco','Gerardo','German',
    'Gianfranco','Giancarlo','Gilberto','Harold','Hernan','Horacio','Humberto','Ismael','Israel','Jacobo',
    'Jaime','Jairo','Jefferson','Jeremy','Jesus','Jonathan','Josue','Julio','Kevin','Leandro',
    'Lorenzo','Luciano','Marcelo','Marcos','Mariano','Mario','Moises','Nestor','Omar','Orlando',
    'Paulo','Ramiro','Renzo','Reynaldo','Rodolfo','Rolando','Ruben','Salvador','Ulises','Victor'
  ];
  v_nombres_f text[] := ARRAY[
    'Abril','Adriana','Alejandra','Alessandra','Alexa','Alexandra','Alicia','Alondra','Amanda','Ana',
    'Andrea','Angela','Antonella','Ariana','Arlet','Beatriz','Belen','Bianca','Brenda','Camila',
    'Carla','Carmen','Carolina','Catalina','Cecilia','Celeste','Clara','Claudia','Cristina','Daniela',
    'Diana','Elena','Elisa','Elizabeth','Emilia','Emily','Esmeralda','Estefania','Estela','Eva',
    'Fabiana','Fatima','Fernanda','Fiorella','Florencia','Francisca','Gabriela','Gala','Gemma','Gisela',
    'Gloria','Gracia','Guadalupe','Helena','Ines','Irene','Isabel','Isabella','Ivanna','Jazmin',
    'Jimena','Johana','Josefina','Juana','Julia','Juliana','Karen','Karla','Kiara','Laura'
  ];
  v_segundos_f text[] := ARRAY[
    'Leticia','Lia','Liliana','Lorena','Lourdes','Lucia','Luciana','Luisa','Luna','Luz',
    'Macarena','Magdalena','Maite','Manuela','Marcela','Margarita','Maria','Mariana','Maribel','Marisol',
    'Marta','Martina','Melissa','Mercedes','Micaela','Milagros','Miranda','Miriam','Monica','Nadia',
    'Natalia','Nicole','Noemi','Nora','Olivia','Paloma','Pamela','Paola','Patricia','Paula',
    'Pilar','Rafaela','Raquel','Rebeca','Regina','Renata','Rocio','Romina','Rosa','Rosario',
    'Roxana','Salome','Samantha','Sandra','Sara','Silvana','Silvia','Sofia','Sol','Sonia',
    'Stefany','Susana','Tamara','Tatiana','Teresa','Valentina','Valeria','Vanesa','Veronica','Victoria',
    'Violeta','Viviana','Ximena','Yadira','Yamila','Yanina','Yesenia','Yolanda','Zoe','Zulema'
  ];
  v_apellidos text[] := ARRAY[
    'Acosta','Aguilar','Alarcon','Alva','Alvarado','Alvarez','Amaya','Ancajima','Andrade','Angeles',
    'Apaza','Arana','Arce','Arevalo','Arias','Arroyo','Ascencio','Avalos','Avila','Ayala',
    'Baez','Baldeon','Ballesteros','Barbaran','Barreto','Barrios','Bautista','Becerra','Benavides','Benites',
    'Bernal','Berrocal','Blas','Bravo','Briceño','Bustamante','Caballero','Cabrera','Caceres','Calderon',
    'Calla','Camacho','Campos','Canales','Cano','Carbajal','Cardenas','Cardozo','Carhuaz','Carranza',
    'Carrasco','Carrera','Carrillo','Carvajal','Casas','Castañeda','Castillo','Castro','Caycho','Centeno',
    'Cervantes','Chacon','Chamba','Chavez','Chinchay','Choque','Cisneros','Collantes','Conde','Contreras',
    'Cordova','Cornejo','Coronado','Correa','Cortez','Cruz','Cuadros','Cuba','Cueva','Davila',
    'De la Cruz','Delgado','Diaz','Dominguez','Dueñas','Duran','Echevarria','Enciso','Enriquez','Escobar',
    'Espinoza','Esquivel','Esteban','Estrada','Fajardo','Falcon','Fernandez','Figueroa','Flores','Fuentes'
  ];
  v_vias text[] := ARRAY[
    'Av. Universitaria','Jr. Las Begonias','Ca. Los Laureles','Av. Arequipa','Jr. Huiracocha',
    'Av. Brasil','Ca. Los Nogales','Av. Los Proceres','Jr. Los Alamos','Av. El Sol',
    'Ca. Las Orquideas','Av. Tupac Amaru','Jr. Ancash','Av. La Marina','Ca. Los Pinos',
    'Av. Grau','Jr. Junin','Ca. Santa Rosa','Av. Colonial','Jr. Cusco'
  ];
  v_distritos text[] := ARRAY[
    'Los Olivos, Lima','San Martin de Porres, Lima','Comas, Lima','Independencia, Lima',
    'Jesus Maria, Lima','Pueblo Libre, Lima','San Miguel, Lima','Lince, Lima',
    'Rimac, Lima','Breña, Lima','Surco, Lima','San Borja, Lima'
  ];

  v_sec RECORD;
  v_slot integer;
  v_faltantes integer;
  v_idx integer := 0;
  v_dni_est_num integer := 75000000;
  v_dni_apod_num integer := 45000000;
  v_cel_num bigint := 910000000;

  v_genero char(1);
  v_nom_est text;
  v_ap_pat text;
  v_ap_mat text;
  v_nom_apod text;
  v_ap_pat_apod text;
  v_ap_mat_apod text;
  v_parentesco text;
  v_dni_est text;
  v_dni_apod text;
  v_celular text;
  v_correo text;
  v_direccion text;
  v_fecha_nac date;
  v_edad_base integer;

  v_est_id bigint;
  v_apod_id bigint;
  v_mat_id bigint;

  v_i1 integer;
  v_i2 integer;
  v_ip integer;
  v_im integer;
  v_attempts integer;

  -- Variables Bloque B (Horarios 2027)
  v_h RECORD;
  v_alt RECORD;
  v_asig27_id bigint;
  v_doc27_id bigint;
  v_ya_existe boolean;
  v_doc_ocupado boolean;
BEGIN
  SELECT id INTO v_anio_2026_id FROM public.anios_lectivos WHERE anio = 2026;
  SELECT id INTO v_anio_2027_id FROM public.anios_lectivos WHERE anio = 2027;
  SELECT id INTO v_concepto_mat_id
    FROM public.conceptos_cobro
   WHERE codigo_concepto = 'MATRICULA_ANUAL' AND tipo_concepto = 'MATRICULA'
   LIMIT 1;

  IF v_anio_2026_id IS NULL THEN
    RAISE EXCEPTION 'No existe el año lectivo 2026';
  END IF;
  IF v_concepto_mat_id IS NULL THEN
    RAISE EXCEPTION 'No existe el concepto MATRICULA_ANUAL';
  END IF;

  -- =========================================================================
  -- BLOQUE A: Llenar al 100% todas las secciones/aulas del año 2026
  -- =========================================================================
  FOR v_sec IN
    SELECT s.id AS seccion_id,
           s.cupo_maximo,
           s.letra,
           g.id AS grado_id,
           g.nivel_id,
           g.numero_grado,
           (SELECT count(*)::int
              FROM public.matriculas m
             WHERE m.seccion_id = s.id
               AND m.estado_matricula = 'MATRICULADO') AS mat_activas
      FROM public.secciones s
      JOIN public.grados g ON g.id = s.grado_id
     WHERE s.anio_lectivo_id = v_anio_2026_id
     ORDER BY g.nivel_id, g.numero_grado, s.letra
  LOOP
    v_faltantes := GREATEST(0, v_sec.cupo_maximo - v_sec.mat_activas);
    IF v_faltantes = 0 THEN
      CONTINUE;
    END IF;

    -- Edad normativa EBR cumplida al 31 de marzo de 2026
    v_edad_base := CASE v_sec.nivel_id
      WHEN 1 THEN v_sec.numero_grado + 2
      WHEN 2 THEN v_sec.numero_grado + 5
      ELSE v_sec.numero_grado + 11
    END;

    FOR v_slot IN 1..v_faltantes LOOP
      v_attempts := 0;
      LOOP
        v_idx := v_idx + 1;
        v_attempts := v_attempts + 1;
        IF v_attempts > 10000 THEN
          RAISE EXCEPTION 'No se pudo generar combinación única de nombre/apellidos después de 10000 intentos';
        END IF;

        IF (v_idx % 2) = 0 THEN
          v_genero := 'M';
          v_i1 := 1 + ((v_idx - 1) % array_length(v_nombres_m, 1));
          v_i2 := 1 + (((v_idx - 1) / array_length(v_nombres_m, 1) * 7 + v_idx * 3) % array_length(v_segundos_m, 1));
          v_nom_est := v_nombres_m[v_i1] || ' ' || v_segundos_m[v_i2];
        ELSE
          v_genero := 'F';
          v_i1 := 1 + ((v_idx - 1) % array_length(v_nombres_f, 1));
          v_i2 := 1 + (((v_idx - 1) / array_length(v_nombres_f, 1) * 7 + v_idx * 3) % array_length(v_segundos_f, 1));
          v_nom_est := v_nombres_f[v_i1] || ' ' || v_segundos_f[v_i2];
        END IF;

        -- 100 x 99 = 9900 pares únicos de (apellido_paterno, apellido_materno)
        v_ip := 1 + ((v_idx - 1) % array_length(v_apellidos, 1));
        v_im := 1 + ((((v_idx - 1) / array_length(v_apellidos, 1)) + 1 + ((v_idx - 1) % 97)) % array_length(v_apellidos, 1));
        IF v_ip = v_im THEN
          v_im := 1 + (v_im % array_length(v_apellidos, 1));
        END IF;

        v_ap_pat := v_apellidos[v_ip];
        v_ap_mat := v_apellidos[v_im];

        EXIT WHEN NOT EXISTS (
          SELECT 1
            FROM public.estudiantes e
           WHERE (
                   upper(btrim(e.apellido_paterno_estudiante)) = upper(btrim(v_ap_pat))
               AND upper(btrim(e.apellido_materno_estudiante)) = upper(btrim(v_ap_mat))
                 )
              OR (
                   upper(btrim(e.nombres_estudiante)) = upper(btrim(v_nom_est))
               AND upper(btrim(e.apellido_paterno_estudiante)) = upper(btrim(v_ap_pat))
                 )
        );
      END LOOP;

      -- DNI sintético único de 8 dígitos para estudiante
      LOOP
        v_dni_est_num := v_dni_est_num + 1;
        v_dni_est := lpad(v_dni_est_num::text, 8, '0');
        EXIT WHEN NOT EXISTS (
          SELECT 1 FROM public.estudiantes WHERE numero_documento = v_dni_est
        );
      END LOOP;

      -- Fecha de nacimiento realista acorde al grado (entre abril y marzo del rango etario)
      v_fecha_nac := make_date(2025 - v_edad_base, 4, 1) + ((v_idx * 17) % 355);

      INSERT INTO public.estudiantes (
        tipo_documento, numero_documento, nombres_estudiante,
        apellido_paterno_estudiante, apellido_materno_estudiante,
        fecha_nacimiento, genero, validado_reniec, origen_registro,
        activo, fecha_creacion, estado
      ) VALUES (
        'DNI', v_dni_est, v_nom_est,
        v_ap_pat, v_ap_mat,
        v_fecha_nac, v_genero, false, 'MANUAL_CONTINGENCIA',
        true, NOW(), 'ACTIVO'
      )
      RETURNING id INTO v_est_id;

      -- Apoderado realista coherente con apellido del estudiante
      LOOP
        v_dni_apod_num := v_dni_apod_num + 1;
        v_dni_apod := lpad(v_dni_apod_num::text, 8, '0');
        EXIT WHEN NOT EXISTS (
          SELECT 1 FROM public.apoderados WHERE numero_documento = v_dni_apod
        );
      END LOOP;

      v_cel_num := v_cel_num + 1;
      v_celular := v_cel_num::text;
      IF (v_idx % 2) = 1 THEN
        v_parentesco := 'MADRE';
        v_nom_apod := v_nombres_f[1 + ((v_idx * 13) % array_length(v_nombres_f, 1))]
                   || ' ' || v_segundos_f[1 + ((v_idx * 19) % array_length(v_segundos_f, 1))];
        v_ap_pat_apod := v_ap_mat;
        v_ap_mat_apod := v_apellidos[1 + ((v_im + 23) % array_length(v_apellidos, 1))];
      ELSE
        v_parentesco := 'PADRE';
        v_nom_apod := v_nombres_m[1 + ((v_idx * 13) % array_length(v_nombres_m, 1))]
                   || ' ' || v_segundos_m[1 + ((v_idx * 19) % array_length(v_segundos_m, 1))];
        v_ap_pat_apod := v_ap_pat;
        v_ap_mat_apod := v_apellidos[1 + ((v_ip + 29) % array_length(v_apellidos, 1))];
      END IF;

      v_correo := lower(regexp_replace(
        translate(split_part(v_nom_apod, ' ', 1) || '.' || v_ap_pat_apod || '.' || v_dni_apod,
                  'ÁÉÍÓÚáéíóúÑñ', 'AEIOUaeiouNn'),
        '[^a-zA-Z0-9.]', '', 'g'
      )) || '@familia.shuji.pe';

      v_direccion := v_vias[1 + ((v_idx - 1) % array_length(v_vias, 1))]
                  || ' ' || (100 + ((v_idx * 13) % 890))
                  || ', ' || v_distritos[1 + ((v_idx - 1) % array_length(v_distritos, 1))];

      INSERT INTO public.apoderados (
        tipo_documento, numero_documento, nombres_apoderado,
        apellido_paterno_apoderado, apellido_materno_apoderado,
        celular, correo_electronico, direccion,
        validado_reniec, origen_registro, fecha_creacion, activo, estado
      ) VALUES (
        'DNI', v_dni_apod, v_nom_apod,
        v_ap_pat_apod, v_ap_mat_apod,
        v_celular, v_correo, v_direccion,
        false, 'MANUAL_CONTINGENCIA', NOW(), true, 'ACTIVO'
      )
      RETURNING id INTO v_apod_id;

      INSERT INTO public.estudiante_apoderados (
        estudiante_id, apoderado_id, parentesco,
        es_responsable_economico, tiene_custodia, permite_recojo
      ) VALUES (
        v_est_id, v_apod_id, v_parentesco,
        true, true, true
      );

      -- Matricula oficial en marzo 2026 (trg_gestionar_vacante_matricula actualiza vacantes_ocupadas)
      INSERT INTO public.matriculas (
        anio_lectivo_id, estudiante_id, seccion_id,
        fecha_matricula, estado_matricula, modalidad_ingreso
      ) VALUES (
        v_anio_2026_id, v_est_id, v_sec.seccion_id,
        TIMESTAMP '2026-03-02 09:00:00' + ((v_idx % 12) * INTERVAL '1 day'),
        'MATRICULADO', 'REGULAR'
      )
      RETURNING id INTO v_mat_id;

      -- Derecho de matricula pagado (trg_pensiones_pago_matricula genera las 10 cuotas de pension 2026)
      INSERT INTO public.obligaciones_pago (
        matricula_id, concepto_id, tipo_concepto, numero_cuota,
        descripcion, fecha_vencimiento, monto_base, monto_mora,
        monto_descuento, total_pagado, estado
      ) VALUES (
        v_mat_id, v_concepto_mat_id, 'MATRICULA', 0,
        'Derecho de matrícula anual 2026', DATE '2026-03-10', 350.00, 0.00,
        0.00, 350.00, 'PAGADO_TOTAL'
      );
    END LOOP;
  END LOOP;

  -- =========================================================================
  -- BLOQUE B: Completar asignaciones_docentes y horarios_seccion para 2027
  -- (2026 ya cuenta con mallas_horarias_grado y horarios_seccion completos)
  -- =========================================================================
  IF v_anio_2027_id IS NOT NULL THEN
    -- 1. Clonar asignaciones_docentes de 2026 -> 2027 por (grado_id, letra)
    INSERT INTO public.asignaciones_docentes (
      docente_usuario_id, seccion_id, anio_lectivo_id, nivel_id, area_curricular_id
    )
    SELECT a26.docente_usuario_id,
           s27.id,
           v_anio_2027_id,
           a26.nivel_id,
           a26.area_curricular_id
      FROM public.asignaciones_docentes a26
      JOIN public.secciones s26 ON s26.id = a26.seccion_id
      JOIN public.secciones s27
        ON s27.anio_lectivo_id = v_anio_2027_id
       AND s27.grado_id = s26.grado_id
       AND s27.letra = s26.letra
     WHERE a26.anio_lectivo_id = v_anio_2026_id
    ON CONFLICT (seccion_id, area_curricular_id, anio_lectivo_id) DO NOTHING;

    -- 2. Insertar primero los horarios de asignaciones cuyo docente coincide con 2026
    INSERT INTO public.horarios_seccion (
      anio_lectivo_id, seccion_id, docente_usuario_id,
      asignacion_docente_id, dia_semana, bloque_horario_id, activo
    )
    SELECT v_anio_2027_id,
           s27.id,
           a27.docente_usuario_id,
           a27.id,
           h26.dia_semana,
           h26.bloque_horario_id,
           true
      FROM public.horarios_seccion h26
      JOIN public.asignaciones_docentes a26 ON a26.id = h26.asignacion_docente_id
      JOIN public.secciones s26 ON s26.id = h26.seccion_id
      JOIN public.secciones s27
        ON s27.anio_lectivo_id = v_anio_2027_id
       AND s27.grado_id = s26.grado_id
       AND s27.letra = s26.letra
      JOIN public.asignaciones_docentes a27
        ON a27.anio_lectivo_id = v_anio_2027_id
       AND a27.seccion_id = s27.id
       AND a27.area_curricular_id = a26.area_curricular_id
     WHERE h26.anio_lectivo_id = v_anio_2026_id
       AND h26.activo = true
       AND a27.docente_usuario_id = a26.docente_usuario_id
       AND NOT EXISTS (
         SELECT 1
           FROM public.horarios_seccion ex
          WHERE ex.anio_lectivo_id = v_anio_2027_id
            AND ex.seccion_id = s27.id
            AND ex.dia_semana = h26.dia_semana
            AND ex.bloque_horario_id = h26.bloque_horario_id
            AND ex.activo = true
       )
       AND NOT EXISTS (
         SELECT 1
           FROM public.horarios_seccion ex2
          WHERE ex2.anio_lectivo_id = v_anio_2027_id
            AND ex2.docente_usuario_id = a27.docente_usuario_id
            AND ex2.dia_semana = h26.dia_semana
            AND ex2.bloque_horario_id = h26.bloque_horario_id
            AND ex2.activo = true
       );

    -- 3. Completar horarios para asignaciones 2027 cuyo docente fue reasignado previamente
    -- (resuelve posibles colisiones docentes mediante intercambio limpio dentro de la misma sección)
    FOR v_h IN
      SELECT s27.id AS seccion_27_id,
             a27.id AS asig_27_id,
             a27.docente_usuario_id AS doc_27_id,
             h26.dia_semana,
             h26.bloque_horario_id
        FROM public.horarios_seccion h26
        JOIN public.asignaciones_docentes a26 ON a26.id = h26.asignacion_docente_id
        JOIN public.secciones s26 ON s26.id = h26.seccion_id
        JOIN public.secciones s27
          ON s27.anio_lectivo_id = v_anio_2027_id
         AND s27.grado_id = s26.grado_id
         AND s27.letra = s26.letra
        JOIN public.asignaciones_docentes a27
          ON a27.anio_lectivo_id = v_anio_2027_id
         AND a27.seccion_id = s27.id
         AND a27.area_curricular_id = a26.area_curricular_id
       WHERE h26.anio_lectivo_id = v_anio_2026_id
         AND h26.activo = true
         AND a27.docente_usuario_id <> a26.docente_usuario_id
       ORDER BY s27.id, h26.dia_semana, h26.bloque_horario_id
    LOOP
      v_asig27_id := v_h.asig_27_id;
      v_doc27_id := v_h.doc_27_id;

      SELECT EXISTS (
        SELECT 1
          FROM public.horarios_seccion ex
         WHERE ex.anio_lectivo_id = v_anio_2027_id
           AND ex.seccion_id = v_h.seccion_27_id
           AND ex.dia_semana = v_h.dia_semana
           AND ex.bloque_horario_id = v_h.bloque_horario_id
           AND ex.activo = true
      ) INTO v_ya_existe;

      IF v_ya_existe THEN
        CONTINUE;
      END IF;

      SELECT EXISTS (
        SELECT 1
          FROM public.horarios_seccion ex
         WHERE ex.anio_lectivo_id = v_anio_2027_id
           AND ex.docente_usuario_id = v_doc27_id
           AND ex.dia_semana = v_h.dia_semana
           AND ex.bloque_horario_id = v_h.bloque_horario_id
           AND ex.activo = true
      ) INTO v_doc_ocupado;

      IF NOT v_doc_ocupado THEN
        INSERT INTO public.horarios_seccion (
          anio_lectivo_id, seccion_id, docente_usuario_id,
          asignacion_docente_id, dia_semana, bloque_horario_id, activo
        ) VALUES (
          v_anio_2027_id, v_h.seccion_27_id, v_doc27_id,
          v_asig27_id, v_h.dia_semana, v_h.bloque_horario_id, true
        );
      ELSE
        -- Buscar un bloque ya ocupado en la misma sección 2027 para intercambiar sin colisiones
        SELECT ex.id, ex.docente_usuario_id, ex.dia_semana, ex.bloque_horario_id
          INTO v_alt
          FROM public.horarios_seccion ex
         WHERE ex.anio_lectivo_id = v_anio_2027_id
           AND ex.seccion_id = v_h.seccion_27_id
           AND ex.activo = true
           AND NOT EXISTS (
             SELECT 1
               FROM public.horarios_seccion c1
              WHERE c1.anio_lectivo_id = v_anio_2027_id
                AND c1.docente_usuario_id = ex.docente_usuario_id
                AND c1.dia_semana = v_h.dia_semana
                AND c1.bloque_horario_id = v_h.bloque_horario_id
                AND c1.activo = true
           )
           AND NOT EXISTS (
             SELECT 1
               FROM public.horarios_seccion c2
              WHERE c2.anio_lectivo_id = v_anio_2027_id
                AND c2.docente_usuario_id = v_doc27_id
                AND c2.dia_semana = ex.dia_semana
                AND c2.bloque_horario_id = ex.bloque_horario_id
                AND c2.activo = true
           )
         LIMIT 1;

        IF v_alt.id IS NOT NULL THEN
          UPDATE public.horarios_seccion
             SET dia_semana = v_h.dia_semana,
                 bloque_horario_id = v_h.bloque_horario_id
           WHERE id = v_alt.id;

          INSERT INTO public.horarios_seccion (
            anio_lectivo_id, seccion_id, docente_usuario_id,
            asignacion_docente_id, dia_semana, bloque_horario_id, activo
          ) VALUES (
            v_anio_2027_id, v_h.seccion_27_id, v_doc27_id,
            v_asig27_id, v_alt.dia_semana, v_alt.bloque_horario_id, true
          );
        END IF;
      END IF;
    END LOOP;
  END IF;

  -- Sincronizar secuencias afectadas
  PERFORM setval(pg_get_serial_sequence('public.estudiantes', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.estudiantes), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.apoderados', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.apoderados), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.estudiante_apoderados', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.estudiante_apoderados), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.matriculas', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.matriculas), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.obligaciones_pago', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.obligaciones_pago), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.asignaciones_docentes', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.asignaciones_docentes), 1), true);
  PERFORM setval(pg_get_serial_sequence('public.horarios_seccion', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM public.horarios_seccion), 1), true);
END $$;
