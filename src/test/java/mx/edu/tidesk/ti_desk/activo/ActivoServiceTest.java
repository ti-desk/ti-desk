package mx.edu.tidesk.ti_desk.activo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivoServiceTest {

    @Mock
    private ActivoRepository activoRepository;

    @InjectMocks
    private ActivoService activoService;

    @Test
    void buscarPorId_CuandoExiste_RetornaActivo() {
        // 1. Preparar
        Long id = 1L;
        Activo activoFalso = new Activo();
        activoFalso.setId(id);
        activoFalso.setEtiquetaInventario("TID-LAP-001");
        activoFalso.setTipo(TipoActivo.LAPTOP);
        activoFalso.setEstado(EstadoActivo.ACTIVO);
        
        when(activoRepository.findById(id)).thenReturn(Optional.of(activoFalso));

        // 2. Ejecutar
        Activo resultado = activoService.buscarPorId(id);

        // 3. Verificar
        assertNotNull(resultado);
        assertEquals("TID-LAP-001", resultado.getEtiquetaInventario());
        assertEquals(TipoActivo.LAPTOP, resultado.getTipo());
        verify(activoRepository, times(1)).findById(id);
    }

    @Test
    void buscarPorId_CuandoNoExiste_LanzaExcepcion() {
        Long id = 99L;
        when(activoRepository.findById(id)).thenReturn(Optional.empty());

        // Asumo que tu servicio lanza un RuntimeException o similar si no lo encuentra
        assertThrows(RuntimeException.class, () -> activoService.buscarPorId(id));
    }

    @Test
    void guardarActivo_AsignaValoresCorrectamente() {
        // Preparar
        Activo nuevoActivo = new Activo();
        nuevoActivo.setEtiquetaInventario("TID-PC-002");
        nuevoActivo.setTipo(TipoActivo.ESCRITORIO);
        
        when(activoRepository.save(any(Activo.class))).thenReturn(nuevoActivo);

        // Ejecutar
        Activo resultado = activoService.guardar(nuevoActivo); // Ajusta "guardar" al nombre de tu método

        // Verificar
        assertNotNull(resultado);
        assertEquals("TID-PC-002", resultado.getEtiquetaInventario());
        verify(activoRepository, times(1)).save(nuevoActivo);
    }
}