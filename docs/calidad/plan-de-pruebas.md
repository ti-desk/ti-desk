# TI-Desk · Plan de Pruebas v1

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Plan de pruebas v1 (ticket TID-05) |
| Responsable | Brenda, Ingeniera de calidad (QA) |
| Respaldo | Yahir |
| Hito | Release v0.5.0 (semana 8, parcial) |
| Versión / fecha | 1.0 · Octubre 2026 |
| Estado | Borrador para revisión del equipo |

---

## 1. Introducción

### 1.1 Propósito
Definir cómo se verificará la calidad de TI-Desk: qué se prueba, con qué herramientas, con qué criterios de aceptación y quién es responsable. Este documento es la base del reporte de calidad por sprint y del informe final de calidad.

### 1.2 Referencias
- Anteproyecto TI-Desk v1.0 (secciones 7, 8, 12, 13 y 15).
- ISO/IEC 25010 (modelo de calidad del producto).
- Pirámide de pruebas.
- Matriz de trazabilidad requisito → prueba (dueña: Melisa).

### 1.3 Definiciones
| Término | Significado |
| :--- | :--- |
| RF / RNF | Requisito funcional / no funcional |
| CP | Caso de prueba |
| UAT | Pruebas de aceptación de usuario |
| Quality gate | Condición que, si falla, rompe el build del pipeline |
| DoD | Definición de Terminado |

---

## 2. Alcance

### 2.1 Dentro del alcance (parcial, v0.5.0)
RF-01 a RF-08, RF-10, RF-11 y RF-13, más los RNF verificables en esta etapa: seguridad por rol, cobertura ≥ 60 %, arranque con `docker compose up`.

### 2.2 Dentro del alcance (final, v1.0.0)
Todos los requisitos M y S (incluye RF-09, RF-12, RF-14, RF-15 y RF-16), pruebas de carga, cobertura ≥ 80 % en la capa de servicio y checklist OWASP.

### 2.3 Fuera del alcance
Interfaz web, SSO/OAuth2, base de conocimiento, horario hábil y festivos, QR, correo institucional real y CSAT. Los requisitos deseables (RF-17 y RF-18) se prueban solo si el equipo los implementa.

---

## 3. Estrategia de pruebas

Se sigue la pirámide: muchas pruebas rápidas abajo y pocas costosas arriba.

| Nivel | Qué se prueba | Herramienta | Meta | Sprint |
| :--- | :--- | :--- | :--- | :--- |
| Unitarias | Máquina de estados, calculadora de SLA, mapeos, servicios | JUnit 5, Mockito, AssertJ | ≥ 70 % del total de pruebas; suite < 10 s | 1 en adelante |
| De rebanada | Controladores y repositorios aislados | `@WebMvcTest`, `@DataJpaTest` | Cada endpoint y cada consulta personalizada | 1 en adelante |
| Integración | Aplicación completa con PostgreSQL real | `@SpringBootTest` + Testcontainers | Flujos críticos de punta a punta | 2 a 3 |
| Seguridad | Acceso por rol a cada endpoint (positivo y negativo) | `spring-security-test` | 100 % de endpoints protegidos | 1 en adelante |
| Correo | Destinatario y contenido de notificaciones | GreenMail o Mailpit | Cada evento que notifica | 4 |
| Carga | Listado y creación bajo concurrencia | k6 | p95 < 500 ms con 10 000 tickets y 20 usuarios | 5 |
| Aceptación | Criterios Gherkin de las historias | Sesión UAT con usuarios reales | Acta firmada por sprint | Cada review |

### 3.1 Principios
1. Toda prueba tiene aserciones significativas. No se acepta una prueba solo para subir cobertura.
2. Las pruebas de SLA usan un `java.time.Clock` fijo; nunca dependen de la hora real.
3. Las pruebas de integración usan PostgreSQL real (Testcontainers), no H2.
4. Cada defecto corregido genera una prueba de regresión.
5. Cada historia incluye pruebas en el mismo PR que el código.

---

## 4. Entorno de pruebas

| Elemento | Detalle |
| :--- | :--- |
| Lenguaje / build | Java 21, Maven (`mvn -B verify`) |
| Base de datos local | PostgreSQL 16 (docker-compose) |
| Base de datos en pruebas de integración | PostgreSQL 16 vía Testcontainers |
| Correo | Mailpit (puertos 1025 SMTP y 8025 web) |
| CI | GitHub Actions (`ci.yml`) |
| Cobertura | JaCoCo 0.8.12 con umbral en el build |
| Contrato API | Swagger UI / OpenAPI + colección Postman o Bruno |
| Carga | k6 (Sprint 5) |

**Prerrequisito:** Docker Desktop con WSL2 activo en los equipos del equipo. Alternativa: GitHub Codespaces (riesgo del anteproyecto).

---

## 5. Quality gates del pipeline

| # | Gate | Umbral | Falla si... | Estado actual |
| :--- | :--- | :--- | :--- | :--- |
| QG-1 | Compilación y pruebas | 100 % en verde | Alguna falla | Activo |
| QG-2 | Cobertura de líneas (JaCoCo) | ≥ 60 % parcial; ≥ 80 % en servicios (final) | Por debajo del umbral | Activo al 60 % |
| QG-3 | Análisis estático | Sin bloqueantes ni críticos | Hay incidencias | Pendiente (SonarCloud o Checkstyle/SpotBugs) |
| QG-4 | Dependencias e imagen | Sin vulnerabilidades críticas/altas | Se detecta alguna | En curso (Dependabot, escaneo de secretos, Trivy) |
| QG-5 | Revisión de código | ≥ 1 aprobación de otro integrante | Sin aprobación | Activo (rama protegida) |

---

## 6. Criterios de entrada y salida

### 6.1 Entrada 
- El código está en un PR abierto contra `main`.
- La historia tiene criterios de aceptación Gherkin.
- El entorno local levanta con `docker compose up -d`.

### 6.2 Salida / Definición de Terminado (QA)
- Todos los CP de la historia están automatizados y en verde.
- No hay defectos abiertos de severidad Crítica o Alta ligados a la historia.
- La cobertura global no baja del umbral.
- El PR tiene el pipeline en verde y una aprobación.
- Melisa aceptó la historia (UAT) en la review.

### 6.3 Criterio de aceptación del release v0.5.0
- 100 % de los CP de prioridad M ejecutados y aprobados.
- Cobertura ≥ 60 %.
- Cero defectos críticos abiertos.
- `docker compose up` levanta app, BD y Mailpit en una máquina limpia.

---

## 7. Casos de prueba iniciales (alcance del parcial)

Formato: **Dado / Cuando / Entonces**. Tipo: U = unitaria, S = de rebanada, I = integración, SEG = seguridad.

### 7.1 Autenticación y usuarios (módulo de Sashi)

| ID | RF | Caso | Resultado esperado | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| CP-001 | RF-01 | Registro con datos válidos | 201; usuario con rol USUARIO; contraseña guardada con BCrypt | I |
| CP-002 | RF-01 | Registro con correo ya existente | 409 o 400 con ProblemDetail | I |
| CP-003 | RF-01 | Registro con correo inválido o campos vacíos | 400 con detalle por campo | S |
| CP-004 | RF-02 | Login con credenciales correctas | 200 con JWT que expira a los 30 min | I |
| CP-005 | RF-02 | Login con contraseña incorrecta | 401 | I |
| CP-006 | RF-02 | Petición con token expirado o alterado | 401 | SEG |
| CP-007 | RF-03 | ADMIN cambia el rol de un usuario | 200; el nuevo rol aplica en el siguiente login | I |
| CP-008 | RF-03 | USUARIO o TECNICO intenta administrar usuarios | 403 | SEG |
| CP-009 | RF-03 | ADMIN desactiva un usuario | No puede iniciar sesión | I |

### 7.2 Tickets (módulo de Ulises)

| ID | RF | Caso | Resultado esperado | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| CP-010 | RF-04 | Crear ticket válido | 201, encabezado Location, folio con formato `TID-AAAA-NNNNN` | I |
| CP-011 | RF-04 | Crear ticket sin título o con prioridad inválida | 400 con detalle por campo | S |
| CP-012 | RF-04 | Folios consecutivos y únicos | Sin duplicados, incluso con concurrencia | I |
| CP-013 | RF-05 | USUARIO lista tickets | Solo ve los propios | SEG |
| CP-014 | RF-05 | TECNICO lista tickets | Ve los sin asignar y los asignados a él | SEG |
| CP-015 | RF-05 | ADMIN lista con filtros (estado, prioridad, técnico, vencido) y paginación | Resultados correctos y paginados | S |
| CP-016 | RF-05 | USUARIO pide el detalle de un ticket ajeno | 404 (oculto para el rol) | SEG |
| CP-017 | RF-06 | ADMIN asigna ticket a un técnico | Estado ASIGNADO; se registra en historial | I |
| CP-018 | RF-06 | TECNICO se autoasigna | Estado ASIGNADO | I |
| CP-019 | RF-06 | Dos técnicos asignan el mismo ticket a la vez | Uno gana; el otro recibe 409 (`@Version`) | I |
| CP-020 | RF-07 | Comentario público | Lo ven todos los que tienen acceso al ticket | I |
| CP-021 | RF-07 | Comentario interno | No lo ve el USUARIO | SEG |
| CP-022 | RF-08 | Cada transición permitida de la máquina de estados | Cambia el estado y deja registro en historial | U |
| CP-023 | RF-08 | Transición no permitida (por ejemplo ABIERTO → RESUELTO) | 409 con ProblemDetail que indica la regla | U, I |
| CP-024 | RF-08 | Estados finales (CERRADO, CANCELADO) | No admiten ninguna transición | U |
| CP-025 | RF-08 | Transición por un rol no autorizado | 403 | SEG |

### 7.3 SLA (módulo de Fernando)

| ID | RF | Caso | Resultado esperado | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| CP-026 | RF-10 | Fecha límite por cada prioridad (CRÍTICA 4 h, ALTA 24 h, MEDIA 48 h, BAJA 72 h) | `fechaLimite = creadoEn + horasResolucion` con Clock fijo | U |
| CP-027 | RF-10 | Cambio de prioridad | La fecha límite se recalcula | U |
| CP-028 | RF-10 | Cambiar un valor en `politica_sla` | El cálculo usa la tabla, no valores fijos en el código | I |
| CP-029 | RF-11 | Ticket que supera su fecha límite en estado activo | Se marca como vencido | U |
| CP-030 | RF-11 | Ticket RESUELTO, CERRADO o CANCELADO pasado de fecha | No se marca como vencido | U |
| CP-031 | RF-11 | Ticket al 80 % del tiempo | Se marca "en riesgo" | U |
| CP-032 | RF-11 | Ejecución del job `@Scheduled` (cada 5 min) | Procesa los tickets pendientes y es idempotente | I |
| CP-033 | RF-10 | Fechas en UTC y presentación en America/Mexico_City | Sin desfase de horas | U |

### 7.4 Inventario (módulo de Melisa)

| ID | RF | Caso | Resultado esperado | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| CP-034 | RF-13 | ADMIN da de alta un activo | 201 | I |
| CP-035 | RF-13 | Etiqueta de inventario duplicada | 409 o 400 | I |
| CP-036 | RF-13 | Edición de un activo | 200 | I |
| CP-037 | RF-13 | Baja lógica | El activo queda inactivo y no se borra de la BD | I |
| CP-038 | RF-13 | Búsqueda por etiqueta o número de serie | Devuelve el activo correcto | S |
| CP-039 | RF-13 | TECNICO consulta y trata de crear | Consulta 200; creación 403 | SEG |
| CP-040 | RF-04 | Ticket ligado a un activo opcional | Se asocia correctamente | I |

### 7.5 Pruebas transversales de seguridad

| ID | Caso | Resultado esperado |
| :--- | :--- | :--- |
| CP-041 | Cada endpoint de `/api/v1` sin token | 401 (excepto registro y login) |
| CP-042 | Cada endpoint con un rol sin permiso | 403 |
| CP-043 | Respuestas de error | Formato `application/problem+json` |
| CP-044 | Logs de login y registro | Sin contraseñas ni tokens |
| CP-045 | Repositorio | Sin secretos (escaneo en CI) |

### 7.6 Pruebas de humo y arranque

| ID | Caso | Resultado esperado |
| :--- | :--- | :--- |
| CP-046 | `docker compose up` en máquina limpia | App, BD y Mailpit levantan |
| CP-047 | `GET /actuator/health` | `UP` |
| CP-048 | Migraciones Flyway en BD vacía | Se aplican V1..Vn sin errores y Hibernate (`ddl-auto: validate`) no reporta diferencias |

---

## 8. Gestión de defectos

### 8.1 Registro
Cada defecto es un Issue de GitHub con la etiqueta `bug`, con estos campos: título, módulo, pasos para reproducir, resultado esperado y obtenido, severidad, evidencia (log o captura) y la historia o RF relacionada.

### 8.2 Severidad
| Nivel | Criterio | Ejemplo | Plazo de atención |
| :--- | :--- | :--- | :--- |
| Crítica | Bloquea el uso o expone datos | Un USUARIO ve tickets ajenos; la app no arranca | Mismo día |
| Alta | Falla una función principal sin alternativa | Transición inválida permitida | Antes del fin del sprint |
| Media | Falla parcial con alternativa | Filtro de paginación incorrecto | Siguiente sprint |
| Baja | Cosmético o menor | Mensaje de error poco claro | Cuando haya capacidad |

### 8.3 Flujo
`Nuevo → Asignado → En corrección → En verificación (QA) → Cerrado` (o `Reabierto`).
Un defecto solo lo cierra QA después de verificar la corrección y confirmar que existe una prueba de regresión.

---

## 9. Reporte de calidad por sprint

Al cierre de cada sprint, Brenda entrega un reporte breve con:

| Métrica | Fuente |
| :--- | :--- |
| Pruebas totales / pasan / fallan, y proporción de unitarias | Reporte de Surefire |
| Cobertura global y por paquete | JaCoCo |
| Defectos abiertos y cerrados por severidad | Issues de GitHub |
| Estado de los quality gates | GitHub Actions |
| Requisitos con al menos una prueba automatizada | Matriz de trazabilidad |
| Acciones de mejora | Retrospectiva |

---

## 10. Cronograma de pruebas

| Sprint | Semanas | Actividades de QA |
| :--- | :--- | :--- |
| 0 | 1–2 | Diagnóstico técnico, este plan v1, Docker/Testcontainers verificados en los equipos, quality gate JaCoCo |
| 1 | 3–4 | Pruebas de registro, login, roles y alta/consulta de tickets (unitarias, de rebanada y de seguridad) |
| 2 | 5–6 | Pruebas de asignación, comentarios, máquina de estados e inventario |
| 3 | 7–8 | Pruebas de SLA e integración con Testcontainers; regresión completa; reporte de calidad; **Release v0.5.0** |
| 4 | 9–10 | Pruebas de correo (Mailpit/GreenMail) e indicadores |
| 5 | 11–12 | Prueba de carga con k6; pruebas de métricas y despliegue |
| 6 | 13–14 | Cobertura ≥ 80 % en servicios, checklist OWASP, UAT |
| Cierre | 15–16 | Regresión final, informe final de calidad, **Release v1.0.0** |

---

## 11. Riesgos de calidad

| Riesgo | Prob. | Impacto | Mitigación |
| :--- | :--- | :--- | :--- |
| Cobertura inflada con pruebas sin aserciones | Media | Medio | Revisar las pruebas en cada PR; muestreo semanal por QA |
| Pruebas de SLA inestables por la hora real | Alta | Medio | `Clock` inyectado y tiempo fijo en pruebas |
| Testcontainers no corre en algún equipo | Media | Medio | Docker Desktop/WSL2 en la semana 1; Codespaces como alternativa |
| Pruebas lentas que desalientan su ejecución | Media | Medio | Mantener las unitarias < 10 s; separar las de integración |
| Cambios de último minuto sin pruebas | Media | Alto | Regla de DoD: sin pruebas, no hay merge |

---

## 12. Roles en las pruebas

| Persona | Responsabilidad de calidad |
| :--- | :--- |
| Brenda (QA) | Dueña del plan, umbrales, suite de integración/API, k6 y reportes |
| Ulises, Sashi, Fernando, Melisa | Escriben pruebas unitarias y de rebanada de su propio módulo |
| Yahir | Mantiene el pipeline y los quality gates en CI |
| Melisa | Aceptación (UAT) y trazabilidad requisito → prueba |
| Sashi | Revisión de pruebas de seguridad |

---

## 13. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 1.0 | Oct 2026 | Brenda | Versión inicial |
