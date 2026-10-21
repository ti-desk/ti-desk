# Diagrama entidad-relación — TI-Desk

Responsable: Ulises. Se visualiza directo en GitHub (Mermaid). Detalle de columnas en `diccionario-de-datos.md`.

```mermaid
erDiagram
    USUARIO ||--o{ TICKET : "solicita"
    USUARIO |o--o{ TICKET : "atiende (técnico)"
    ACTIVO  |o--o{ TICKET : "se vincula (opcional)"
    USUARIO |o--o{ ACTIVO : "es responsable"
    TICKET  ||--o{ COMENTARIO : "tiene"
    USUARIO ||--o{ COMENTARIO : "escribe"
    TICKET  ||--o{ HISTORIAL_ESTADO : "registra"
    USUARIO |o--o{ HISTORIAL_ESTADO : "realiza (NULL = sistema)"
    POLITICA_SLA ||--o{ TICKET : "define plazos por prioridad"

    USUARIO {
        bigint id PK
        varchar nombre
        varchar email UK
        varchar password_hash
        varchar rol "USUARIO | TECNICO | ADMIN"
        varchar area
        boolean activo
        timestamptz creado_en
    }
    ACTIVO {
        bigint id PK
        varchar etiqueta_inventario UK
        varchar tipo
        varchar marca
        varchar modelo
        varchar numero_serie
        varchar ubicacion
        varchar estado "ACTIVO | BAJA"
        date fecha_adquisicion
        bigint responsable_id FK
    }
    TICKET {
        bigint id PK
        varchar folio UK "TID-AAAA-NNNNN"
        varchar titulo
        text descripcion
        varchar categoria
        varchar prioridad
        varchar estado
        bigint solicitante_id FK
        bigint tecnico_id FK
        bigint activo_id FK
        timestamptz creado_en
        timestamptz fecha_limite_sla
        boolean sla_vencido
        boolean sla_en_riesgo
        bigint version "bloqueo optimista"
    }
    COMENTARIO {
        bigint id PK
        bigint ticket_id FK
        bigint autor_id FK
        text texto
        boolean interno
        timestamptz creado_en
    }
    HISTORIAL_ESTADO {
        bigint id PK
        bigint ticket_id FK
        bigint usuario_id FK
        varchar estado_anterior
        varchar estado_nuevo
        timestamptz fecha
        varchar motivo
    }
    POLITICA_SLA {
        varchar prioridad PK
        numeric horas_primera_respuesta
        numeric horas_resolucion
    }
```

## Enumeraciones
- **Categoría:** RED, CORREO, EQUIPO, SOFTWARE, ACCESOS, OTRO
- **Prioridad:** CRITICA, ALTA, MEDIA, BAJA
- **Estado del ticket:** ABIERTO, ASIGNADO, EN_PROCESO, EN_ESPERA, RESUELTO, CERRADO, CANCELADO
- **Rol:** USUARIO, TECNICO, ADMIN
- **Tipo de activo:** LAPTOP, ESCRITORIO, IMPRESORA, SWITCH, ACCESS_POINT, PROYECTOR, OTRO

## Transiciones permitidas del ticket (se implementan en Sprint 2, RF-08)
- ABIERTO → ASIGNADO, CANCELADO
- ASIGNADO → EN_PROCESO, CANCELADO
- EN_PROCESO → EN_ESPERA, RESUELTO
- EN_ESPERA → EN_PROCESO
- RESUELTO → CERRADO, EN_PROCESO
- CERRADO y CANCELADO son estados finales
