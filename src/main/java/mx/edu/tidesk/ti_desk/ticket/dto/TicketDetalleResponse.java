package mx.edu.tidesk.ti_desk.ticket.dto;

import java.util.List;

import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;

/**
 * Detalle de un ticket: sus datos, sus comentarios visibles y su historial de estados.
 */
public record TicketDetalleResponse(
        TicketResponse ticket,
        List<ComentarioResponse> comentarios,
        List<HistorialEstadoResponse> historial) {

    public TicketDetalleResponse {
        comentarios = List.copyOf(comentarios);
        historial = List.copyOf(historial);
    }
}
