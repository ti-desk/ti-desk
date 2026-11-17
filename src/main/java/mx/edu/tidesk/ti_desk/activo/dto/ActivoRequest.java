package mx.edu.tidesk.ti_desk.activo.dto;

import jakarta.validation.constraints.*;
import mx.edu.tidesk.ti_desk.activo.TipoActivo;
import java.time.LocalDate;

public record ActivoRequest(
        @NotBlank @Size(max = 50) String etiquetaInventario,
        @NotNull TipoActivo tipo,
        @Size(max = 80) String marca,
        @Size(max = 80) String modelo,
        @Size(max = 80) String numeroSerie,
        @Size(max = 120) String ubicacion,
        @PastOrPresent LocalDate fechaAdquisicion,
        Long responsableId) {
}