package mx.edu.tidesk.ti_desk.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginRequest;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginResponse;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión (RF-02, ADR-003): valida las credenciales y emite el token.
 */
@Service
public class AuthService {

    private static final int MAX_BYTES_BCRYPT = 72;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // Hash de relleno: se compara contra él cuando el correo no existe,
    // para que la respuesta tarde lo mismo que con un correo real.
    private final String hashSenuelo;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashSenuelo = passwordEncoder.encode("sin-usuario");
    }

    /**
     * @throws CredencialesInvalidasException si el correo no existe, la contraseña
     *         no coincide o la cuenta está desactivada (siempre el mismo error)
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest solicitud) {
        String contrasena = solicitud.contrasena();
        // BCrypt solo admite 72 bytes; ninguna contraseña registrada es más larga.
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_BCRYPT) {
            throw new CredencialesInvalidasException();
        }

        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = usuarios.findByEmail(correo);

        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashSenuelo);
        boolean coincide = passwordEncoder.matches(contrasena, hash);

        if (encontrado.isEmpty() || !coincide || !encontrado.get().isActivo()) {
            throw new CredencialesInvalidasException();
        }
        return LoginResponse.bearer(jwtService.generarToken(encontrado.get().getId()));
    }
}