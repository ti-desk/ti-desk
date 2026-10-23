-- V3: políticas de SLA por prioridad (horas). Configurables en BD, no en el código.
INSERT INTO politica_sla (prioridad, horas_primera_respuesta, horas_resolucion) VALUES
    ('CRITICA', 0.5,  4),
    ('ALTA',    2,   24),
    ('MEDIA',   8,   48),
    ('BAJA',   24,   72);