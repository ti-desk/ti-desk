package mx.edu.tidesk.ti_desk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TiDeskApplicationTests {

    @Test
    void contextLoads() {
        // Si el contexto (datasource, Flyway, JPA) no levanta, esta prueba falla.
    }
}