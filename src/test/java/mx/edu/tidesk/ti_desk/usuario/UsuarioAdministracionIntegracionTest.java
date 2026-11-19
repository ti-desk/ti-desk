package mx.edu.tidesk.ti_desk.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import mx.edu.tidesk.ti_desk.IntegracionBaseTest;

/**
 * Pruebas de integración de RF-03 (perfil propio y administración de usuarios).
 * Levantan la aplicación completa con PostgreSQL 16 real (Testcontainers) y usan
 * tokens reales obtenidos con el login: así se prueba juntos el filtro JWT, las
 * reglas de @PreAuthorize, el servicio y la base de datos.
 *
 * El ADMIN de cada prueba se crea registrando un usuario y promoviéndolo en la
 * base, porque el registro público siempre crea rol USUARIO.
 */
class UsuarioAdministracionIntegracionTest extends IntegracionBaseTest {

    private static final String CONTRASENA = "Segura#2026";
    private static final String PROBLEM_JSON = "application/problem+json";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    // ------------------------------------------------------- Perfil propio

    @Test
    void usuariosMeDevuelveElPerfilDelDuenoDelTokenSinContrasena() throws Exception {
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = get("/api/v1/usuarios/me", ana.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Integer>read(respuesta.body(), "$.id")).isEqualTo(ana.id());
        assertThat(JsonPath.<String>read(respuesta.body(), "$.correo")).isEqualTo(ana.correo());
        assertThat(respuesta.body()).doesNotContain(CONTRASENA).doesNotContain("$2");
    }

    @Test
    void cp041UsuariosMeSinTokenResponde401() throws Exception {
        HttpResponse<String> respuesta = get("/api/v1/usuarios/me", null);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
    }

    // ------------------------------------------------- Permisos por rol

    @Test
    void cp008Cp042UsuarioYTecnicoNoPuedenAdministrarUsuarios() throws Exception {
        Sesion victima = nuevaSesion("USUARIO");
        String cambio = """
                {"rol":"ADMIN"}
                """;

        for (String rol : List.of("USUARIO", "TECNICO")) {
            Sesion sesion = nuevaSesion(rol);

            HttpResponse<String> listar = get("/api/v1/usuarios", sesion.token());
            HttpResponse<String> cambiar = patch("/api/v1/usuarios/" + victima.id(), cambio, sesion.token());

            assertThat(listar.statusCode()).as("GET /usuarios como %s", rol).isEqualTo(403);
            assertThat(tipoDeContenido(listar)).startsWith(PROBLEM_JSON);
            assertThat(cambiar.statusCode()).as("PATCH /usuarios como %s", rol).isEqualTo(403);
        }
        assertThat(rolEnBase(victima.correo()))
                .as("un 403 no debe aplicar el cambio")
                .isEqualTo("USUARIO");
    }

    @Test
    void cp042AdminListaUsuariosSinExponerContrasenas() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion otro = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = get("/api/v1/usuarios", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(respuesta.body(), "$[*].correo"))
                .contains(admin.correo(), otro.correo());
        assertThat(respuesta.body()).doesNotContain("$2").doesNotContainIgnoringCase("password");
    }

    // ------------------------------------------------- Cambios de rol y estado

    @Test
    void cp007ElCambioDeRolSeAplicaEnLaSiguientePeticionConElMismoToken() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        assertThat(get("/api/v1/usuarios", ana.token()).statusCode()).isEqualTo(403);

        HttpResponse<String> cambio = patch("/api/v1/usuarios/" + ana.id(), """
                {"rol":"ADMIN"}
                """, admin.token());

        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(cambio.body(), "$.rol")).isEqualTo("ADMIN");
        assertThat(get("/api/v1/usuarios", ana.token()).statusCode())
                .as("el rol se lee de la base en cada petición (ADR-003), no del token")
                .isEqualTo(200);
    }

    @Test
    void cp009AdminDesactivaUnUsuarioYEsteQuedaFueraConSuTokenYEnElLogin() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> cambio = patch("/api/v1/usuarios/" + ana.id(), """
                {"activo":false}
                """, admin.token());

        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(cambio.body(), "$.activo")).isFalse();
        assertThat(get("/api/v1/usuarios/me", ana.token()).statusCode())
                .as("token vigente de un usuario desactivado")
                .isEqualTo(401);
        assertThat(login(ana.correo(), CONTRASENA).statusCode()).isEqualTo(401);
    }

    @Test
    void adminReactivaUnUsuarioYPuedeVolverAEntrar() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");
        patch("/api/v1/usuarios/" + ana.id(), "{\"activo\":false}", admin.token());

        HttpResponse<String> cambio = patch("/api/v1/usuarios/" + ana.id(), "{\"activo\":true}", admin.token());

        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(login(ana.correo(), CONTRASENA).statusCode()).isEqualTo(200);
    }

    @Test
    void adminNoPuedeDesactivarseNiQuitarseElRolASiMismo() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");

        HttpResponse<String> desactivarse = patch("/api/v1/usuarios/" + admin.id(),
                "{\"activo\":false}", admin.token());
        HttpResponse<String> degradarse = patch("/api/v1/usuarios/" + admin.id(),
                "{\"rol\":\"USUARIO\"}", admin.token());

        assertThat(desactivarse.statusCode()).isEqualTo(409);
        assertThat(degradarse.statusCode()).isEqualTo(409);
        assertThat(tipoDeContenido(desactivarse)).startsWith(PROBLEM_JSON);
        assertThat(rolEnBase(admin.correo())).isEqualTo("ADMIN");
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE email = ?",
                Boolean.class, admin.correo())).isTrue();
    }

    // ------------------------------------------------- Errores de la petición

    @Test
    void cambiarUnUsuarioQueNoExisteResponde404() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");

        HttpResponse<String> respuesta = patch("/api/v1/usuarios/999999999", "{\"activo\":false}", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(404);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
    }

    @Test
    void cambioSinCamposResponde400() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = patch("/api/v1/usuarios/" + ana.id(), "{}", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
    }

    @Test
    void rolInexistenteResponde400SinCambiarNada() throws Exception {
        Sesion admin = nuevaSesion("ADMIN");
        Sesion ana = nuevaSesion("USUARIO");

        HttpResponse<String> respuesta = patch("/api/v1/usuarios/" + ana.id(),
                "{\"rol\":\"SUPERADMIN\"}", admin.token());

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(rolEnBase(ana.correo())).isEqualTo("USUARIO");
    }

    // ---------------------------------------------------------------- Apoyo

    private record Sesion(int id, String correo, String token) {
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

        HttpResponse<String> login = login(correo, CONTRASENA);
        assertThat(login.statusCode()).as("login previo").isEqualTo(200);
        return new Sesion(JsonPath.read(registro.body(), "$.id"), correo,
                JsonPath.read(login.body(), "$.token"));
    }

    private String rolEnBase(String correo) {
        return jdbc.queryForObject("SELECT rol FROM usuario WHERE email = ?", String.class, correo);
    }

    private HttpResponse<String> login(String correo, String contrasena) throws Exception {
        return post("/api/v1/auth/login", """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena), null);
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

    private HttpResponse<String> patch(String ruta, String json, String token) throws Exception {
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
