# ADR-003: Autenticación stateless con JWT, BCrypt y autorización por rol

- **Estado:** Aceptado
- **Fecha:** 21 de octubre de 2026
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

TI-Desk expone una API REST con tres roles (USUARIO, TECNICO, ADMIN) y
requisitos de seguridad medibles: contraseñas con BCrypt, JWT firmado con
expiración de 30 minutos, autorización por rol en cada endpoint y ningún
secreto en el repositorio (RF-01, RF-02 y requisitos no funcionales de
seguridad). El inicio de sesión con la cuenta institucional (OAuth2/SSO)
está fuera del alcance obligatorio.

## Decisión

1. **Autenticación stateless con JWT.** `POST /api/v1/auth/login` devuelve
   un token firmado que el cliente envía en el encabezado
   `Authorization: Bearer <token>`.
2. **Expiración de 30 minutos.** No se implementan tokens de refresco en
   esta versión.
3. **Firma con HS256** y un secreto de al menos 256 bits tomado de la
   variable de entorno `JWT_SECRET`. El secreto nunca se escribe en el
   repositorio.
4. **Contraseñas con BCrypt** (`BCryptPasswordEncoder`); solo se guarda
   el hash.
5. **Autorización por rol** con `@PreAuthorize` en cada endpoint, más un
   `SecurityFilterChain` que deja públicos únicamente el registro, el
   login, la documentación de la API y el estado de salud.
6. **Componentes:** `JwtService` (generar y validar), `JwtAuthFilter`
   (leer el token en cada petición) y `SecurityConfig`.
7. **Rol por defecto:** USUARIO al registrarse. Solo un ADMIN puede
   cambiar roles.

## Alternativas consideradas

1. **Sesiones con cookie (stateful):** permiten invalidar la sesión al
   instante, pero requieren estado en el servidor y encajan peor con una
   API REST consumida por distintos clientes.
2. **OAuth2 / SSO institucional:** es la opción más completa, pero depende
   de la infraestructura de la universidad y el anteproyecto lo ubica
   fuera del alcance obligatorio.
3. **JWT stateless (elegida):** cumple los requisitos con la menor
   dependencia externa.

## Consecuencias

**Positivas**
- API sin estado en el servidor, fácil de escalar y de probar.
- Cumple los requisitos de seguridad del anteproyecto.

**Negativas / riesgos**
- Un token emitido no se puede revocar antes de expirar; se mitiga con
  la expiración corta de 30 minutos.
- Si el secreto se filtra, cualquiera puede firmar tokens válidos; se
  mitiga con variables de entorno, GitHub Secrets y escaneo de secretos
  en el pipeline.
- Sin tokens de refresco, el usuario debe iniciar sesión de nuevo cada
  30 minutos.