# TI-Desk · Reporte de calidad del Sprint 0

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Reporte de calidad del Sprint 0 (semanas 1–2, arranque) |
| Responsable | Brenda Nashelly Gómez Reyes, Ingeniera de calidad (QA) |
| Referencia | Plan de pruebas v1, secciones 5, 9 y 10 |
| Fecha de corte | [completar: fecha de cierre del Sprint 0] |
| Versión / estado | 0.2 · Borrador (los campos entre corchetes se completan antes de entregar) |

---

## 1. Resumen ejecutivo

- El plan de pruebas v1 está redactado: pirámide de pruebas de 7 niveles, 5 quality gates, criterios de entrada y salida, gestión de defectos y 48 casos de prueba iniciales (CP-001 a CP-048).
- El quality gate de cobertura JaCoCo (≥ 60 %) está activo en el build.
- El entorno local (Docker, Docker Compose, Mailpit y `mvnw verify`) se verificó en 3 de 6 equipos (Brenda, Ulises y Sashid), con 6 hallazgos registrados (H-01 a H-06).
- Se decidió la herramienta de análisis estático: SpotBugs y Checkstyle (ADR-004).
- Quedó pendiente verificar Docker en los equipos de Fernando, Yahir y Melissa, y comprobar Testcontainers, que todavía no estaba en el `pom.xml`.

---

## 2. Objetivos de QA del sprint

| Objetivo | Estado |
| :--- | :--- |
| Redactar el plan de pruebas v1 (ISO/IEC 25010 y pirámide de pruebas) | Hecho (TID-07) |
| Definir los quality gates del pipeline | Hecho (sección 5 del plan) |
| Verificar Docker y Testcontainers en los equipos personales | Parcial: Docker verificado en 3 de 6; Testcontainers pendiente (H-06) |
| Elegir la herramienta de análisis estático junto con Yahir | Hecho (ADR-004, TID-14) |

---

## 3. Plan de pruebas v1

| Elemento | Contenido |
| :--- | :--- |
| Niveles de prueba | Unitarias, de rebanada, integración, seguridad, correo, carga y aceptación |
| Metas | ≥ 70 % de pruebas unitarias y suite unitaria < 10 s; p95 < 500 ms con 10 000 tickets y 20 usuarios (k6, Sprint 5) |
| Alcance del parcial (v0.5.0) | RF-01 a RF-08, RF-10, RF-11 y RF-13, más los RNF verificables |
| Casos de prueba | 48 en total: autenticación 9, tickets 16, SLA 8, inventario 7, seguridad transversal 5, humo y arranque 3 |
| Defectos | Issues de GitHub con etiqueta `bug`; severidades Crítica, Alta, Media y Baja con plazo de atención |
| Reportes | Un reporte de calidad por sprint (plan, sección 9) |

---

## 4. Quality gates

| # | Gate | Umbral | Estado al cierre del Sprint 0 |
| :--- | :--- | :--- | :--- |
| QG-1 | Compilación y pruebas | 100 % en verde | Activo |
| QG-2 | Cobertura de líneas (JaCoCo) | ≥ 60 % parcial; ≥ 80 % en servicios (final) | Activo al 60 %; se comprobó que el build falla por debajo del umbral (evidencia en el reporte del Sprint 1, sección 3) |
| QG-3 | Análisis estático | Sin bloqueantes ni críticos | Pendiente: herramienta decidida (ADR-004), configuración sin integrar |
| QG-4 | Dependencias e imagen | Sin vulnerabilidades críticas ni altas | En curso (Dependabot, escaneo de secretos, Trivy) |
| QG-5 | Revisión de código | ≥ 1 aprobación de otro integrante | Activo (rama protegida) |

---

## 5. Verificación del entorno

Procedimiento: `docker --version`, `docker run hello-world`, `docker compose up -d`, `docker compose ps`, Mailpit en `http://localhost:8025` y `.\mvnw.cmd clean verify`.

| Integrante | Rol | Resultado | Observaciones |
| :--- | :--- | :--- | :--- |
| Brenda Nashelly Gómez Reyes | QA | ✅ BUILD SUCCESS | Docker 29.9.1; Java 21.0.12.1; PostgreSQL 16.15 |
| Ulises Díaz Antonio | Backend / BD | ✅ BUILD SUCCESS | Docker 29.9.2 |
| Sashid Blanco Garrido | Arquitectura / Seguridad | ✅ BUILD SUCCESS | Docker 29.9.1; ejecución del 21/10 |
| Fernando Gawalek Felipe | Project Manager | ⏳ | |
| Yahir Antonio Bautista Ramirez | DevOps / SRE | ⏳ | |
| Melissa Vásquez Zarate | Product Owner | ⏳ | |

Ejecución de referencia (Brenda, 21/10/2026): 2 pruebas, 0 fallas, JaCoCo cumplido, `BUILD SUCCESS` en 17.9 s.

---

## 6. Hallazgos del entorno

| ID | Hallazgo | Severidad | Estado al cierre |
| :--- | :--- | :--- | :--- |
| H-01 | `version: '3.8'` obsoleta en `docker-compose.yml` | Baja | Abierto al cierre (Yahir); corrección posterior en el PR #34 (TID-19), ya integrado |
| H-02 | Las pruebas necesitan PostgreSQL en `localhost:5432` | Media | Abierto: se migrará a Testcontainers en el Sprint 1 |
| H-03 | Flyway sin migraciones | Informativa | Resuelto (V1 y V2) |
| H-04 | HTTP 401 en repositorios Maven secundarios; no afecta el build | Baja | En monitoreo |
| H-05 | Advertencias de Mockito/ByteBuddy y de `open-in-view` | Baja | Parcial |
| H-06 | Testcontainers no estaba en el `pom.xml` | Media | Abierto |

---

## 7. Decisión de análisis estático (ADR-004)

Se adoptan SpotBugs (defectos probables) y Checkstyle (estilo) como plugins de Maven dentro de `mvn -B verify`. El build fallará con incidencias de severidad alta (SpotBugs) o violaciones `error` (Checkstyle). La adopción es gradual: primero en modo reporte y después como gate. Se descartó SonarCloud por depender de un servicio externo y de un token.

---

## 8. Riesgos de calidad al cierre

| Riesgo | Prob. | Impacto | Mitigación |
| :--- | :--- | :--- | :--- |
| Testcontainers no corre en algún equipo | Media | Medio | Docker Desktop/WSL2 y GitHub Codespaces como alternativa |
| Cobertura inflada con pruebas sin aserciones | Media | Medio | Revisión de pruebas en cada PR y muestreo por QA |
| Pruebas de SLA inestables por la hora real | Alta | Medio | `Clock` inyectado y tiempo fijo |

---

## 9. Acciones de mejora

| Acción | Responsable | Origen |
| :--- | :--- | :--- |
| [completar: acción acordada en la retrospectiva del Sprint 0] | [completar] | Retrospectiva |
| Migrar las pruebas de integración a Testcontainers (H-02) | Brenda | Hallazgo H-02 |
| Completar la verificación de Docker en los 3 equipos pendientes | Brenda / Fernando / Yahir / Melissa | Verificación del entorno |

---

## 10. Entregables de QA del sprint

| Entregable | Referencia | Estado |
| :--- | :--- | :--- |
| Plan de pruebas v1 | TID-07, PR #9 | Integrado en `main` (aprobado) |
| Verificación del entorno v0.1 y v0.2 | TID-11 (PR #11) y TID-13 (PR #28) | Integrados en `main` |
| ADR-004 de análisis estático | TID-14, PR #29 | Integrado en `main` |

---

## 11. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 25/10/2026 | Brenda | Borrador con los datos disponibles del Sprint 0 |
| 0.2 | 25/10/2026 | Brenda | Se confirmó que los 4 entregables de QA (PR #9, #11, #28 y #29) están integrados en `main`; se agregó la referencia a la evidencia del gate de cobertura y a la corrección de H-01 (PR #34) |
