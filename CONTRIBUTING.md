# Guía de contribución de TI-Desk

Esta guía resume cómo trabajamos en el repositorio. Las decisiones de fondo están en los ADR (`docs/adr`) y la explicación completa de la arquitectura en `docs/arquitectura.md`.

## 1. Flujo de trabajo con Git

- Trabajamos con desarrollo basado en tronco (trunk-based): ramas cortas (máximo 3 días) que salen de `main` y vuelven por Pull Request.
- `main` está protegida: no se permite push directo. Todo cambio entra por Pull Request con al menos una aprobación y el pipeline en verde.
- **Nadie aprueba ni integra su propio Pull Request.**

### Nombres de ramas

```
feature/TID-XX-descripcion-corta
fix/TID-XX-descripcion-corta
```

### Mensajes de commit (Conventional Commits)

| Prefijo | Uso |
|---------|-----|
| `feat(TID-XX):` | Funcionalidad nueva |
| `fix(TID-XX):` | Corrección de errores |
| `test(TID-XX):` | Pruebas unitarias o de integración |
| `ci(TID-XX):` | GitHub Actions, Docker o JaCoCo |
| `docs(TID-XX):` | Documentación |

`TID-XX` es el número de la historia en el tablero de GitHub Projects.

### Revisores obligatorios por dominio

| Dominio del cambio | Revisor obligatorio |
|--------------------|---------------------|
| Pipeline, CI/CD y Docker | Yahir |
| Seguridad y JWT | Sashi |
| Base de datos y Flyway | Ulises |
| Negocio y servicios | Brenda (QA) |

## 2. Arquitectura en capas

Capas: presentación, seguridad, servicio, persistencia, integración y operación (ver ADR-002).

**Reglas obligatorias**

1. Ninguna capa se salta a la de abajo: controlador → servicio → repositorio.
2. Los controladores **nunca** acceden a repositorios.
3. Las entidades JPA **nunca salen de la capa de servicio**. Hacia afuera se exponen solo DTO.
4. La lógica de negocio va en los servicios, nunca en los controladores.
5. Las transacciones (`@Transactional`) se declaran en la capa de servicio.
6. Los DTO de entrada se validan con Bean Validation en la capa de presentación.

Un Pull Request que ponga reglas de negocio en un controlador o devuelva una entidad JPA se detiene en la revisión.

## 3. Estructura de paquetes

Organización por funcionalidad bajo `mx.edu.tidesk`:

```
config/  seguridad/  usuario/  ticket/  comentario/
activo/  sla/  notificacion/  indicador/  comun/
```

Dentro de cada funcionalidad se respetan las capas. Los DTO van en el subpaquete `dto`.

## 4. Convenciones de código

- Clases con sufijo `Controller`, `Service` y `Repository` según su capa.
- Nombres de clases, métodos y variables claros y consistentes con el dominio (el proyecto usa términos en español: `ticket`, `activo`, `usuario`).
- Las fechas se almacenan en UTC (`Instant`) y se presentan en la zona America/Mexico_City.
- Se inyecta `java.time.Clock` en lugar de llamar a `Instant.now()`, para poder probar con tiempo fijo.

### Respuestas de la API

- Rutas versionadas: `/api/v1/...`; formato JSON.
- Errores en formato `application/problem+json` (ProblemDetail, RFC 9457).

| Código | Cuándo |
|--------|--------|
| 201 con `Location` | Recurso creado |
| 400 | Validación fallida, con detalle por campo |
| 401 | Sin token o token inválido |
| 403 | Sin permiso por rol |
| 404 | El recurso no existe o está oculto para el rol |
| 409 | Transición de estado inválida o conflicto de versión |

## 5. Seguridad

- **Nunca** se suben contraseñas, claves ni credenciales al repositorio.
- Los secretos (clave del JWT, contraseñas, credenciales de base de datos) viven en variables de entorno o en GitHub Secrets.
- Todo endpoint debe tener autorización explícita por rol con `@PreAuthorize`.
- No basta con revisar el rol: los servicios verifican que el recurso pertenezca a quien lo pide (prevención de IDOR, ver ADR-003).
- Cada endpoint se prueba con un caso de acceso permitido y uno de acceso denegado.

## 6. Base de datos

- Todo cambio de esquema es una migración nueva de Flyway (`V<n>__descripcion.sql`).
- **Nunca** se edita una migración que ya está en `main`.
- Los cambios de esquema los revisa Ulises.

## 7. Pruebas y calidad

- Pruebas con JUnit 5, Mockito y AssertJ; pruebas de integración con Testcontainers.
- El pipeline falla si la cobertura de líneas (JaCoCo) baja del 60 %.
- Antes de abrir un Pull Request, ejecutar en local:

```
.\mvnw.cmd clean verify
```

(En Linux o macOS: `./mvnw clean verify`.) Para que las pruebas funcionen, la base de datos debe estar arriba con `docker compose up -d`.

## 8. Antes de abrir un Pull Request

- La rama sale de `main` y está actualizada.
- Los commits siguen Conventional Commits con el ID de la historia.
- El build local termina en `BUILD SUCCESS`.
- Hay pruebas para el código nuevo.
- Si cambió el esquema, existe la migración de Flyway.
- Si cambió un endpoint, el contrato OpenAPI está actualizado.
- Se asignó al revisor obligatorio del dominio.

## 9. Definición de Terminado

- Cumple los criterios de aceptación de la historia y está integrado en `main`.
- Tiene pruebas automatizadas y el pipeline está en verde.
- El contrato OpenAPI está actualizado y el endpoint se probó en Swagger UI.
- Existe migración de Flyway si cambió el esquema.
- La Product Owner (Melisa) aceptó la historia en la review.