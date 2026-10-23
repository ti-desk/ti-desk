# ADR-002: Arquitectura en capas con DTO

- **Estado:** Aceptado
- **Fecha:** 21 de octubre de 2026
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

El equipo de seis integrantes desarrolla TI-Desk en 16 semanas, con módulos
repartidos entre personas distintas (autenticación, tickets, SLA,
inventario, indicadores). Se necesita una estructura que permita trabajar
en paralelo, que sea fácil de probar y que evite acoplamientos entre
módulos.

## Decisión

Se adopta una **arquitectura en capas** con separación estricta de
responsabilidades:

| Capa | Responsabilidad | Componentes Spring |
|------|-----------------|--------------------|
| Presentación | Recibir peticiones HTTP, validar entrada, mapear DTO, códigos de estado | `@RestController`, Bean Validation, `@RestControllerAdvice` |
| Seguridad (transversal) | Autenticar el JWT y autorizar por rol | `SecurityFilterChain`, filtro JWT, `@PreAuthorize` |
| Servicio (dominio) | Reglas de negocio, transacciones | `@Service`, `@Transactional` |
| Persistencia | Acceso a datos | Spring Data JPA, Flyway, PostgreSQL |
| Integración | Correo y tareas programadas | `JavaMailSender`, `@Scheduled` |
| Operación | Salud y métricas | Actuator, Micrometer, Prometheus |

**Reglas obligatorias**

1. Los controladores **nunca** acceden a repositorios; solo llaman a servicios.
2. Las entidades JPA **nunca salen de la capa de servicio**; hacia afuera
   se exponen únicamente DTO.
3. Los DTO de entrada se validan con Bean Validation en la capa de
   presentación.
4. Las transacciones se declaran en la capa de servicio.
5. Los errores se devuelven en formato ProblemDetail (RFC 9457), con
   código 409 para transiciones de estado inválidas y 404 cuando el
   recurso no existe o está oculto para el rol.
6. Los datos se exponen siempre como DTO; nunca como entidades.

**Estructura de paquetes:** se organiza por funcionalidad bajo
`mx.edu.tidesk` (`usuario/`, `ticket/`, `seguridad/`, `sla/`, `activo/`,
`notificacion/`, `indicador/`, `config/`, `comun/`), y dentro de cada una
se respeta la separación en capas.

**Verificación automática:** las reglas 1 y 2 se comprueban con pruebas
de arquitectura (ArchUnit) que se ejecutan en el pipeline de CI.

## Alternativas consideradas

1. **Controladores con acceso directo a repositorios y entidades expuestas
   en la API:** más rápido al inicio, pero filtra datos internos (por
   ejemplo, `password_hash`), acopla el contrato público al esquema de la
   base de datos y provoca problemas de carga perezosa (lazy loading).
2. **Arquitectura hexagonal (puertos y adaptadores):** más desacoplada,
   pero añade complejidad y código repetitivo que el equipo no puede
   absorber en 16 semanas.
3. **Capas con DTO (elegida):** equilibrio entre orden y simplicidad,
   conocida por todo el equipo.

## Consecuencias

**Positivas**
- Contrato de la API estable e independiente del modelo de datos.
- Los servicios se prueban de forma aislada con pruebas unitarias.
- Cada integrante puede trabajar en su módulo con menos conflictos.

**Negativas**
- Más clases (DTO y mapeos) por cada funcionalidad.
- Disciplina de revisión de código para que nadie rompa las reglas;
  se mitiga con ArchUnit y con la revisión obligatoria en los PR.