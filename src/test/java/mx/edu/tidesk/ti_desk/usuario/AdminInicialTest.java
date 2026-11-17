package mx.edu.tidesk.ti_desk.usuario;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInicialTest {

    private static final String CORREO = "admin@unistmo.edu.mx";
    private static final String CONTRASENA = "Admin#Segura2026";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    // Costo 4 (el mínimo) para que las pruebas sean rápidas.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminInicial adminInicial(String correo, String contrasena) {
        return new AdminInicial(repositorio, encoder, correo, contrasena);
    }

    @Test
    void creaElAdminSiNoExiste() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.empty());

        adminInicial("  Admin@UNISTMO.edu.mx ", CONTRASENA).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getEmail()).isEqualTo(CORREO);
        assertThat(creado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getPasswordHash()).isNotEqualTo(CONTRASENA);
        assertThat(encoder.matches(CONTRASENA, creado.getPasswordHash())).isTrue();
    }

    @Test
    void noModificaLaCuentaSiElCorreoYaExiste() {
        when(repositorio.findByEmail(CORREO)).thenReturn(Optional.of(new Usuario()));

        adminInicial(CORREO, CONTRASENA).run(null);

        verify(repositorio, never()).save(any());
    }

    @Test
    void noHaceNadaSiFaltanLasVariables() {
        adminInicial("", "").run(null);
        adminInicial(CORREO, "").run(null);
        adminInicial(null, null).run(null);

        verify(repositorio, never()).findByEmail(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaUnaContrasenaDemasiadoCorta() {
        assertThatThrownBy(() -> adminInicial(CORREO, "corta").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");

        verify(repositorio, never()).save(any());
    }
}