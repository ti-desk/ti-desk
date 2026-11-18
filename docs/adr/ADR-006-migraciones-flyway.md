# ADR-006: Reglas para las migraciones de base de datos con Flyway

- **Estado:** Aceptado
- **Fecha:** 16 de noviembre de 2026
- **Responsable:** Sashid Blanco Garrido (Arquitecto de software / Líder técnico)

## Contexto

El esquema de la base de datos se versiona con Flyway y varias personas
trabajan en paralelo sobre él: Ulises es dueño del modelo de datos, Melisa
del inventario y cada módulo puede necesitar cambios. El anteproyecto ya
fija dos reglas: todo cambio de esquema es una migración nueva, y nunca se
edita una migración que ya está en `main`.

En el Sprint 2 aparecieron tres situaciones que esas reglas no resolvían:

1. Dos ramas crearon al mismo tiempo una migración `V4`
   (`V4__datos_semilla_usuarios` y `V4__agregar_tipo_servidor`). Si las dos
   llegaban a `main`, Flyway fallaba al arrancar por versión duplicada.
2. Al renumerar una de ellas a `V5`, su rama quedó con un hueco (V1, V2, V3,
   V5) y con una prueba que esperaba una `V4` que aún no existía en `main`.
3. La semilla de usuarios de prueba incluye un ADMIN con una contraseña
   escrita en el repositorio. Sirve para desarrollo y para la demo, pero una
   migración se ejecuta en todos los entornos por igual.

## Decisión

1. **El esquema solo cambia con migraciones.** Hibernate se mantiene en
   `ddl-auto: validate`: verifica que las entidades coincidan con las
   tablas, pero nunca las modifica.
2. **Una migración integrada en `main` no se edita.** Para corregirla se
   crea una migración nueva.
3. **Los números son consecutivos y se confirman al integrar.** Antes de
   crear una migración se revisa el número más alto en `main` y en los Pull
   Requests abiertos, y se avisa en el canal del equipo cuál se va a usar.
   Si dos ramas coinciden, la que se integra segunda se renumera antes del
   merge.
4. **No se integran huecos.** Un Pull Request con la migración `V(n)` espera
   a que `V(n-1)` esté en `main`. Flyway, por defecto, rechaza aplicar una
   versión menor a la última ya aplicada, así que integrar fuera de orden
   rompería las bases que ya existen (las locales y la del entorno de
   pruebas).
5. **Una migración por Pull Request**, con nombre descriptivo:
   `V<n>__<verbo>_<objeto>.sql`.
6. **Las pruebas no dependen del número de migraciones.** Verifican que
   todas se aplicaron con éxito, no que sean exactamente "1, 2 y 3".
7. **Datos de referencia sí, credenciales no.** Los catálogos que el
   sistema necesita para funcionar (por ejemplo `politica_sla`) van en
   migraciones. Los usuarios de prueba con contraseña conocida se aceptan
   solo para desarrollo y demostración, y deben salir de la ruta común de
   migraciones antes del primer despliegue al entorno de pruebas.
8. **El administrador real no se siembra.** Se crea al arrancar a partir de
   las variables de entorno `ADMIN_EMAIL` y `ADMIN_PASSWORD`, de modo que su
   contraseña nunca está en el repositorio.
9. **Revisión.** Todo Pull Request que cambie el esquema lo aprueba Ulises;
   Sashid valida que encaje con la arquitectura (matriz RACI).

## Alternativas consideradas

1. **Versionar con fecha y hora** (`V20261116_1930__...`): evita que dos
   ramas elijan el mismo número, pero no evita el problema de orden y hace
   más difícil leer la secuencia. Descartada por ahora.
2. **Activar `outOfOrder` en Flyway:** permite aplicar versiones atrasadas,
   pero entonces cada base puede haber ejecutado las migraciones en un orden
   distinto y un error deja de ser reproducible. Descartada.
3. **Dejar que Hibernate genere el esquema** (`ddl-auto: update`): cómodo al
   inicio, pero sin historial, sin revisión y con riesgo de perder datos.
   Descartada desde el Sprint 0.
4. **Numeración consecutiva confirmada al integrar (elegida):** la más
   simple para un equipo de seis con ramas cortas.

## Consecuencias

**Positivas**
- Cualquier base (local, CI o entorno de pruebas) aplica las mismas
  migraciones en el mismo orden.
- El choque de números se detecta en la revisión y no al arrancar.
- La contraseña del administrador no está en el historial de Git.

**Negativas / riesgos**
- La regla del número depende de que el equipo avise; si alguien no lo
  hace, el choque aparece hasta el Pull Request. El CI lo detecta, porque
  la aplicación no arranca con dos versiones iguales.
- Un Pull Request puede quedar en espera de otro por el orden de las
  migraciones, lo que alarga su vida más allá de los 3 días recomendados.
- Queda pendiente mover la semilla de usuarios de prueba a una ubicación
  exclusiva de desarrollo antes del despliegue (Sprint 5).