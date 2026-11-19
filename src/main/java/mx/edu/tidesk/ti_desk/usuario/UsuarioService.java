package mx.edu.tidesk.ti_desk.usuario;

import java.util.List;
import java.util.Locale;
import mx.edu.tidesk.ti_desk.usuario.dto.ActualizarUsuarioRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.RegistroRequest;
import mx.edu.tidesk.ti_desk.usuario.dto.UsuarioResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * RF-01: registra un usuario nuevo. El rol siempre es USUARIO y la cuenta queda activa.
     * @throws CorreoDuplicadoException si el correo ya existe
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (repositorio.findByEmail(correo).isPresent()) {
            throw new CorreoDuplicadoException();
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setEmail(correo);
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.contrasena()));
        // rol (USUARIO) y activo (true) toman los valores por defecto de la entidad
        try {
            return UsuarioResponse.desde(repositorio.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: lo detiene la restricción única
            throw new CorreoDuplicadoException();
        }
    }

    /**
     * Perfil de un usuario por su id.
     * @throws UsuarioNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return repositorio.findById(id)
                .map(UsuarioResponse::desde)
                .orElseThrow(UsuarioNoEncontradoException::new);
    }

    /** RF-03: lista todos los usuarios, ordenados por id. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repositorio.findAll(Sort.by("id")).stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /**
     * RF-03: cambia el rol o el estado activo de un usuario.
     * Un administrador no puede desactivarse ni quitarse el rol a sí mismo,
     * para que el sistema nunca se quede sin quien lo administre.
     *
     * @param idDeQuienCambia id del administrador que hace el cambio
     * @throws UsuarioNoEncontradoException si el usuario no existe
     * @throws CambioNoPermitidoException si el cambio lo dejaría sin acceso a sí mismo
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest cambios, Long idDeQuienCambia) {
        Usuario usuario = repositorio.findById(id)
                .orElseThrow(UsuarioNoEncontradoException::new);

        boolean esSuPropiaCuenta = id.equals(idDeQuienCambia);
        if (esSuPropiaCuenta && cambios.rol() != null && cambios.rol() != Rol.ADMIN) {
            throw new CambioNoPermitidoException(
                    "No puedes quitarte a ti mismo el rol de administrador");
        }
        if (esSuPropiaCuenta && Boolean.FALSE.equals(cambios.activo())) {
            throw new CambioNoPermitidoException("No puedes desactivar tu propia cuenta");
        }

        if (cambios.rol() != null) {
            usuario.setRol(cambios.rol());
        }
        if (cambios.activo() != null) {
            usuario.setActivo(cambios.activo());
        }
        return UsuarioResponse.desde(repositorio.save(usuario));
    }
}