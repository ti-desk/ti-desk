package mx.edu.tidesk.ti_desk.comentario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;
import mx.edu.tidesk.ti_desk.comentario.dto.CrearComentarioRequest;
import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.Ticket;
import mx.edu.tidesk.ti_desk.ticket.TicketService;
import mx.edu.tidesk.ti_desk.usuario.CambioNoPermitidoException;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

/**
 * Comentarios de tickets (RF-07).
 */
class ComentarioServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T15:00:00Z");

    private ComentarioRepository comentarios;
    private TicketService ticketService;
    private UsuarioRepository usuarios;
    private ComentarioService servicio;
    private Ticket ticket;

    @BeforeEach
    void preparar() {
        comentarios = mock(ComentarioRepository.class);
        ticketService = mock(TicketService.class);
        usuarios = mock(UsuarioRepository.class);
        servicio = new ComentarioService(comentarios, ticketService, usuarios,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        ticket = new Ticket();
        ticket.setId(15L);
        ticket.setEstado(EstadoTicket.ASIGNADO);
        when(ticketService.buscarVisible(any(), any(), any())).thenReturn(ticket);
        when(usuarios.findById(any())).thenAnswer(inv -> Optional.of(usuario(inv.getArgument(0))));
        when(comentarios.save(any(Comentario.class))).thenAnswer(inv -> {
            Comentario c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
    }

    @Test
    void elSolicitanteComentaYSeGuardaConAutorTextoYFecha() {
        ComentarioResponse respuesta = servicio.comentar(15L,
                new CrearComentarioRequest("  Sigue sin red  ", false), 7L, Rol.USUARIO);

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.ticketId()).isEqualTo(15L);
        assertThat(respuesta.autorId()).isEqualTo(7L);
        assertThat(respuesta.autorNombre()).isEqualTo("Usuario 7");
        assertThat(respuesta.texto()).isEqualTo("Sigue sin red");
        assertThat(respuesta.interno()).isFalse();
        assertThat(respuesta.creadoEn()).isEqualTo(AHORA);
        verify(ticketService).buscarVisible(15L, 7L, Rol.USUARIO);
    }

    @Test
    void unUsuarioNoPuedeEscribirComentariosInternos() {
        assertThatThrownBy(() -> servicio.comentar(15L,
                new CrearComentarioRequest("Nota", true), 7L, Rol.USUARIO))
                .isInstanceOf(AccionNoPermitidaException.class);
        verify(comentarios, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"3, TECNICO", "1, ADMIN"})
    void tecnicosYAdminsPuedenEscribirComentariosInternos(Long id, Rol rol) {
        ComentarioResponse respuesta = servicio.comentar(15L,
                new CrearComentarioRequest("Revisar el switch del piso 2", true), id, rol);

        assertThat(respuesta.interno()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTicket.class, names = {"CERRADO", "CANCELADO"})
    void noSeComentaUnTicketCerradoOCancelado(EstadoTicket estado) {
        ticket.setEstado(estado);

        assertThatThrownBy(() -> servicio.comentar(15L,
                new CrearComentarioRequest("Hola", false), 7L, Rol.USUARIO))
                .isInstanceOf(CambioNoPermitidoException.class)
                .hasMessageContaining(estado.name());
        verify(comentarios, never()).save(any());
    }

    @Test
    void elPrimerComentarioDelTecnicoMarcaLaPrimeraRespuesta() {
        servicio.comentar(15L, new CrearComentarioRequest("Voy para allá", false), 3L, Rol.TECNICO);

        assertThat(ticket.getPrimeraRespuestaEn()).isEqualTo(AHORA);
    }

    @Test
    void laPrimeraRespuestaNoSeSobrescribe() {
        Instant antes = AHORA.minusSeconds(600);
        ticket.setPrimeraRespuestaEn(antes);

        servicio.comentar(15L, new CrearComentarioRequest("Otro avance", false), 3L, Rol.TECNICO);

        assertThat(ticket.getPrimeraRespuestaEn()).isEqualTo(antes);
    }

    @Test
    void elComentarioDelSolicitanteNoCuentaComoPrimeraRespuesta() {
        servicio.comentar(15L, new CrearComentarioRequest("¿Hay avance?", false), 7L, Rol.USUARIO);

        assertThat(ticket.getPrimeraRespuestaEn()).isNull();
    }

    @Test
    void siElTicketNoEsVisibleResponde404YNoGuarda() {
        when(ticketService.buscarVisible(99L, 8L, Rol.USUARIO))
                .thenThrow(new RecursoNoEncontradoException("No existe el ticket con id 99"));

        assertThatThrownBy(() -> servicio.comentar(99L,
                new CrearComentarioRequest("Hola", false), 8L, Rol.USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.listar(99L, 8L, Rol.USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(comentarios, never()).save(any());
        verify(comentarios, never()).findDelTicket(any(), anyBoolean());
    }

    @ParameterizedTest
    @CsvSource({"USUARIO, false", "TECNICO, true", "ADMIN, true"})
    void soloTecnicosYAdminsVenLosComentariosInternos(Rol rol, boolean incluyeInternos) {
        Comentario publico = new Comentario();
        publico.setTicket(ticket);
        publico.setAutor(usuario(7L));
        publico.setTexto("Sigue sin red");
        when(comentarios.findDelTicket(15L, incluyeInternos)).thenReturn(List.of(publico));

        List<ComentarioResponse> lista = servicio.listar(15L, 7L, rol);

        assertThat(lista).extracting(ComentarioResponse::texto).containsExactly("Sigue sin red");
        verify(comentarios).findDelTicket(15L, incluyeInternos);
    }

    @Test
    void guardaLaRelacionConElTicket() {
        servicio.comentar(15L, new CrearComentarioRequest("Hola", false), 7L, Rol.USUARIO);

        ArgumentCaptor<Comentario> captor = ArgumentCaptor.forClass(Comentario.class);
        verify(comentarios).save(captor.capture());
        assertThat(captor.getValue().getTicket()).isSameAs(ticket);
    }

    private static Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario " + id);
        return u;
    }
}
