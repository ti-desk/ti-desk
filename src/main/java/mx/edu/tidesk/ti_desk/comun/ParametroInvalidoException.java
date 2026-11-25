package mx.edu.tidesk.ti_desk.comun;

/**
 * Parámetro de consulta con un valor no permitido. Se traduce a 400.
 */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
