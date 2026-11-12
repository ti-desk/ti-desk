package mx.edu.tidesk.ti_desk.ticket.dto;

import java.time.Instant;

import mx.edu.tidesk.ti_desk.ticket.Categoria;
import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;
import mx.edu.tidesk.ti_desk.ticket.Ticket;

/**
 * Representación pública de un ticket. La entidad JPA nunca sale del servicio.
 */
public record TicketResponse(
        Long id,
        String folio,
        String titulo,
        String descripcion,
        Categoria categoria,
        Prioridad prioridad,
        EstadoTicket estado,
        Long solicitanteId,
        Long tecnicoId,
        Long activoId,
        Instant creadoEn,
        Instant fechaLimiteSla,
        boolean slaVencido) {

    public static TicketResponse desde(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getFolio(),
                t.getTitulo(),
                t.getDescripcion(),
                t.getCategoria(),
                t.getPrioridad(),
                t.getEstado(),
                t.getSolicitante() != null ? t.getSolicitante().getId() : null,
                t.getTecnico() != null ? t.getTecnico().getId() : null,
                t.getActivo() != null ? t.getActivo().getId() : null,
                t.getCreadoEn(),
                t.getFechaLimiteSla(),
                t.isSlaVencido());
    }
}
