<!-- TÍTULO DEL PR: tipo(TID-XX): descripción corta  (ej. feat(TID-14): Registro de Activos) -->

## 1. Historia de usuario y requisito

- **Historia (tablero GitHub Projects):** TID-XX
- **Requisito(s) del SRS:** RF-XX
- **Sprint:** X
- **Rama:** `feature/TID-XX-descripcion-corta`

## 2. Tipo de cambio

- [ ] `feat`: funcionalidad nueva
- [ ] `fix`: corrección de error
- [ ] `test`: pruebas
- [ ] `docs`: documentación
- [ ] `ci`: pipeline / Docker / infraestructura

## 3. Dominio del cambio y revisor obligatorio

- [ ] Seguridad / Autenticación / JWT → **Sashid**
- [ ] Pipeline / CI/CD / Docker / GitHub Actions → **Yahir**
- [ ] Esquema BD / Flyway / Modelos JPA → **Ulises**
- [ ] Funcionalidad de negocio (Tickets, Activos, SLA) → **Brenda (QA)** o integrante libre

**Revisor(es) asignado(s):** @usuario

## 4. Descripción

<!-- ¿Qué hace este PR y por qué? Entre 2 y 4 líneas. -->

## 5. Cambios principales

-
-

## 6. Pruebas realizadas

- [ ] `mvn clean verify` en local, sin errores
- [ ] `docker compose up -d` levanta correctamente
- [ ] Pruebas unitarias agregadas o actualizadas (JUnit 5 + Mockito)
- [ ] Pruebas de integración / API con Testcontainers (si aplica)
- [ ] Pruebas de seguridad por rol, positivas y negativas (si aplica)

**Evidencia:** <!-- captura de Swagger UI, respuesta HTTP o reporte JaCoCo -->

## 7. Quality gates del pipeline

- [ ] Pipeline de GitHub Actions en verde (`build-and-test`)
- [ ] Cobertura JaCoCo ≥ 60 % (parcial) / ≥ 80 % en capa de servicio (final)
- [ ] Análisis estático sin problemas bloqueantes ni críticos
- [ ] Sin vulnerabilidades críticas ni altas en dependencias o imagen
- [ ] Sin secretos ni credenciales en el código

## 8. Definición de Terminado (DoD)

- [ ] Cumple los criterios de aceptación de la historia
- [ ] Contrato OpenAPI actualizado y endpoint probado en Swagger UI
- [ ] Nueva migración Flyway si cambió el esquema (nunca editar una ya integrada en `main`)
- [ ] Lista para aceptación del Product Owner (Melissa) en la review

## 9. Checklist del autor

- [ ] Rama derivada de `main` con vida máxima de 3 días
- [ ] Commits con Conventional Commits y referencia a la historia (TID-XX)
- [ ] No voy a integrar mi propio código: espero la aprobación de otro integrante
- [ ] Resolveré todos los comentarios del revisor antes del merge