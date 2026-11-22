# TI-Desk · Reporte de calidad del Sprint 1

- **Documento:** Reporte de calidad del Sprint 1 (semanas 3–4)
- **Responsable:** Brenda Nashelly Gómez Reyes, QA
- **Respaldo:** Yahir Antonio Bautista Ramirez, DevOps / SRE
- **Referencia:** Plan de pruebas v1.1, secciones 5, 8 y 9
- **Corte de datos:** cierre del Sprint 1 (28/10/2026), rama `main` en el commit `7a8a2ad` (PR #40)
- **Fecha de elaboración:** 16/11/2026

---

## 1. Resumen ejecutivo

- La base de pruebas de integración con Testcontainers y PostgreSQL 16 está en `main` (TID-17, PR #31). Las pruebas ya no necesitan `docker compose up`, y H-02 quedó resuelto.
- La suite pasó 16 de 16 pruebas (12 unitarias y 4 de integración) en `BUILD SUCCESS`.
- El gate de cobertura (QG-2) está activo y se comprobó que rompe el build por debajo del 60 %. El valor actual (100 % de 7 líneas) **no es representativo**: el gate solo mide los enumerados del dominio, porque las 6 entidades JPA quedaron excluidas en el PR #32 (ver sección 5).
- Los escenarios Gherkin de autenticación (RF-01, RF-02 y seguridad de rutas) están validados por la PO y listos para automatizarse (TID-18, PR #35).
- **Ninguna prueba de seguridad, `@DataJpaTest` o `@WebMvcTest` se automatizó** en el sprint: registro, login y JWT siguen fuera de `main`, y no existen controladores ni consultas personalizadas de tickets.
- QG-3 (análisis estático) sigue sin integrarse al build: la herramienta está decidida (ADR-004), pero falta configurarla en `pom.xml` y en CI.
- 0 defectos `bug` abiertos; de los 6 hallazgos de entorno hay 3 resueltos, 1 parcial, 1 en monitoreo y 1 en verificación.

---

## 2. Objetivos de QA del sprint

| Objetivo (documento de tareas, Sprint 1) | Estado | Evidencia |
| :--- | :--- | :--- |
| Configurar JaCoCo con umbral en el build, que falle si no se cumple | Hecho | Regla `check-coverage` en `pom.xml`; prueba de fallo del 23/10 (sección 3) |
| Base de pruebas de integración con `@SpringBootTest` y Testcontainers | Hecho | TID-17, PR #31 |
| Pruebas de seguridad de registro, login y rutas protegidas | Bloqueado | Escenarios listos (PR #35); el código de seguridad no está en `main` |
| Pruebas `@DataJpaTest` de consultas de tickets y `@WebMvcTest` de controladores | Bloqueado | `TicketRepository` sin consultas personalizadas; no hay controladores |
| Abrir y dar seguimiento a defectos | Hecho, sin defectos | 0 issues con etiqueta `bug`; hallazgos de entorno en la sección 6 |
| Entregar el reporte de calidad del sprint | Hecho | Este documento |

---

## 3. Quality gates

| # | Gate | Umbral | Estado al cierre del Sprint 1 |
| :--- | :--- | :--- | :--- |
| QG-1 | Compilación y pruebas | 100 % en verde | Activo en CI (`mvn -B verify`) |
| QG-2 | Cobertura de líneas (JaCoCo) | ≥ 60 % parcial; ≥ 80 % en servicios (final) | Activo; fallo comprobado (abajo). Valor medido no representativo por exclusiones (sección 5) |
| QG-3 | Análisis estático | Sin bloqueantes ni críticos | Pendiente: herramienta decidida (ADR-004), sin configurar en `pom.xml` ni en CI |
| QG-4 | Dependencias e imagen | Sin vulnerabilidades críticas ni altas | En curso: Dependabot y escaneo de secretos (Yahir); Trivy planeado para el Sprint 4 |
| QG-5 | Revisión de código | ≥ 1 aprobación de otro integrante | Activo (rama protegida); hubo una excepción (sección 9) |

### 3.1 Evidencia de que QG-2 rompe el build

Para confirmar que el gate no es decorativo, el 23/10/2026 agregué temporalmente una clase `ClaseSinPruebas` con 6 métodos sin pruebas, sin tocar el umbral. Resultado de `.\mvnw.cmd clean verify`:

```text
Rule violated for bundle ti-desk: lines covered ratio is 0.50, but expected minimum is 0.60
BUILD FAILURE (25.9 s)
```

La clase se eliminó y no se integró a `main`.

---

## 4. Pruebas

Ejecución de referencia: `./mvnw clean verify` en Git Bash, 25/10/2026, Testcontainers 2.0.5 con `postgres:16`.

| Clase | Tipo | Pruebas | Fallas | Tiempo |
| :--- | :--- | ---: | ---: | ---: |
| `InfraestructuraTest` | Integración (Testcontainers) | 4 | 0 | 20.20 s |
| `EstadoTicketTest` | Unitaria | 8 | 0 | 0.15 s |
| `CategoriaTest` | Unitaria | 2 | 0 | 0.02 s |
| `PrioridadTest` | Unitaria | 2 | 0 | < 0.01 s |
| **Total** | | **16** | **0** | `BUILD SUCCESS` en 27.7 s |

| Métrica | Valor | Meta del plan |
| :--- | :--- | :--- |
| Pruebas que pasan | 16 de 16 | 100 % |
| Proporción de unitarias | 75 % (12 de 16) | ≥ 70 % |
| Suite unitaria | < 1 s | < 10 s |
| Pruebas nuevas en el sprint | 4 de integración (`InfraestructuraTest`) | — |

Pruebas de integración de `InfraestructuraTest`:

| Prueba | Qué verifica |
| :--- | :--- |
| `flywayCreoLasTablasDelModelo` | Existen las 7 tablas del modelo, incluida `folio_secuencia` |
| `flywayAplicoV1yV2SinErrores` | El historial de Flyway solo tiene migraciones exitosas y en orden |
| `losIndicesDeV2Existen` | Existen los índices `ix_ticket_estado_prioridad`, `ix_ticket_tecnico` e `ix_ticket_fecha_limite_sla` |
| `actuatorHealthResponde200ConEstadoUp` | `GET /actuator/health` responde 200 con `"status":"UP"` |

**Observación:** `flywayAplicoV1yV2SinErrores` compara con `containsExactly("1", "2", "3")`. El nombre ya no corresponde y la prueba fallará con cada migración nueva. Se corrige en un PR de prueba aparte (acción A-05).

---

## 5. Cobertura

| Medición (25/10/2026) | Resultado |
| :--- | :--- |
| Cobertura de líneas que mide el gate | 100 % (7 de 7) |
| Cobertura global del reporte HTML | 80 % de líneas (8 de 10); 96 % de instrucciones |
| Paquete `ticket` (3 enumerados) | 7 de 7 líneas; 4 de 4 ramas |
| Paquete raíz (`TiDeskApplication`, excluida) | 1 de 3 líneas |

Exclusiones del gate en `pom.xml`: `**/config/**`, `**/dto/**`, `**/*Application.class` y, desde el PR #32, las 6 entidades JPA (`Ticket`, `HistorialEstado`, `Usuario`, `Activo`, `PoliticaSla` y `Comentario`).

**Evaluación de QA:** con estas exclusiones, el 60 % se cumple solo con pruebas de enumerados y no verifica ninguna regla de negocio. Es el riesgo de "cobertura inflada" que registré en el plan de pruebas. Acepté la exclusión de entidades como temporal y no debí dejarla sin fecha de retiro. Acción A-02: quitar las exclusiones de entidades en el mismo PR en que entre la primera clase de servicio, y no excluir nunca una entidad con lógica (por ejemplo, transiciones de estado dentro de `Ticket`).

---

## 6. Defectos y hallazgos

**Defectos (`bug`):** 0 abiertos, 0 cerrados. No hubo código de negocio que probar en el sprint.

**Hallazgos de entorno:**

| ID | Hallazgo | Severidad | Estado al cierre del Sprint 1 |
| :--- | :--- | :--- | :--- |
| H-01 | `version: '3.8'` obsoleta en `docker-compose.yml` | Baja | Resuelto (PR #34, Yahir) |
| H-02 | Las pruebas necesitaban PostgreSQL en `localhost:5432` | Media | Resuelto (TID-17, PR #31) |
| H-03 | Flyway sin migraciones | Informativa | Resuelto (V1, V2 y V3) |
| H-04 | HTTP 401 en repositorios Maven secundarios; no afecta el build | Baja | En monitoreo |
| H-05 | Advertencias de Mockito/ByteBuddy y de `open-in-view` | Baja | Parcial: `open-in-view` resuelto; Mockito pendiente |
| H-06 | Testcontainers sin verificar en los 6 equipos | Media | En verificación: 1 de 6 (Brenda) |

---

## 7. Trazabilidad requisito → prueba

| Requisito | Casos del plan | Escenario Gherkin | Prueba automatizada |
| :--- | :--- | :--- | :--- |
| RF-01 Registro | CP-001 a CP-003 | Sí (6 escenarios) | No: código fuera de `main` |
| RF-02 Login con JWT | CP-004 a CP-006 | Sí (4 escenarios) | No: código fuera de `main` |
| RF-03 Administración de usuarios | CP-007 a CP-009 | CP-008 y CP-009; CP-007 sin escenario | No: no iniciado |
| Seguridad transversal | CP-041, CP-042 y CP-044 | Sí (7 escenarios) | No: faltan controladores para listar rutas |
| RF-04 Crear ticket | CP-010 a CP-012 | No | No: no iniciado |
| RF-05 Listar tickets | CP-013 a CP-016 | No | No: no iniciado |
| Humo y arranque | CP-047 y CP-048 (CP-046 es manual) | — | Sí: `InfraestructuraTest` (health, migraciones en BD vacía e índices) |

Requisitos del Sprint 1 con al menos una prueba automatizada: **0 de 5** (RF-01 a RF-05). El plan cubre 51 de 52 escenarios del parcial con caso de prueba (98 %); falta el CP-058 (activo dado de baja, HU-04), pendiente de que Ulises y Melissa confirmen la regla.

---

## 8. Riesgos y bloqueos

| Riesgo / bloqueo | Prob. | Impacto | Mitigación | Responsable |
| :--- | :--- | :--- | :--- | :--- |
| Seguridad (HU-01, HU-02) en una rama sin PR: no hay qué probar | Alta | Alto | Integrar registro y login primero en el Sprint 2; QA automatiza los escenarios del PR #35 en cuanto entren | Sashid / Brenda |
| HU-04 y HU-05 no iniciadas: sin consultas ni controladores para `@DataJpaTest` y `@WebMvcTest` | Alta | Alto | Priorizar crear y listar tickets en el Sprint 2 | Ulises / Fernando |
| Cobertura inflada por exclusiones | Alta | Medio | Acción A-02 | Brenda / Ulises |
| QG-3 sin integrar | Media | Medio | Acción A-03, en modo reporte | Yahir / Brenda |
| Testcontainers sin confirmar en 5 equipos | Media | Medio | Acción A-04; Codespaces como alternativa | Brenda / Yahir |

---

## 9. Observaciones de QA sobre `main`

1. **Commit directo a `main`.** El commit `a725019` (mío, 23/10) entró sin PR, contra la regla de rama protegida y QG-5. A partir de aquí, todo cambio, incluidos los de documentación, entra por PR.
2. **PR con reformateo y contenido mezclados.** El PR #40 cambió 359 líneas del plan, casi todas de formato. Los reformateos irán en PR aparte.
3. **`ci.yml` levanta un servicio PostgreSQL** que las pruebas ya no usan con Testcontainers. Se propone quitarlo (decisión de Yahir).
4. **Dependabot actualizó `spring-boot-starter-parent` (#41) y JaCoCo 0.8.16 (#42)** después de la ejecución de referencia. Hay que repetir `mvnw clean verify` al inicio del Sprint 2 para confirmar las métricas.

---

## 10. Acciones de mejora para el Sprint 2

| ID | Acción | Responsable | Origen |
| :--- | :--- | :--- | :--- |
| A-01 | Ningún cambio entra a `main` sin PR y aprobación de otro integrante | Todo el equipo; vigila Brenda | Observación 1 |
| A-02 | Quitar las exclusiones de entidades de JaCoCo en el PR de la primera clase de servicio | Brenda / Ulises | Sección 5 |
| A-03 | Integrar SpotBugs y Checkstyle en modo reporte y después como gate (QG-3) | Yahir / Brenda | ADR-004 |
| A-04 | Confirmar Docker y Testcontainers en los equipos de Fernando, Yahir y Melissa | Brenda / Fernando / Yahir / Melissa | H-06 |
| A-05 | Corregir `flywayAplicoV1yV2SinErrores` para que no dependa del número de migraciones | Brenda | Sección 4 |
| A-06 | Automatizar los escenarios Gherkin de autenticación en cuanto el módulo de seguridad entre a `main` | Brenda / Sashid | Sección 7 |
| A-07 | Configurar Mockito como agente para quitar la advertencia de ByteBuddy | Brenda | H-05 |

---

## 11. Entregables de QA del sprint

| Entregable | Referencia | Estado |
| :--- | :--- | :--- |
| Base de integración con Testcontainers y PostgreSQL 16 | TID-17, PR #31 | Integrado en `main` |
| Plan de pruebas v1.1 (57 casos, CP-049 a CP-057 nuevos) | TID-15, PR #30 | Integrado en `main` |
| Verificación del entorno v0.3 con Testcontainers | TID-13, PR #37 | Integrado en `main` |
| Escenarios Gherkin de registro, login y rutas protegidas | TID-18, PR #35 | Integrado en `main` |
| Reporte de calidad del Sprint 0 | TID-20, PR #40 | Integrado en `main` |
| Reporte de calidad del Sprint 1 | TID-20, este documento | En PR |

---

## 12. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 1.0 | 16/11/2026 | Brenda | Reporte del Sprint 1 con datos al cierre (28/10/2026): pruebas, cobertura, gates, hallazgos, trazabilidad, riesgos y acciones de mejora |
