package mx.edu.tidesk.ti_desk.seguridad;

import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.UsuarioActual;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Usuario actual tomado del SecurityContext que deja el filtro JWT.
 */
@Component
public class UsuarioActualJwt implements UsuarioActual {

    @Override
    public Long id() {
        return principal().id();
    }

    @Override
    public Rol rol() {
        return principal().rol();
    }

    private UsuarioAutenticado principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new IllegalStateException("No hay un usuario autenticado en la petición");
        }
        return usuario;
    }
}
