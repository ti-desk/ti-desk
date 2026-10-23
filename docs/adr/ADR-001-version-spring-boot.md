# ADR-001: Versión de Spring Boot y Java

- **Estado:** Aceptado
- **Fecha:** 21 de octubre de 2026
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

TI-Desk es un sistema de mesa de ayuda ITIL 4 para el área de TI de la
universidad. El planteamiento original indicaba Spring Boot 3 con Java 21.
Según el anteproyecto, toda la línea 3.x (incluida la 3.5) dejó de recibir
soporte de código abierto el 30 de junio de 2026, y la línea vigente es
Spring Boot 4.x. El proyecto base ya fue generado con Java 21 LTS y
Spring Boot 4.1.1.

## Decisión

Se adopta **Java 21 (LTS)** y **Spring Boot 4.1.1**.

## Alternativas consideradas

1. **Spring Boot 3.5.x:** más conocida y con más material en línea, pero su
   soporte de código abierto terminó el 30 de junio de 2026, por lo que no
   recibiría parches de seguridad.
2. **Spring Boot 4.1.1 (elegida):** línea vigente, con soporte por delante
   durante todo el semestre y después.

## Consecuencias

**Positivas**
- Soporte y parches de seguridad vigentes durante todo el proyecto.
- Se evita una migración de 3.x a 4.x durante o después del semestre.

**Negativas / riesgos**
- Menos tutoriales y ejemplos en línea para la versión 4.x.
- Posibles diferencias de API respecto a 3.x en Spring Security y en la
  autoconfiguración.

**Mitigación**
- Consultar la documentación oficial y la guía de migración de Spring Boot.
- Cubrir el código con pruebas automatizadas (JaCoCo, mínimo 60 % en el parcial).

## Nota

El README del repositorio indica "Spring Boot 3.x" y debe actualizarse a 4.1.1.