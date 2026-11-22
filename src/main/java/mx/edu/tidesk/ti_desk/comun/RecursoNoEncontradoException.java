package mx.edu.tidesk.ti_desk.comun;

/**
 * Se lanza cuando un recurso no existe (o no es visible para el usuario).
 * Se traduce a 404 con ProblemDetail.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
