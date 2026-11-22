package mx.edu.tidesk.ti_desk.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    /**
     * Historial de un ticket en orden cronológico, con el usuario en la misma
     * consulta (left join: los cambios del sistema no tienen usuario).
     */
    @Query("""
            select h from HistorialEstado h
            left join fetch h.usuario
            where h.ticket.id = :ticketId
            order by h.fecha asc, h.id asc
            """)
    List<HistorialEstado> findDelTicket(@Param("ticketId") Long ticketId);
}
