CREATE INDEX ix_ticket_estado_prioridad ON ticket (estado, prioridad);
CREATE INDEX ix_ticket_tecnico          ON ticket (tecnico_id);
CREATE INDEX ix_ticket_fecha_limite_sla ON ticket (fecha_limite_sla);
CREATE INDEX ix_ticket_solicitante      ON ticket (solicitante_id);
CREATE INDEX ix_ticket_activo           ON ticket (activo_id);
CREATE INDEX ix_comentario_ticket       ON comentario (ticket_id);
CREATE INDEX ix_historial_ticket        ON historial_estado (ticket_id);
CREATE INDEX ix_activo_numero_serie     ON activo (numero_serie);