package mx.edu.tidesk.ti_desk.ticket;

import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;
import mx.edu.tidesk.ti_desk.comun.ParametroInvalidoException;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.FiltroTickets;
import mx.edu.tidesk.ti_desk.ticket.dto.PaginaResponse;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.UsuarioActual;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API REST de tickets. Solo usa servicios, nunca repositorios.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    static final int TAMANO_MAXIMO = 100;
    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("creadoEn", "prioridad", "estado", "fechaLimiteSla", "folio");

    private final TicketService ticketService;
    private final UsuarioActual usuarioActual;

    public TicketController(TicketService ticketService, UsuarioActual usuarioActual) {
        this.ticketService = ticketService;
        this.usuarioActual = usuarioActual;
    }

    /**
     * RF-04: crear ticket. Responde 201 con el encabezado Location.
     */
    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody CrearTicketRequest datos) {
        TicketResponse creado = ticketService.crear(datos, usuarioActual.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /**
     * RF-05: listar tickets con filtros opcionales y paginación.
     * Ejemplo: GET /api/v1/tickets?estado=ABIERTO&prioridad=ALTA&page=0&size=20&sort=creadoEn,desc
     */
    @GetMapping
    public PaginaResponse<TicketResponse> listar(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) Boolean vencido,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "creadoEn,desc") String sort) {

        FiltroTickets filtro = new FiltroTickets(estado, prioridad, tecnicoId, vencido);
        return ticketService.listar(filtro, paginacion(page, size, sort),
                usuarioActual.id(), usuarioActual.rol());
    }

    static Pageable paginacion(int page, int size, String sort) {
        if (page < 0) {
            throw new ParametroInvalidoException("'page' no puede ser negativo");
        }
        if (size < 1) {
            throw new ParametroInvalidoException("'size' debe ser al menos 1");
        }
        String[] partes = sort.split(",");
        String campo = partes[0].strip();
        if (!CAMPOS_ORDENABLES.contains(campo)) {
            throw new ParametroInvalidoException(
                    "No se puede ordenar por '" + campo + "'. Campos permitidos: " + CAMPOS_ORDENABLES);
        }
        Sort.Direction direccion = Sort.Direction.DESC;
        if (partes.length > 1) {
            direccion = Sort.Direction.fromOptionalString(partes[1].strip())
                    .orElseThrow(() -> new ParametroInvalidoException(
                            "La dirección de orden debe ser 'asc' o 'desc'"));
        }
        return PageRequest.of(page, Math.min(size, TAMANO_MAXIMO), Sort.by(direccion, campo));
    }
}
