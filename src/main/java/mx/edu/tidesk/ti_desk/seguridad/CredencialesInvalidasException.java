package mx.edu.tidesk.ti_desk.seguridad;

/**
 * Correo o contraseña incorrectos, o cuenta desactivada (se traduce a 401).
 * El mensaje es el mismo en los tres casos para no revelar si el correo existe.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}