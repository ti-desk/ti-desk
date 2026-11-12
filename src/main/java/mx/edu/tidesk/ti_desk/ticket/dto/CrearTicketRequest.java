package mx.edu.tidesk.ti_desk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mx.edu.tidesk.ti_desk.ticket.Categoria;
import mx.edu.tidesk.ti_desk.ticket.Prioridad;

/**
 * Datos para crear un ticket (RF-04). El activo es opcional.
 */
public record CrearTicketRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede tener más de 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 5000, message = "La descripción no puede tener más de 5000 caracteres")
        String descripcion,

        @NotNull(message = "La categoría es obligatoria")
        Categoria categoria,

        @NotNull(message = "La prioridad es obligatoria")
        Prioridad prioridad,

        @Positive(message = "El id del activo debe ser positivo")
        Long activoId) {
}
