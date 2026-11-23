# Plantilla Oficial del Pull Request (PR)

**Proyecto:** ti-desk (Help Desk ITSM) · **BYSU-FM Software Factory**

---

## ¿De qué trata esta plantilla?

Todo Pull Request hacia `main` **debe** usar esta plantilla. Resume lo que el anteproyecto exige para integrar código y reúne en un solo formato:

| Elemento | Para qué sirve |
|---|---|
| **Historia asociada (TID-XX)** | Vincula el PR con la tarjeta del tablero de GitHub Projects y con el requisito del SRS. |
| **Matriz de revisores por dominio** | Define quién debe aprobar según lo que toca el cambio (seguridad, pipeline, BD o negocio). |
| **Quality Gates del pipeline** | Verifica build verde, cobertura JaCoCo, análisis estático, vulnerabilidades y ausencia de secretos. |
| **Definición de Terminado (DoD)** | Confirma criterios de aceptación, OpenAPI, migraciones Flyway y listo para el Product Owner. |

Se llena en la descripción del PR: copia el bloque, complétalo y pégalo.

---

## 1. Cómo se configura

- **Ubicación:** guardar la plantilla en `.github/pull_request_template.md` dentro de la rama `main`, para que GitHub la cargue automáticamente al abrir cada PR.
- **Título del PR:** `tipo(TID-XX): descripción corta`, igual que el mensaje de commit (ej. `feat(TID-14): Registro de Activos`).

Tipos válidos (Conventional Commits): `feat`, `fix`, `test`, `docs`, `ci`.

## 2. Matriz de revisores por dominio

| Dominio del cambio | Revisor obligatorio |
|---|---|
| Seguridad / Autenticación / JWT | Sashid Blanco |
| Pipeline / CI/CD / Docker / GitHub Actions | Yahir Bautista |
| Esquema BD / Flyway / Modelos JPA | Ulises Díaz |
| Funcionalidad de negocio (Tickets, Activos, SLA) | Brenda Gómez (QA) o integrante libre |

## 3. Plantilla (copiar y pegar)

> El archivo listo para usar en el repositorio es `pull_request_template.md` (sin esta explicación).

````markdown
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
````

## 4. Reglas de llenado

- **Marca solo lo que cumpliste:** un check sin evidencia (pruebas, pipeline, Swagger UI) será rechazado por el revisor.
- **Varios dominios, varios revisores:** si el PR toca seguridad, pipeline o esquema de BD, asigna a Sashid, Yahir o Ulises respectivamente, además del revisor de negocio.
- **Cambios pequeños:** ramas de máximo 3 días y PR acotados a una historia, para evitar la integración tardía ("big bang").
- **Validación del revisor:** el revisor valida esta plantilla junto con los criterios de la sección 4 antes de dar el *Approve*.