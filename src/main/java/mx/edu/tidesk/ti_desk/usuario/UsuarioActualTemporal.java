package mx.edu.tidesk.ti_desk.usuario;

import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import org.springframework.stereotype.Component;

/**
 * Implementación TEMPORAL mientras se integra la autenticación JWT.
 * Siempre devuelve el usuario de prueba de la migración V4.
 *
 * TODO(TID-04): reemplazar por una implementación que lea el usuario
 * autenticado del SecurityContext (JWT) y borrar esta clase.
 */
@Component
public class UsuarioActualTemporal implements UsuarioActual {

    static final String CORREO_USUARIO_DEMO = "usuario.demo@unistmo.edu.mx";

    private final UsuarioRepository usuarios;

    public UsuarioActualTemporal(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Long id() {
        return usuarios.findByEmail(CORREO_USUARIO_DEMO)
                .map(Usuario::getId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario de prueba " + CORREO_USUARIO_DEMO));
    }
}
