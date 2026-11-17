package mx.edu.tidesk.ti_desk.seguridad.dto;

/**
 * Respuesta del login: el token JWT y su tipo para el encabezado Authorization.
 */
public record LoginResponse(String token, String tipo) {

    public static LoginResponse bearer(String token) {
        return new LoginResponse(token, "Bearer");
    }

    // Evita que el token aparezca en logs si alguien imprime el objeto (CP-044).
    @Override
    public String toString() {
        return "LoginResponse[token=***, tipo=" + tipo + "]";
    }
}