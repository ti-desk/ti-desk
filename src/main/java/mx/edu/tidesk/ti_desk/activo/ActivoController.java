package mx.edu.tidesk.ti_desk.activo;

import mx.edu.tidesk.ti_desk.activo.dto.ActivoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // --- AQUÍ APLICAMOS LOS CAMBIOS DE SASHI ---
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECNICO')") // 1. Seguridad
    public ResponseEntity<Activo> crear(@RequestBody ActivoDTO activoDTO) { // 2. Uso de DTO
        
        // Mapeamos el DTO a la Entidad
        Activo nuevoActivo = new Activo();
        nuevoActivo.setEtiquetaInventario(activoDTO.getEtiquetaInventario());
        nuevoActivo.setTipo(activoDTO.getTipo());
        nuevoActivo.setMarca(activoDTO.getMarca());
        nuevoActivo.setModelo(activoDTO.getModelo());
        nuevoActivo.setNumeroSerie(activoDTO.getNumeroSerie());
        nuevoActivo.setUbicacion(activoDTO.getUbicacion());
        nuevoActivo.setEstado(activoDTO.getEstado() != null ? activoDTO.getEstado() : EstadoActivo.ACTIVO);

        return ResponseEntity.status(HttpStatus.CREATED).body(activoService.guardar(nuevoActivo));
    }
}