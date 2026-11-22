package mx.edu.tidesk.ti_desk.comentario;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;
import mx.edu.tidesk.ti_desk.comentario.dto.CrearComentarioRequest;
import mx.edu.tidesk.ti_desk.comun.AccionNoPermitidaException;
import mx.edu.tidesk.ti_desk.comun.RecursoNoEncontradoException;
import mx.edu.tidesk.ti_desk.ticket.Ticket;
import mx.edu.tidesk.ti_desk.ticket.TicketService;
import mx.edu.tidesk.ti_desk.usuario.CambioNoPermitidoException;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;
import mx.edu.tidesk.ti_desk.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comentarios de tickets (RF-07). Los internos solo los escriben y los ven
 * técnicos y administradores.
 */
@Service
public class ComentarioService {

    private final ComentarioRepository comentarios;
    private final TicketService ticketService;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ComentarioService(ComentarioRepository comentarios, TicketService ticketService,
                             UsuarioRepository usuarios, Clock clock) {
        this.comentarios = comentarios;
        this.ticketService = ticketService;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    /**
     * Agrega un comentario. 404 si el ticket no existe o no es visible, 403 si un
     * USUARIO intenta un comentario interno, 409 si el ticket ya está cerrado o cancelado.
     * El primer comentario de un técnico o administrador marca la primera respuesta del SLA.
     */
    @Transactional
    public ComentarioResponse comentar(Long ticketId, CrearComentarioRequest datos,
                                       Long usuarioId, Rol rol) {
        Ticket ticket = ticketService.buscarVisible(ticketId, usuarioId, rol);
        if (datos.esInterno() && rol == Rol.USUARIO) {
            throw new AccionNoPermitidaException(
                    "Solo técnicos y administradores pueden escribir comentarios internos.");
        }
        if (ticket.getEstado().esFinal()) {
            throw new CambioNoPermitidoException("No se puede comentar un ticket " + ticket.getEstado());
        }
        Usuario autor = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + usuarioId));
        Instant ahora = Instant.now(clock);

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setAutor(autor);
        comentario.setTexto(datos.texto().strip());
        comentario.setInterno(datos.esInterno());
        comentario.setCreadoEn(ahora);

        if (rol != Rol.USUARIO && ticket.getPrimeraRespuestaEn() == null) {
            ticket.setPrimeraRespuestaEn(ahora);
        }
        return ComentarioResponse.desde(comentarios.save(comentario));
    }

    /**
     * Lista los comentarios del ticket. Un USUARIO no ve los internos.
     */
    @Transactional(readOnly = true)
    public List<ComentarioResponse> listar(Long ticketId, Long usuarioId, Rol rol) {
        ticketService.buscarVisible(ticketId, usuarioId, rol);
        return comentarios.findDelTicket(ticketId, rol != Rol.USUARIO).stream()
                .map(ComentarioResponse::desde)
                .toList();
    }
}
