package mx.edu.tidesk.ti_desk.usuario;

import java.util.List;
import java.util.Optional;
import mx.edu.tidesk.ti_desk.usuario.dto.ActualizarUsuarioRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la administración de usuarios (RF-03).
 */
class UsuarioAdministracionTest {

    private static final long ID_ADMIN = 1L;
    private static final long ID_OTRO = 5L;

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final UsuarioService servicio =
            new UsuarioService(repositorio, new BCryptPasswordEncoder(4));

    private Usuario existe(long id, Rol rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario " + id);
        u.setEmail("usuario" + id + "@unistmo.edu.mx");
        u.setPasswordHash("hash-que-no-debe-salir");
        u.setRol(rol);
        u.setActivo(activo);
        when(repositorio.findById(id)).thenReturn(Optional.of(u));
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        return u;
    }

    @Test
    void obtenerDevuelveElPerfilSinElHash() {
        existe(ID_OTRO, Rol.TECNICO, true);

        UsuarioResponse respuesta = servicio.obtener(ID_OTRO);

        assertThat(respuesta.id()).isEqualTo(ID_OTRO);
        assertThat(respuesta.rol()).isEqualTo(Rol.TECNICO);
        assertThat(respuesta.toString()).doesNotContain("hash-que-no-debe-salir");
    }

    @Test
    void obtenerUsuarioInexistenteLanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void listarDevuelveTodosLosUsuariosComoDto() {
        Usuario a = existe(ID_ADMIN, Rol.ADMIN, true);
        Usuario b = existe(ID_OTRO, Rol.USUARIO, false);
        when(repositorio.findAll(any(Sort.class))).thenReturn(List.of(a, b));

        List<UsuarioResponse> lista = servicio.listar();

        assertThat(lista).extracting(UsuarioResponse::id).containsExactly(ID_ADMIN, ID_OTRO);
        assertThat(lista.get(1).activo()).isFalse();
    }

    @Test
    void adminCambiaElRolDeOtroUsuario() {
        Usuario u = existe(ID_OTRO, Rol.USUARIO, true);

        UsuarioResponse respuesta = servicio.actualizar(
                ID_OTRO, new ActualizarUsuarioRequest(Rol.TECNICO, null), ID_ADMIN);

        assertThat(respuesta.rol()).isEqualTo(Rol.TECNICO);
        assertThat(respuesta.activo()).isTrue();
        verify(repositorio).save(u);
    }

    @Test
    void adminDesactivaYReactivaAOtroUsuario() {
        existe(ID_OTRO, Rol.USUARIO, true);

        assertThat(servicio.actualizar(
                ID_OTRO, new ActualizarUsuarioRequest(null, false), ID_ADMIN).activo()).isFalse();
        assertThat(servicio.actualizar(
                ID_OTRO, new ActualizarUsuarioRequest(null, true), ID_ADMIN).activo()).isTrue();
    }

    @Test
    void actualizarUsuarioInexistenteLanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(
                99L, new ActualizarUsuarioRequest(Rol.ADMIN, null), ID_ADMIN))
                .isInstanceOf(UsuarioNoEncontradoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void adminNoPuedeQuitarseElRolASiMismo() {
        Usuario admin = existe(ID_ADMIN, Rol.ADMIN, true);

        assertThatThrownBy(() -> servicio.actualizar(
                ID_ADMIN, new ActualizarUsuarioRequest(Rol.USUARIO, null), ID_ADMIN))
                .isInstanceOf(CambioNoPermitidoException.class);

        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        verify(repositorio, never()).save(any());
    }

    @Test
    void adminNoPuedeDesactivarseASiMismo() {
        Usuario admin = existe(ID_ADMIN, Rol.ADMIN, true);

        assertThatThrownBy(() -> servicio.actualizar(
                ID_ADMIN, new ActualizarUsuarioRequest(null, false), ID_ADMIN))
                .isInstanceOf(CambioNoPermitidoException.class);

        assertThat(admin.isActivo()).isTrue();
        verify(repositorio, never()).save(any());
    }

    @Test
    void laSolicitudSinCambiosNoEsValida() {
        assertThat(new ActualizarUsuarioRequest(null, null).isConCambios()).isFalse();
        assertThat(new ActualizarUsuarioRequest(Rol.TECNICO, null).isConCambios()).isTrue();
        assertThat(new ActualizarUsuarioRequest(null, false).isConCambios()).isTrue();
    }
}