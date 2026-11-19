package mx.edu.tidesk.ti_desk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Con el perfil "dev", Flyway también aplica db/seed-dev: un usuario demo por rol.
 * La prueba contraria (sin perfil no hay usuarios demo) está en InfraestructuraTest.
 */
@ActiveProfiles("dev")
class SemillaDesarrolloTest extends IntegracionBaseTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void conPerfilDevSeCargaUnUsuarioDemoPorRol() {
        List<String> roles = jdbc.queryForList(
                "SELECT rol FROM usuario WHERE email LIKE '%.demo@unistmo.edu.mx' ORDER BY rol",
                String.class);

        assertThat(roles).containsExactly("ADMIN", "TECNICO", "USUARIO");
    }

    @Test
    void laSemillaEsRepetibleYNoOcupaNumeroDeVersion() {
        List<String> semillas = jdbc.queryForList(
                "SELECT script FROM flyway_schema_history WHERE version IS NULL AND success = true",
                String.class);

        assertThat(semillas).containsExactly("R__datos_semilla_usuarios.sql");
    }
}
