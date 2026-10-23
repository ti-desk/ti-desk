# TI-Desk · Verificación del entorno de desarrollo

| Campo | Valor |
| :--- | :--- |
| Proyecto | TI-Desk: mesa de ayuda del área de TI |
| Documento | Verificación de Docker y entorno local (Sprint 0) |
| Responsable | Brenda Nashelly Gómez Reyes, Ingeniera de calidad (QA) |
| Fecha de la verificación | 21 de octubre de 2026 |
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
| Brenda Nashelly Gómez Reyes | QA | ⏳ | ✅ | ✅ | ⏳ | ✅ BUILD SUCCESS | Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 16.15. Ver sección 4 |
| Ulises Díaz Antonio | Backend / BD | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Sashid Blanco Garrido | Arquitectura / Seguridad | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Fernando Gawalek Felipe | Project Manager | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Yahir Antonio Bautista Ramirez | DevOps / SRE | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |
| Melissa Vásquez Zarate | Product Owner | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ | |

---

## 4. Detalle de la ejecución de referencia (Brenda)

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

---

## 5. Hallazgos

| ID | Hallazgo | Severidad | Acción sugerida | Responsable |
| :--- | :--- | :--- | :--- | :--- |
| H-01 | `docker-compose.yml` conserva `version: '3.8'`; Docker lo marca como obsoleto | Baja | Eliminar la línea | Yahir |
| H-02 | Las pruebas actuales (`contextLoads`) necesitan PostgreSQL corriendo en `localhost:5432`; sin `docker compose up -d`, `mvnw verify` falla en local | Media | Migrar las pruebas de integración a Testcontainers en el Sprint 1 | Brenda |
| H-03 | Flyway advierte "No migrations found" | Informativa | Se resuelve cuando Ulises agregue las migraciones V1..Vn | Ulises |
| H-04 | Maven reporta HTTP 401 al consultar los repositorios `flyway-community-db-support` y `MavenFeed`; no afecta el build | Baja | Monitorear; no requiere acción inmediata | Yahir |
| H-05 | Advertencias de Mockito (auto-attach de agente) y `spring.jpa.open-in-view` | Baja | Configurar Mockito como agente y definir `open-in-view` en `application.yml` | Brenda / Ulises |
| H-06 | Testcontainers aún no está en el `pom.xml`; no se puede verificar directamente | Media | Agregar la dependencia en el Sprint 1 y confirmar que corre en los 6 equipos | Brenda / Yahir |

---

## 6. Conclusión

⏳ Por completar cuando los 6 integrantes registren su resultado.

Criterio de cierre: los 6 integrantes con ✅ en todos los criterios. Si alguien tiene ❌, usará GitHub Codespaces mientras se resuelve, y se anotará aquí.

---

## 7. Historial del documento

| Versión | Fecha | Autor | Cambio |
| :--- | :--- | :--- | :--- |
| 0.1 | 21/10/2026 | Brenda | Plantilla y resultado de referencia |
