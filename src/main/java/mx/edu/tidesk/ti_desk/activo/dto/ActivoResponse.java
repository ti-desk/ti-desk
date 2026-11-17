package mx.edu.tidesk.ti_desk.activo.dto;

import mx.edu.tidesk.ti_desk.activo.*;
import java.time.LocalDate;

public record ActivoResponse(
        Long id, String etiquetaInventario, TipoActivo tipo, String marca,
        String modelo, String numeroSerie, String ubicacion, EstadoActivo estado,
        LocalDate fechaAdquisicion, Long responsableId) {

    // La entidad nunca sale de la capa de servicio: se expone como DTO.
    public static ActivoResponse de(Activo a) {
        return new ActivoResponse(a.getId(), a.getEtiquetaInventario(), a.getTipo(),
                a.getMarca(), a.getModelo(), a.getNumeroSerie(), a.getUbicacion(),
                a.getEstado(), a.getFechaAdquisicion(),
                a.getResponsable() != null ? a.getResponsable().getId() : null);
    }
}