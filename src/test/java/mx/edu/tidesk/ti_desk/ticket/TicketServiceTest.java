package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import mx.edu.tidesk.ti_desk.activo.Activo;
import mx.edu.tidesk.ti_desk.activo.ActivoRepository;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.sla.PoliticaSla;
import mx.edu.tidesk.ti_desk.sla.PoliticaSlaRepository;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.PaginaResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class TicketServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-12T10:00:00Z");

    private TicketRepository tickets;
    private UsuarioRepository usuarios;
    private ActivoRepository activos;
    private PoliticaSlaRepository politicas;
    private GeneradorFolio generadorFolio;
    private TicketService servicio;
    private Usuario solicitante;

    @BeforeEach
    void preparar() {
        tickets = mock(TicketRepository.class);
        usuarios = mock(UsuarioRepository.class);
        activos = mock(ActivoRepository.class);
        politicas = mock(PoliticaSlaRepository.class);
        generadorFolio = mock(GeneradorFolio.class);
        servicio = new TicketService(tickets, usuarios, activos, politicas, generadorFolio,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        solicitante = new Usuario();
        solicitante.setId(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(solicitante));
        when(generadorFolio.siguiente(2026)).thenReturn("TID-2026-00001");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        for (Prioridad p : Prioridad.values()) {
            when(politicas.findById(p)).thenReturn(Optional.of(politica(p)));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "CRITICA, 2026-11-12T14:00:00Z",
            "ALTA,    2026-11-13T10:00:00Z",
            "MEDIA,   2026-11-14T10:00:00Z",
            "BAJA,    2026-11-15T10:00:00Z"
    })
    void calculaLaFechaLimiteSegunLaPrioridad(Prioridad prioridad, String limiteEsperado) {
        TicketResponse creado = servicio.crear(peticion(prioridad, null), 7L);

        assertThat(creado.fechaLimiteSla()).isEqualTo(Instant.parse(limiteEsperado));
    }

    @Test
    void creaElTicketAbiertoConFolioYSolicitante() {
        TicketResponse creado = servicio.crear(peticion(Prioridad.MEDIA, null), 7L);

        assertThat(creado.id()).isEqualTo(100L);
        assertThat(creado.folio()).isEqualTo("TID-2026-00001");
        assertThat(creado.estado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(creado.solicitanteId()).isEqualTo(7L);
        assertThat(creado.creadoEn()).isEqualTo(AHORA);
        assertThat(creado.activoId()).isNull();
        assertThat(creado.titulo()).isEqualTo("No hay internet");
    }

    @Test
    void vinculaElActivoCuandoSeEnvia() {
        Activo activo = new Activo();
        activo.setId(5L);
        when(activos.findById(5L)).thenReturn(Optional.of(activo));

        TicketResponse creado = servicio.crear(peticion(Prioridad.ALTA, 5L), 7L);

        assertThat(creado.activoId()).isEqualTo(5L);
    }

    @Test
    void responde404SiElActivoNoExiste() {
        when(activos.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(peticion(Prioridad.ALTA, 99L), 7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("activo");
        verify(tickets, never()).save(any());
    }

    @Test
    void responde404SiElSolicitanteNoExiste() {
        when(usuarios.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(peticion(Prioridad.BAJA, null), 8L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("usuario");
        verify(tickets, never()).save(any());
    }

    private static CrearTicketRequest peticion(Prioridad prioridad, Long activoId) {
        return new CrearTicketRequest("  No hay internet  ", "El laboratorio 3 no tiene red",
                Categoria.RED, prioridad, activoId);
    }

    private static PoliticaSla politica(Prioridad prioridad) {
        PoliticaSla p = new PoliticaSla();
        p.setPrioridad(prioridad);
        p.setHorasPrimeraRespuesta(BigDecimal.ONE);
        p.setHorasResolucion(switch (prioridad) {
            case CRITICA -> new BigDecimal("4");
            case ALTA -> new BigDecimal("24");
            case MEDIA -> new BigDecimal("48");
            case BAJA -> new BigDecimal("72");
        });
        return p;
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarConvierteLaPaginaDeEntidadesADto() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setFolio("TID-2026-00001");
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setSolicitante(solicitante);
        Pageable pagina = PageRequest.of(0, 20);
        when(tickets.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket), pagina, 1));

        PaginaResponse<TicketResponse> resultado =
                servicio.listar(FiltroTickets.sinFiltros(), pagina, 7L, Rol.USUARIO);

        assertThat(resultado.contenido()).extracting(TicketResponse::folio).containsExactly("TID-2026-00001");
        assertThat(resultado.contenido().get(0).solicitanteId()).isEqualTo(7L);
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.pagina()).isZero();
        assertThat(resultado.tamano()).isEqualTo(20);
    }
}
