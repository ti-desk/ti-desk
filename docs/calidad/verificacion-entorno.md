# TI-Desk · Verificación del entorno de desarrollo

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Verificación de Docker y entorno local (Sprint 0) |
| Responsable | Brenda Nashelly Gómez Reyes, Ingeniera de calidad (QA) |
| Fecha de la verificación | 22 y 23 de octubre de 2026 |
| Riesgo que atiende | "Docker o Testcontainers no corren en equipos personales" (anteproyecto, sección 16) |
| Alternativa si falla | GitHub Codespaces |

---

## 1. Objetivo

Confirmar que los 6 integrantes pueden levantar el entorno local del proyecto (base de datos y servidor de correo de pruebas) y ejecutar el build con el quality gate de cobertura, antes de iniciar el desarrollo del Sprint 1.

## 2. Procedimiento

Cada integrante ejecuta, desde la raíz del repositorio clonado, en PowerShell:

```powershell
docker --version
docker run hello-world
docker compose up -d
docker compose ps
.\mvnw.cmd clean verify
docker compose down
```

Desde que se integró la base de Testcontainers (TID-17), `mvnw verify` ya no necesita la base del compose: levanta su propio PostgreSQL 16 y lo elimina al terminar. Solo hace falta Docker Desktop abierto.

### 2.1 Confirmación de Testcontainers (H-06)

Cada integrante ejecuta, desde la raíz del repositorio, en PowerShell:

```powershell
git checkout main
git pull
docker compose down
.\mvnw.cmd clean verify
```

Resultado esperado: `BUILD SUCCESS` y "All coverage checks have been met". Si falla, se usa GitHub Codespaces mientras se resuelve.

### Criterios de aceptación

| # | Criterio | Resultado esperado |
| :--- | :--- | :--- |
| 1 | `docker --version` | Muestra una versión instalada |
| 2 | `docker run hello-world` | Imprime el mensaje de bienvenida de Docker |
| 3 | `docker compose up -d` | Crea la red y arranca los contenedores `tidesk-postgres` y `tidesk-mailpit` |
| 4 | `docker compose ps` | Ambos contenedores en estado `healthy` |
| 5 | Mailpit en `http://localhost:8025` | La bandeja web carga |
| 6 | `.\mvnw.cmd clean verify` | `BUILD SUCCESS` y "All coverage checks have been met" |

---

## 3. Resultados por integrante

Leyenda: ✅ cumple · ❌ no cumple · ⏳ por completar

| Integrante | Rol | Docker (versión) | hello-world | compose ps (healthy) | Mailpit :8025 | `mvnw verify` | Observaciones |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Brenda Nashelly Gómez Reyes | QA | ✅ 29.9.1 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 16.15. Flyway aplicó 2 migraciones. Repetido el 23/10 con Testcontainers (ver 3.1). |
| Ulises Díaz Antonio | Backend / BD | ✅ 29.9.2 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Flyway validó y aplicó 2 migraciones. |
| Sashid Blanco Garrido | Arquitectura / Seguridad | ✅ 29.9.1 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Ejecución del 21/10. |
| Fernando Gawalek Felipe | Project Manager | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Yahir Antonio Bautista Ramirez | DevOps / SRE | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Melissa Vásquez Zarate | Product Owner | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |

Avance: 3 de 6 integrantes con ✅ en todos los criterios.

### 3.1 Confirmación de Testcontainers por integrante (H-06)

| Integrante | Rol | `mvnw verify` con Testcontainers (compose abajo) | Fecha | Observaciones |
| :--- | :--- | :--- | :--- | :--- |
| Brenda Nashelly Gómez Reyes | QA | ✅ BUILD SUCCESS | 23/10/2026 | 16 pruebas, 0 fallas. Testcontainers 2.0.5 con PostgreSQL 16.15 propio. |
| Ulises Díaz Antonio | Backend / BD | ⏳ | | |
| Sashid Blanco Garrido | Arquitectura / Seguridad | ⏳ | | |
| Fernando Gawalek Felipe | Project Manager | ⏳ | | |
| Yahir Antonio Bautista Ramirez | DevOps / SRE | ⏳ | | |
| Melissa Vásquez Zarate | Product Owner | ⏳ | | |

Avance: 1 de 6 integrantes confirmados.

---

## 4. Detalle de la ejecución de referencia (Brenda, 21/10/2026)

| Elemento | Resultado |
| :--- | :--- |
| Red `ti-desk_default` | Creada |
| `tidesk-mailpit` | `Up (healthy)`, puertos 1025 y 8025 |
| `tidesk-postgres` | `Up (healthy)`, puerto 5432 |
| JDK | 21.0.12.1 |
| Spring Boot | 4.1.1 |
| PostgreSQL | 16.15 |
| Pruebas | 2 ejecutadas, 0 fallas, 0 errores |
| JaCoCo | "All coverage checks have been met" |
| Maven | `BUILD SUCCESS` (17.9 s) |

Nota: esta ejecución se hizo antes de integrar las migraciones de Flyway. En la repetición con `main` actualizado, Flyway aplicó `1 - crear tablas` y `2 - indices` sin errores.

### 4.1 Ejecución con Testcontainers (Brenda, 23/10/2026)

Ejecución con `main` actualizado (incluye TID-17) y con `docker compose down` previo.

| Elemento | Resultado |
| :--- | :--- |
| Docker | 29.9.1 (Docker Desktop, WSL2), 0 contenedores en ejecución antes de la prueba |
| Testcontainers | 2.0.5; contenedor `postgres:16` propio en un puerto aleatorio (62208) |
| PostgreSQL | 16.15 |
| Flyway | Validó y aplicó 2 migraciones sobre una base vacía (`1 - crear tablas`, `2 - indices`) |
| Pruebas | 16 ejecutadas, 0 fallas, 0 errores, 0 omitidas (InfraestructuraTest 4, CategoriaTest 2, EstadoTicketTest 8, PrioridadTest 2) |
| JaCoCo | "All coverage checks have been met" |
| Maven | `BUILD SUCCESS` (23.2 s) |

---

## 5. Hallazgos

| ID | Hallazgo | Severidad | Acción sugerida | Responsable | Estado |
| :--- | :--- | :--- | :--- | :--- | :--- |
| H-01 | `docker-compose.yml` conserva `version: '3.8'`; Docker lo marca como obsoleto y lo ignora (confirmado por Brenda, Sashid y Ulises) | Baja | Eliminar la línea | Yahir | Abierto |
| H-02 | Las pruebas actuales (`contextLoads`) necesitan PostgreSQL corriendo en `localhost:5432`; sin `docker compose up -d`, `mvnw verify` falla en local | Media | Migrar las pruebas de integración a Testcontainers en el Sprint 1 | Brenda | Resuelto: TID-17 integrado en `main`; `mvnw verify` pasa con el compose abajo (23/10/2026) |
| H-03 | Flyway advertía "No migrations found" | Informativa | Se resuelve cuando Ulises agregue las migraciones V1..Vn | Ulises | Resuelto: V1 y V2 aplicadas en las ejecuciones de Brenda y Ulises |
| H-04 | Maven reporta HTTP 401 al consultar los repositorios `flyway-community-db-support` y `MavenFeed`; no afecta el build (presente en los 3 equipos verificados) | Baja | Monitorear; no requiere acción inmediata | Yahir | En monitoreo |
| H-05 | Advertencias de Mockito/ByteBuddy (carga dinámica de agente en Java 21) y de `spring.jpa.open-in-view` | Baja | Configurar Mockito como agente y definir `open-in-view` en `application.yml` | Brenda / Ulises | Parcial: `open-in-view` ya está definido en `application.yml` y no apareció en las ejecuciones de Brenda ni de Ulises; Mockito sigue apareciendo (también el 23/10) |
| H-06 | Testcontainers no se había verificado en los equipos del equipo | Media | Confirmar que `mvnw verify` con Testcontainers corre en los 6 equipos (sección 3.1) | Brenda / Yahir | En verificación: confirmado en 1 de 6 equipos (Brenda); esperando a los otros 5 |

Observaciones menores de los integrantes:

- Ulises usa Docker 29.9.2 y Brenda y Sashid 29.9.1; ambas versiones funcionan, sin impacto.

---

## 6. Conclusión

Estado parcial: Docker, Compose, Mailpit y el build con JaCoCo funcionan en 3 de 6 equipos (Brenda, Ulises y Sashid). Faltan los resultados de Fernando, Yahir y Melissa. Con la base de Testcontainers integrada (TID-17), H-02 queda resuelto. H-06 sigue en verificación: Brenda lo confirmó el 23/10/2026 y faltan los otros 5 integrantes.

Criterio de cierre: los 6 integrantes con ✅ en todos los criterios. Si alguien tiene ❌, usará GitHub Codespaces mientras se resuelve, y se anotará aquí.

---

## 7. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 21/10/2026 | Brenda | Plantilla y resultado de referencia |
| 0.2 | 22/10/2026 | Brenda | Resultados de Brenda, Ulises y Sashid; estado actualizado de los hallazgos H-01 a H-06 |
| 0.3 | 23/10/2026 | Brenda | Procedimiento y resultados con Testcontainers (TID-17); H-02 resuelto; H-06 en verificación (1 de 6) |
