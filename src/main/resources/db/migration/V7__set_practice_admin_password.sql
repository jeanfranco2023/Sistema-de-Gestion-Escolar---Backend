-- Restablece la cuenta solo cuando se proporciona un hash BCrypt local.
UPDATE usuarios
SET password_hash = current_setting('app.seed_password_hash', true)
WHERE email = 'fq94289@gmail.com' AND current_setting('app.seed_password_hash', true) ~ '^\[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' AND password_hash <> current_setting('app.seed_password_hash', true);
