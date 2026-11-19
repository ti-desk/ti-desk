package mx.edu.tidesk.ti_desk.ticket.dto;

import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;

/**
 * Filtros opcionales del listado de tickets (RF-05). Un valor null no filtra.
 */
public record FiltroTickets(EstadoTicket estado, Prioridad prioridad, Long tecnicoId, Boolean vencido) {

    public static FiltroTickets sinFiltros() {
        return new FiltroTickets(null, null, null, null);
    }
}
