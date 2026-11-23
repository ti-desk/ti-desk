package mx.edu.tidesk.ti_desk;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

/**
 * Base para las pruebas de API de tickets: crea usuarios con su rol, inicia sesión
 * con el login real y hace las peticiones HTTP con el token.
 *
 * La base se comparte entre clases de prueba: cada prueba crea sus propios usuarios
 * y tickets y solo revisa esos, nunca cuenta el total de una tabla.
 */
public abstract class ApiIntegracionTest extends IntegracionBaseTest {

    protected static final String TICKETS = "/api/v1/tickets";
    protected static final String PROBLEM_JSON = "application/problem+json";
    private static final String CONTRASENA = "Segura#2026";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    protected int puerto;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Usuario con sesión iniciada. */
    protected record Sesion(long id, String token) {
    }

    /** Registra un usuario, le asigna el rol en la base e inicia sesión. */
    protected Sesion nuevaSesion(String rol) throws Exception {
        String correo = "qa." + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "@unistmo.edu.mx";
        HttpResponse<String> registro = post("/api/v1/auth/registro", """
                {"nombre":"Prueba QA","correo":"%s","contrasena":"%s"}
                """.formatted(correo, CONTRASENA), null);
        assertThat(registro.statusCode()).as("registro previo").isEqualTo(201);
        jdbc.update("UPDATE usuario SET rol = ? WHERE email = ?", rol, correo);

        HttpResponse<String> login = post("/api/v1/auth/login", """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, CONTRASENA), null);
        assertThat(login.statusCode()).as("login previo").isEqualTo(200);
        return new Sesion(numero(registro, "$.id"), JsonPath.read(login.body(), "$.token"));
    }

    /** Crea un ticket con el endpoint real (RF-04) y devuelve su id. */
    protected long nuevoTicket(Sesion solicitante) throws Exception {
        HttpResponse<String> respuesta = post(TICKETS, """
                {"titulo":"Sin red en el laboratorio","descripcion":"No hay conexión desde las 9:00",
                 "categoria":"RED","prioridad":"ALTA"}
                """, solicitante.token());
        assertThat(respuesta.statusCode()).as("ticket creado").isEqualTo(201);
        return numero(respuesta, "$.id");
    }

    /** PATCH /asignacion. Con {@code tecnicoId} null el técnico se autoasigna. */
    protected HttpResponse<String> asignar(long ticketId, Long tecnicoId, Sesion quien) throws Exception {
        String cuerpo = tecnicoId == null ? "{}" : "{\"tecnicoId\":" + tecnicoId + "}";
        return patch(TICKETS + "/" + ticketId + "/asignacion", cuerpo, quien.token());
    }

    protected HttpResponse<String> cambiarEstado(long ticketId, String estado, Sesion quien) throws Exception {
        return cambiarEstado(ticketId, estado, null, quien);
    }

    protected HttpResponse<String> cambiarEstado(long ticketId, String estado, String motivo, Sesion quien)
            throws Exception {
        String cuerpo = motivo == null
                ? "{\"estado\":\"" + estado + "\"}"
                : "{\"estado\":\"" + estado + "\",\"motivo\":\"" + motivo + "\"}";
        return patch(TICKETS + "/" + ticketId + "/estado", cuerpo, quien.token());
    }

    protected HttpResponse<String> detalle(long ticketId, Sesion quien) throws Exception {
        return get(TICKETS + "/" + ticketId, quien.token());
    }

    /** Estados del historial, del más antiguo al más reciente, según el detalle. */
    protected List<String> historialDe(long ticketId, Sesion quien) throws Exception {
        HttpResponse<String> respuesta = detalle(ticketId, quien);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return JsonPath.read(respuesta.body(), "$.historial[*].estadoNuevo");
    }

    protected String estadoEnBase(long ticketId) {
        return jdbc.queryForObject("SELECT estado FROM ticket WHERE id = ?", String.class, ticketId);
    }

    protected static long numero(HttpResponse<String> respuesta, String ruta) {
        return ((Number) JsonPath.read(respuesta.body(), ruta)).longValue();
    }

    protected static String tipoDeContenido(HttpResponse<String> respuesta) {
        return respuesta.headers().firstValue("Content-Type").orElse("");
    }

    protected HttpResponse<String> get(String ruta, String token) throws Exception {
        return enviar(peticion(ruta, token).GET());
    }

    protected HttpResponse<String> post(String ruta, String json, String token) throws Exception {
        return enviar(peticion(ruta, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    protected HttpResponse<String> patch(String ruta, String json, String token) throws Exception {
        return enviar(peticion(ruta, token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    private HttpRequest.Builder peticion(String ruta, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:" + puerto + ruta));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private HttpResponse<String> enviar(HttpRequest.Builder builder) throws Exception {
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
