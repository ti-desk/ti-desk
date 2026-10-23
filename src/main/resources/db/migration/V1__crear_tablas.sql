-- V1: esquema base de TI-Desk. Fechas en timestamptz (UTC).

CREATE TABLE usuario (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL DEFAULT 'USUARIO',
    area          VARCHAR(100),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('USUARIO', 'TECNICO', 'ADMIN'))
);

CREATE TABLE politica_sla (
    prioridad               VARCHAR(10)  PRIMARY KEY,
    horas_primera_respuesta NUMERIC(6,2) NOT NULL,
    horas_resolucion        NUMERIC(6,2) NOT NULL,
    CONSTRAINT ck_politica_prioridad CHECK (prioridad IN ('CRITICA', 'ALTA', 'MEDIA', 'BAJA'))
);

CREATE TABLE activo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    etiqueta_inventario VARCHAR(40) NOT NULL UNIQUE,
    tipo                VARCHAR(20) NOT NULL,
    marca               VARCHAR(80),
    modelo              VARCHAR(80),
    numero_serie        VARCHAR(80),
    ubicacion           VARCHAR(160),
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_adquisicion   DATE,
    responsable_id      BIGINT REFERENCES usuario(id),
    CONSTRAINT ck_activo_tipo CHECK (tipo IN
        ('LAPTOP', 'ESCRITORIO', 'IMPRESORA', 'SWITCH', 'ACCESS_POINT', 'PROYECTOR', 'OTRO')),
    CONSTRAINT ck_activo_estado CHECK (estado IN ('ACTIVO', 'BAJA'))
);

CREATE TABLE ticket (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    folio                 VARCHAR(20)  NOT NULL UNIQUE,
    titulo                VARCHAR(150) NOT NULL,
    descripcion           TEXT         NOT NULL,
    categoria             VARCHAR(20)  NOT NULL,
    prioridad             VARCHAR(10)  NOT NULL,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
    solicitante_id        BIGINT       NOT NULL REFERENCES usuario(id),
    tecnico_id            BIGINT       REFERENCES usuario(id),
    activo_id             BIGINT       REFERENCES activo(id),
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    asignado_en           TIMESTAMPTZ,
    primera_respuesta_en  TIMESTAMPTZ,
    resuelto_en           TIMESTAMPTZ,
    cerrado_en            TIMESTAMPTZ,
    fecha_limite_sla      TIMESTAMPTZ  NOT NULL,
    sla_vencido           BOOLEAN      NOT NULL DEFAULT FALSE,
    sla_en_riesgo         BOOLEAN      NOT NULL DEFAULT FALSE,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_ticket_categoria CHECK (categoria IN
        ('RED', 'CORREO', 'EQUIPO', 'SOFTWARE', 'ACCESOS', 'OTRO')),
    CONSTRAINT ck_ticket_prioridad CHECK (prioridad IN ('CRITICA', 'ALTA', 'MEDIA', 'BAJA')),
    CONSTRAINT ck_ticket_estado CHECK (estado IN
        ('ABIERTO', 'ASIGNADO', 'EN_PROCESO', 'EN_ESPERA', 'RESUELTO', 'CERRADO', 'CANCELADO'))
);

CREATE TABLE comentario (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id BIGINT      NOT NULL REFERENCES ticket(id),
    autor_id  BIGINT      NOT NULL REFERENCES usuario(id),
    texto     TEXT        NOT NULL,
    interno   BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE historial_estado (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id       BIGINT      NOT NULL REFERENCES ticket(id),
    usuario_id      BIGINT      REFERENCES usuario(id),  -- NULL = sistema
    estado_anterior VARCHAR(20),                          -- NULL al crear el ticket
    estado_nuevo    VARCHAR(20) NOT NULL,
    fecha           TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo          VARCHAR(500)
);

CREATE TABLE folio_secuencia (
    anio   INT PRIMARY KEY,
    ultimo INT NOT NULL DEFAULT 0
);