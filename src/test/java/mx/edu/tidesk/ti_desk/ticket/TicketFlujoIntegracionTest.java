package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import mx.edu.tidesk.ti_desk.ApiIntegracionTest;

/**
 * Pruebas de integración de RF-06 (asignación), RF-08 (máquina de estados con
 * historial) y el detalle del ticket (GET /api/v1/tickets/{id}), con PostgreSQL
 * real y tokens reales del login.
 */
class TicketFlujoIntegracionTest extends ApiIntegracionTest {

    // ------------------------------------------------ CP-016 Detalle por rol

    @Test
    void cp016ElUsuarioRecibe404AlPedirElDetalleDeUnTicketAjeno() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion beto = nuevaSesion("USUARIO");
        long deAna = nuevoTicket(ana);

        HttpResponse<String> ajeno = detalle(deAna, beto);
        HttpResponse<String> propio = detalle(deAna, ana);

        assertThat(ajeno.statusCode()).as("oculto para el rol, no 403").isEqualTo(404);
        assertThat(tipoDeContenido(ajeno)).startsWith(PROBLEM_JSON);
        assertThat(propio.statusCode()).isEqualTo(200);
        assertThat(numero(propio, "$.ticket.id")).isEqualTo(deAna);
    }

    @Test
    void cp016ElTecnicoNoVeElDetalleDeUnTicketDeOtroTecnicoPeroSiUnoSinAsignar() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long sinAsignar = nuevoTicket(ana);
        long deOtro = nuevoTicket(ana);
        assertThat(asignar(deOtro, null, otroTecnico).statusCode()).isEqualTo(200);

        assertThat(detalle(sinAsignar, tecnico).statusCode()).isEqualTo(200);
        assertThat(detalle(deOtro, tecnico).statusCode()).isEqualTo(404);
    }

    @Test
    void detalleDeUnTicketNuevoTraeSuCreacionEnElHistorial() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = detalle(id, ana);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(respuesta.body(), "$.historial[*].estadoNuevo"))
                .containsExactly("ABIERTO");
        assertThat((Object) JsonPath.read(respuesta.body(), "$.historial[0].estadoAnterior")).isNull();
        assertThat(numero(respuesta, "$.historial[0].usuarioId")).isEqualTo(ana.id());
        assertThat(JsonPath.<List<?>>read(respuesta.body(), "$.comentarios")).isEmpty();
    }

    // ------------------------------------------------------ RF-06 Asignación

    @Test
    void cp017ElAdminAsignaUnTecnicoYQuedaEnElHistorialConSuMotivo() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = patch(TICKETS + "/" + id + "/asignacion",
                "{\"tecnicoId\":" + tecnico.id() + ",\"motivo\":\"Le toca el edificio B\"}", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.estado")).isEqualTo("ASIGNADO");
        assertThat(numero(respuesta, "$.tecnicoId")).isEqualTo(tecnico.id());
        assertThat(jdbc.queryForObject("SELECT asignado_en IS NOT NULL FROM ticket WHERE id = ?",
                Boolean.class, id)).isTrue();

        HttpResponse<String> conHistorial = detalle(id, admin);
        assertThat(JsonPath.<List<String>>read(conHistorial.body(), "$.historial[*].estadoNuevo"))
                .containsExactly("ABIERTO", "ASIGNADO");
        assertThat(JsonPath.<String>read(conHistorial.body(), "$.historial[1].estadoAnterior")).isEqualTo("ABIERTO");
        assertThat(JsonPath.<String>read(conHistorial.body(), "$.historial[1].motivo"))
                .isEqualTo("Le toca el edificio B");
        assertThat(numero(conHistorial, "$.historial[1].usuarioId")).as("lo registró el admin").isEqualTo(admin.id());
    }

    @Test
    void cp017LaReasignacionDelAdminTambienQuedaEnElHistorial() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        assertThat(asignar(id, tecnico.id(), admin).statusCode()).isEqualTo(200);

        HttpResponse<String> respuesta = asignar(id, otroTecnico.id(), admin);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(numero(respuesta, "$.tecnicoId")).isEqualTo(otroTecnico.id());
        assertThat(historialDe(id, admin)).containsExactly("ABIERTO", "ASIGNADO", "ASIGNADO");
    }

    @Test
    void cp018ElTecnicoSeAutoasignaSinMandarCuerpo() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = asignar(id, null, tecnico);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.estado")).isEqualTo("ASIGNADO");
        assertThat(numero(respuesta, "$.tecnicoId")).isEqualTo(tecnico.id());
        assertThat(historialDe(id, tecnico)).containsExactly("ABIERTO", "ASIGNADO");
    }

    @Test
    void asignacionNegadaAlUsuarioYAlTecnicoQueAsignaAOtro() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);

        HttpResponse<String> usuario = asignar(id, tecnico.id(), ana);
        HttpResponse<String> tecnicoAOtro = asignar(id, otroTecnico.id(), tecnico);

        assertThat(usuario.statusCode()).isEqualTo(403);
        assertThat(tecnicoAOtro.statusCode()).isEqualTo(403);
        assertThat(tipoDeContenido(tecnicoAOtro)).startsWith(PROBLEM_JSON);
        assertThat(estadoEnBase(id)).as("nada cambió").isEqualTo("ABIERTO");
    }

    @Test
    void asignarAUnUsuarioQueNoEsTecnicoResponde400() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = asignar(id, ana.id(), admin);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(estadoEnBase(id)).isEqualTo("ABIERTO");
    }

    @Test
    void asignarUnTicketInexistenteResponde404() throws Exception {
        Sesion tecnico = nuevaSesion("TECNICO");

        assertThat(asignar(999_999_999L, null, tecnico).statusCode()).isEqualTo(404);
    }

    /**
     * CP-019: cuatro técnicos toman el mismo ticket al mismo tiempo. Gana uno solo;
     * los demás reciben 409 (nunca 404: el ticket existe y lo acababan de ver).
     * Se repite porque un error de concurrencia no aparece en todas las ejecuciones.
     */
    @RepeatedTest(5)
    void cp019DosOMasTecnicosTomanElMismoTicketAlMismoTiempoYSoloUnoGana() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);
        List<Sesion> tecnicos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tecnicos.add(nuevaSesion("TECNICO"));
        }

        List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
        for (Sesion tecnico : tecnicos) {
            tareas.add(() -> asignar(id, null, tecnico));
        }
        List<HttpResponse<String>> respuestas = new ArrayList<>();
        ExecutorService hilos = Executors.newFixedThreadPool(tecnicos.size());
        try {
            for (Future<HttpResponse<String>> futuro : hilos.invokeAll(tareas)) {
                respuestas.add(futuro.get());
            }
        } finally {
            hilos.shutdown();
        }

        List<HttpResponse<String>> ganadores = respuestas.stream()
                .filter(r -> r.statusCode() == 200).toList();
        assertThat(ganadores).as("gana exactamente uno").hasSize(1);
        assertThat(respuestas).filteredOn(r -> r.statusCode() != 200)
                .as("los demás reciben 409 con ProblemDetail")
                .hasSize(tecnicos.size() - 1)
                .allSatisfy(r -> {
                    assertThat(r.statusCode()).isEqualTo(409);
                    assertThat(tipoDeContenido(r)).startsWith(PROBLEM_JSON);
                });

        long ganador = numero(ganadores.get(0), "$.tecnicoId");
        assertThat(jdbc.queryForObject("SELECT tecnico_id FROM ticket WHERE id = ?", Long.class, id))
                .as("el ticket se queda con el ganador").isEqualTo(ganador);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historial_estado WHERE ticket_id = ? AND estado_nuevo = 'ASIGNADO'",
                Integer.class, id)).as("una sola asignación en el historial").isEqualTo(1);
    }

    @Test
    void cp019ElTecnicoQueLlegaTardeRecibe409ConElMotivo() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion tarde = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        assertThat(asignar(id, null, tecnico).statusCode()).isEqualTo(200);

        HttpResponse<String> respuesta = asignar(id, null, tarde);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.detail")).contains("otro técnico");
    }

    // ------------------------------------------ RF-08 Máquina de estados

    /**
     * CP-022: recorre las transiciones permitidas con quien tiene permiso para cada
     * una, y revisa el historial completo y las fechas que guarda el ticket.
     */
    @Test
    void cp022FlujoCompletoConReaperturaYCierreDejaTodoEnElHistorial() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);

        assertThat(asignar(id, null, tecnico).statusCode()).isEqualTo(200);
        assertThat(cambiarEstado(id, "EN_PROCESO", tecnico).statusCode()).isEqualTo(200);
        assertThat(cambiarEstado(id, "EN_ESPERA", "Falta que el usuario reinicie el equipo", tecnico)
                .statusCode()).isEqualTo(200);
        assertThat(cambiarEstado(id, "EN_PROCESO", ana).statusCode()).as("el solicitante responde").isEqualTo(200);
        assertThat(cambiarEstado(id, "RESUELTO", tecnico).statusCode()).isEqualTo(200);
        assertThat(resueltoEn(id)).isTrue();

        assertThat(cambiarEstado(id, "EN_PROCESO", ana).statusCode()).as("el solicitante reabre").isEqualTo(200);
        assertThat(resueltoEn(id)).as("al reabrir deja de contar como resuelto").isFalse();

        assertThat(cambiarEstado(id, "RESUELTO", tecnico).statusCode()).isEqualTo(200);
        HttpResponse<String> cierre = cambiarEstado(id, "CERRADO", ana);
        assertThat(cierre.statusCode()).as("el solicitante confirma el cierre").isEqualTo(200);
        assertThat(JsonPath.<String>read(cierre.body(), "$.estado")).isEqualTo("CERRADO");

        HttpResponse<String> respuesta = detalle(id, ana);
        assertThat(JsonPath.<List<String>>read(respuesta.body(), "$.historial[*].estadoNuevo")).containsExactly(
                "ABIERTO", "ASIGNADO", "EN_PROCESO", "EN_ESPERA", "EN_PROCESO",
                "RESUELTO", "EN_PROCESO", "RESUELTO", "CERRADO");
        assertThat(JsonPath.<List<String>>read(respuesta.body(), "$.historial[*].estadoAnterior")).containsExactly(
                null, "ABIERTO", "ASIGNADO", "EN_PROCESO", "EN_ESPERA",
                "EN_PROCESO", "RESUELTO", "EN_PROCESO", "RESUELTO");
        assertThat(JsonPath.<String>read(respuesta.body(), "$.historial[3].motivo"))
                .isEqualTo("Falta que el usuario reinicie el equipo");
        assertThat(numero(respuesta, "$.historial[8].usuarioId")).as("cerró la solicitante").isEqualTo(ana.id());
        assertThat(jdbc.queryForObject("SELECT cerrado_en IS NOT NULL AND resuelto_en IS NOT NULL FROM ticket WHERE id = ?",
                Boolean.class, id)).isTrue();
    }

    @Test
    void cp022ElSolicitanteCancelaUnTicketAbierto() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = cambiarEstado(id, "CANCELADO", "Ya se resolvió solo", ana);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(historialDe(id, ana)).containsExactly("ABIERTO", "CANCELADO");
    }

    @Test
    void cp022ElTecnicoAsignadoCancelaUnTicketAsignado() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        assertThat(asignar(id, null, tecnico).statusCode()).isEqualTo(200);

        assertThat(cambiarEstado(id, "CANCELADO", tecnico).statusCode()).isEqualTo(200);
        assertThat(historialDe(id, tecnico)).containsExactly("ABIERTO", "ASIGNADO", "CANCELADO");
    }

    @Test
    void cp023TransicionNoPermitidaResponde409ConLaReglaYNoCambiaNada() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = cambiarEstado(id, "RESUELTO", admin);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.estadoActual")).isEqualTo("ABIERTO");
        assertThat(JsonPath.<String>read(respuesta.body(), "$.estadoSolicitado")).isEqualTo("RESUELTO");
        assertThat(JsonPath.<String>read(respuesta.body(), "$.detail"))
                .as("indica la regla violada").contains("ABIERTO").contains("ASIGNADO");
        assertThat(estadoEnBase(id)).isEqualTo("ABIERTO");
        assertThat(historialDe(id, admin)).as("no se registra nada").containsExactly("ABIERTO");
    }

    @Test
    void cp023PasarAAsignadoPorElEndpointDeEstadoResponde409() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = cambiarEstado(id, "ASIGNADO", admin);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.detail")).contains("/asignacion");
    }

    @Test
    void cp023EstadoInexistenteResponde400() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        long id = nuevoTicket(nuevaSesion("USUARIO"));

        assertThat(cambiarEstado(id, "PERDIDO", admin).statusCode()).isEqualTo(400);
        assertThat(estadoEnBase(id)).isEqualTo("ABIERTO");
    }

    @Test
    void cp024UnTicketCerradoNoAdmiteNingunaTransicionNiSiquieraDelAdmin() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);
        cambiarEstado(id, "EN_PROCESO", tecnico);
        cambiarEstado(id, "RESUELTO", tecnico);
        assertThat(cambiarEstado(id, "CERRADO", ana).statusCode()).isEqualTo(200);

        for (String destino : List.of("ABIERTO", "EN_PROCESO", "EN_ESPERA", "RESUELTO", "CANCELADO", "CERRADO")) {
            assertThat(cambiarEstado(id, destino, admin).statusCode())
                    .as("CERRADO → %s", destino).isEqualTo(409);
        }
        assertThat(asignar(id, tecnico.id(), admin).statusCode()).as("tampoco se reasigna").isEqualTo(409);
        assertThat(estadoEnBase(id)).isEqualTo("CERRADO");
    }

    @Test
    void cp024UnTicketCanceladoNoAdmiteNingunaTransicionNiSiquieraDelAdmin() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        assertThat(cambiarEstado(id, "CANCELADO", ana).statusCode()).isEqualTo(200);

        for (String destino : List.of("ABIERTO", "EN_PROCESO", "EN_ESPERA", "RESUELTO", "CERRADO", "CANCELADO")) {
            assertThat(cambiarEstado(id, destino, admin).statusCode())
                    .as("CANCELADO → %s", destino).isEqualTo(409);
        }
        assertThat(asignar(id, tecnico.id(), admin).statusCode()).as("tampoco se asigna").isEqualTo(409);
        assertThat(asignar(id, null, tecnico).statusCode()).as("ni se autoasigna").isEqualTo(409);
        assertThat(estadoEnBase(id)).isEqualTo("CANCELADO");
    }

    /**
     * CP-025: si el ticket es de otro técnico, para él está oculto (404, regla 7.3);
     * si no tiene técnico, lo ve pero no puede trabajarlo sin asignárselo (403).
     */
    @Test
    void cp025ElTecnicoNoAsignadoNoPuedeCambiarElEstado() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long deOtro = nuevoTicket(ana);
        long sinAsignar = nuevoTicket(ana);
        assertThat(asignar(deOtro, null, otroTecnico).statusCode()).isEqualTo(200);

        HttpResponse<String> ajeno = cambiarEstado(deOtro, "EN_PROCESO", tecnico);
        HttpResponse<String> libre = cambiarEstado(sinAsignar, "EN_PROCESO", tecnico);

        assertThat(ajeno.statusCode()).as("ticket de otro técnico: oculto").isEqualTo(404);
        assertThat(libre.statusCode()).as("ticket sin técnico: visible pero sin permiso").isEqualTo(403);
        assertThat(tipoDeContenido(libre)).startsWith(PROBLEM_JSON);
        assertThat(estadoEnBase(deOtro)).isEqualTo("ASIGNADO");
        assertThat(estadoEnBase(sinAsignar)).isEqualTo("ABIERTO");
    }

    @Test
    void cp025ElSolicitanteNoPuedeResolverSuPropioTicket() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);
        assertThat(cambiarEstado(id, "EN_PROCESO", tecnico).statusCode()).isEqualTo(200);

        assertThat(cambiarEstado(id, "RESUELTO", ana).statusCode()).isEqualTo(403);
        assertThat(cambiarEstado(id, "EN_ESPERA", ana).statusCode()).isEqualTo(403);
        assertThat(estadoEnBase(id)).isEqualTo("EN_PROCESO");
    }

    @Test
    void cambiarEstadoSinTokenResponde401() throws Exception {
        long id = nuevoTicket(nuevaSesion("USUARIO"));

        assertThat(patch(TICKETS + "/" + id + "/estado", "{\"estado\":\"CANCELADO\"}", null).statusCode())
                .isEqualTo(401);
        assertThat(estadoEnBase(id)).isEqualTo("ABIERTO");
    }

    private Boolean resueltoEn(long id) {
        return jdbc.queryForObject("SELECT resuelto_en IS NOT NULL FROM ticket WHERE id = ?", Boolean.class, id);
    }
}
