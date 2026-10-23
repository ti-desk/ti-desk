# ADR-003: Autenticación stateless con JWT, BCrypt y autorización por rol y propiedad

- **Estado:** Aceptado
- **Fecha:** 21 de octubre de 2026 (revisado el 22 de octubre de 2026)
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

TI-Desk expone una API REST con tres roles (USUARIO, TECNICO, ADMIN) y
requisitos de seguridad medibles: contraseñas con BCrypt, JWT firmado con
expiración de 30 minutos, autorización por rol en cada endpoint y ningún
secreto en el repositorio (RF-01, RF-02, RF-03 y requisitos no funcionales
de seguridad). Además de revisar el rol, el sistema debe impedir que un
usuario acceda a recursos de otra persona cambiando un identificador en la
URL (IDOR). El inicio de sesión con la cuenta institucional (OAuth2/SSO)
está fuera del alcance obligatorio.

## Decisión

1. **Autenticación stateless con JWT.** `POST /api/v1/auth/login` devuelve
   un token firmado que el cliente envía en `Authorization: Bearer <token>`.
2. **Expiración de 30 minutos.** No hay tokens de refresco en esta versión.
3. **Firma con HS256** y un secreto de al menos 256 bits tomado de la
   variable de entorno `JWT_SECRET`. Nunca se escribe en el repositorio.
4. **Contraseñas con BCrypt** (`BCryptPasswordEncoder`); solo se guarda
   el hash.
5. **El token no lleva el rol.** Contiene solo el identificador del usuario
   y la expiración. En cada petición, `JwtAuthFilter` valida firma y
   expiración, carga al usuario desde la base de datos y rechaza la petición
   si `activo = false`. El rol también se lee de la base de datos.
   Así, un usuario desactivado o con rol cambiado queda bloqueado o
   actualizado en su siguiente petición, sin esperar a que expire el token.
6. **Autorización por rol** con `@PreAuthorize` en cada endpoint y un
   `SecurityFilterChain` que deja públicos solo el registro, el login, la
   documentación de la API y el estado de salud.
7. **Control de propiedad (prevención de IDOR).** Revisar el rol no basta.
   La capa de servicio verifica que el recurso pertenezca al solicitante:
   USUARIO solo accede a sus propios tickets; TECNICO, a los tickets sin
   asignar y a los asignados a sí mismo; ADMIN, a todos. Un identificador
   de la URL nunca se usa sin esa verificación. Si el recurso existe pero
   no corresponde al rol, la API responde **404** (no 403), para no revelar
   que existe.
8. **Pruebas de seguridad:** cada endpoint tiene al menos un caso de acceso
   permitido y uno de acceso denegado, definidos junto con QA.
9. **Primer ADMIN:** al arrancar, si no existe ningún ADMIN, se crea uno con
   las credenciales de las variables de entorno `ADMIN_EMAIL` y
   `ADMIN_PASSWORD`. No hay valores por defecto en el repositorio.
10. **Rol por defecto:** USUARIO al registrarse. Solo un ADMIN cambia roles.
11. **Errores de seguridad:** 401 y 403 se devuelven en formato
    `application/problem+json` (ProblemDetail, RFC 9457).

## Alternativas consideradas

1. **Sesiones con cookie (stateful):** permiten invalidar al instante, pero
   requieren estado en el servidor y encajan peor con una API REST.
2. **OAuth2 / SSO institucional:** opción más completa, pero depende de la
   infraestructura de la universidad y está fuera del alcance obligatorio.
3. **JWT con el rol dentro del token:** más rápido, pero un cambio de rol
   o una desactivación no se reflejan hasta que expira. Descartada.
4. **JWT con lista negra de tokens revocados:** exige guardar estado,
   que es justo lo que se quiere evitar. Descartada.
5. **JWT stateless validando al usuario en base de datos (elegida):**
   equilibrio entre simplicidad y capacidad de bloqueo inmediato.

## Consecuencias

**Positivas**
- API sin sesiones en el servidor, fácil de probar y de escalar.
- Un usuario desactivado queda bloqueado en la siguiente petición.
- Los cambios de rol se aplican de inmediato.
- El control de propiedad evita el error de seguridad más común en APIs.

**Negativas / riesgos**
- Una consulta por petición para cargar al usuario; es una búsqueda por
  clave primaria. Si fuera un problema, se puede añadir un caché de corta
  duración.
- Un token robado sirve hasta que expire (30 minutos) mientras la cuenta
  siga activa; se mitiga con la expiración corta.
- Si el secreto se filtra, se pueden firmar tokens válidos; se mitiga con
  variables de entorno, GitHub Secrets y escaneo de secretos en el pipeline.
- Sin tokens de refresco, hay que iniciar sesión de nuevo cada 30 minutos.