package mx.edu.tidesk.ti_desk.seguridad;

import java.time.Instant;
import mx.edu.tidesk.ti_desk.comun.ManejadorErrores;
import mx.edu.tidesk.ti_desk.usuario.CorreoDuplicadoException;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.UsuarioService;
import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de rebanada web del registro (RF-01): CP-001, CP-002 y CP-003.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, ManejadorErrores.class})
class AuthControllerTest {

    private static final String RUTA = "/api/v1/auth/registro";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    private static String cuerpo(String nombre, String correo, String contrasena) {
        return """
                {"nombre":"%s","correo":"%s","contrasena":"%s"}
                """.formatted(nombre, correo, contrasena);
    }

    @Test
    void registroValidoResponde201SinContrasena() throws Exception {
        when(usuarioService.registrar(any(RegistroRequest.class))).thenReturn(
                new UsuarioResponse(1L, "Ana López", "ana.lopez@unistmo.edu.mx",
                        Rol.USUARIO, true, Instant.parse("2026-11-15T00:00:00Z")));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ana López", "ana.lopez@unistmo.edu.mx", "Segura#2026")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.correo").value("ana.lopez@unistmo.edu.mx"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void correoDuplicadoResponde409ConProblemJson() throws Exception {
        when(usuarioService.registrar(any(RegistroRequest.class)))
                .thenThrow(new CorreoDuplicadoException());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ana López", "ana.lopez@unistmo.edu.mx", "Segura#2026")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El correo ya está registrado."));
    }

    @Test
    void contrasenaDebilResponde400ConDetalleDelCampo() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ana López", "ana.lopez@unistmo.edu.mx", "SinSimbolo2026")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.contrasena").exists());

        verify(usuarioService, never()).registrar(any());
    }

    @Test
    void correoNoInstitucionalResponde400ConDetalleDelCampo() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ana López", "ana.lopez@gmail.com", "Segura#2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").exists());

        verify(usuarioService, never()).registrar(any());
    }

    @Test
    void nombreVacioResponde400ConDetalleDelCampo() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "ana.lopez@unistmo.edu.mx", "Segura#2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void cuerpoQueNoEsJsonResponde400ConProblemJson() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Petición mal formada"));
    }
}