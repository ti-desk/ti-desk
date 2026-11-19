package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import mx.edu.tidesk.ti_desk.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

/**
 * Máquina de estados del ticket (RF-08): transiciones, fechas e historial.
 */
class MaquinaEstadosTicketTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T15:00:00Z");

    private HistorialEstadoRepository historial;
    private MaquinaEstadosTicket maquina;
    private Usuario tecnico;

    @BeforeEach
    void preparar() {
        historial = mock(HistorialEstadoRepository.class);
        maquina = new MaquinaEstadosTicket(historial, Clock.fixed(AHORA, ZoneOffset.UTC));
        tecnico = new Usuario();
        tecnico.setId(3L);
    }

    @Test
    void guardaElCambioEnElHistorialConAnteriorNuevoMotivoUsuarioYFecha() {
        Ticket ticket = ticketEn(EstadoTicket.ASIGNADO);

        maquina.cambiar(ticket, EstadoTicket.EN_PROCESO, tecnico, "Reviso el switch");

        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        HistorialEstado cambio = captor.getValue();
        assertThat(cambio.getTicket()).isSameAs(ticket);
        assertThat(cambio.getEstadoAnterior()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(cambio.getEstadoNuevo()).isEqualTo(EstadoTicket.EN_PROCESO);
        assertThat(cambio.getMotivo()).isEqualTo("Reviso el switch");
        assertThat(cambio.getUsuario()).isSameAs(tecnico);
        assertThat(cambio.getFecha()).isEqualTo(AHORA);
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.EN_PROCESO);
    }

    @Test
    void alAsignarGuardaLaFechaDeAsignacion() {
        Ticket ticket = ticketEn(EstadoTicket.ABIERTO);

        maquina.cambiar(ticket, EstadoTicket.ASIGNADO, tecnico, null);

        assertThat(ticket.getAsignadoEn()).isEqualTo(AHORA);
    }

    @Test
    void alResolverGuardaLaFechaDeResolucion() {
        Ticket ticket = ticketEn(EstadoTicket.EN_PROCESO);

        maquina.cambiar(ticket, EstadoTicket.RESUELTO, tecnico, null);

        assertThat(ticket.getResueltoEn()).isEqualTo(AHORA);
    }

    @Test
    void alCerrarGuardaLaFechaDeCierre() {
        Ticket ticket = ticketEn(EstadoTicket.RESUELTO);

        maquina.cambiar(ticket, EstadoTicket.CERRADO, tecnico, null);

        assertThat(ticket.getCerradoEn()).isEqualTo(AHORA);
    }

    @Test
    void alReabrirUnTicketResueltoBorraLaFechaDeResolucion() {
        Ticket ticket = ticketEn(EstadoTicket.RESUELTO);
        ticket.setResueltoEn(AHORA.minusSeconds(3600));

        maquina.cambiar(ticket, EstadoTicket.EN_PROCESO, tecnico, "Volvió a fallar");

        assertThat(ticket.getResueltoEn()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "ABIERTO,    EN_PROCESO",
            "ABIERTO,    RESUELTO",
            "ASIGNADO,   RESUELTO",
            "EN_PROCESO, CANCELADO",
            "EN_PROCESO, CERRADO",
            "EN_ESPERA,  RESUELTO",
            "RESUELTO,   CANCELADO"
    })
    void rechazaTransicionesFueraDelDiagrama(EstadoTicket actual, EstadoTicket nuevo) {
        Ticket ticket = ticketEn(actual);

        assertThatThrownBy(() -> maquina.cambiar(ticket, nuevo, tecnico, null))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("Desde " + actual + " solo se puede pasar a");
        assertThat(ticket.getEstado()).isEqualTo(actual);
        verify(historial, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTicket.class, names = {"CERRADO", "CANCELADO"})
    void unEstadoFinalNoCambia(EstadoTicket finalizado) {
        Ticket ticket = ticketEn(finalizado);

        assertThatThrownBy(() -> maquina.cambiar(ticket, EstadoTicket.EN_PROCESO, tecnico, null))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("es final");
        verify(historial, never()).save(any());
    }

    @Test
    void rechazaPasarAlMismoEstado() {
        Ticket ticket = ticketEn(EstadoTicket.EN_PROCESO);

        assertThatThrownBy(() -> maquina.cambiar(ticket, EstadoTicket.EN_PROCESO, tecnico, null))
                .isInstanceOf(TransicionNoPermitidaException.class)
                .hasMessageContaining("ya está en EN_PROCESO");
    }

    @Test
    void registrarCreacionGuardaElPrimerRenglonSinEstadoAnterior() {
        Usuario solicitante = new Usuario();
        solicitante.setId(7L);
        Ticket ticket = ticketEn(EstadoTicket.ABIERTO);
        ticket.setSolicitante(solicitante);
        ticket.setCreadoEn(AHORA);

        maquina.registrarCreacion(ticket);

        ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isNull();
        assertThat(captor.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(captor.getValue().getUsuario()).isSameAs(solicitante);
        assertThat(captor.getValue().getFecha()).isEqualTo(AHORA);
    }

    private static Ticket ticketEn(EstadoTicket estado) {
        Ticket ticket = new Ticket();
        ticket.setId(15L);
        ticket.setEstado(estado);
        return ticket;
    }
}
