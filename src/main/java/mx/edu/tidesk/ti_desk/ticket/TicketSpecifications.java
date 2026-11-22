package mx.edu.tidesk.ti_desk.ticket;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import org.springframework.data.jpa.domain.Specification;

/**
 * Consultas dinámicas de tickets: visibilidad por rol + filtros opcionales.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    /**
     * USUARIO: solo los tickets que él creó.
     * TECNICO: los asignados a él y los que no tienen técnico.
     * ADMIN: todos.
     */
    public static Specification<Ticket> visiblesPara(FiltroTickets filtro, Long usuarioId, Rol rol) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            Join<Ticket, Usuario> tecnico = root.join("tecnico", JoinType.LEFT);

            switch (rol) {
                case USUARIO -> condiciones.add(cb.equal(root.get("solicitante").get("id"), usuarioId));
                case TECNICO -> condiciones.add(cb.or(
                        cb.isNull(tecnico.get("id")),
                        cb.equal(tecnico.get("id"), usuarioId)));
                case ADMIN -> {
                    // sin restricción
                }
            }

            if (filtro.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.prioridad() != null) {
                condiciones.add(cb.equal(root.get("prioridad"), filtro.prioridad()));
            }
            if (filtro.tecnicoId() != null) {
                condiciones.add(cb.equal(tecnico.get("id"), filtro.tecnicoId()));
            }
            if (filtro.vencido() != null) {
                condiciones.add(cb.equal(root.get("slaVencido"), filtro.vencido()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
