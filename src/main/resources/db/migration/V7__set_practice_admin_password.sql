-- Contraseña sin hash del administrador de práctica (no usar en producción): Admin2026!
UPDATE usuarios
SET password_hash = '$2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2'
WHERE email = 'fq94289@gmail.com'
  AND password_hash <> '$2a$10$m.2CkRxR18uYeRfeZKj8uOc3tcA77GRnQ1TVnhS9.GaOKLr2IMUa2';
