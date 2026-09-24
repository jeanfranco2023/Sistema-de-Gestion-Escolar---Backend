# Regla de trabajo del proyecto

- Trabajar directamente en la rama `chore/limpieza-deps` de este repositorio; no usar `main-verify`, worktrees ni copias paralelas.
- Backend vive en esta carpeta. Frontend vive separado en `C:\Users\PC\Desktop\proyecto Integrador\frontend`; no recrear ni mantener otra copia de código frontend dentro de `backend`.
- Verificar el frontend en su carpeta externa y el backend aquí, cada uno con sus propias dependencias y compilación.
- Preservar todos los cambios locales existentes; no incluirlos en commits salvo que el usuario lo solicite.
- No cambiar de rama. Hacer `commit` o `push` cuando el usuario lo pida explícitamente.
