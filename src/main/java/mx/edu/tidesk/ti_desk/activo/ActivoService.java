package mx.edu.tidesk.ti_desk.activo;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));
    }

    public Activo guardar(Activo activo) {
        return activoRepository.save(activo);
    }
}