package mx.edu.tidesk.ti_desk.activo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activos")
public class ActivoController {

    private final ActivoService activoService;

    public ActivoController(ActivoService activoService) {
        this.activoService = activoService;
    }

    @GetMapping
    public List<Activo> listar() {
        return activoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activo> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(activoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Activo> crear(@RequestBody Activo activo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activoService.guardar(activo));
    }
}