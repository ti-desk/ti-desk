package mx.edu.tidesk.ti_desk.ticket;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del ticket y sus transiciones permitidas (anteproyecto, sección 7.2).
 */
public enum EstadoTicket {
    ABIERTO, ASIGNADO, EN_PROCESO, EN_ESPERA, RESUELTO, CERRADO, CANCELADO;

    public boolean esFinal() {
        return this == CERRADO || this == CANCELADO;
    }

    /**
     * Estados a los que se puede pasar desde este. CERRADO y CANCELADO no tienen salida.
     */
    public Set<EstadoTicket> siguientes() {
        return switch (this) {
            case ABIERTO -> EnumSet.of(ASIGNADO, CANCELADO);
            case ASIGNADO -> EnumSet.of(EN_PROCESO, CANCELADO);
            case EN_PROCESO -> EnumSet.of(EN_ESPERA, RESUELTO);
            case EN_ESPERA -> EnumSet.of(EN_PROCESO);
            case RESUELTO -> EnumSet.of(CERRADO, EN_PROCESO);
            case CERRADO, CANCELADO -> EnumSet.noneOf(EstadoTicket.class);
        };
    }

    public boolean puedePasarA(EstadoTicket nuevo) {
        return siguientes().contains(nuevo);
    }
}
