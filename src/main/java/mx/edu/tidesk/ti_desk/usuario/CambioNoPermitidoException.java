package mx.edu.tidesk.ti_desk.usuario;

/**
 * El cambio dejaría al sistema en un estado inválido, por ejemplo un ADMIN
 * que se desactiva o se quita el rol a sí mismo (se traduce a 409).
 */
public class CambioNoPermitidoException extends RuntimeException {

    public CambioNoPermitidoException(String mensaje) {
        super(mensaje);
    }
}