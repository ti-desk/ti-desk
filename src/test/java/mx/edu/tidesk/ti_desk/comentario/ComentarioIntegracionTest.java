package mx.edu.tidesk.ti_desk.comentario;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import mx.edu.tidesk.ti_desk.ApiIntegracionTest;

/**
 * Pruebas de integración de RF-07 (comentarios): visibilidad de los comentarios
 * públicos e internos por rol, en la lista y en el detalle del ticket, con
 * PostgreSQL real y tokens reales del login.
 */
class ComentarioIntegracionTest extends ApiIntegracionTest {

    private static String comentarios(long ticketId) {
        return TICKETS + "/" + ticketId + "/comentarios";
    }

    private HttpResponse<String> comentar(long ticketId, String texto, boolean interno, Sesion quien)
            throws Exception {
        return post(comentarios(ticketId),
                "{\"texto\":\"" + texto + "\",\"interno\":" + interno + "}", quien.token());
    }

    private List<String> textosVistosPor(long ticketId, Sesion quien) throws Exception {
        HttpResponse<String> respuesta = get(comentarios(ticketId), quien.token());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return JsonPath.read(respuesta.body(), "$[*].texto");
    }

    private List<String> textosEnElDetalle(long ticketId, Sesion quien) throws Exception {
        HttpResponse<String> respuesta = detalle(ticketId, quien);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return JsonPath.read(respuesta.body(), "$.comentarios[*].texto");
    }

    @Test
    void cp020ElComentarioPublicoLoVenTodosLosQueTienenAccesoAlTicket() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);

        HttpResponse<String> respuesta = comentar(id, "Voy en camino al laboratorio", false, tecnico);

        assertThat(respuesta.statusCode()).isEqualTo(201);
        assertThat(numero(respuesta, "$.autorId")).isEqualTo(tecnico.id());
        assertThat(numero(respuesta, "$.ticketId")).isEqualTo(id);
        assertThat(JsonPath.<Boolean>read(respuesta.body(), "$.interno")).isFalse();
        for (Sesion quien : List.of(ana, tecnico, admin)) {
            assertThat(textosVistosPor(id, quien)).containsExactly("Voy en camino al laboratorio");
            assertThat(textosEnElDetalle(id, quien)).containsExactly("Voy en camino al laboratorio");
        }
    }

    @Test
    void cp020LosComentariosSalenEnOrdenDeCreacion() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);

        comentar(id, "uno", false, ana);
        comentar(id, "dos", false, tecnico);
        comentar(id, "tres", false, ana);

        assertThat(textosVistosPor(id, ana)).containsExactly("uno", "dos", "tres");
    }

    @Test
    void cp021ElComentarioInternoNoLoVeElUsuarioNiEnLaListaNiEnElDetalle() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);

        assertThat(comentar(id, "Parece falla del switch del piso 2", true, tecnico).statusCode()).isEqualTo(201);
        assertThat(comentar(id, "Ya revisamos tu equipo", false, tecnico).statusCode()).isEqualTo(201);

        assertThat(textosVistosPor(id, ana)).containsExactly("Ya revisamos tu equipo");
        assertThat(textosEnElDetalle(id, ana)).containsExactly("Ya revisamos tu equipo");
        assertThat(textosVistosPor(id, tecnico))
                .containsExactly("Parece falla del switch del piso 2", "Ya revisamos tu equipo");
        assertThat(textosEnElDetalle(id, admin))
                .containsExactly("Parece falla del switch del piso 2", "Ya revisamos tu equipo");
    }

    @Test
    void cp021ElUsuarioNoPuedeEscribirComentariosInternos() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion admin = nuevaSesion("ADMIN");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = comentar(id, "Quiero que sea secreto", true, ana);

        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(textosVistosPor(id, admin)).as("no se guardó").isEmpty();
    }

    @Test
    void cp050ElUsuarioRecibe404AlComentarOListarUnTicketAjeno() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion beto = nuevaSesion("USUARIO");
        Sesion admin = nuevaSesion("ADMIN");
        long deAna = nuevoTicket(ana);

        HttpResponse<String> comentarAjeno = comentar(deAna, "Hola", false, beto);
        HttpResponse<String> listarAjeno = get(comentarios(deAna), beto.token());

        assertThat(comentarAjeno.statusCode()).as("oculto para el rol, no 403").isEqualTo(404);
        assertThat(listarAjeno.statusCode()).isEqualTo(404);
        assertThat(tipoDeContenido(comentarAjeno)).startsWith(PROBLEM_JSON);
        assertThat(textosVistosPor(deAna, admin)).as("no se guardó").isEmpty();
    }

    @Test
    void cp050ElTecnicoRecibe404ConLosComentariosDeUnTicketDeOtroTecnico() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, otroTecnico);

        assertThat(comentar(id, "Hola", true, tecnico).statusCode()).isEqualTo(404);
        assertThat(get(comentarios(id), tecnico.token()).statusCode()).isEqualTo(404);
    }

    @Test
    void comentarioVacioResponde400ConDetallePorCampo() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        long id = nuevoTicket(ana);

        HttpResponse<String> respuesta = post(comentarios(id), "{\"texto\":\"   \"}", ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.errores.texto")).isNotBlank();
    }

    @Test
    void noSePuedeComentarUnTicketCancelado() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long id = nuevoTicket(ana);
        asignar(id, null, tecnico);
        assertThat(cambiarEstado(id, "CANCELADO", ana).statusCode()).isEqualTo(200);

        HttpResponse<String> respuesta = comentar(id, "¿Sigue fallando?", false, tecnico);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
    }

    @Test
    void comentariosDeUnTicketInexistenteYSinTokenResponden404Y401() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        long id = nuevoTicket(nuevaSesion("USUARIO"));

        assertThat(get(comentarios(999_999_999L), admin.token()).statusCode()).isEqualTo(404);
        assertThat(get(comentarios(id), null).statusCode()).isEqualTo(401);
        assertThat(post(comentarios(id), "{\"texto\":\"hola\"}", null).statusCode()).isEqualTo(401);
    }
}
