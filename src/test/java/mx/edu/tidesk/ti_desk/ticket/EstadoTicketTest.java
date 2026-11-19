package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    /** Transiciones del anteproyecto, sección 7.2. */
    @ParameterizedTest
    @CsvSource({
            "ABIERTO,    ASIGNADO CANCELADO",
            "ASIGNADO,   EN_PROCESO CANCELADO",
            "EN_PROCESO, EN_ESPERA RESUELTO",
            "EN_ESPERA,  EN_PROCESO",
            "RESUELTO,   EN_PROCESO CERRADO",
            "CERRADO,    ''",
            "CANCELADO,  ''"
    })
    void cadaEstadoTieneSoloSusTransicionesPermitidas(EstadoTicket actual, String esperados) {
        Set<EstadoTicket> permitidos = esperados.isBlank()
                ? EnumSet.noneOf(EstadoTicket.class)
                : Arrays.stream(esperados.split(" ")).map(EstadoTicket::valueOf)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoTicket.class)));

        assertThat(actual.siguientes()).isEqualTo(permitidos);
        for (EstadoTicket nuevo : EstadoTicket.values()) {
            assertThat(actual.puedePasarA(nuevo)).isEqualTo(permitidos.contains(nuevo));
        }
    }
}
