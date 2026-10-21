package mx.edu.tidesk.ti_desk.ticket;

public enum EstadoTicket {
    ABIERTO, ASIGNADO, EN_PROCESO, EN_ESPERA, RESUELTO, CERRADO, CANCELADO;

    public boolean esFinal() {
        return this == CERRADO || this == CANCELADO;
    }
}
