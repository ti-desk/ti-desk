package mx.edu.tidesk.ti_desk.comentario;

import java.util.List;

import jakarta.validation.Valid;
import mx.edu.tidesk.ti_desk.comentario.dto.ComentarioResponse;
import mx.edu.tidesk.ti_desk.comentario.dto.CrearComentarioRequest;
import mx.edu.tidesk.ti_desk.usuario.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de comentarios de un ticket (RF-07). Solo usa servicios, nunca repositorios.
 */
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final UsuarioActual usuarioActual;

    public ComentarioController(ComentarioService comentarioService, UsuarioActual usuarioActual) {
        this.comentarioService = comentarioService;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Comentar un ticket. Responde 201 con el comentario.
     * Ejemplo: {"texto":"Ya cambié el cable","interno":false}
     */
    @PostMapping
    public ResponseEntity<ComentarioResponse> comentar(@PathVariable Long ticketId,
                                                       @Valid @RequestBody CrearComentarioRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.comentar(ticketId, datos, usuarioActual.id(), usuarioActual.rol()));
    }

    /**
     * Comentarios del ticket del más viejo al más nuevo. Un USUARIO no ve los internos.
     */
    @GetMapping
    public List<ComentarioResponse> listar(@PathVariable Long ticketId) {
        return comentarioService.listar(ticketId, usuarioActual.id(), usuarioActual.rol());
    }
}
