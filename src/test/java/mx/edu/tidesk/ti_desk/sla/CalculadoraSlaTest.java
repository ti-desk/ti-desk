package mx.edu.tidesk.ti_desk.sla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;

@ExtendWith(MockitoExtension.class)
class CalculadoraSlaTest {

    private static final Instant CREADO = Instant.parse("2026-11-16T10:00:00Z");
    private static final Instant LIMITE_48H = CREADO.plus(Duration.ofHours(48));

    @Mock
    private PoliticaSlaRepository politicas;

    private CalculadoraSla calculadoraEn(Instant ahora) {
        return new CalculadoraSla(Clock.fixed(ahora, ZoneOffset.UTC), politicas);
    }

    private static PoliticaSla politica(Prioridad prioridad, String primera, String resolucion) {
        PoliticaSla politica = new PoliticaSla();
        politica.setPrioridad(prioridad);
        politica.setHorasPrimeraRespuesta(new BigDecimal(primera));
        politica.setHorasResolucion(new BigDecimal(resolucion));
        return politica;
    }

    @ParameterizedTest
    @CsvSource({"CRITICA,0.5,4", "ALTA,2,24", "MEDIA,8,48", "BAJA,24,72"})
    void fechaLimiteSumaLasHorasDeResolucionDeCadaPrioridad(Prioridad prioridad, String primera, long horas) {
        when(politicas.findById(prioridad))
                .thenReturn(Optional.of(politica(prioridad, primera, String.valueOf(horas))));

        Instant limite = calculadoraEn(CREADO).calcularFechaLimite(CREADO, prioridad);

        assertEquals(CREADO.plus(Duration.ofHours(horas)), limite);
    }

    @Test
    void sinPoliticaLanzaExcepcion() {
        when(politicas.findById(Prioridad.ALTA)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> calculadoraEn(CREADO).calcularFechaLimite(CREADO, Prioridad.ALTA));
    }

    @Test
    void enElSegundoExactoDelLimiteNoEstaVencido() {
        assertFalse(calculadoraEn(LIMITE_48H).estaVencido(LIMITE_48H, EstadoTicket.EN_PROCESO));
    }

    @Test
    void unSegundoDespuesDelLimiteEstaVencido() {
        assertTrue(calculadoraEn(LIMITE_48H.plusSeconds(1)).estaVencido(LIMITE_48H, EstadoTicket.EN_PROCESO));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTicket.class, names = {"RESUELTO", "CERRADO", "CANCELADO"})
    void losEstadosFinalesNuncaEstanVencidosNiEnRiesgo(EstadoTicket estado) {
        Instant muyTarde = LIMITE_48H.plus(Duration.ofDays(10));
        assertFalse(calculadoraEn(muyTarde).estaVencido(LIMITE_48H, estado));
        assertFalse(calculadoraEn(CREADO.plus(Duration.ofHours(40)))
                .estaEnRiesgo(CREADO, LIMITE_48H, estado));
    }

    @Test
    void antesDelOchentaPorCientoNoEstaEnRiesgo() {
        // 80 % de 48 h = 38 h 24 min
        Instant ahora = CREADO.plus(Duration.ofHours(38).plusMinutes(23).plusSeconds(59));
        assertFalse(calculadoraEn(ahora).estaEnRiesgo(CREADO, LIMITE_48H, EstadoTicket.EN_PROCESO));
    }

    @Test
    void enElOchentaPorCientoExactoEstaEnRiesgo() {
        Instant ahora = CREADO.plus(Duration.ofHours(38).plusMinutes(24));
        assertTrue(calculadoraEn(ahora).estaEnRiesgo(CREADO, LIMITE_48H, EstadoTicket.EN_PROCESO));
    }

    @Test
    void unTicketVencidoYaNoCuentaComoEnRiesgo() {
        Instant ahora = LIMITE_48H.plusSeconds(1);
        assertFalse(calculadoraEn(ahora).estaEnRiesgo(CREADO, LIMITE_48H, EstadoTicket.EN_PROCESO));
    }
}