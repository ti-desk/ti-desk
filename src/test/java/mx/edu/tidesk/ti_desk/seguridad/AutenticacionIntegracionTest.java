package mx.edu.tidesk.ti_desk.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

import mx.edu.tidesk.ti_desk.IntegracionBaseTest;

/**
 * Pruebas de integración del módulo de autenticación (RF-01, RF-02, ADR-003).
 * Levantan la aplicación completa con PostgreSQL 16 real (Testcontainers) y
 * recorren los escenarios Gherkin de docs/calidad/gherkin-autenticacion.md
 * por HTTP, sin mocks: controlador, servicio, filtro JWT, BCrypt y base de datos.
 *
 * Cada prueba usa un correo único para no depender del orden de ejecución.
 */
@ExtendWith(OutputCaptureExtension.class)
class AutenticacionIntegracionTest extends IntegracionBaseTest {

    private static final String CONTRASENA = "Segura#2026";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String RUTA_PROTEGIDA = "/api/v1/usuarios/me";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    // ---------------------------------------------------------------- RF-01

    @Test
    void cp001RegistroValidoGuardaHashBcryptConRolUsuarioYActivo() throws Exception {
        String correo = correoUnico();

        HttpResponse<String> respuesta = registrar("Ana López", correo, CONTRASENA);

        assertThat(respuesta.statusCode()).isEqualTo(201);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.correo")).isEqualTo(correo);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.rol")).isEqualTo("USUARIO");
        assertThat(respuesta.body()).doesNotContain(CONTRASENA).doesNotContain("$2");

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT password_hash, rol, activo FROM usuario WHERE email = ?", correo);
        assertThat((String) fila.get("password_hash"))
                .as("la contraseña se guarda como hash BCrypt, nunca en claro")
                .startsWith("$2")
                .isNotEqualTo(CONTRASENA);
        assertThat(fila.get("rol")).isEqualTo("USUARIO");
        assertThat(fila.get("activo")).isEqualTo(true);
    }

    @Test
    void registroIgnoraElRolQueMandaElCliente() throws Exception {
        String correo = correoUnico();
        String cuerpo = """
                {"nombre":"Intruso","correo":"%s","contrasena":"%s","rol":"ADMIN"}
                """.formatted(correo, CONTRASENA);

        HttpResponse<String> respuesta = post("/api/v1/auth/registro", cuerpo);

        assertThat(respuesta.statusCode()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT rol FROM usuario WHERE email = ?", String.class, correo))
                .isEqualTo("USUARIO");
    }

    @Test
    void cp002CorreoDuplicadoResponde409SinCrearSegundoUsuario() throws Exception {
        String correo = correoUnico();
        assertThat(registrar("Ana López", correo, CONTRASENA).statusCode()).isEqualTo(201);

        // Mismo correo con mayúsculas: el servicio lo normaliza, así que también es duplicado.
        HttpResponse<String> respuesta = registrar("Otra Ana", correo.toUpperCase(), CONTRASENA);

        assertThat(respuesta.statusCode()).isEqualTo(409);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE lower(email) = ?", Integer.class, correo))
                .isEqualTo(1);
    }

    @Test
    void cp003ContrasenaDebilResponde400ConDetalleEnErroresYNoCreaUsuario() throws Exception {
        String correo = correoUnico();

        HttpResponse<String> respuesta = registrar("Ana López", correo, "sinmayuscula#1");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.errores.contrasena")).isNotBlank();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE email = ?", Integer.class, correo)).isZero();
    }

    // ---------------------------------------------------------------- RF-02

    @Test
    void cp004LoginCorrectoDevuelveTokenBearerQueExpiraEn30MinutosYNoLlevaElRol() throws Exception {
        String correo = correoUnico();
        HttpResponse<String> registro = registrar("Ana López", correo, CONTRASENA);
        Integer id = JsonPath.read(registro.body(), "$.id");

        HttpResponse<String> respuesta = login(correo, CONTRASENA);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.tipo")).isEqualTo("Bearer");

        String carga = cargaDelToken(JsonPath.read(respuesta.body(), "$.token"));
        assertThat(JsonPath.<String>read(carga, "$.sub")).isEqualTo(String.valueOf(id));
        long iat = ((Number) JsonPath.read(carga, "$.iat")).longValue();
        long exp = ((Number) JsonPath.read(carga, "$.exp")).longValue();
        assertThat(exp - iat).as("vigencia del token en segundos").isEqualTo(30 * 60);
        assertThat(carga).as("el rol se lee de la base, no viaja en el token")
                .doesNotContain("USUARIO").doesNotContainIgnoringCase("rol");
    }

    @Test
    void cp005LoginIncorrectoRespondeElMismo401ExistaONoElCorreo() throws Exception {
        String correo = correoUnico();
        registrar("Ana López", correo, CONTRASENA);

        HttpResponse<String> contrasenaMala = login(correo, "ContraseñaMala#1");
        HttpResponse<String> correoInexistente = login(correoUnico(), CONTRASENA);

        assertThat(contrasenaMala.statusCode()).isEqualTo(401);
        assertThat(correoInexistente.statusCode()).isEqualTo(401);
        assertThat(tipoDeContenido(contrasenaMala)).startsWith(PROBLEM_JSON);
        assertThat(contrasenaMala.body())
                .as("el mensaje no revela si el correo existe")
                .isEqualTo(correoInexistente.body());
    }

    @Test
    void cp009UsuarioDesactivadoNoPuedeIniciarSesion() throws Exception {
        String correo = correoUnico();
        registrar("Ana López", correo, CONTRASENA);
        desactivar(correo);

        HttpResponse<String> respuesta = login(correo, CONTRASENA);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).isEqualTo(login(correoUnico(), CONTRASENA).body());
    }

    // ------------------------------------------------- Rutas protegidas y token

    @Test
    void cp041RutaProtegidaSinTokenResponde401ConProblemJson() throws Exception {
        HttpResponse<String> respuesta = get(RUTA_PROTEGIDA, null);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(tipoDeContenido(respuesta)).startsWith(PROBLEM_JSON);
    }

    @Test
    void cp041RutasPublicasNoPidenToken() throws Exception {
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
        assertThat(get("/v3/api-docs", null).statusCode()).isEqualTo(200);
    }

    @Test
    void cp006TokenAlteradoResponde401() throws Exception {
        String token = tokenDe(registrarYEntrar());
        // Se cambia el último carácter de la firma.
        String alterado = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(get(RUTA_PROTEGIDA, alterado).statusCode()).isEqualTo(401);
    }

    @Test
    void usuarioDesactivadoConTokenVigenteRecibe401() throws Exception {
        String correo = correoUnico();
        registrar("Ana López", correo, CONTRASENA);
        String token = tokenDe(login(correo, CONTRASENA));
        desactivar(correo);

        HttpResponse<String> respuesta = get(RUTA_PROTEGIDA, token);

        assertThat(respuesta.statusCode()).isEqualTo(401);
    }

    /**
     * Con un token válido el filtro deja pasar la petición: una ruta que no
     * existe debe responder 404, no 401. Si responde 401, el usuario
     * autenticado no puede distinguir "no tengo permiso" de "no existe".
     */
    @Test
    void tokenValidoEnRutaInexistenteResponde404YNo401() throws Exception {
        String token = tokenDe(registrarYEntrar());

        HttpResponse<String> respuesta = get("/api/v1/ruta-que-no-existe", token);

        assertThat(respuesta.statusCode()).isEqualTo(404);
    }

    @Test
    void metodoNoPermitidoEnRutaPublicaResponde405YNo401() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(url("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(405);
    }

    // ---------------------------------------------------------------- CP-044

    @Test
    void cp044RegistroYLoginNoEscribenContrasenaNiTokenEnLosLogs(CapturedOutput salida) throws Exception {
        String contrasena = "Secreta#1" + UUID.randomUUID().toString().substring(0, 8);
        String correo = correoUnico();
        registrar("Ana López", correo, contrasena);
        String token = tokenDe(login(correo, contrasena));
        get(RUTA_PROTEGIDA, token);

        assertThat(salida.getAll())
                .doesNotContain(contrasena)
                .doesNotContain(token);
    }


    // ---------------------------------------------------------------- Apoyo

    private static String correoUnico() {
        return "qa." + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "@unistmo.edu.mx";
    }

    private HttpResponse<String> registrar(String nombre, String correo, String contrasena)
            throws Exception {
        return post("/api/v1/auth/registro", """
                {"nombre":"%s","correo":"%s","contrasena":"%s"}
                """.formatted(nombre, correo, contrasena));
    }

    private HttpResponse<String> login(String correo, String contrasena) throws Exception {
        return post("/api/v1/auth/login", """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena));
    }

    private HttpResponse<String> registrarYEntrar() throws Exception {
        String correo = correoUnico();
        registrar("Ana López", correo, CONTRASENA);
        return login(correo, CONTRASENA);
    }

    private void desactivar(String correo) {
        jdbc.update("UPDATE usuario SET activo = false WHERE email = ?", correo);
    }

    private static String tokenDe(HttpResponse<String> login) {
        assertThat(login.statusCode()).as("login previo").isEqualTo(200);
        return JsonPath.read(login.body(), "$.token");
    }

    private static String cargaDelToken(String token) {
        String[] partes = token.split("\\.");
        assertThat(partes).as("un JWT tiene encabezado, carga y firma").hasSize(3);
        return new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
    }

    private static String tipoDeContenido(HttpResponse<String> respuesta) {
        return respuesta.headers().firstValue("Content-Type").orElse("");
    }

    private HttpResponse<String> post(String ruta, String json) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(url(ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String ruta, String token) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(url(ruta)).GET();
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
