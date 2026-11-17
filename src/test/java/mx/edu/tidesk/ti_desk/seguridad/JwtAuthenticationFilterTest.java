package mx.edu.tidesk.ti_desk.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del filtro JWT con tokens reales (ADR-003): CP-006, CP-008, CP-009 y CP-041.
 */
@WebMvcTest(controllers = JwtAuthenticationFilterTest.RutasDePrueba.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, JwtService.class,
        JwtAuthenticationFilterTest.RutasDePrueba.class})
class JwtAuthenticationFilterTest {

    private static final String YO = "/api/v1/filtro/yo";
    private static final String ADMIN = "/api/v1/filtro/admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String secreto;

    @MockitoBean
    private UsuarioRepository usuarios;

    @RestController
    static class RutasDePrueba {

        @GetMapping("/api/v1/filtro/yo")
        String yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
            return usuario.id() + ":" + usuario.rol();
        }

        @GetMapping("/api/v1/filtro/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok";
        }
    }

    private void existeUsuario(long id, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Ana López");
        u.setEmail("ana.lopez@unistmo.edu.mx");
        u.setRol(rol);
        u.setActivo(activo);
        when(usuarios.findById(id)).thenReturn(Optional.of(u));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void tokenValidoAutenticaConIdYRolDeLaBaseDeDatos() throws Exception {
        existeUsuario(7L, Rol.TECNICO, true);

        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarToken(7L))))
                .andExpect(status().isOk())
                .andExpect(content().string("7:TECNICO"));
    }

    @Test
    void sinEncabezadoResponde401ConProblemJson() throws Exception {
        mockMvc.perform(get(YO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenMalFormadoResponde401() throws Exception {
        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer("esto.no.es-un-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void tokenFirmadoConOtraClaveResponde401() throws Exception {
        existeUsuario(7L, Rol.USUARIO, true);
        JwtService otro = new JwtService(
                "otra-clave-distinta-para-jwt-con-mas-de-32-caracteres", 30, Clock.systemUTC());

        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer(otro.generarToken(7L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoResponde401() throws Exception {
        existeUsuario(7L, Rol.USUARIO, true);
        JwtService delPasado = new JwtService(secreto, 30,
                Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));

        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer(delPasado.generarToken(7L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesactivadoResponde401AunqueSuTokenSigaVigente() throws Exception {
        existeUsuario(7L, Rol.USUARIO, false);

        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarToken(7L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioQueYaNoExisteResponde401() throws Exception {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarToken(99L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoResponde403ConProblemJson() throws Exception {
        existeUsuario(7L, Rol.USUARIO, true);

        mockMvc.perform(get(ADMIN).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarToken(7L))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rolAdminAccedeARutaDeAdmin() throws Exception {
        existeUsuario(1L, Rol.ADMIN, true);

        mockMvc.perform(get(ADMIN).header(HttpHeaders.AUTHORIZATION, bearer(jwtService.generarToken(1L))))
                .andExpect(status().isOk());
    }
}