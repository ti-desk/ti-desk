# TI-Desk · Escenarios de aceptación (Gherkin): registro, login y rutas protegidas

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Escenarios Gherkin del módulo de autenticación (Sprint 1) |
| Responsable | Brenda (QA) |
| Validación | Melissa (Product Owner) |
| Revisión técnica | Sashi (módulo de seguridad) |
| Requisitos | RF-01, RF-02, RF-03 (parcial) y RNF de seguridad |
| Casos del plan de pruebas | CP-001 a CP-006, CP-008, CP-009, CP-041, CP-042 |
| Versión / fecha | 0.1 · 23/10/2026 |
| Estado | Borrador para validación de Melissa |

Convenciones: las respuestas de error usan `application/problem+json` (RFC 9457). El token JWT expira a los 30 minutos y solo lleva el identificador del usuario y la expiración; el rol y el estado activo se leen de la base de datos en cada petición (ADR-003).

---

## Característica 1: Registro de usuarios (RF-01)

```gherkin
# language: es
Característica: Registro de usuarios
  Como persona de la comunidad universitaria
  Quiero registrarme con mi correo institucional
  Para poder crear solicitudes de soporte

  Antecedentes:
    Dado que el servicio de TI-Desk está disponible

  @CP-001 @integracion
  Escenario: Registro con datos válidos
    Cuando envío POST /api/v1/auth/registro con nombre "Ana López", correo "ana.lopez@unistmo.edu.mx" y una contraseña válida
    Entonces la respuesta tiene código 201
    Y el usuario queda registrado con rol "USUARIO"
    Y el usuario queda activo
    Y la contraseña se guarda como hash BCrypt
    Y la respuesta no incluye la contraseña ni el hash

  @CP-002 @integracion
  Escenario: Registro con un correo que ya existe
    Dado que ya existe un usuario con correo "ana.lopez@unistmo.edu.mx"
    Cuando envío POST /api/v1/auth/registro con ese mismo correo
    Entonces la respuesta tiene código 409
    Y el cuerpo es un ProblemDetail que indica que el correo ya está registrado
    Y no se crea un segundo usuario

  @CP-003 @rebanada
  Esquema del escenario: Registro con datos inválidos
    Cuando envío POST /api/v1/auth/registro con nombre "<nombre>", correo "<correo>" y contraseña "<contraseña>"
    Entonces la respuesta tiene código 400
    Y el cuerpo es un ProblemDetail con el detalle del campo "<campo>"

    Ejemplos:
      | nombre   | correo                    | contraseña   | campo      |
      |          | ana.lopez@unistmo.edu.mx  | Segura#2026  | nombre     |
      | Ana López|                           | Segura#2026  | correo     |
      | Ana López| correo-sin-arroba         | Segura#2026  | correo     |
      | Ana López| ana.lopez@unistmo.edu.mx  |              | contraseña |

  @seguridad @integracion
  Escenario: El registro no permite elegir el rol
    Cuando envío POST /api/v1/auth/registro incluyendo el campo rol con valor "ADMIN"
    Entonces el usuario queda registrado con rol "USUARIO"
```

---

## Característica 2: Inicio de sesión (RF-02)

```gherkin
# language: es
Característica: Inicio de sesión
  Como usuario registrado
  Quiero iniciar sesión con mi correo y contraseña
  Para obtener un token que me permita usar la API

  Antecedentes:
    Dado que existe un usuario activo con correo "ana.lopez@unistmo.edu.mx" y contraseña "Segura#2026"

  @CP-004 @integracion
  Escenario: Login con credenciales correctas
    Cuando envío POST /api/v1/auth/login con ese correo y esa contraseña
    Entonces la respuesta tiene código 200
    Y la respuesta contiene un token JWT firmado
    Y el token expira 30 minutos después de emitirse
    Y el token no contiene el rol del usuario

  @CP-005 @integracion
  Esquema del escenario: Login con credenciales incorrectas
    Cuando envío POST /api/v1/auth/login con correo "<correo>" y contraseña "<contraseña>"
    Entonces la respuesta tiene código 401
    Y el cuerpo es un ProblemDetail
    Y el mensaje no revela si el correo existe

    Ejemplos:
      | correo                    | contraseña      |
      | ana.lopez@unistmo.edu.mx  | ContraseñaMala  |
      | nadie@unistmo.edu.mx      | Segura#2026     |

  @CP-009 @integracion
  Escenario: Un usuario desactivado no puede iniciar sesión
    Dado que un ADMIN desactivó la cuenta de "ana.lopez@unistmo.edu.mx"
    Cuando envío POST /api/v1/auth/login con sus credenciales correctas
    Entonces la respuesta tiene código 401

  @CP-044 @seguridad
  Escenario: El login y el registro no escriben secretos en los logs
    Cuando se ejecuta un registro y un login
    Entonces los logs de la aplicación no contienen la contraseña ni el token
```

---

## Característica 3: Rutas protegidas y token (RF-02, RNF de seguridad)

```gherkin
# language: es
Característica: Protección de rutas
  Como responsable de la seguridad del sistema
  Quiero que solo usuarios autenticados y autorizados usen la API
  Para proteger la información de los tickets

  @CP-006 @seguridad
  Esquema del escenario: Petición con un token no válido
    Cuando envío GET /api/v1/usuarios/me con un token <situación>
    Entonces la respuesta tiene código 401
    Y el cuerpo es un ProblemDetail

    Ejemplos:
      | situación                              |
      | expirado                               |
      | con la firma alterada                  |
      | mal formado                            |

  @CP-041 @seguridad
  Esquema del escenario: Endpoints protegidos sin token
    Cuando envío <método> <ruta> sin encabezado Authorization
    Entonces la respuesta tiene código 401
    Y el cuerpo tiene formato application/problem+json

    Ejemplos:
      | método | ruta                      |
      | GET    | /api/v1/usuarios/me       |
      | GET    | /api/v1/usuarios          |
      | POST   | /api/v1/tickets           |
      | GET    | /api/v1/tickets           |
      | GET    | /api/v1/activos           |
      | GET    | /api/v1/dashboard/indicadores |

  @CP-041 @seguridad
  Esquema del escenario: Endpoints públicos
    Cuando envío <método> <ruta> sin encabezado Authorization
    Entonces la respuesta no es 401

    Ejemplos:
      | método | ruta                    |
      | POST   | /api/v1/auth/registro   |
      | POST   | /api/v1/auth/login      |
      | GET    | /actuator/health        |

  @CP-008 @CP-042 @seguridad
  Esquema del escenario: Acceso con un rol sin permiso
    Dado que inicié sesión como <rol>
    Cuando envío <método> <ruta> con mi token
    Entonces la respuesta tiene código 403
    Y el cuerpo tiene formato application/problem+json

    Ejemplos:
      | rol      | método | ruta                              |
      | USUARIO  | GET    | /api/v1/usuarios                  |
      | TECNICO  | GET    | /api/v1/usuarios                  |
      | USUARIO  | PATCH  | /api/v1/usuarios/1                |
      | TECNICO  | PATCH  | /api/v1/usuarios/1                |
      | USUARIO  | GET    | /api/v1/dashboard/indicadores     |
      | TECNICO  | GET    | /api/v1/dashboard/indicadores     |

  @CP-042 @seguridad
  Escenario: Acceso permitido con el rol correcto
    Dado que inicié sesión como ADMIN
    Cuando envío GET /api/v1/usuarios con mi token
    Entonces la respuesta tiene código 200

  @seguridad @integracion
  Escenario: Un cambio de rol se aplica en la siguiente petición
    Dado que inicié sesión como USUARIO y conservo mi token
    Y un ADMIN me cambia el rol a TECNICO
    Cuando envío una petición que solo permite TECNICO con el mismo token
    Entonces la respuesta tiene código 200

  @seguridad @integracion
  Escenario: Un usuario desactivado queda bloqueado con su token vigente
    Dado que inicié sesión y conservo mi token vigente
    Y un ADMIN desactiva mi cuenta
    Cuando envío GET /api/v1/usuarios/me con el mismo token
    Entonces la respuesta tiene código 401
```

---

## Decisiones pendientes (resolver con Melissa y Sashi antes de automatizar)

| # | Pendiente | Propuesta |
| :--- | :--- | :--- |
| 1 | ~~CP-002 dice "409 o 400" para correo duplicado~~ | **Resuelto:** 409, según el backlog v1.1 de Melissa; el ajuste de redacción va en el plan de pruebas v1.1 (TID-15) |
| 2 | Regla de contraseña válida (longitud mínima, complejidad) no está definida en el anteproyecto | Que Melissa la defina como criterio de aceptación; los ejemplos de arriba usan "Segura#2026" como valor válido |
| 3 | Dominio del correo institucional | Confirmar si se valida (por ejemplo `@unistmo.edu.mx`) o solo el formato |
| 4 | Código al desactivar: ¿401 en login y 401 con token vigente? | Confirmar con Sashi que el filtro JWT responde 401 (ADR-003, punto 5) |
| 5 | Las listas de rutas en CP-041/CP-042 son las del contrato v1 | Completarlas con las rutas reales conforme existan los controladores |

## Trazabilidad

| Escenario | Caso del plan | Requisito | Nivel de prueba |
| :--- | :--- | :--- | :--- |
| Registro válido | CP-001 | RF-01 | Integración |
| Correo duplicado | CP-002 | RF-01 | Integración |
| Datos inválidos | CP-003 | RF-01 | Rebanada (`@WebMvcTest`) |
| Login correcto | CP-004 | RF-02 | Integración |
| Login incorrecto | CP-005 | RF-02 | Integración |
| Token inválido | CP-006 | RF-02 | Seguridad |
| Rol sin permiso | CP-008, CP-042 | RF-03 | Seguridad |
| Usuario desactivado | CP-009 | RF-03 | Integración |
| Rutas sin token | CP-041 | RNF seguridad | Seguridad |
| Logs sin secretos | CP-044 | RNF seguridad | Seguridad |

## Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 23/10/2026 | Brenda | Borrador inicial para validación de Melissa |
