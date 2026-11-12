package mx.edu.tidesk.ti_desk.ticket;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import mx.edu.tidesk.ti_desk.activo.Activo;
import mx.edu.tidesk.ti_desk.activo.ActivoRepository;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.sla.PoliticaSla;
import mx.edu.tidesk.ti_desk.sla.PoliticaSlaRepository;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de tickets.
 */
@Service
public class TicketService {

    private final TicketRepository tickets;
    private final UsuarioRepository usuarios;
    private final ActivoRepository activos;
    private final PoliticaSlaRepository politicas;
    private final GeneradorFolio generadorFolio;
    private final Clock clock;

    public TicketService(TicketRepository tickets, UsuarioRepository usuarios,
                         ActivoRepository activos, PoliticaSlaRepository politicas,
                         GeneradorFolio generadorFolio, Clock clock) {
        this.tickets = tickets;
        this.usuarios = usuarios;
        this.activos = activos;
        this.politicas = politicas;
        this.generadorFolio = generadorFolio;
        this.clock = clock;
    }

    /**
     * RF-04: crea un ticket en estado ABIERTO con folio TID-AAAA-NNNNN y
     * fecha límite de SLA = creadoEn + horas de resolución de su prioridad.
     */
    @Transactional
    public TicketResponse crear(CrearTicketRequest datos, Long solicitanteId) {
        Usuario solicitante = usuarios.findById(solicitanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + solicitanteId));

        Activo activo = null;
        if (datos.activoId() != null) {
            activo = activos.findById(datos.activoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el activo con id " + datos.activoId()));
        }

        PoliticaSla politica = politicas.findById(datos.prioridad())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay política de SLA para la prioridad " + datos.prioridad()));

        Instant ahora = Instant.now(clock);

        Ticket ticket = new Ticket();
        ticket.setFolio(generadorFolio.siguiente(ahora.atZone(ZoneOffset.UTC).getYear()));
        ticket.setTitulo(datos.titulo().strip());
        ticket.setDescripcion(datos.descripcion().strip());
        ticket.setCategoria(datos.categoria());
        ticket.setPrioridad(datos.prioridad());
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setSolicitante(solicitante);
        ticket.setActivo(activo);
        ticket.setCreadoEn(ahora);
        ticket.setFechaLimiteSla(ahora.plus(duracion(politica)));

        return TicketResponse.desde(tickets.save(ticket));
    }

    private static Duration duracion(PoliticaSla politica) {
        // Las horas pueden tener decimales (CRÍTICA = 0.5 h de primera respuesta).
        long segundos = politica.getHorasResolucion()
                .multiply(BigDecimal.valueOf(3600))
                .longValue();
        return Duration.ofSeconds(segundos);
    }
}
