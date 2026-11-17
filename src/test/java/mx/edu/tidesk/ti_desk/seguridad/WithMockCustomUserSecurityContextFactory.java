package mx.edu.tidesk.ti_desk.seguridad;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Construye el contexto de seguridad de {@link WithMockCustomUser} con el mismo
 * principal (UsuarioAutenticado) y la misma autoridad ROLE_x que deja el filtro JWT.
 */
public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser anotacion) {
        UsuarioAutenticado principal = new UsuarioAutenticado(anotacion.id(), anotacion.rol());
        UsernamePasswordAuthenticationToken autenticacion =
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + anotacion.rol().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        return contexto;
    }
}