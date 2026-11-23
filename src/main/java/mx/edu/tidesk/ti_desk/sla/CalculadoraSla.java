package mx.edu.tidesk.ti_desk.sla;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;

@Component
public class CalculadoraSla {

    private static final Set<EstadoTicket> ESTADOS_FINALES =
            EnumSet.of(EstadoTicket.RESUELTO, EstadoTicket.CERRADO, EstadoTicket.CANCELADO);
    private static final long PORCENTAJE_RIESGO = 80;
    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    private final Clock clock;
    private final PoliticaSlaRepository politicas;

    public CalculadoraSla(Clock clock, PoliticaSlaRepository politicas) {
        this.clock = clock;
        this.politicas = politicas;
    }

    public Duration duracionResolucion(Prioridad prioridad) {
        PoliticaSla politica = politicas.findById(prioridad)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay política de SLA para la prioridad " + prioridad));
        long segundos = politica.getHorasResolucion()
                .multiply(SEGUNDOS_POR_HORA)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return Duration.ofSeconds(segundos);
    }

    public Instant calcularFechaLimite(Instant creadoEn, Prioridad prioridad) {
        return creadoEn.plus(duracionResolucion(prioridad));
    }

    public boolean estaVencido(Instant fechaLimite, EstadoTicket estado) {
        return !ESTADOS_FINALES.contains(estado) && Instant.now(clock).isAfter(fechaLimite);
    }

    public boolean estaEnRiesgo(Instant creadoEn, Instant fechaLimite, EstadoTicket estado) {
        if (ESTADOS_FINALES.contains(estado)) {
            return false;
        }
        Instant ahora = Instant.now(clock);
        if (ahora.isAfter(fechaLimite)) {
            return false; // ya está vencido, no "en riesgo"
        }
        long total = Duration.between(creadoEn, fechaLimite).toMillis();
        long consumido = Duration.between(creadoEn, ahora).toMillis();
        return consumido * 100 >= total * PORCENTAJE_RIESGO;
    }
}