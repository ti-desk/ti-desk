package mx.edu.tidesk.ti_desk.ticket;

import java.time.Clock;
import java.time.Instant;

import mx.edu.tidesk.ti_desk.usuario.Usuario;
import org.springframework.stereotype.Component;

/**
 * Aplica la máquina de estados del ticket (RF-08): valida la transición,
 * actualiza las fechas del ticket y deja el cambio en historial_estado.
 * La usan el cambio de estado y la asignación (RF-06), para que ambos
 * sigan las mismas reglas.
 */
@Component
public class MaquinaEstadosTicket {

    private final HistorialEstadoRepository historial;
    private final Clock clock;

    public MaquinaEstadosTicket(HistorialEstadoRepository historial, Clock clock) {
        this.historial = historial;
        this.clock = clock;
    }

    /**
     * Cambia el estado del ticket o lanza TransicionNoPermitidaException con la regla violada.
     *
     * @param autor  usuario que hace el cambio; null si lo hace el sistema
     * @param motivo texto opcional que se guarda en el historial
     */
    public void cambiar(Ticket ticket, EstadoTicket nuevo, Usuario autor, String motivo) {
        EstadoTicket actual = ticket.getEstado();
        validar(actual, nuevo);

        Instant ahora = Instant.now(clock);
        ticket.setEstado(nuevo);
        switch (nuevo) {
            case ASIGNADO -> ticket.setAsignadoEn(ahora);
            case RESUELTO -> ticket.setResueltoEn(ahora);
            case CERRADO -> ticket.setCerradoEn(ahora);
            case EN_PROCESO -> {
                if (actual == EstadoTicket.RESUELTO) {
                    // Reapertura: deja de contar como resuelto.
                    ticket.setResueltoEn(null);
                }
            }
            default -> {
                // EN_ESPERA y CANCELADO no tienen fecha propia en el ticket.
            }
        }
        registrar(ticket, actual, nuevo, autor, motivo, ahora);
    }

    /**
     * Primer renglón del historial: el ticket nace en ABIERTO (estado anterior NULL).
     */
    public void registrarCreacion(Ticket ticket) {
        registrar(ticket, null, ticket.getEstado(), ticket.getSolicitante(), null, ticket.getCreadoEn());
    }

    static void validar(EstadoTicket actual, EstadoTicket nuevo) {
        if (actual == nuevo) {
            throw new TransicionNoPermitidaException(actual, nuevo,
                    "El ticket ya está en " + actual);
        }
        if (actual.esFinal()) {
            throw new TransicionNoPermitidaException(actual, nuevo,
                    "Un ticket " + actual + " es final y ya no puede cambiar de estado");
        }
        if (!actual.puedePasarA(nuevo)) {
            throw new TransicionNoPermitidaException(actual, nuevo,
                    "Desde " + actual + " solo se puede pasar a " + actual.siguientes());
        }
    }

    private void registrar(Ticket ticket, EstadoTicket anterior, EstadoTicket nuevo,
                           Usuario autor, String motivo, Instant fecha) {
        HistorialEstado cambio = new HistorialEstado();
        cambio.setTicket(ticket);
        cambio.setUsuario(autor);
        cambio.setEstadoAnterior(anterior);
        cambio.setEstadoNuevo(nuevo);
        cambio.setMotivo(motivo);
        cambio.setFecha(fecha);
        historial.save(cambio);
    }
}
