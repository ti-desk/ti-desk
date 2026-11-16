# ADR-004: Herramienta de análisis estático (SpotBugs y Checkstyle)

- **Estado:** Aceptado
- **Fecha:** 22 de octubre de 2026
- **Responsable:** Brenda Nashelly Gómez Reyes (Ingeniera de calidad), con Yahir Antonio Bautista Ramirez (DevOps / SRE)

## Contexto

El quality gate QG-3 del plan de pruebas exige análisis estático sin
problemas bloqueantes ni críticos, y el anteproyecto (secciones 8 y 12.1)
pide mantenibilidad sin "code smells" bloqueantes. El anteproyecto ofrece
dos opciones: SonarCloud o Checkstyle con SpotBugs. Al cierre del Sprint 0
el gate estaba definido pero sin herramienta, por lo que era necesario
decidirla antes de que empiece el desarrollo del Sprint 1.

## Decisión

Se adoptan **SpotBugs** (detección de defectos probables en el código
compilado) y **Checkstyle** (estilo y convenciones del código fuente) como
plugins de Maven, ejecutados dentro de `mvn -B verify`, tanto en local como
en el pipeline de GitHub Actions.

1. El build falla si SpotBugs encuentra incidencias de severidad alta o si
   Checkstyle encuentra violaciones de severidad `error`.
2. Las reglas de Checkstyle se alinean con las convenciones de código
   definidas por el arquitecto (ADR-002).
3. La configuración se agrega en un PR aparte. Los cambios al `pom.xml` y a
   `ci.yml` requieren la aprobación de Yahir.
4. Adopción gradual: las primeras ejecuciones se hacen en modo reporte para
   ajustar reglas y exclusiones; después se activa el fallo del build.

## Alternativas consideradas

1. **SonarCloud:** ofrece panel histórico y más reglas, pero depende de un
   servicio externo, de una cuenta y un token guardado como secreto, y de
   configuración fuera del repositorio.
2. **Solo Checkstyle:** cubre estilo, pero no detecta defectos probables.
3. **Solo SpotBugs:** detecta defectos, pero no verifica convenciones.
4. **Ninguna herramienta:** incumple el quality gate QG-3.
5. **SpotBugs + Checkstyle (elegida):** funciona con el mismo comando en
   local y en CI, sin servicios externos ni secretos adicionales.

## Consecuencias

**Positivas**
- Se activa el quality gate QG-3 (4 de 5 gates activos).
- Cada integrante ve los hallazgos en su equipo antes de abrir el PR.
- No se añaden dependencias de servicios externos ni secretos.

**Negativas / riesgos**
- No hay panel histórico ni métricas de deuda técnica como en SonarCloud.
- Posibles falsos positivos y reglas demasiado estrictas que frenen PR.
- El build local y el de CI tardan unos segundos más.

**Mitigación**
- Empezar en modo reporte y registrar las exclusiones con su justificación.
- Revisar las reglas en la retrospectiva de cada sprint.
- Reevaluar SonarCloud si el equipo necesita un panel de tendencias.
