package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import mx.edu.tidesk.ti_desk.IntegracionBaseTest;

/**
 * Pruebas de integración de RF-04 (crear ticket) y RF-05 (listar tickets).
 * Levantan la aplicación completa con PostgreSQL 16 real (Testcontainers) y usan
 * tokens reales del login, así que prueban juntos el filtro JWT, el controlador,
 * el servicio, las Specifications y la base de datos.
 *
 * La base se comparte con las demás pruebas de integración: cada prueba crea sus
 * propios usuarios y tickets y solo revisa esos (por id o por técnico), nunca
 * cuenta el total de la tabla.
 */
class TicketIntegracionTest extends IntegracionBaseTest {

    private static final String CONTRASENA = "Segura#2026";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String TICKETS = "/api/v1/tickets";
    /** Página grande y lo más nuevo primero: los tickets de cada prueba quedan en la primera página. */
    private static final String PAGINA_RECIENTES = "?size=100&sort=creadoEn,desc";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    // ------------------------------------------------------- RF-04 Crear ticket

    @Test
    void cp010CrearTicketResponde201ConLocationFolioYFechaLimiteDeSla() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Instant antes = Instant.now();

        HttpResponse<String> respuesta = crearTicket(ana, "ALTA");

        assertThat(respuesta.statusCode()).isEqualTo(201);
        long id = idDe(respuesta);
        assertThat(respuesta.headers().firstValue("Location"))
                .hasValueSatisfying(ubicacion -> assertThat(ubicacion).endsWith(TICKETS + "/" + id));
        assertThat(JsonPath.<String>read(respuesta.body(), "$.folio")).matches("TID-\\d{4}-\\d{5}");
        assertThat(JsonPath.<String>read(respuesta.body(), "$.estado")).isEqualTo("ABIERTO");
        assertThat(((Number) JsonPath.read(respuesta.body(), "$.solicitanteId")).longValue())
                .as("el solicitante sale del token, no del cuerpo")
                .isEqualTo(ana.id());
        assertThat((Object) JsonPath.read(respuesta.body(), "$.tecnicoId")).isNull();

        // ALTA = 24 h de resolución (V3). Se lee de la base, que es la fuente de verdad.
        Instant creado = jdbc.queryForObject(
                "SELECT creado_en FROM ticket WHERE id = ?", java.sql.Timestamp.class, id).toInstant();
        Instant limite = jdbc.queryForObject(
                "SELECT fecha_limite_sla FROM ticket WHERE id = ?", java.sql.Timestamp.class, id).toInstant();
        assertThat(Duration.between(creado, limite)).isEqualTo(Duration.ofHours(24));
        assertThat(creado).isBetween(antes.minusSeconds(5), Instant.now().plusSeconds(5));
    }

    @Test
    void cp010CrearTicketConUnActivoExistenteLoVincula() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        long activoId = nuevoActivo();

        HttpResponse<String> respuesta = post(TICKETS, cuerpoTicket("MEDIA", activoId), ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT activo_id FROM ticket WHERE id = ?",
                Long.class, idDe(respuesta))).isEqualTo(activoId);
    }

    @Test
    void cp011CrearTicketSinTituloResponde400ConDetallePorCampoYNoGuardaNada() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = post(TICKETS, """
                {"titulo":"  ","descripcion":"No hay red","categoria":"RED","prioridad":"ALTA"}
                """, ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.errores.titulo")).isNotBlank();
        assertThat(ticketsDe(ana)).as("un 400 no debe crear el ticket").isZero();
    }

    @Test
    void cp011CrearTicketConPrioridadInexistenteResponde400() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = post(TICKETS, """
                {"titulo":"Sin red","descripcion":"No hay red","categoria":"RED","prioridad":"URGENTISIMA"}
                """, ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(ticketsDe(ana)).isZero();
    }

    @Test
    void crearTicketConActivoInexistenteResponde404YNoGuardaNada() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = post(TICKETS, cuerpoTicket("BAJA", 999_999_999L), ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(ticketsDe(ana)).isZero();
    }

    @Test
    void cp041CrearYListarSinTokenResponden401() throws Exception {
        HttpResponse<String> crear = post(TICKETS, cuerpoTicket("BAJA", null), null);
        HttpResponse<String> listar = get(TICKETS, null);

        assertThat(crear.statusCode()).isEqualTo(401);
        assertThat(listar.statusCode()).isEqualTo(401);
        assertThat(tipoDeContenido(crear)).startsWith(PROBLEM_JSON);
    }

    @Test
    void cp012FoliosConsecutivosYUnicosAunqueSeCreenAlMismoTiempo() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        int simultaneos = 10;

        List<Callable<HttpResponse<String>>> tareas = new ArrayList<>();
        for (int i = 0; i < simultaneos; i++) {
            tareas.add(() -> crearTicket(ana, "MEDIA"));
        }
        List<HttpResponse<String>> respuestas = new ArrayList<>();
        ExecutorService hilos = Executors.newFixedThreadPool(simultaneos);
        try {
            for (Future<HttpResponse<String>> futuro : hilos.invokeAll(tareas)) {
                respuestas.add(futuro.get());
            }
        } finally {
            hilos.shutdown();
        }

        Set<String> folios = new HashSet<>();
        List<Integer> consecutivos = new ArrayList<>();
        for (HttpResponse<String> respuesta : respuestas) {
            assertThat(respuesta.statusCode()).isEqualTo(201);
            String folio = JsonPath.read(respuesta.body(), "$.folio");
            folios.add(folio);
            consecutivos.add(Integer.parseInt(folio.substring(folio.lastIndexOf('-') + 1)));
        }
        assertThat(folios).as("ningún folio repetido").hasSize(simultaneos);
        consecutivos.sort(null);
        assertThat(consecutivos.get(simultaneos - 1) - consecutivos.get(0))
                .as("los %d números son consecutivos, sin huecos", simultaneos)
                .isEqualTo(simultaneos - 1);
    }

    // ---------------------------------------------- RF-05 Visibilidad por rol

    @Test
    void cp013ElUsuarioSoloVeLosTicketsQueCreo() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion beto = nuevaSesion("USUARIO");
        long deAna = idDe(crearTicket(ana, "MEDIA"));
        long deBeto = idDe(crearTicket(beto, "MEDIA"));

        HttpResponse<String> respuesta = get(TICKETS + PAGINA_RECIENTES, ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(idsDe(respuesta)).contains(deAna).doesNotContain(deBeto);
        assertThat(JsonPath.<List<Number>>read(respuesta.body(), "$.contenido[*].solicitanteId"))
                .as("todo lo que ve Ana es suyo")
                .allSatisfy(solicitante -> assertThat(solicitante.longValue()).isEqualTo(ana.id()));
    }

    @Test
    void cp013ElUsuarioNoVeTicketsAjenosNiFiltrandoPorTecnico() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion beto = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long deBeto = idDe(crearTicket(beto, "ALTA"));
        asignar(deBeto, tecnico);

        HttpResponse<String> respuesta = get(TICKETS + "?tecnicoId=" + tecnico.id(), ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(idsDe(respuesta)).doesNotContain(deBeto);
    }

    @Test
    void cp014ElTecnicoVeLosSuyosYLosSinAsignarPeroNoLosDeOtroTecnico() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        Sesion otroTecnico = nuevaSesion("TECNICO");
        long suyo = idDe(crearTicket(ana, "ALTA"));
        long sinAsignar = idDe(crearTicket(ana, "MEDIA"));
        long deOtro = idDe(crearTicket(ana, "BAJA"));
        asignar(suyo, tecnico);
        asignar(deOtro, otroTecnico);

        HttpResponse<String> respuesta = get(TICKETS + PAGINA_RECIENTES, tecnico.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(idsDe(respuesta)).contains(suyo, sinAsignar).doesNotContain(deOtro);
    }

    @Test
    void cp015ElAdminVeTodoYFiltraPorTecnicoConPaginacion() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        List<Long> delTecnico = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long id = idDe(crearTicket(ana, "MEDIA"));
            asignar(id, tecnico);
            delTecnico.add(id);
        }
        idDe(crearTicket(ana, "MEDIA")); // de Ana pero sin técnico: no debe salir con el filtro

        String ruta = TICKETS + "?tecnicoId=" + tecnico.id() + "&size=2&sort=folio,asc";
        HttpResponse<String> pagina0 = get(ruta + "&page=0", admin.token());
        HttpResponse<String> pagina1 = get(ruta + "&page=1", admin.token());

        assertThat(pagina0.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(pagina0.body(), "$.totalElementos")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(pagina0.body(), "$.totalPaginas")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(pagina0.body(), "$.tamano")).isEqualTo(2);
        List<Long> recorridos = new ArrayList<>(idsDe(pagina0));
        recorridos.addAll(idsDe(pagina1));
        assertThat(recorridos).as("las dos páginas juntas, en orden y sin repetir")
                .containsExactlyElementsOf(delTecnico);
    }

    @Test
    void cp015ElAdminCombinaFiltrosDeEstadoPrioridadYVencido() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        Sesion tecnico = nuevaSesion("TECNICO");
        long criticoVencido = idDe(crearTicket(ana, "CRITICA"));
        long criticoAlDia = idDe(crearTicket(ana, "CRITICA"));
        long bajaVencido = idDe(crearTicket(ana, "BAJA"));
        for (long id : List.of(criticoVencido, criticoAlDia, bajaVencido)) {
            asignar(id, tecnico);
        }
        jdbc.update("UPDATE ticket SET sla_vencido = TRUE WHERE id IN (?, ?)", criticoVencido, bajaVencido);

        HttpResponse<String> respuesta = get(TICKETS + "?tecnicoId=" + tecnico.id()
                + "&estado=ABIERTO&prioridad=CRITICA&vencido=true", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(idsDe(respuesta)).containsExactly(criticoVencido);
    }

    @Test
    void listarConFiltroInvalidoResponde400ConProblemDetail() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");

        HttpResponse<String> estado = get(TICKETS + "?estado=PERDIDO", admin.token());
        HttpResponse<String> orden = get(TICKETS + "?sort=password,asc", admin.token());

        assertThat(estado.statusCode()).isEqualTo(400);
        assertThat(orden.statusCode()).isEqualTo(400);
        assertThat(tipoDeContenido(estado)).startsWith(PROBLEM_JSON);
        assertThat(estado.body()).as("no repite el valor que mandó el cliente").doesNotContain("PERDIDO");
    }

    // ---------------------------------------------------------------- Apoyo

    private record Sesion(long id, String correo, String token) {
    }

    /** Registra un usuario, le asigna el rol en la base e inicia sesión. */
    private Sesion nuevaSesion(String rol) throws Exception {
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
        return new Sesion(((Number) JsonPath.read(registro.body(), "$.id")).longValue(), correo,
                JsonPath.read(login.body(), "$.token"));
    }

    private HttpResponse<String> crearTicket(Sesion sesion, String prioridad) throws Exception {
        return post(TICKETS, cuerpoTicket(prioridad, null), sesion.token());
    }

    private static String cuerpoTicket(String prioridad, Long activoId) {
        return """
                {"titulo":"Sin red en el laboratorio","descripcion":"No hay conexión desde las 9:00",
                 "categoria":"RED","prioridad":"%s","activoId":%s}
                """.formatted(prioridad, activoId);
    }

    /**
     * Asigna el ticket directo en la base. RF-06 (PATCH /asignacion) todavía no
     * está en main; cuando entre, estas pruebas pueden usar el endpoint.
     */
    private void asignar(long ticketId, Sesion tecnico) {
        jdbc.update("UPDATE ticket SET tecnico_id = ? WHERE id = ?", tecnico.id(), ticketId);
    }

    private long nuevoActivo() {
        String etiqueta = "QA-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO activo (etiqueta_inventario, tipo) VALUES (?, 'LAPTOP')", etiqueta);
        return jdbc.queryForObject("SELECT id FROM activo WHERE etiqueta_inventario = ?", Long.class, etiqueta);
    }

    private int ticketsDe(Sesion sesion) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket WHERE solicitante_id = ?",
                Integer.class, sesion.id());
    }

    private static long idDe(HttpResponse<String> respuesta) {
        assertThat(respuesta.statusCode()).as("ticket creado").isEqualTo(201);
        return ((Number) JsonPath.read(respuesta.body(), "$.id")).longValue();
    }

    private static List<Long> idsDe(HttpResponse<String> respuesta) {
        return JsonPath.<List<Number>>read(respuesta.body(), "$.contenido[*].id").stream()
                .map(Number::longValue)
                .toList();
    }

    private static String tipoDeContenido(HttpResponse<String> respuesta) {
        return respuesta.headers().firstValue("Content-Type").orElse("");
    }

    private HttpResponse<String> get(String ruta, String token) throws Exception {
        return enviar(peticion(ruta, token).GET());
    }

    private HttpResponse<String> post(String ruta, String json, String token) throws Exception {
        return enviar(peticion(ruta, token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
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
