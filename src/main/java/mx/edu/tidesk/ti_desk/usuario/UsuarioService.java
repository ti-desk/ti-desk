package mx.edu.tidesk.ti_desk.usuario;

import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * RF-01: registra un usuario nuevo. El rol siempre es USUARIO y la cuenta queda activa.
     * @throws CorreoDuplicadoException si el correo ya existe
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        if (repositorio.findByEmail(correo).isPresent()) {
            throw new CorreoDuplicadoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setEmail(correo);
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.contrasena()));
        // rol (USUARIO) y activo (true) toman los valores por defecto de la entidad

        try {
            return UsuarioResponse.desde(repositorio.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: lo detiene la restricción única
            throw new CorreoDuplicadoException();
        }
    }
}