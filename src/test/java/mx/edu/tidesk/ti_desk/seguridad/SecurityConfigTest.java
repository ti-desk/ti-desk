package mx.edu.tidesk.ti_desk.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.PruebaController.class)
@Import({SecurityConfig.class, ProblemDetailSeguridad.class, SecurityConfigTest.PruebaController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class PruebaController {

        @GetMapping("/api/v1/auth/publico")
        String publico() {
            return "ok";
        }

        @GetMapping("/api/v1/prueba/protegido")
        String protegido() {
            return "ok";
        }

        @PostMapping("/api/v1/prueba/protegido")
        String crear() {
            return "ok";
        }

        @GetMapping("/api/v1/prueba/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() {
            return "ok";
        }
    }

    @Test
    void rutaPublicaNoRequiereToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/publico"))
                .andExpect(status().isOk());
    }

    @Test
    void rutaProtegidaSinTokenResponde401ConProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void postSinTokenResponde401YNoPorCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/prueba/protegido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioAutenticadoAccedeARutaProtegida() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/protegido"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void rolSinPermisoResponde403ConProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/admin"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rolCorrectoAccedeARutaDeAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/admin"))
                .andExpect(status().isOk());
    }
}