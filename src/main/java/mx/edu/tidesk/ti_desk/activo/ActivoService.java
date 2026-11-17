package mx.edu.tidesk.ti_desk.activo;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException; // <-- Nuevo import
import org.springframework.http.HttpStatus; // <-- Nuevo import
import java.util.List;

@Service
public class ActivoService {

    private final ActivoRepository activoRepository;

    public ActivoService(ActivoRepository activoRepository) {
        this.activoRepository = activoRepository;
    }

    public List<Activo> listarTodos() {
        return activoRepository.findAll();
    }

    public Activo buscarPorId(Long id) {
        return activoRepository.findById(id)
                // Cambiamos la excepción genérica por una que Spring entienda como 404
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
    }

    public Activo guardar(Activo activo) {
        return activoRepository.save(activo);
    }
}