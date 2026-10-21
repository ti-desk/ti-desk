package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoriaTest {

    @Test
    void contieneLasSeisCategoriasDelAnteproyecto() {
        assertThat(Categoria.values()).containsExactly(
                Categoria.RED, Categoria.CORREO, Categoria.EQUIPO,
                Categoria.SOFTWARE, Categoria.ACCESOS, Categoria.OTRO);
    }

    @Test
    void sePuedeObtenerLaCategoriaPorNombre() {
        assertThat(Categoria.valueOf("RED")).isEqualTo(Categoria.RED);
    }
}
