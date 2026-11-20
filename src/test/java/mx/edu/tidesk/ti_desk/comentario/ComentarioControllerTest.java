package mx.edu.tidesk.ti_desk.comentario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;
import mx.edu.tidesk.ti_desk.comentario.dto.CrearComentarioRequest;
import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.ManejadorErrores;
import mx.edu.tidesk.ti_desk.seguridad.JwtService;
import mx.edu.tidesk.ti_desk.seguridad.ProblemDetailSeguridad;
import mx.edu.tidesk.ti_desk.seguridad.SecurityConfig;
import mx.edu.tidesk.ti_desk.seguridad.UsuarioActualJwt;
import mx.edu.tidesk.ti_desk.seguridad.WithMockCustomUser;
import mx.edu.tidesk.ti_desk.usuario.CambioNoPermitidoException;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato y seguridad de la API de comentarios (RF-07). Por defecto la
 * petición va autenticada como el usuario 7 con rol USUARIO.
 */
@WebMvcTest(ComentarioController.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, ManejadorErrores.class, UsuarioActualJwt.class})
@WithMockCustomUser(id = 7, rol = Rol.USUARIO)
class ComentarioControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ComentarioService comentarioService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    UsuarioRepository usuarioRepository;

    @Test
    void comentarResponde201ConElComentario() throws Exception {
        when(comentarioService.comentar(eq(15L), any(CrearComentarioRequest.class), eq(7L), eq(Rol.USUARIO)))
                .thenReturn(comentario(false));

        mvc.perform(post("/api/v1/tickets/15/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Sigue sin red\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Sigue sin red"))
                .andExpect(jsonPath("$.interno").value(false))
                .andExpect(jsonPath("$.autorNombre").value("Ana"));

        ArgumentCaptor<CrearComentarioRequest> captor = ArgumentCaptor.forClass(CrearComentarioRequest.class);
        verify(comentarioService).comentar(eq(15L), captor.capture(), eq(7L), eq(Rol.USUARIO));
        assertThat(captor.getValue().esInterno()).isFalse();
    }

    @Test
    void comentarSinTextoResponde400ConDetalle() throws Exception {
        mvc.perform(post("/api/v1/tickets/15/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.texto").exists());

        verify(comentarioService, never()).comentar(any(), any(), any(), any());
    }

    @Test
    void comentarioInternoComoUsuarioResponde403() throws Exception {
        when(comentarioService.comentar(eq(15L), any(), any(), any()))
                .thenThrow(new AccionNoPermitidaException(
                        "Solo técnicos y administradores pueden escribir comentarios internos."));

        mvc.perform(post("/api/v1/tickets/15/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Nota\",\"interno\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void comentarUnTicketCerradoResponde409() throws Exception {
        when(comentarioService.comentar(eq(15L), any(), any(), any()))
                .thenThrow(new CambioNoPermitidoException("No se puede comentar un ticket CERRADO"));

        mvc.perform(post("/api/v1/tickets/15/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Hola\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockCustomUser(id = 3, rol = Rol.TECNICO)
    void listarUsaElIdYRolDelUsuarioAutenticado() throws Exception {
        when(comentarioService.listar(15L, 3L, Rol.TECNICO)).thenReturn(List.of(comentario(true)));

        mvc.perform(get("/api/v1/tickets/15/comentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interno").value(true));

        verify(comentarioService).listar(15L, 3L, Rol.TECNICO);
    }

    @Test
    @WithAnonymousUser
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/tickets/15/comentarios"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tickets/15/comentarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());

        verify(comentarioService, never()).listar(any(), any(), any());
        verify(comentarioService, never()).comentar(any(), any(), any(), any());
    }

    private static ComentarioResponse comentario(boolean interno) {
        return new ComentarioResponse(1L, 15L, 7L, "Ana", "Sigue sin red", interno,
                Instant.parse("2026-11-17T15:00:00Z"));
    }
}
