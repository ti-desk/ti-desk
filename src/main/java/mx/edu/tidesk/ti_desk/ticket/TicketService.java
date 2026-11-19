package mx.edu.tidesk.ti_desk.ticket;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;

import mx.edu.tidesk.ti_desk.activo.Activo;
import mx.edu.tidesk.ti_desk.activo.ActivoRepository;
import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.ParametroInvalidoException;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.sla.PoliticaSla;
import mx.edu.tidesk.ti_desk.sla.PoliticaSlaRepository;
import mx.edu.tidesk.ti_desk.ticket.dto.AsignarTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.CambiarEstadoRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.PaginaResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de tickets.
 */
@Service
public class TicketService {

    /** Estados en los que un ADMIN puede cambiar el técnico sin cambiar el estado. */
    private static final Set<EstadoTicket> REASIGNABLES =
            EnumSet.of(EstadoTicket.ASIGNADO, EstadoTicket.EN_PROCESO, EstadoTicket.EN_ESPERA);

    private final TicketRepository tickets;
    private final UsuarioRepository usuarios;
    private final ActivoRepository activos;
    private final PoliticaSlaRepository politicas;
    private final GeneradorFolio generadorFolio;
    private final MaquinaEstadosTicket maquinaEstados;
    private final Clock clock;

    public TicketService(TicketRepository tickets, UsuarioRepository usuarios,
                         ActivoRepository activos, PoliticaSlaRepository politicas,
                         GeneradorFolio generadorFolio, MaquinaEstadosTicket maquinaEstados,
                         Clock clock) {
        this.tickets = tickets;
        this.usuarios = usuarios;
        this.activos = activos;
        this.politicas = politicas;
        this.generadorFolio = generadorFolio;
        this.maquinaEstados = maquinaEstados;
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

        Ticket guardado = tickets.save(ticket);
        maquinaEstados.registrarCreacion(guardado);
        return TicketResponse.desde(guardado);
    }

    /**
     * RF-05: lista los tickets visibles para el usuario según su rol,
     * con filtros opcionales y paginación. La respuesta solo usa los ids de
     * las relaciones, así que no se disparan consultas extra (sin N+1).
     */
    @Transactional(readOnly = true)
    public PaginaResponse<TicketResponse> listar(FiltroTickets filtro, Pageable pagina,
                                                 Long usuarioId, Rol rol) {
        return PaginaResponse.desde(
                tickets.findAll(TicketSpecifications.visiblesPara(filtro, usuarioId, rol), pagina),
                TicketResponse::desde);
    }

    /**
     * RF-08: cambia el estado del ticket según la máquina de estados y deja el
     * cambio en el historial. Para pasar a ASIGNADO se usa la asignación (RF-06).
     *
     * <ul>
     *   <li>404 si el ticket no existe o el usuario no lo puede ver.</li>
     *   <li>403 si lo puede ver, pero no le toca hacer ese cambio.</li>
     *   <li>409 si la transición no está permitida o alguien más lo modificó al mismo tiempo.</li>
     * </ul>
     */
    @Transactional
    public TicketResponse cambiarEstado(Long ticketId, CambiarEstadoRequest datos,
                                        Long usuarioId, Rol rol) {
        Ticket ticket = buscarVisible(ticketId, usuarioId, rol);
        EstadoTicket nuevo = datos.estado();

        if (nuevo == EstadoTicket.ASIGNADO) {
            throw new TransicionNoPermitidaException(ticket.getEstado(), nuevo,
                    "Para asignar un ticket usa PATCH /api/v1/tickets/{id}/asignacion");
        }
        verificarPermiso(ticket, nuevo, usuarioId, rol);

        maquinaEstados.cambiar(ticket, nuevo, usuarios.getReferenceById(usuarioId),
                limpiar(datos.motivo()));
        // saveAndFlush: si otro usuario cambió el ticket, el @Version falla aquí y responde 409.
        return TicketResponse.desde(tickets.saveAndFlush(ticket));
    }

    /**
     * RF-06: asigna el ticket a un técnico.
     *
     * <ul>
     *   <li>ADMIN: asigna a cualquier técnico activo, o reasigna un ticket en curso.</li>
     *   <li>TÉCNICO: solo se autoasigna tickets ABIERTO.</li>
     *   <li>USUARIO: 403.</li>
     * </ul>
     * Si dos técnicos toman el mismo ticket a la vez, el @Version hace que el
     * segundo reciba 409 y el ticket se quede con el primero.
     *
     * <p>El ticket se busca por id y no con la visibilidad de RF-05: si otro técnico
     * lo acaba de tomar, ya no sería visible y el que perdió recibiría 404 en vez de
     * 409 (CP-019). El técnico ya ve todos los tickets sin asignar, así que responder
     * "ya lo tomó otro técnico" no expone nada nuevo.
     */
    @Transactional
    public TicketResponse asignar(Long ticketId, AsignarTicketRequest datos, Long usuarioId, Rol rol) {
        if (rol == Rol.USUARIO) {
            throw new AccionNoPermitidaException("Solo un técnico o un administrador pueden asignar tickets.");
        }
        Long pedido = datos != null ? datos.tecnicoId() : null;
        String motivo = datos != null ? limpiar(datos.motivo()) : null;

        Long tecnicoId;
        if (rol == Rol.TECNICO) {
            if (pedido != null && !pedido.equals(usuarioId)) {
                throw new AccionNoPermitidaException("Un técnico solo puede asignarse tickets a sí mismo.");
            }
            tecnicoId = usuarioId;
        } else {
            if (pedido == null) {
                throw new ParametroInvalidoException("Indica el técnico en 'tecnicoId'.");
            }
            tecnicoId = pedido;
        }

        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el ticket con id " + ticketId));
        if (rol == Rol.TECNICO && ticket.getTecnico() != null
                && !usuarioId.equals(ticket.getTecnico().getId())) {
            throw new TransicionNoPermitidaException(ticket.getEstado(), EstadoTicket.ASIGNADO,
                    "El ticket ya lo tomó otro técnico");
        }
        Usuario tecnico = usuarios.findById(tecnicoId)
                .filter(u -> u.getRol() == Rol.TECNICO && u.isActivo())
                .orElseThrow(() -> new ParametroInvalidoException(
                        "El usuario " + tecnicoId + " no es un técnico activo."));
        Usuario autor = usuarios.getReferenceById(usuarioId);
        EstadoTicket actual = ticket.getEstado();

        if (actual == EstadoTicket.ABIERTO) {
            ticket.setTecnico(tecnico);
            maquinaEstados.cambiar(ticket, EstadoTicket.ASIGNADO, autor, motivo);
        } else if (REASIGNABLES.contains(actual)) {
            if (rol != Rol.ADMIN) {
                throw new TransicionNoPermitidaException(actual, EstadoTicket.ASIGNADO,
                        "El ticket ya está asignado; solo un administrador puede reasignarlo");
            }
            if (ticket.getTecnico() != null && tecnicoId.equals(ticket.getTecnico().getId())) {
                throw new TransicionNoPermitidaException(actual, EstadoTicket.ASIGNADO,
                        "El ticket ya está asignado a ese técnico");
            }
            ticket.setTecnico(tecnico);
            maquinaEstados.registrarReasignacion(ticket, autor,
                    motivo != null ? motivo : "Reasignado a otro técnico");
        } else {
            throw new TransicionNoPermitidaException(actual, EstadoTicket.ASIGNADO,
                    "Un ticket " + actual + " ya no se puede asignar");
        }
        // saveAndFlush: si otro técnico lo tomó al mismo tiempo, el @Version falla aquí y responde 409.
        return TicketResponse.desde(tickets.saveAndFlush(ticket));
    }

    /**
     * Busca el ticket aplicando la misma visibilidad por rol que el listado (RF-05).
     */
    private Ticket buscarVisible(Long ticketId, Long usuarioId, Rol rol) {
        Specification<Ticket> conId = (root, query, cb) -> cb.equal(root.get("id"), ticketId);
        return tickets.findOne(
                        TicketSpecifications.visiblesPara(FiltroTickets.sinFiltros(), usuarioId, rol).and(conId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el ticket con id " + ticketId));
    }

    /**
     * Quién puede hacer cada cambio (anteproyecto, sección 7.2). ADMIN puede todos.
     */
    static void verificarPermiso(Ticket ticket, EstadoTicket nuevo, Long usuarioId, Rol rol) {
        if (rol == Rol.ADMIN) {
            return;
        }
        boolean esSolicitante = ticket.getSolicitante() != null
                && usuarioId.equals(ticket.getSolicitante().getId());
        boolean esTecnicoAsignado = ticket.getTecnico() != null
                && usuarioId.equals(ticket.getTecnico().getId());
        boolean sinTecnico = ticket.getTecnico() == null;
        EstadoTicket actual = ticket.getEstado();

        boolean permitido = switch (nuevo) {
            // Cancela el solicitante, o un técnico si el ticket es suyo o aún no tiene técnico.
            case CANCELADO -> esSolicitante || (rol == Rol.TECNICO && (esTecnicoAsignado || sinTecnico));
            // El técnico asignado lo trabaja; el solicitante responde (EN_ESPERA) o reabre (RESUELTO).
            case EN_PROCESO -> esTecnicoAsignado || (esSolicitante
                    && (actual == EstadoTicket.EN_ESPERA || actual == EstadoTicket.RESUELTO));
            case EN_ESPERA, RESUELTO -> esTecnicoAsignado;
            // El solicitante confirma el cierre (el cierre automático lo hace el sistema, RF-09).
            case CERRADO -> esSolicitante;
            default -> false;
        };
        if (!permitido) {
            throw new AccionNoPermitidaException(
                    "No tienes permiso para pasar este ticket a " + nuevo + ".");
        }
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }

    private static Duration duracion(PoliticaSla politica) {
        // Las horas pueden tener decimales (CRÍTICA = 0.5 h de primera respuesta).
        long segundos = politica.getHorasResolucion()
                .multiply(BigDecimal.valueOf(3600))
                .longValue();
        return Duration.ofSeconds(segundos);
    }
}
