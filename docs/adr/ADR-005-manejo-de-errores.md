# ADR-005: Manejo unificado de errores con ProblemDetail

- **Estado:** Aceptado
- **Fecha:** 16 de noviembre de 2026
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

El anteproyecto exige que todos los errores de la API se respondan en
formato `application/problem+json` (RFC 9457) y fija los códigos: 400 para
validación con detalle por campo, 401 sin token, 403 sin permiso, 404
inexistente y 409 para conflictos.

Durante el Sprint 1 y el Sprint 2 aparecieron tres problemas concretos:

1. Dos ramas crearon cada una su propio `@RestControllerAdvice` en el
   paquete `comun` (`ManejadorErrores` y `ManejadorGlobalErrores`), y los
   dos atendían los mismos errores 400 con textos distintos.
2. Los errores 401 y 403 no pasan por los controladores: los genera Spring
   Security antes de llegar a ellos, así que un `@RestControllerAdvice` no
   los puede atender.
3. Las pruebas de integración (PR #53) detectaron que, con un token válido,
   una ruta inexistente respondía 401 en lugar de 404, porque el reenvío
   interno a `/error` volvía a pasar por la cadena de seguridad sin
   autenticación.

## Decisión

1. **Una sola clase para los errores de la aplicación.** Todo error que
   nace en un controlador o en un servicio se traduce en
   `comun/ManejadorErrores`. No se crea un segundo `@RestControllerAdvice`:
   quien necesite un caso nuevo lo agrega a esa clase.
2. **Una sola clase para los errores de seguridad.** Los 401 y 403 los
   escribe `seguridad/ProblemDetailSeguridad`, registrada en
   `SecurityConfig` como punto de entrada de autenticación y como manejador
   de acceso denegado. Usa el mismo formato que el resto de la API.
3. **Excepciones de dominio propias.** Cada regla de negocio lanza una
   excepción con nombre (`CorreoDuplicadoException`,
   `CredencialesInvalidasException`, `UsuarioNoEncontradoException`,
   `CambioNoPermitidoException`). Los servicios no conocen códigos HTTP; el
   código lo decide el manejador.
4. **Mensajes fijos.** El texto que recibe el cliente lo escribe el
   manejador o viene de las reglas de validación. Nunca se devuelve el
   mensaje interno de una excepción técnica ni la traza.
5. **Validación con detalle por campo.** Los 400 incluyen la propiedad
   `errores`, un objeto `campo → mensaje`, con los mismos nombres de campo
   que el JSON de entrada.
6. **El login no distingue la causa.** Correo inexistente, contraseña
   incorrecta y cuenta desactivada responden el mismo 401, para no revelar
   qué correos están registrados.
7. **El despacho de error no se vuelve a autenticar.** `SecurityConfig`
   permite el tipo de despacho `ERROR`, para que un 404 o un 405 no se
   conviertan en 401.

## Alternativas consideradas

1. **Un manejador por módulo** (uno en tickets, otro en usuarios): cada
   dueño controla el suyo, pero los errores comunes (validación, JSON mal
   formado) quedan repetidos y con textos distintos, que fue justo lo que
   ocurrió. Descartada.
2. **Dejar el formato de error por defecto de Spring Boot:** no requiere
   código, pero no garantiza el detalle por campo ni mensajes controlados, y
   puede exponer información interna. Descartada.
3. **Lanzar `ResponseStatusException` desde los servicios:** menos clases,
   pero mete códigos HTTP en la capa de servicio y rompe la separación de
   capas del ADR-002. Descartada.
4. **Manejador único más excepciones de dominio (elegida):** un solo lugar
   para ver y revisar todo lo que la API puede responder cuando algo falla.

## Consecuencias

**Positivas**
- El cliente recibe siempre la misma estructura, sin importar el módulo.
- Revisar la seguridad de los mensajes de error es revisar dos clases.
- Los servicios quedan libres de detalles HTTP y se prueban sin contexto web.

**Negativas / riesgos**
- `ManejadorErrores` es un archivo que tocan varios integrantes, así que es
  propenso a conflictos de merge; se mitiga con ramas cortas y agregando
  cada caso al final de la clase.
- El paquete `comun` conoce las excepciones de los demás módulos. Se acepta
  porque la dependencia va en un solo sentido.
- Quien agregue una excepción de dominio y olvide registrarla obtendrá un
  500; se mitiga con una prueba de controlador por cada caso de error.