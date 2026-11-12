package mx.edu.tidesk.ti_desk.usuario;

/**
 * Da el id y el rol del usuario que hace la petición.
 */
public interface UsuarioActual {

    Long id();

    Rol rol();
}
