package mx.edu.tidesk.ti_desk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import mx.edu.tidesk.ti_desk.IntegracionBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GeneradorFolioTest extends IntegracionBaseTest {

    @Autowired
    GeneradorFolio generadorFolio;

    @Test
    void generaFoliosConsecutivosConElFormatoCorrecto() {
        // Año de prueba distinto al actual para no chocar con otros tickets.
        String primero = generadorFolio.siguiente(1999);
        String segundo = generadorFolio.siguiente(1999);

        assertThat(primero).isEqualTo("TID-1999-00001");
        assertThat(segundo).isEqualTo("TID-1999-00002");
    }

    @Test
    void elConsecutivoEsIndependientePorAnio() {
        generadorFolio.siguiente(1998);

        assertThat(generadorFolio.siguiente(1997)).isEqualTo("TID-1997-00001");
    }

    @Test
    void formateaConCerosALaIzquierda() {
        assertThat(GeneradorFolio.formatear(2026, 42)).isEqualTo("TID-2026-00042");
    }
}
