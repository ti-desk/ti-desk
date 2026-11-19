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
import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.ParametroInvalidoException;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.sla.PoliticaSla;
import mx.edu.tidesk.ti_desk.sla.PoliticaSlaRepository;
import mx.edu.tidesk.ti_desk.ticket.dto.AsignarTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.CambiarEstadoRequest;
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
import org.mockito.ArgumentCaptor;
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
    private HistorialEstadoRepository historial;
    private TicketService servicio;
    private Usuario solicitante;

    @BeforeEach
    void preparar() {
        tickets = mock(TicketRepository.class);
        usuarios = mock(UsuarioRepository.class);
        activos = mock(ActivoRepository.class);
        politicas = mock(PoliticaSlaRepository.class);
        generadorFolio = mock(GeneradorFolio.class);
        historial = mock(HistorialEstadoRepository.class);
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        servicio = new TicketService(tickets, usuarios, activos, politicas, generadorFolio,
                new MaquinaEstadosTicket(historial, reloj), reloj);

        solicitante = new Usuario();
        solicitante.setId(7L);
        when(usuarios.findById(7L)).thenReturn(Optional.of(solicitante));
        when(generadorFolio.siguiente(2026)).thenReturn("TID-2026-00001");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(tickets.saveAndFlush(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarios.getReferenceById(any())).thenAnswer(inv -> usuario(inv.getArgument(0)));
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

    @Test
    void crearDejaElPrimerRenglonEnElHistorial() {
        servicio.crear(peticion(Prioridad.MEDIA, null), 7L);

        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isNull();
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.ABIERTO);
    }

    // ---------- RF-08: cambio de estado ----------

    @Test
    void elTecnicoAsignadoPoneElTicketEnProceso() {
        Ticket ticket = visible(ticket(EstadoTicket.ASIGNADO, 3L));

        TicketResponse respuesta = servicio.cambiarEstado(15L,
                new CambiarEstadoRequest(EstadoTicket.EN_PROCESO, "  Reviso el switch  "), 3L, Rol.TECNICO);

        assertThat(respuesta.estado()).isEqualTo(EstadoTicket.EN_PROCESO);
        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getMotivo()).isEqualTo("Reviso el switch");
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(3L);
        verify(tickets).saveAndFlush(ticket);
    }

    @Test
    void responde404SiElTicketNoExisteONoEsVisible() {
        when(tickets.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarEstado(99L,
                new CambiarEstadoRequest(EstadoTicket.CANCELADO, null), 7L, Rol.USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("ticket");
    }

    @Test
    void pasarAAsignadoPorEsteEndpointSeRechazaConLaRegla() {
        visible(ticket(EstadoTicket.ABIERTO, null));

        assertThatThrownBy(() -> servicio.cambiarEstado(15L,
                new CambiarEstadoRequest(EstadoTicket.ASIGNADO, null), 1L, Rol.ADMIN))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("/asignacion");
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void unaTransicionNoPermitidaNoGuardaNada() {
        visible(ticket(EstadoTicket.ABIERTO, null));

        assertThatThrownBy(() -> servicio.cambiarEstado(15L,
                new CambiarEstadoRequest(EstadoTicket.RESUELTO, null), 1L, Rol.ADMIN))
                .isInstanceOf(TransicionNoPermitidaException.class);
        verify(historial, never()).save(any());
        verify(tickets, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({
            // estado actual, nuevo, usuario, rol, técnico asignado (vacío = sin técnico), ¿permitido?
            "ABIERTO,    CANCELADO,  7, USUARIO, ,  true",
            "ABIERTO,    CANCELADO,  8, USUARIO, ,  false",
            "ABIERTO,    CANCELADO,  3, TECNICO, ,  true",
            "ASIGNADO,   CANCELADO,  4, TECNICO, 3, false",
            "ASIGNADO,   EN_PROCESO, 3, TECNICO, 3, true",
            "ASIGNADO,   EN_PROCESO, 4, TECNICO, 3, false",
            "ASIGNADO,   EN_PROCESO, 7, USUARIO, 3, false",
            "EN_PROCESO, RESUELTO,   3, TECNICO, 3, true",
            "EN_PROCESO, RESUELTO,   7, USUARIO, 3, false",
            "EN_PROCESO, EN_ESPERA,  3, TECNICO, 3, true",
            "EN_ESPERA,  EN_PROCESO, 7, USUARIO, 3, true",
            "RESUELTO,   CERRADO,    7, USUARIO, 3, true",
            "RESUELTO,   CERRADO,    3, TECNICO, 3, false",
            "RESUELTO,   EN_PROCESO, 7, USUARIO, 3, true",
            "EN_PROCESO, RESUELTO,   1, ADMIN,   3, true"
    })
    void soloQuienLeTocaPuedeHacerCadaCambio(EstadoTicket actual, EstadoTicket nuevo, Long usuarioId,
                                             Rol rol, Long tecnicoId, boolean permitido) {
        Ticket ticket = ticket(actual, tecnicoId);

        if (permitido) {
            TicketService.verificarPermiso(ticket, nuevo, usuarioId, rol);
        } else {
            assertThatThrownBy(() -> TicketService.verificarPermiso(ticket, nuevo, usuarioId, rol))
                    .isInstanceOf(AccionNoPermitidaException.class)
                    .hasMessageContaining(nuevo.name());
        }
    }

    @Test
    void unUsuarioSinPermisoRecibe403YNoSeGuardaNada() {
        visible(ticket(EstadoTicket.EN_PROCESO, 3L));

        assertThatThrownBy(() -> servicio.cambiarEstado(15L,
                new CambiarEstadoRequest(EstadoTicket.RESUELTO, null), 7L, Rol.USUARIO))
                .isInstanceOf(AccionNoPermitidaException.class);
        verify(historial, never()).save(any());
        verify(tickets, never()).saveAndFlush(any());
    }

    // ---------- RF-06: asignación ----------

    @Test
    void elTecnicoSeAutoasignaUnTicketAbierto() {
        Ticket ticket = existente(ticket(EstadoTicket.ABIERTO, null));
        tecnicoActivo(3L);

        TicketResponse respuesta = servicio.asignar(15L, null, 3L, Rol.TECNICO);

        assertThat(respuesta.estado()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(respuesta.tecnicoId()).isEqualTo(3L);
        assertThat(ticket.getAsignadoEn()).isEqualTo(AHORA);
        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.ASIGNADO);
        verify(tickets).saveAndFlush(ticket);
    }

    @Test
    void elAdminAsignaAUnTecnicoConMotivo() {
        existente(ticket(EstadoTicket.ABIERTO, null));
        tecnicoActivo(3L);

        TicketResponse respuesta = servicio.asignar(15L,
                new AsignarTicketRequest(3L, "  Es de redes  "), 1L, Rol.ADMIN);

        assertThat(respuesta.tecnicoId()).isEqualTo(3L);
        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getMotivo()).isEqualTo("Es de redes");
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(1L);
    }

    @Test
    void unUsuarioNoPuedeAsignar() {
        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(3L, null), 7L, Rol.USUARIO))
                .isInstanceOf(AccionNoPermitidaException.class);
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void unTecnicoNoPuedeAsignarleElTicketAOtro() {
        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(4L, null), 3L, Rol.TECNICO))
                .isInstanceOf(AccionNoPermitidaException.class)
                .hasMessageContaining("sí mismo");
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void elAdminDebeIndicarElTecnico() {
        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(null, null), 1L, Rol.ADMIN))
                .isInstanceOf(ParametroInvalidoException.class)
                .hasMessageContaining("tecnicoId");
    }

    @Test
    void noSePuedeAsignarAQuienNoEsTecnicoActivo() {
        existente(ticket(EstadoTicket.ABIERTO, null));
        Usuario inactivo = usuario(3L);
        inactivo.setRol(Rol.TECNICO);
        inactivo.setActivo(false);
        when(usuarios.findById(3L)).thenReturn(Optional.of(inactivo));
        when(usuarios.findById(7L)).thenReturn(Optional.of(solicitante));

        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(3L, null), 1L, Rol.ADMIN))
                .isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(7L, null), 1L, Rol.ADMIN))
                .isInstanceOf(ParametroInvalidoException.class);
        verify(tickets, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"ASIGNADO", "EN_PROCESO", "RESUELTO"})
    void elTecnicoQuePierdeRecibe409YNo404(EstadoTicket estado) {
        // CP-019: el ticket ya es de otro técnico, así que no pasaría la visibilidad de RF-05.
        existente(ticket(estado, 4L));
        tecnicoActivo(3L);

        assertThatThrownBy(() -> servicio.asignar(15L, null, 3L, Rol.TECNICO))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("ya lo tomó otro técnico");
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void unTecnicoNoPuedeVolverATomarSuPropioTicket() {
        existente(ticket(EstadoTicket.ASIGNADO, 3L));
        tecnicoActivo(3L);

        assertThatThrownBy(() -> servicio.asignar(15L, null, 3L, Rol.TECNICO))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("solo un administrador puede reasignarlo");
    }

    @Test
    void asignarUnTicketQueNoExisteResponde404() {
        when(tickets.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(99L, null, 3L, Rol.TECNICO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("ticket");
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void elAdminReasignaUnTicketEnProcesoSinCambiarSuEstado() {
        Ticket ticket = existente(ticket(EstadoTicket.EN_PROCESO, 4L));
        tecnicoActivo(3L);

        TicketResponse respuesta = servicio.asignar(15L, new AsignarTicketRequest(3L, null), 1L, Rol.ADMIN);

        assertThat(respuesta.tecnicoId()).isEqualTo(3L);
        assertThat(respuesta.estado()).isEqualTo(EstadoTicket.EN_PROCESO);
        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoTicket.EN_PROCESO);
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.EN_PROCESO);
        assertThat(captor.getValue().getMotivo()).isEqualTo("Reasignado a otro técnico");
        verify(tickets).saveAndFlush(ticket);
    }

    @Test
    void reasignarAlMismoTecnicoSeRechaza() {
        existente(ticket(EstadoTicket.ASIGNADO, 3L));
        tecnicoActivo(3L);

        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(3L, null), 1L, Rol.ADMIN))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("ya está asignado a ese técnico");
    }

    @ParameterizedTest
    @CsvSource({"RESUELTO", "CERRADO", "CANCELADO"})
    void unTicketTerminadoYaNoSeAsigna(EstadoTicket estado) {
        existente(ticket(estado, 4L));
        tecnicoActivo(3L);

        assertThatThrownBy(() -> servicio.asignar(15L, new AsignarTicketRequest(3L, null), 1L, Rol.ADMIN))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("ya no se puede asignar");
        verify(tickets, never()).saveAndFlush(any());
    }

    private void tecnicoActivo(Long id) {
        Usuario tecnico = usuario(id);
        tecnico.setRol(Rol.TECNICO);
        when(usuarios.findById(id)).thenReturn(Optional.of(tecnico));
    }

    private Ticket existente(Ticket ticket) {
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        return ticket;
    }

    @SuppressWarnings("unchecked")
    private Ticket visible(Ticket ticket) {
        when(tickets.findOne(any(Specification.class))).thenReturn(Optional.of(ticket));
        return ticket;
    }

    private Ticket ticket(EstadoTicket estado, Long tecnicoId) {
        Ticket ticket = new Ticket();
        ticket.setId(15L);
        ticket.setFolio("TID-2026-00015");
        ticket.setEstado(estado);
        ticket.setSolicitante(solicitante);
        if (tecnicoId != null) {
            ticket.setTecnico(usuario(tecnicoId));
        }
        return ticket;
    }

    private static Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }
}
