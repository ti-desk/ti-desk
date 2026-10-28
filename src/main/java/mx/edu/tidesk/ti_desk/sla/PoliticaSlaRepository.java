package mx.edu.tidesk.ti_desk.sla;

import mx.edu.tidesk.ti_desk.ticket.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaSlaRepository extends JpaRepository<PoliticaSla, Prioridad> {
}