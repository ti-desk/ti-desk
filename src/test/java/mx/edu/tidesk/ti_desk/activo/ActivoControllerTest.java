package mx.edu.tidesk.ti_desk.activo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import mx.edu.tidesk.ti_desk.IntegracionBaseTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc

class ActivoControllerTest extends IntegracionBaseTest { // <-- AGREGA ESTE "extends"

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crearActivo_Retorna201() throws Exception {
        // Usamos los campos reales de tu entidad Activo
        String jsonActivo = """
            {
                "etiquetaInventario": "TID-LAP-999",
                "tipo": "LAPTOP",
                "marca": "Dell",
                "modelo": "Latitude 5420",
                "numeroSerie": "SN-123456",
                "ubicacion": "Edificio A, Aula 101",
                "estado": "ACTIVO"
            }
            """;

        // Ajusta "/api/v1/activos" a la ruta real de tu controlador
        mockMvc.perform(post("/api/v1/activos") 
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonActivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etiquetaInventario").value("TID-LAP-999"))
                .andExpect(jsonPath("$.tipo").value("LAPTOP"));
    }

    @Test
    void buscarActivoInexistente_Retorna404() throws Exception {
        // Buscamos un ID que no exista en la BD de pruebas
        mockMvc.perform(get("/api/v1/activos/9999"))
                .andExpect(status().isNotFound());
    }
}