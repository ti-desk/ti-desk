package mx.edu.tidesk.ti_desk.comentario.dto;

import java.time.Instant;

import mx.edu.tidesk.ti_desk.comentario.Comentario;

/**
 * Representación pública de un comentario. Del autor solo sale el id y el nombre.
 */
public record ComentarioResponse(
        Long id,
        Long ticketId,
        Long autorId,
        String autorNombre,
        String texto,
        boolean interno,
        Instant creadoEn) {

    public static ComentarioResponse desde(Comentario c) {
        return new ComentarioResponse(
                c.getId(),
                c.getTicket() != null ? c.getTicket().getId() : null,
                c.getAutor() != null ? c.getAutor().getId() : null,
                c.getAutor() != null ? c.getAutor().getNombre() : null,
                c.getTexto(),
                c.isInterno(),
                c.getCreadoEn());
    }
}
