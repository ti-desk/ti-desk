# TI-Desk · Escenarios de aceptación (Gherkin): registro, login y rutas protegidas

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Escenarios Gherkin del módulo de autenticación (Sprint 1) |
| Responsable | Brenda (QA) |
| Validación | Melissa (Product Owner): validado el 23/10/2026 |
| Revisión técnica | Sashi (módulo de seguridad) |
| Requisitos | RF-01, RF-02, RF-03 (parcial) y RNF de seguridad |
| Casos del plan de pruebas | CP-001 a CP-006, CP-008, CP-009, CP-041, CP-042, CP-044 |
| Versión / fecha | 0.2 · 23/10/2026 |
| Estado | Validado por el Product Owner; pendiente revisión técnica de Sashi |

Convenciones: las respuestas de error usan `application/problem+json` (RFC 9457). El token JWT expira a los 30 minutos y solo lleva el identificador del usuario y la expiración; el rol y el estado activo se leen de la base de datos en cada petición (ADR-003).

## Reglas de negocio de registro (definidas por el Product Owner)

| Regla | Definición |
| :--- | :--- |
| Contraseña válida | Mínimo 8 caracteres, con al menos una mayúscula, una minúscula, un número y un carácter especial (por ejemplo `#`, `$`, `@`, `!`) |
| Correo institucional | Formato de correo válido **y** dominio `@unistmo.edu.mx`; cualquier otro dominio se rechaza con 400 y ProblemDetail |
| Correo duplicado | 409 con ProblemDetail |
| Rol por defecto | USUARIO; el cliente no puede elegirlo |

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
    Cuando envío POST /api/v1/auth/registro con nombre "Ana López", correo "ana.lopez@unistmo.edu.mx" y contraseña "Segura#2026"
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
  Esquema del escenario: Registro con campos vacíos o formato de correo inválido
    Cuando envío POST /api/v1/auth/registro con nombre "<nombre>", correo "<correo>" y contraseña "<contraseña>"
    Entonces la respuesta tiene código 400
    Y el cuerpo es un ProblemDetail con el detalle del campo "<campo>"

    Ejemplos:
      | nombre     | correo                   | contraseña  | campo      |
      |            | ana.lopez@unistmo.edu.mx | Segura#2026 | nombre     |
      | Ana López  |                          | Segura#2026 | correo     |
      | Ana López  | correo-sin-arroba        | Segura#2026 | correo     |
      | Ana López  | ana.lopez@unistmo.edu.mx |             | contraseña |

  @CP-003 @rebanada
  Esquema del escenario: Registro con un dominio de correo no institucional
    Cuando envío POST /api/v1/auth/registro con correo "<correo>" y una contraseña válida
    Entonces la respuesta tiene código 400
    Y el cuerpo es un ProblemDetail con el detalle del campo "correo"
    Y no se crea ningún usuario

    Ejemplos:
      | correo                     |
      | ana.lopez@gmail.com        |
      | ana.lopez@unistmo.com      |
      | ana.lopez@unistmo.edu.mx.co |

  @CP-003 @rebanada
  Esquema del escenario: Registro con una contraseña que no cumple la política
    Cuando envío POST /api/v1/auth/registro con un correo institucional válido y contraseña "<contraseña>"
    Entonces la respuesta tiene código 400
    Y el cuerpo es un ProblemDetail con el detalle del campo "contraseña"
    Y no se crea ningún usuario

    Ejemplos:
      | contraseña     | motivo                 |
      | Corta#1        | menos de 8 caracteres  |
      | sinmayuscula#1 | sin mayúscula          |
      | SINMINUSCULA#1 | sin minúscula          |
      | SinNumero#abc  | sin número             |
      | SinSimbolo2026 | sin carácter especial  |

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
      | correo                   | contraseña     |
      | ana.lopez@unistmo.edu.mx | ContraseñaMala |
      | nadie@unistmo.edu.mx     | Segura#2026    |

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
      | situación              |
      | expirado               |
      | con la firma alterada  |
      | mal formado            |

  @CP-041 @seguridad
  Esquema del escenario: Endpoints protegidos sin token
    Cuando envío <método> <ruta> sin encabezado Authorization
    Entonces la respuesta tiene código 401
    Y el cuerpo tiene formato application/problem+json

    Ejemplos:
      | método | ruta                          |
      | GET    | /api/v1/usuarios/me           |
      | GET    | /api/v1/usuarios              |
      | POST   | /api/v1/tickets               |
      | GET    | /api/v1/tickets               |
      | GET    | /api/v1/activos               |
      | GET    | /api/v1/dashboard/indicadores |

  @CP-041 @seguridad
  Esquema del escenario: Endpoints públicos
    Cuando envío <método> <ruta> sin encabezado Authorization
    Entonces la respuesta no es 401

    Ejemplos:
      | método | ruta                  |
      | POST   | /api/v1/auth/registro |
      | POST   | /api/v1/auth/login    |
      | GET    | /actuator/health      |

  @CP-008 @CP-042 @seguridad
  Esquema del escenario: Acceso con un rol sin permiso
    Dado que inicié sesión como <rol>
    Cuando envío <método> <ruta> con mi token
    Entonces la respuesta tiene código 403
    Y el cuerpo tiene formato application/problem+json

    Ejemplos:
      | rol     | método | ruta                          |
      | USUARIO | GET    | /api/v1/usuarios              |
      | TECNICO | GET    | /api/v1/usuarios              |
      | USUARIO | PATCH  | /api/v1/usuarios/1            |
      | TECNICO | PATCH  | /api/v1/usuarios/1            |
      | USUARIO | GET    | /api/v1/dashboard/indicadores |
      | TECNICO | GET    | /api/v1/dashboard/indicadores |

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

## Decisiones pendientes

| # | Pendiente | Estado |
| :--- | :--- | :--- |
| 1 | Correo duplicado: ¿409 o 400? | **Resuelto:** 409, según el backlog v1.1; el ajuste de redacción de CP-002 va en el plan de pruebas v1.1 (TID-15) |
| 2 | Regla de contraseña válida | **Resuelto (Melissa, 23/10/2026):** mínimo 8 caracteres, con mayúscula, minúscula, número y carácter especial |
| 3 | Dominio del correo institucional | **Resuelto (Melissa, 23/10/2026):** se valida `@unistmo.edu.mx` además del formato; otro dominio responde 400 con ProblemDetail |
| 4 | ¿El filtro JWT responde 401 con token vigente de usuario desactivado? | **Abierto:** confirmar con Sashi (ADR-003, punto 5) |
| 5 | Las listas de rutas de CP-041 y CP-042 son las del contrato v1 | **Abierto:** completarlas conforme existan los controladores |

## Trazabilidad

| Escenario | Caso del plan | Requisito | Nivel de prueba |
| :--- | :--- | :--- | :--- |
| Registro válido | CP-001 | RF-01 | Integración |
| Correo duplicado | CP-002 | RF-01 | Integración |
| Campos vacíos, correo inválido, dominio no institucional y contraseña débil | CP-003 | RF-01 | Rebanada (`@WebMvcTest`) |
| Login correcto | CP-004 | RF-02 | Integración |
| Login incorrecto | CP-005 | RF-02 | Integración |
| Token inválido | CP-006 | RF-02 | Seguridad |
| Rol sin permiso | CP-008, CP-042 | RF-03 | Seguridad |
| Usuario desactivado | CP-009 | RF-03 | Integración |
| Rutas sin token | CP-041 | RNF seguridad | Seguridad |
| Logs sin secretos | CP-044 | RNF seguridad | Seguridad |

Nota para Melissa: las reglas de contraseña y de dominio no estaban en el SRS ni en el anteproyecto; conviene reflejarlas en el backlog (criterios de aceptación de la historia de registro) y en la matriz de trazabilidad.

## Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 23/10/2026 | Brenda | Borrador inicial para validación de Melissa |
| 0.2 | 23/10/2026 | Brenda | Se incorporan las reglas de contraseña y dominio definidas por Melissa; se agregan escenarios de dominio no institucional y contraseña débil; pendientes 1 a 3 resueltas |
