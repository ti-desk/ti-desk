-- Agrega SERVIDOR a los tipos de activo permitidos (pedido por el tecnico de soporte, SRS v1.3)
ALTER TABLE activo DROP CONSTRAINT ck_activo_tipo;
ALTER TABLE activo ADD CONSTRAINT ck_activo_tipo CHECK (tipo IN 
    ('LAPTOP', 'ESCRITORIO', 'IMPRESORA', 'SWITCH', 'ACCESS_POINT', 'PROYECTOR', 'SERVIDOR', 'OTRO')
);