package mx.edu.tidesk.ti_desk.usuario.dto;

import mx.edu.tidesk.ti_desk.usuario.Rol;
import mx.edu.tidesk.ti_desk.usuario.Usuario;

import java.time.Instant;

/**
 * Respuesta pública de un usuario. Nunca incluye la contraseña ni su hash.
 */
public record UsuarioResponse(Long id, String nombre, String correo, Rol rol,
                              boolean activo, Instant creadoEn) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(),
                u.getRol(), u.isActivo(), u.getCreadoEn());
    }
}