package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.ManejadorErrores;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.ticket.dto.AsignarTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.CambiarEstadoRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.HistorialEstadoResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.PaginaResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketDetalleResponse;
import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.seguridad.JwtService;
import mx.edu.tidesk.ti_desk.seguridad.ProblemDetailSeguridad;
import mx.edu.tidesk.ti_desk.seguridad.SecurityConfig;
import mx.edu.tidesk.ti_desk.seguridad.UsuarioActualJwt;
import mx.edu.tidesk.ti_desk.seguridad.WithMockCustomUser;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato y seguridad de la API de tickets (RF-04 y RF-05). Por defecto la
 * petición va autenticada como el usuario 7 con rol USUARIO.
 */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, ManejadorErrores.class, UsuarioActualJwt.class})
@WithMockCustomUser(id = 7, rol = Rol.USUARIO)
class TicketControllerTest {

    private static final String JSON_VALIDO = """
            {"titulo":"No hay internet","descripcion":"Laboratorio 3 sin red",
             "categoria":"RED","prioridad":"ALTA"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TicketService ticketService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    UsuarioRepository usuarioRepository;

    @Test
    void crearResponde201ConLocationYElTicket() throws Exception {
        when(ticketService.crear(any(CrearTicketRequest.class), eq(7L))).thenReturn(new TicketResponse(
                15L, "TID-2026-00015", "No hay internet", "Laboratorio 3 sin red",
                Categoria.RED, Prioridad.ALTA, EstadoTicket.ABIERTO, 7L, null, null,
                Instant.parse("2026-11-12T10:00:00Z"), Instant.parse("2026-11-13T10:00:00Z"), false));

        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tickets/15"))
                .andExpect(jsonPath("$.folio").value("TID-2026-00015"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
    }

    @Test
    void crearConCamposVaciosResponde400ConDetallePorCampo() throws Exception {
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descripcion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores.categoria").exists())
                .andExpect(jsonPath("$.errores.prioridad").exists());

        verify(ticketService, never()).crear(any(), any());
    }

    @Test
    void crearConCategoriaInexistenteResponde400() throws Exception {
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO.replace("\"RED\"", "\"CAFETERA\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void crearConActivoInexistenteResponde404() throws Exception {
        when(ticketService.crear(any(CrearTicketRequest.class), eq(7L)))
                .thenThrow(new RecursoNoEncontradoException("No existe el activo con id 99"));

        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el activo con id 99"));
    }

    // ---------- RF-05: listar ----------

    @Test
    void listarResponde200ConLaPaginaYPasaFiltrosYRol() throws Exception {
        when(ticketService.listar(any(FiltroTickets.class), any(Pageable.class), eq(7L), eq(Rol.USUARIO)))
                .thenReturn(new PaginaResponse<>(List.of(ticketDeEjemplo()), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/tickets").param("estado", "ABIERTO").param("prioridad", "ALTA")
                        .param("vencido", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].folio").value("TID-2026-00015"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        ArgumentCaptor<FiltroTickets> filtro = ArgumentCaptor.forClass(FiltroTickets.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketService).listar(filtro.capture(), pagina.capture(), eq(7L), eq(Rol.USUARIO));
        assertThat(filtro.getValue()).isEqualTo(
                new FiltroTickets(EstadoTicket.ABIERTO, Prioridad.ALTA, null, false));
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "creadoEn"));
    }

    @Test
    void listarLimitaElTamanoDePaginaA100() throws Exception {
        when(ticketService.listar(any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 100, 0, 0));

        mvc.perform(get("/api/v1/tickets").param("size", "5000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketService).listar(any(), pagina.capture(), any(), any());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listarConEstadoInexistenteResponde400() throws Exception {
        mvc.perform(get("/api/v1/tickets").param("estado", "PERDIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listarOrdenandoPorCampoNoPermitidoResponde400() throws Exception {
        mvc.perform(get("/api/v1/tickets").param("sort", "descripcion,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void listarConPaginaNegativaResponde400() throws Exception {
        mvc.perform(get("/api/v1/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarConDireccionDeOrdenInvalidaResponde400() throws Exception {
        mvc.perform(get("/api/v1/tickets").param("sort", "folio,arriba"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Seguridad ----------

    @Test
    @WithAnonymousUser
    void crearSinTokenResponde401() throws Exception {
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).crear(any(), any());
    }

    @Test
    @WithAnonymousUser
    void listarSinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).listar(any(), any(), any(), any());
    }

    @Test
    @WithMockCustomUser(id = 3, rol = Rol.TECNICO)
    void listarUsaElIdYRolDelUsuarioAutenticado() throws Exception {
        when(ticketService.listar(any(), any(), eq(3L), eq(Rol.TECNICO)))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk());

        verify(ticketService).listar(any(), any(), eq(3L), eq(Rol.TECNICO));
    }

    // ---------- RF-08: cambio de estado ----------

    @Test
    @WithMockCustomUser(id = 3, rol = Rol.TECNICO)
    void cambiarEstadoResponde200ConElTicketActualizado() throws Exception {
        when(ticketService.cambiarEstado(eq(15L), any(CambiarEstadoRequest.class), eq(3L), eq(Rol.TECNICO)))
                .thenReturn(ticketEn(EstadoTicket.EN_PROCESO));

        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\",\"motivo\":\"Reviso el switch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));

        ArgumentCaptor<CambiarEstadoRequest> captor = ArgumentCaptor.forClass(CambiarEstadoRequest.class);
        verify(ticketService).cambiarEstado(eq(15L), captor.capture(), eq(3L), eq(Rol.TECNICO));
        assertThat(captor.getValue().motivo()).isEqualTo("Reviso el switch");
    }

    @Test
    void cambiarEstadoNoPermitidoResponde409ConLaRegla() throws Exception {
        when(ticketService.cambiarEstado(eq(15L), any(), any(), any()))
                .thenThrow(new TransicionNoPermitidaException(EstadoTicket.ABIERTO, EstadoTicket.RESUELTO,
                        "Desde ABIERTO solo se puede pasar a [ASIGNADO, CANCELADO]"));

        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTO\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Desde ABIERTO solo se puede pasar a [ASIGNADO, CANCELADO]"))
                .andExpect(jsonPath("$.estadoActual").value("ABIERTO"))
                .andExpect(jsonPath("$.estadoSolicitado").value("RESUELTO"));
    }

    @Test
    void cambiarEstadoSinPermisoResponde403() throws Exception {
        when(ticketService.cambiarEstado(eq(15L), any(), any(), any()))
                .thenThrow(new AccionNoPermitidaException("No tienes permiso para pasar este ticket a RESUELTO."));

        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void cambiarEstadoConModificacionSimultaneaResponde409() throws Exception {
        when(ticketService.cambiarEstado(eq(15L), any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Ticket.class, 15L));

        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Modificación concurrente"));
    }

    @Test
    void cambiarEstadoSinEstadoResponde400() throws Exception {
        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"sin estado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.estado").exists());

        verify(ticketService, never()).cambiarEstado(any(), any(), any(), any());
    }

    @Test
    void cambiarEstadoConEstadoInexistenteResponde400() throws Exception {
        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PERDIDO\"}"))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).cambiarEstado(any(), any(), any(), any());
    }

    @Test
    @WithAnonymousUser
    void cambiarEstadoSinTokenResponde401() throws Exception {
        mvc.perform(patch("/api/v1/tickets/15/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\"}"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).cambiarEstado(any(), any(), any(), any());
    }

    // ---------- Detalle ----------

    @Test
    void detalleResponde200ConTicketComentariosEHistorial() throws Exception {
        when(ticketService.detalle(15L, 7L, Rol.USUARIO)).thenReturn(new TicketDetalleResponse(
                ticketEn(EstadoTicket.ASIGNADO),
                List.of(new ComentarioResponse(1L, 15L, 7L, "Ana", "Sigue sin red", false,
                        Instant.parse("2026-11-12T11:00:00Z"))),
                List.of(new HistorialEstadoResponse(null, EstadoTicket.ABIERTO,
                        Instant.parse("2026-11-12T10:00:00Z"), null, 7L, "Ana"))));

        mvc.perform(get("/api/v1/tickets/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.folio").value("TID-2026-00015"))
                .andExpect(jsonPath("$.comentarios[0].texto").value("Sigue sin red"))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("ABIERTO"));
    }

    @Test
    void detalleDeTicketNoVisibleResponde404() throws Exception {
        when(ticketService.detalle(99L, 7L, Rol.USUARIO))
                .thenThrow(new RecursoNoEncontradoException("No existe el ticket con id 99"));

        mvc.perform(get("/api/v1/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithAnonymousUser
    void detalleSinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/tickets/15"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).detalle(any(), any(), any());
    }

    // ---------- RF-06: asignación ----------

    @Test
    @WithMockCustomUser(id = 3, rol = Rol.TECNICO)
    void unTecnicoSeAutoasignaSinCuerpo() throws Exception {
        when(ticketService.asignar(eq(15L), any(), eq(3L), eq(Rol.TECNICO)))
                .thenReturn(ticketEn(EstadoTicket.ASIGNADO));

        mvc.perform(patch("/api/v1/tickets/15/asignacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.tecnicoId").value(3));

        verify(ticketService).asignar(eq(15L), eq(null), eq(3L), eq(Rol.TECNICO));
    }

    @Test
    @WithMockCustomUser(id = 1, rol = Rol.ADMIN)
    void elAdminAsignaIndicandoElTecnico() throws Exception {
        when(ticketService.asignar(eq(15L), any(AsignarTicketRequest.class), eq(1L), eq(Rol.ADMIN)))
                .thenReturn(ticketEn(EstadoTicket.ASIGNADO));

        mvc.perform(patch("/api/v1/tickets/15/asignacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":3}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AsignarTicketRequest> captor = ArgumentCaptor.forClass(AsignarTicketRequest.class);
        verify(ticketService).asignar(eq(15L), captor.capture(), eq(1L), eq(Rol.ADMIN));
        assertThat(captor.getValue().tecnicoId()).isEqualTo(3L);
    }

    @Test
    @WithMockCustomUser(id = 1, rol = Rol.ADMIN)
    void asignarConTecnicoNegativoResponde400() throws Exception {
        mvc.perform(patch("/api/v1/tickets/15/asignacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tecnicoId").exists());

        verify(ticketService, never()).asignar(any(), any(), any(), any());
    }

    @Test
    void asignarComoUsuarioResponde403() throws Exception {
        when(ticketService.asignar(eq(15L), any(), any(), any()))
                .thenThrow(new AccionNoPermitidaException("Solo un técnico o un administrador pueden asignar tickets."));

        mvc.perform(patch("/api/v1/tickets/15/asignacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockCustomUser(id = 4, rol = Rol.TECNICO)
    void dosTecnicosAlMismoTiempoElSegundoRecibe409() throws Exception {
        when(ticketService.asignar(eq(15L), any(), eq(4L), eq(Rol.TECNICO)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Ticket.class, 15L));

        mvc.perform(patch("/api/v1/tickets/15/asignacion"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Modificación concurrente"));
    }

    @Test
    @WithAnonymousUser
    void asignarSinTokenResponde401() throws Exception {
        mvc.perform(patch("/api/v1/tickets/15/asignacion"))
                .andExpect(status().isUnauthorized());

        verify(ticketService, never()).asignar(any(), any(), any(), any());
    }

    private static TicketResponse ticketEn(EstadoTicket estado) {
        return new TicketResponse(15L, "TID-2026-00015", "No hay internet", "Laboratorio 3 sin red",
                Categoria.RED, Prioridad.ALTA, estado, 7L, 3L, null,
                Instant.parse("2026-11-12T10:00:00Z"), Instant.parse("2026-11-13T10:00:00Z"), false);
    }

    private static TicketResponse ticketDeEjemplo() {
        return new TicketResponse(15L, "TID-2026-00015", "No hay internet", "Laboratorio 3 sin red",
                Categoria.RED, Prioridad.ALTA, EstadoTicket.ABIERTO, 7L, null, null,
                Instant.parse("2026-11-12T10:00:00Z"), Instant.parse("2026-11-13T10:00:00Z"), false);
    }
}
