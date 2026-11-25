package mx.edu.tidesk.ti_desk.comun;

/**
 * El usuario puede ver el recurso, pero su rol o su relación con él no le
 * permite hacer esta acción. Se traduce a 403 con ProblemDetail.
 */
public class AccionNoPermitidaException extends RuntimeException {

    public AccionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
