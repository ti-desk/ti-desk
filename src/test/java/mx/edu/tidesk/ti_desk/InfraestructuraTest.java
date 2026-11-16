package mx.edu.tidesk.ti_desk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
    void flywayAplicoTodasLasMigracionesSinErrores() throws Exception {
        List<String> esperadas = Arrays.stream(new PathMatchingResourcePatternResolver()
                        .getResources("classpath:db/migration/V*__*.sql"))
                .map(recurso -> recurso.getFilename().substring(1, recurso.getFilename().indexOf("__")).replace('_', '.'))
                .sorted(Comparator.comparing(BigDecimal::new))
                .toList();

        List<String> aplicadas = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank",
                String.class);
        Integer fallidas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);

        assertThat(esperadas).as("migraciones en db/migration").isNotEmpty();
        assertThat(aplicadas).as("versiones aplicadas, en orden").containsExactlyElementsOf(esperadas);
        assertThat(fallidas).as("migraciones fallidas").isZero();
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