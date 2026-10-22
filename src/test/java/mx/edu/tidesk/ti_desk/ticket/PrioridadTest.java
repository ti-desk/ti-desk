package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrioridadTest {

    @Test
    void contieneLasCuatroPrioridadesDeMayorAMenorUrgencia() {
        assertThat(Prioridad.values()).containsExactly(
                Prioridad.CRITICA, Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA);
    }

    @Test
    void sePuedeObtenerLaPrioridadPorNombre() {
        assertThat(Prioridad.valueOf("CRITICA")).isEqualTo(Prioridad.CRITICA);
    }
}
