package mx.edu.tidesk.ti_desk.comentario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para comentar un ticket (RF-07). interno es opcional: si no se envía,
 * el comentario es público (por eso es Boolean y no boolean, que Jackson
 * rechaza cuando falta en el JSON).
 */
public record CrearComentarioRequest(

        @NotBlank(message = "El texto del comentario es obligatorio")
        @Size(max = 5000, message = "El comentario no puede tener más de 5000 caracteres")
        String texto,

        Boolean interno) {

    /** true solo si se pidió explícitamente un comentario interno. */
    public boolean esInterno() {
        return Boolean.TRUE.equals(interno);
    }
}
