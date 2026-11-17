package mx.edu.tidesk.ti_desk.seguridad;

import mx.edu.tidesk.ti_desk.usuario.Rol;

/**
 * Usuario que hace la petición, ya validado por el filtro JWT.
 * El rol viene de la base de datos, no del token (ADR-003).
 */
public record UsuarioAutenticado(Long id, Rol rol) {
}