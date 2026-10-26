package mx.edu.tidesk.ti_desk.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Respuestas de error de seguridad en formato ProblemDetail (RFC 9457).
 * 401: sin token o token inválido. 403: autenticado pero sin permiso por rol.
 * El contenido es fijo: no refleja datos de la petición.
 */
@Component
public class ProblemDetailSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Se requiere un token válido.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tienes permiso para realizar esta acción.");
    }

    private static void escribir(HttpServletResponse response, HttpStatus estado,
                                 String titulo, String detalle) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{\"type\":\"about:blank\",\"title\":\"" + titulo
                + "\",\"status\":" + estado.value()
                + ",\"detail\":\"" + detalle + "\"}";
        response.getWriter().write(json);
    }
}