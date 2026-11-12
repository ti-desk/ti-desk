package mx.edu.tidesk.ti_desk.ticket;

import java.net.URI;

import jakarta.validation.Valid;
import mx.edu.tidesk.ti_desk.ticket.dto.CrearTicketRequest;
import mx.edu.tidesk.ti_desk.ticket.dto.TicketResponse;
import mx.edu.tidesk.ti_desk.usuario.UsuarioActual;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API REST de tickets. Solo usa servicios, nunca repositorios.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

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
}
