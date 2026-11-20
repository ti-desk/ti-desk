package mx.edu.tidesk.ti_desk.ticket.dto;

import java.time.Instant;

import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;
import mx.edu.tidesk.ti_desk.ticket.HistorialEstado;

/**
 * Un renglón del historial de estados. usuarioId y usuarioNombre son null si
 * el cambio lo hizo el sistema; estadoAnterior es null en la creación.
 */
public record HistorialEstadoResponse(
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        Instant fecha,
        String motivo,
        Long usuarioId,
        String usuarioNombre) {

    public static HistorialEstadoResponse desde(HistorialEstado h) {
        return new HistorialEstadoResponse(
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getFecha(),
                h.getMotivo(),
                h.getUsuario() != null ? h.getUsuario().getId() : null,
                h.getUsuario() != null ? h.getUsuario().getNombre() : null);
    }
}
