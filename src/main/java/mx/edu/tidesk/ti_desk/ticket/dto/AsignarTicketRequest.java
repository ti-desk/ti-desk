package mx.edu.tidesk.ti_desk.ticket.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para asignar un ticket (RF-06).
 * El ADMIN indica el técnico; el TÉCNICO puede omitirlo para autoasignarse.
 */
public record AsignarTicketRequest(

        @Positive(message = "El id del técnico debe ser positivo")
        Long tecnicoId,

        @Size(max = 500, message = "El motivo no puede tener más de 500 caracteres")
        String motivo) {
}
