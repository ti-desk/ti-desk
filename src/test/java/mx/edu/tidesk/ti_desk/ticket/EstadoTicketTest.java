package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EstadoTicketTest {

    @Test
    void contieneLosSieteEstadosDelAnteproyecto() {
        assertThat(EstadoTicket.values()).containsExactly(
                EstadoTicket.ABIERTO,
                EstadoTicket.ASIGNADO,
                EstadoTicket.EN_PROCESO,
                EstadoTicket.EN_ESPERA,
                EstadoTicket.RESUELTO,
                EstadoTicket.CERRADO,
                EstadoTicket.CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTicket.class, names = {"CERRADO", "CANCELADO"})
    void cerradoYCanceladoSonEstadosFinales(EstadoTicket estado) {
        assertThat(estado.esFinal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTicket.class, names = {"CERRADO", "CANCELADO"},
            mode = EnumSource.Mode.EXCLUDE)
    void losDemasEstadosNoSonFinales(EstadoTicket estado) {
        assertThat(estado.esFinal()).isFalse();
    }
}
