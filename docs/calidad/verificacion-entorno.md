# TI-Desk · Verificación del entorno de desarrollo

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Verificación de Docker y entorno local (Sprint 0) |
| Responsable | Brenda Nashelly Gómez Reyes, Ingeniera de calidad (QA) |
| Fecha de la verificación | 22 de octubre de 2026 |
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
| Brenda Nashelly Gómez Reyes | QA | ✅ 29.9.1 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 16.15. Flyway aplicó 2 migraciones. |
| Ulises Díaz Antonio | Backend / BD | ✅ 29.9.2 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Flyway validó y aplicó 2 migraciones. |
| Sashid Blanco Garrido | Arquitectura / Seguridad | ✅ 29.9.1 | ✅ | ✅ | ✅ | ✅ BUILD SUCCESS | Ejecución del 21/10. |
| Fernando Gawalek Felipe | Project Manager | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Yahir Antonio Bautista Ramirez | DevOps / SRE | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Melissa Vásquez Zarate | Product Owner | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |

Avance: 3 de 6 integrantes con ✅ en todos los criterios.

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

---

## 5. Hallazgos

| ID | Hallazgo | Severidad | Acción sugerida | Responsable | Estado |
| :--- | :--- | :--- | :--- | :--- | :--- |
| H-01 | `docker-compose.yml` conserva `version: '3.8'`; Docker lo marca como obsoleto y lo ignora (confirmado por Brenda, Sashid y Ulises) | Baja | Eliminar la línea | Yahir | Abierto |
| H-02 | Las pruebas actuales (`contextLoads`) necesitan PostgreSQL corriendo en `localhost:5432`; sin `docker compose up -d`, `mvnw verify` falla en local | Media | Migrar las pruebas de integración a Testcontainers en el Sprint 1 | Brenda | Abierto |
| H-03 | Flyway advertía "No migrations found" | Informativa | Se resuelve cuando Ulises agregue las migraciones V1..Vn | Ulises | Resuelto: V1 y V2 aplicadas en las ejecuciones de Brenda y Ulises |
| H-04 | Maven reporta HTTP 401 al consultar los repositorios `flyway-community-db-support` y `MavenFeed`; no afecta el build (presente en los 3 equipos verificados) | Baja | Monitorear; no requiere acción inmediata | Yahir | En monitoreo |
| H-05 | Advertencias de Mockito/ByteBuddy (carga dinámica de agente en Java 21) y de `spring.jpa.open-in-view` | Baja | Configurar Mockito como agente y definir `open-in-view` en `application.yml` | Brenda / Ulises | Parcial: `open-in-view` ya está definido en `application.yml` y no apareció en las ejecuciones de Brenda ni de Ulises; Mockito sigue apareciendo |
| H-06 | Testcontainers aún no está en el `pom.xml`; no se puede verificar directamente | Media | Agregar la dependencia en el Sprint 1 y confirmar que corre en los 6 equipos | Brenda / Yahir | Abierto |

Observaciones menores de los integrantes:

- Ulises usa Docker 29.9.2 y Brenda y Sashid 29.9.1; ambas versiones funcionan, sin impacto.

---

## 6. Conclusión

Estado parcial: Docker, Compose, Mailpit y el build con JaCoCo funcionan en 3 de 6 equipos (Brenda, Ulises y Sashid). Faltan los resultados de Fernando, Yahir y Melissa. La verificación de Testcontainers sigue pendiente hasta que se agregue al `pom.xml` (H-06).

Criterio de cierre: los 6 integrantes con ✅ en todos los criterios. Si alguien tiene ❌, usará GitHub Codespaces mientras se resuelve, y se anotará aquí.

---

## 7. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 21/10/2026 | Brenda | Plantilla y resultado de referencia |
| 0.2 | 22/10/2026 | Brenda | Resultados de Brenda, Ulises y Sashid; estado actualizado de los hallazgos H-01 a H-06 |
