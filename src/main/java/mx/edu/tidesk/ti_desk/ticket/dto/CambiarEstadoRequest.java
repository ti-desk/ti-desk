package mx.edu.tidesk.ti_desk.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.edu.tidesk.ti_desk.ticket.EstadoTicket;

/**
 * Datos para cambiar el estado de un ticket (RF-08). El motivo es opcional
 * y se guarda en historial_estado.
 */
public record CambiarEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoTicket estado,

        @Size(max = 500, message = "El motivo no puede tener más de 500 caracteres")
        String motivo) {
}
