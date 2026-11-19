package mx.edu.tidesk.ti_desk.usuario;

/**
 * No existe un usuario con ese id (se traduce a 404).
 */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException() {
        super("El usuario no existe");
    }
}