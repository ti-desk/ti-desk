package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import mx.edu.tidesk.ti_desk.comun.ManejadorGlobalErrores;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.PaginaResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
@Import(ManejadorGlobalErrores.class)
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
    UsuarioActual usuarioActual;

    @BeforeEach
    void preparar() {
        when(usuarioActual.id()).thenReturn(7L);
        when(usuarioActual.rol()).thenReturn(Rol.USUARIO);
    }

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

    private static TicketResponse ticketDeEjemplo() {
        return new TicketResponse(15L, "TID-2026-00015", "No hay internet", "Laboratorio 3 sin red",
                Categoria.RED, Prioridad.ALTA, EstadoTicket.ABIERTO, 7L, null, null,
                Instant.parse("2026-11-12T10:00:00Z"), Instant.parse("2026-11-13T10:00:00Z"), false);
    }
}
