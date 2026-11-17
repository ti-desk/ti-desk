package mx.edu.tidesk.ti_desk.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailSeguridadTest {

    private final ProblemDetailSeguridad manejador = new ProblemDetailSeguridad();

    @Test
    void sinAutenticacionResponde401ConProblemJson() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        manejador.commence(new MockHttpServletRequest(), respuesta,
                new BadCredentialsException("detalle-interno-secreto"));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentType()).startsWith("application/problem+json");
        String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).contains("\"status\":401").contains("No autenticado");
        assertThat(cuerpo).doesNotContain("detalle-interno-secreto");
    }

    @Test
    void sinPermisoResponde403ConProblemJson() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        manejador.handle(new MockHttpServletRequest(), respuesta,
                new AccessDeniedException("detalle-interno-secreto"));

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(respuesta.getContentType()).startsWith("application/problem+json");
        String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).contains("\"status\":403").contains("Acceso denegado");
        assertThat(cuerpo).doesNotContain("detalle-interno-secreto");
    }
}