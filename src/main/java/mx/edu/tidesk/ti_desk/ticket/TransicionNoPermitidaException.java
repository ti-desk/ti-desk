package mx.edu.tidesk.ti_desk.ticket;

/**
 * Cambio de estado que la máquina de estados no permite (RF-08).
 * Se traduce a 409 con ProblemDetail que indica la regla violada.
 */
public class TransicionNoPermitidaException extends RuntimeException {

    private final EstadoTicket estadoActual;
    private final EstadoTicket estadoSolicitado;

    public TransicionNoPermitidaException(EstadoTicket estadoActual, EstadoTicket estadoSolicitado,
                                          String regla) {
        super(regla);
        this.estadoActual = estadoActual;
        this.estadoSolicitado = estadoSolicitado;
    }

    public EstadoTicket getEstadoActual() {
        return estadoActual;
    }

    public EstadoTicket getEstadoSolicitado() {
        return estadoSolicitado;
    }
}
