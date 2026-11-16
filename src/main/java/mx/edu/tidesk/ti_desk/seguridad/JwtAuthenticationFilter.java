package mx.edu.tidesk.ti_desk.seguridad;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el token del encabezado "Authorization: Bearer ..." en cada petición (ADR-003).
 * Si el token es válido y el usuario existe y está activo, lo deja autenticado
 * con su rol actual de la base de datos. En cualquier otro caso no autentica
 * y la petición sigue: si la ruta es protegida, Spring Security responde 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            autenticar(encabezado.substring(PREFIJO.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token) {
        try {
            Long usuarioId = jwtService.extraerUsuarioId(token);
            usuarios.findById(usuarioId)
                    .filter(Usuario::isActivo)
                    .ifPresent(this::dejarAutenticado);
        } catch (JwtException | IllegalArgumentException e) {
            // Token expirado, alterado o mal formado: la petición queda sin autenticar.
            SecurityContextHolder.clearContext();
        }
    }

    private void dejarAutenticado(Usuario usuario) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getRol());
        UsernamePasswordAuthenticationToken autenticacion =
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
    }
}