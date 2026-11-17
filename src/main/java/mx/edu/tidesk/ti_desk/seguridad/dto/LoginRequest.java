package mx.edu.tidesk.ti_desk.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de inicio de sesión (RF-02).
 * Aquí solo se exige que no vengan vacías: las reglas de formato son del registro.
 */
public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena) {

    // Evita que la contraseña aparezca en logs si alguien imprime el objeto (CP-044).
    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + ", contrasena=***]";
    }
}