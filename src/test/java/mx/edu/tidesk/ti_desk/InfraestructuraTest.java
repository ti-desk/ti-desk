package mx.edu.tidesk.ti_desk;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

class InfraestructuraTest extends IntegracionBaseTest {

    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    int puerto;

    @Test
    void flywayCreoLasTablasDelModelo() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tablas).contains("usuario", "ticket", "comentario",
                "historial_estado", "activo", "politica_sla", "folio_secuencia");
    }

    @Test
    void flywayAplicoV1yV2SinErrores() {
        List<String> versiones = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class);

        assertThat(versiones).containsExactly("1", "2");
    }

    @Test
    void losIndicesDeV2Existen() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).contains("ix_ticket_estado_prioridad", "ix_ticket_tecnico",
                "ix_ticket_fecha_limite_sla");
    }

    @Test
    void actuatorHealthResponde200ConEstadoUp() throws Exception {
        HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }
}