package mx.edu.tidesk.ti_desk.usuario;

import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UsuarioService servicio = new UsuarioService(repositorio, encoder);

    private static RegistroRequest solicitud(String correo) {
        return new RegistroRequest("  Ana López ", correo, "Segura#2026");
    }

    @Test
    void registroValidoCreaUsuarioActivoConRolUsuarioYHashBcrypt() {
        when(repositorio.findByEmail("ana.lopez@unistmo.edu.mx")).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.registrar(solicitud("ana.lopez@unistmo.edu.mx"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getNombre()).isEqualTo("Ana López");
        assertThat(guardado.getPasswordHash()).isNotEqualTo("Segura#2026").startsWith("$2");
        assertThat(encoder.matches("Segura#2026", guardado.getPasswordHash())).isTrue();
        assertThat(respuesta.rol()).isEqualTo(Rol.USUARIO);
        assertThat(respuesta.toString()).doesNotContain(guardado.getPasswordHash());
    }

    @Test
    void elCorreoSeGuardaEnMinusculas() {
        when(repositorio.findByEmail("ana.lopez@unistmo.edu.mx")).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse respuesta = servicio.registrar(solicitud("Ana.Lopez@UNISTMO.edu.mx"));

        assertThat(respuesta.correo()).isEqualTo("ana.lopez@unistmo.edu.mx");
    }

    @Test
    void correoDuplicadoLanzaExcepcionYNoGuarda() {
        when(repositorio.findByEmail("ana.lopez@unistmo.edu.mx")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> servicio.registrar(solicitud("ANA.LOPEZ@unistmo.edu.mx")))
                .isInstanceOf(CorreoDuplicadoException.class);
        verify(repositorio, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void laRestriccionUnicaDeLaBaseTambienLanzaCorreoDuplicado() {
        when(repositorio.findByEmail("ana.lopez@unistmo.edu.mx")).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("correo duplicado"));

        assertThatThrownBy(() -> servicio.registrar(solicitud("ana.lopez@unistmo.edu.mx")))
                .isInstanceOf(CorreoDuplicadoException.class);
    }
}