package mx.edu.tidesk.ti_desk.seguridad;

import jakarta.validation.Valid;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginRequest;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginResponse;
import mx.edu.tidesk.ti_desk.usuario.UsuarioService;
import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación (ADR-003): registro y login.
 * Solo recibe y responde; la lógica vive en los servicios (ADR-002).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    /** RF-01: registra un usuario y responde 201 sin contraseña ni hash. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest solicitud) {
        return usuarioService.registrar(solicitud);
    }

    /** RF-02: valida las credenciales y responde 200 con el token JWT. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest solicitud) {
        return authService.login(solicitud);
    }
}