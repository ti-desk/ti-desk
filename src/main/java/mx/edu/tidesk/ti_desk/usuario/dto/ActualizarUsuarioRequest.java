package mx.edu.tidesk.ti_desk.usuario.dto;

import jakarta.validation.constraints.AssertTrue;
import mx.edu.tidesk.ti_desk.usuario.Rol;

/**
 * Cambios que un ADMIN puede hacer sobre un usuario (RF-03).
 * Los dos campos son opcionales, pero debe venir al menos uno.
 * No permite cambiar correo ni contraseña.
 */
public record ActualizarUsuarioRequest(Rol rol, Boolean activo) {

    @AssertTrue(message = "Debes indicar al menos un cambio: rol o activo")
    public boolean isConCambios() {
        return rol != null || activo != null;
    }
}