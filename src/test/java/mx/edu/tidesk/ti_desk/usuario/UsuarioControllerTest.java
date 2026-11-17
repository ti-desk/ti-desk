package mx.edu.tidesk.ti_desk.usuario;

import java.time.Instant;
import java.util.List;
import mx.edu.tidesk.ti_desk.comun.ManejadorErrores;
import mx.edu.tidesk.ti_desk.seguridad.JwtService;
import mx.edu.tidesk.ti_desk.seguridad.ProblemDetailSeguridad;
import mx.edu.tidesk.ti_desk.seguridad.SecurityConfig;
import mx.edu.tidesk.ti_desk.seguridad.WithMockCustomUser;
import mx.edu.tidesk.ti_desk.usuario.dto.ActualizarUsuarioRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad y de contrato de la administración de usuarios (RF-03):
 * cada endpoint con el rol correcto y con uno incorrecto.
 */
@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, ManejadorErrores.class})
class UsuarioControllerTest {

    private static final String USUARIOS = "/api/v1/usuarios";
    private static final String YO = "/api/v1/usuarios/me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static UsuarioResponse respuesta(long id, Rol rol, boolean activo) {
        return new UsuarioResponse(id, "Usuario " + id, "usuario" + id + "@unistmo.edu.mx",
                rol, activo, Instant.parse("2026-11-16T00:00:00Z"));
    }

    // ---------- GET /usuarios/me ----------

    @Test
    @WithMockCustomUser(id = 7, rol = Rol.TECNICO)
    void miPerfilUsaElIdDelTokenYNoIncluyeElHash() throws Exception {
        when(usuarioService.obtener(7L)).thenReturn(respuesta(7, Rol.TECNICO, true));

        mockMvc.perform(get(YO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(usuarioService).obtener(7L);
    }

    @Test
    void miPerfilSinTokenResponde401() throws Exception {
        mockMvc.perform(get(YO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---------- GET /usuarios ----------

    @Test
    @WithMockCustomUser(rol = Rol.ADMIN)
    void adminListaLosUsuarios() throws Exception {
        when(usuarioService.listar()).thenReturn(
                List.of(respuesta(1, Rol.ADMIN, true), respuesta(5, Rol.USUARIO, false)));

        mockMvc.perform(get(USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].activo").value(false));
    }

    @Test
    @WithMockCustomUser(rol = Rol.USUARIO)
    void usuarioNoPuedeListarResponde403() throws Exception {
        mockMvc.perform(get(USUARIOS))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verify(usuarioService, never()).listar();
    }

    @Test
    @WithMockCustomUser(rol = Rol.TECNICO)
    void tecnicoNoPuedeListarResponde403() throws Exception {
        mockMvc.perform(get(USUARIOS))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).listar();
    }

    @Test
    void listarSinTokenResponde401() throws Exception {
        mockMvc.perform(get(USUARIOS))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PATCH /usuarios/{id} ----------

    @Test
    @WithMockCustomUser(id = 1, rol = Rol.ADMIN)
    void adminCambiaElRolYSeEnviaSuIdAlServicio() throws Exception {
        when(usuarioService.actualizar(eq(5L), any(ActualizarUsuarioRequest.class), eq(1L)))
                .thenReturn(respuesta(5, Rol.TECNICO, true));

        mockMvc.perform(patch(USUARIOS + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"TECNICO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("TECNICO"));

        verify(usuarioService).actualizar(eq(5L), eq(new ActualizarUsuarioRequest(Rol.TECNICO, null)), eq(1L));
    }

    @Test
    @WithMockCustomUser(rol = Rol.USUARIO)
    void usuarioNoPuedeCambiarRolesResponde403() throws Exception {
        mockMvc.perform(patch(USUARIOS + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizar(any(), any(), any());
    }

    @Test
    @WithMockCustomUser(rol = Rol.ADMIN)
    void cuerpoSinCambiosResponde400() throws Exception {
        mockMvc.perform(patch(USUARIOS + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.conCambios").exists());

        verify(usuarioService, never()).actualizar(any(), any(), any());
    }

    @Test
    @WithMockCustomUser(rol = Rol.ADMIN)
    void rolQueNoExisteResponde400() throws Exception {
        mockMvc.perform(patch(USUARIOS + "/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"JEFE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithMockCustomUser(rol = Rol.ADMIN)
    void usuarioInexistenteResponde404() throws Exception {
        when(usuarioService.actualizar(eq(99L), any(), any()))
                .thenThrow(new UsuarioNoEncontradoException());

        mockMvc.perform(patch(USUARIOS + "/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Usuario no encontrado"));
    }

    @Test
    @WithMockCustomUser(id = 1, rol = Rol.ADMIN)
    void adminQueSeDesactivaASiMismoResponde409() throws Exception {
        when(usuarioService.actualizar(eq(1L), any(), eq(1L)))
                .thenThrow(new CambioNoPermitidoException("No puedes desactivar tu propia cuenta"));

        mockMvc.perform(patch(USUARIOS + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No puedes desactivar tu propia cuenta."));
    }
}