package mx.edu.tidesk.ti_desk.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginRequest;
import mx.edu.tidesk.ti_desk.seguridad.dto.LoginResponse;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del login (RF-02): CP-004, CP-005, CP-009 y CP-044.
 */
class AuthServiceTest {

    private static final String SECRETO = "clave-de-prueba-para-jwt-con-mas-de-32-caracteres-aaa";
    private static final String CORREO = "ana.lopez@unistmo.edu.mx";
    private static final String CONTRASENA = "Segura#2026";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    // Costo 4 (el mínimo) para que las pruebas sean rápidas.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(SECRETO, 30,
            Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC));
    private final AuthService servicio = new AuthService(repositorio, encoder, jwtService);

    private Usuario usuario(boolean activo) {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setNombre("Ana López");
        u.setEmail(CORREO);
        u.setPasswordHash(encoder.encode(CONTRASENA));
        u.setActivo(activo);
        return u;
    }

    @Test
    void loginCorrectoDevuelveTokenBearerConElIdDelUsuario() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true)));

        LoginResponse respuesta = servicio.login(new LoginRequest(CORREO, CONTRASENA));

        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(jwtService.extraerUsuarioId(respuesta.token())).isEqualTo(7L);
    }

    @Test
    void elCorreoSeNormalizaAntesDeBuscar() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true)));

        LoginResponse respuesta = servicio.login(
                new LoginRequest("  Ana.Lopez@UNISTMO.edu.mx ", CONTRASENA));

        assertThat(respuesta.token()).isNotBlank();
    }

    @Test
    void contrasenaIncorrectaLanzaCredencialesInvalidas() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> servicio.login(new LoginRequest(CORREO, "ContraseñaMala")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void correoInexistenteLanzaElMismoErrorQueContrasenaIncorrecta() {
        when(repositorio.findByEmail("nadie@unistmo.edu.mx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.login(
                new LoginRequest("nadie@unistmo.edu.mx", CONTRASENA)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void usuarioDesactivadoNoPuedeIniciarSesion() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> servicio.login(new LoginRequest(CORREO, CONTRASENA)))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void contrasenaDeMasDe72BytesSeRechazaSinFallar() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> servicio.login(new LoginRequest(CORREO, "a".repeat(100))))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void losDtoNoExponenContrasenaNiTokenAlImprimirse() {
        assertThat(new LoginRequest(CORREO, CONTRASENA).toString()).doesNotContain(CONTRASENA);
        assertThat(LoginResponse.bearer("token-secreto").toString()).doesNotContain("token-secreto");
    }
}