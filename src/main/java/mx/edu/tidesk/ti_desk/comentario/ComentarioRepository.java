package mx.edu.tidesk.ti_desk.comentario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /**
     * Comentarios de un ticket del más viejo al más nuevo, con su autor en la
     * misma consulta (sin N+1). Si incluirInternos es false, omite los internos.
     */
    @Query("""
            select c from Comentario c
            join fetch c.autor
            where c.ticket.id = :ticketId
              and (:incluirInternos = true or c.interno = false)
            order by c.creadoEn asc, c.id asc
            """)
    List<Comentario> findDelTicket(@Param("ticketId") Long ticketId,
                                   @Param("incluirInternos") boolean incluirInternos);
}
