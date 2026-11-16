package mx.edu.tidesk.ti_desk.seguridad;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-para-jwt-con-mas-de-32-caracteres-aaa";
    private static final String OTRO_SECRETO = "otra-clave-distinta-para-jwt-con-mas-de-32-caracteres";
    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    private static Clock reloj(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private static JwtService servicio(String secreto, Instant instante) {
        return new JwtService(secreto, 30, reloj(instante));
    }

    @Test
    void tokenValidoDevuelveElIdDelUsuario() {
        JwtService servicio = servicio(SECRETO, AHORA);

        String token = servicio.generarToken(7L);

        assertThat(servicio.extraerUsuarioId(token)).isEqualTo(7L);
    }

    @Test
    void tokenVigenteAlos29MinutosYExpiradoALos31() {
        String token = servicio(SECRETO, AHORA).generarToken(7L);

        JwtService a29 = servicio(SECRETO, AHORA.plus(29, ChronoUnit.MINUTES));
        JwtService a31 = servicio(SECRETO, AHORA.plus(31, ChronoUnit.MINUTES));

        assertThat(a29.extraerUsuarioId(token)).isEqualTo(7L);
        assertThatThrownBy(() -> a31.extraerUsuarioId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() {
        String token = servicio(OTRO_SECRETO, AHORA).generarToken(7L);

        assertThatThrownBy(() -> servicio(SECRETO, AHORA).extraerUsuarioId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenConPayloadAlteradoSeRechaza() {
        JwtService servicio = servicio(SECRETO, AHORA);
        String[] token7 = servicio.generarToken(7L).split("\\.");
        String[] token8 = servicio.generarToken(8L).split("\\.");
        // Payload del usuario 8 con la firma del usuario 7
        String falsificado = token8[0] + "." + token8[1] + "." + token7[2];

        assertThatThrownBy(() -> servicio.extraerUsuarioId(falsificado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenMalFormadoSeRechaza() {
        JwtService servicio = servicio(SECRETO, AHORA);

        assertThatThrownBy(() -> servicio.extraerUsuarioId("esto-no-es-un-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void elTokenNoContieneElRol() {
        String token = servicio(SECRETO, AHORA).generarToken(7L);

        String payload = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).contains("\"sub\":\"7\"").doesNotContain("rol");
    }

    @Test
    void secretoCortoImpideArrancar() {
        assertThatThrownBy(() -> new JwtService("corto", 30))
                .isInstanceOf(IllegalStateException.class);
    }
}