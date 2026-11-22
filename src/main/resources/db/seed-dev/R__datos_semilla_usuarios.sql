-- Semilla de usuarios de prueba, uno por rol. SOLO PARA DESARROLLO Y PRUEBAS.
--
-- Esta carpeta (db/seed-dev) NO está en la ubicación por defecto de Flyway:
-- solo se aplica con el perfil "dev" (ver application-dev.yml). En el entorno
-- desplegado el administrador se crea con ADMIN_EMAIL / ADMIN_PASSWORD (AdminInicial).
--
-- Es una migración repetible (R__) y no versionada para que:
--   * no ocupe un número de versión ni choque con las migraciones de db/migration;
--   * Flyway la aplique después de todas las versionadas.
-- Por eso es idempotente (ON CONFLICT): puede volver a correr sin duplicar.
--
-- La contraseña de estos usuarios se comparte por el canal del equipo, no en el repositorio.
INSERT INTO usuario (nombre, email, password_hash, rol, area) VALUES
    ('Administrador Demo', 'admin.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'ADMIN',   'Departamento de TI'),
    ('Técnico Demo',       'tecnico.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'TECNICO', 'Departamento de TI'),
    ('Usuario Demo',       'usuario.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'USUARIO', 'Instituto de Tecnologías de la Información')
ON CONFLICT (email) DO NOTHING;
