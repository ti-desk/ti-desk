-- V4: usuarios de prueba, uno por rol, para pruebas y demo.
-- Contraseña de los tres: TiDesk#2026 (hash BCrypt, costo 10).
-- Solo para entornos de desarrollo y pruebas; no usar en producción.
INSERT INTO usuario (nombre, email, password_hash, rol, area) VALUES
    ('Administrador Demo', 'admin.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'ADMIN',   'Departamento de TI'),
    ('Técnico Demo',       'tecnico.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'TECNICO', 'Departamento de TI'),
    ('Usuario Demo',       'usuario.demo@unistmo.edu.mx',
     '$2a$10$26oYGd.Zx/A7WGT07iASBu.TES/6NfxXvLfLnQEQOBFdWbQBbKLhW', 'USUARIO', 'Instituto de Tecnologías de la Información');
