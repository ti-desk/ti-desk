package mx.edu.tidesk.ti_desk.usuario;

/**
 * El correo ya está registrado (se traduce a 409).
 */
public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException() {
        super("El correo ya está registrado");
    }
}