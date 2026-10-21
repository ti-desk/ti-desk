# Diccionario de datos — TI-Desk (V1)

Responsable: Ulises. Todas las fechas se guardan en `timestamptz` (UTC); se presentan en America/Mexico_City.

## usuario
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | BIGINT (identity) | No | PK |
| nombre | VARCHAR(120) | No | Nombre completo |
| email | VARCHAR(160) | No | Correo institucional, único |
| password_hash | VARCHAR(100) | No | Hash BCrypt |
| rol | VARCHAR(20) | No | USUARIO, TECNICO, ADMIN (default USUARIO) |
| area | VARCHAR(100) | Sí | Área o carrera |
| activo | BOOLEAN | No | Activar/desactivar (RF-03) |
| creado_en | TIMESTAMPTZ | No | Alta |

## politica_sla
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| prioridad | VARCHAR(10) | No | PK: CRITICA, ALTA, MEDIA, BAJA |
| horas_primera_respuesta | NUMERIC(6,2) | No | Horas objetivo de primera respuesta (CRITICA = 0.5) |
| horas_resolucion | NUMERIC(6,2) | No | Horas objetivo de resolución |

## activo
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | BIGINT | No | PK |
| etiqueta_inventario | VARCHAR(40) | No | Única |
| tipo | VARCHAR(20) | No | LAPTOP, ESCRITORIO, IMPRESORA, SWITCH, ACCESS_POINT, PROYECTOR, OTRO |
| marca, modelo | VARCHAR(80) | Sí | |
| numero_serie | VARCHAR(80) | Sí | Indexado para búsqueda |
| ubicacion | VARCHAR(160) | Sí | |
| estado | VARCHAR(20) | No | ACTIVO o BAJA (baja lógica) |
| fecha_adquisicion | DATE | Sí | |
| responsable_id | BIGINT | Sí | FK usuario |

## ticket
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | BIGINT | No | PK |
| folio | VARCHAR(20) | No | Único, formato TID-AAAA-NNNNN |
| titulo | VARCHAR(150) | No | |
| descripcion | TEXT | No | |
| categoria | VARCHAR(20) | No | RED, CORREO, EQUIPO, SOFTWARE, ACCESOS, OTRO |
| prioridad | VARCHAR(10) | No | CRITICA, ALTA, MEDIA, BAJA |
| estado | VARCHAR(20) | No | ABIERTO, ASIGNADO, EN_PROCESO, EN_ESPERA, RESUELTO, CERRADO, CANCELADO |
| solicitante_id | BIGINT | No | FK usuario |
| tecnico_id | BIGINT | Sí | FK usuario (técnico asignado) |
| activo_id | BIGINT | Sí | FK activo |
| creado_en | TIMESTAMPTZ | No | |
| asignado_en, primera_respuesta_en, resuelto_en, cerrado_en | TIMESTAMPTZ | Sí | Marcas de tiempo del ciclo de vida |
| fecha_limite_sla | TIMESTAMPTZ | No | creado_en + horas_resolucion |
| sla_vencido | BOOLEAN | No | Lo marca RevisorSlaJob |
| sla_en_riesgo | BOOLEAN | No | ≥ 80 % del tiempo consumido |
| version | BIGINT | No | @Version, bloqueo optimista |

Índices: (estado, prioridad), tecnico_id, fecha_limite_sla, solicitante_id, activo_id.

## comentario
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | BIGINT | No | PK |
| ticket_id | BIGINT | No | FK ticket |
| autor_id | BIGINT | No | FK usuario |
| texto | TEXT | No | |
| interno | BOOLEAN | No | true = solo TECNICO/ADMIN |
| creado_en | TIMESTAMPTZ | No | |

## historial_estado
| Columna | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | BIGINT | No | PK |
| ticket_id | BIGINT | No | FK ticket |
| usuario_id | BIGINT | Sí | FK usuario; NULL = sistema |
| estado_anterior | VARCHAR(20) | Sí | NULL al crear el ticket |
| estado_nuevo | VARCHAR(20) | No | |
| fecha | TIMESTAMPTZ | No | |
| motivo | VARCHAR(500) | Sí | |

## folio_secuencia
| Columna | Tipo | Descripción |
|---|---|---|
| anio | INT (PK) | Año del folio |
| ultimo | INT | Último consecutivo usado en ese año |

## Relaciones (E-R)
- usuario 1—N ticket (como solicitante) y 1—N ticket (como técnico)
- activo 1—N ticket (opcional)
- ticket 1—N comentario · ticket 1—N historial_estado
- usuario 1—N comentario (autor) · usuario 1—N historial_estado
- usuario 1—N activo (responsable)
- politica_sla se consulta por `prioridad` (sin FK; la validan las restricciones CHECK)
