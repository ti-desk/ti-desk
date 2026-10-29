package mx.edu.tidesk.ti_desk.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro (RF-01). No incluye el rol: siempre se crea como USUARIO.
 */
public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 160, message = "El correo no puede tener más de 160 caracteres")
        @Email(message = "El correo no tiene un formato válido")
        @Pattern(regexp = "^[^@\\s]+@unistmo\\.edu\\.mx$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Debe ser un correo institucional @unistmo.edu.mx")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x20-\\x7E]{8,72}$",
                message = "La contraseña debe tener entre 8 y 72 caracteres ASCII, con mayúscula, minúscula, número y un carácter especial")
        String contrasena) {

    // Evita que la contraseña aparezca en logs si alguien imprime el objeto.
    @Override
    public String toString() {
        return "RegistroRequest[nombre=" + nombre + ", correo=" + correo + ", contrasena=***]";
    }
}