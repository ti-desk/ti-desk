package mx.edu.tidesk.ti_desk.comun;

import java.util.LinkedHashMap;
import java.util.Map;
import mx.edu.tidesk.ti_desk.seguridad.CredencialesInvalidasException;
import mx.edu.tidesk.ti_desk.ticket.TransicionNoPermitidaException;
import mx.edu.tidesk.ti_desk.usuario.CambioNoPermitidoException;
import mx.edu.tidesk.ti_desk.usuario.CorreoDuplicadoException;
import mx.edu.tidesk.ti_desk.usuario.UsuarioNoEncontradoException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones de la API a ProblemDetail (RFC 9457).
 * Los mensajes son fijos o vienen de las reglas de validación:
 * nunca se devuelve el detalle interno de la excepción.
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** 400: un campo no cumple las reglas de validación. Indica cuál y por qué. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos.");
        problema.setTitle("Datos inválidos");
        problema.setProperty("errores", errores);
        return problema;
    }

    /** 400: el cuerpo no es JSON válido o viene vacío. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoIlegible(HttpMessageNotReadableException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.");
        problema.setTitle("Petición mal formada");
        return problema;
    }

    /** 401: login fallido. Mismo mensaje sin importar la causa (RF-02). */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        problema.setTitle("Credenciales inválidas");
        return problema;
    }

    /** 409: el correo ya está registrado (RF-01). */
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ProblemDetail correoDuplicado(CorreoDuplicadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "El correo ya está registrado.");
        problema.setTitle("Correo duplicado");
        return problema;
    }

    /** 404: el usuario no existe (RF-03). */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ProblemDetail usuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "El usuario no existe.");
        problema.setTitle("Usuario no encontrado");
        return problema;
    }

    /** 409: el cambio no se permite, por ejemplo un ADMIN que se desactiva a sí mismo (RF-03). */
    @ExceptionHandler(CambioNoPermitidoException.class)
    public ProblemDetail cambioNoPermitido(CambioNoPermitidoException ex) {
        // El mensaje lo escribe UsuarioService: es texto fijo, no datos de la petición.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage() + ".");
        problema.setTitle("Cambio no permitido");
        return problema;
    }

    /** 400: un parámetro de consulta no tiene el tipo esperado, por ejemplo ?estado=PERDIDO (RF-05). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException ex) {
        // Solo se usa el nombre del parámetro, nunca el valor que mandó el cliente.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido.");
        problema.setTitle("Parámetro inválido");
        return problema;
    }

    /** 400: un parámetro tiene un valor no permitido, por ejemplo un campo de orden (RF-05). */
    @ExceptionHandler(ParametroInvalidoException.class)
    public ProblemDetail parametroInvalido(ParametroInvalidoException ex) {
        // El mensaje lo escribe TicketController: es texto fijo.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Parámetro inválido");
        return problema;
    }

    /** 404: el recurso no existe o no es visible para el usuario (RF-04). */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
        // El mensaje lo escribe TicketService: es texto fijo con el id buscado.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Recurso no encontrado");
        return problema;
    }

    /** 409: la máquina de estados no permite el cambio; indica la regla violada (RF-08). */
    @ExceptionHandler(TransicionNoPermitidaException.class)
    public ProblemDetail transicionNoPermitida(TransicionNoPermitidaException ex) {
        // El mensaje lo escribe MaquinaEstadosTicket o TicketService: es texto fijo.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Transición de estado no permitida");
        problema.setProperty("estadoActual", ex.getEstadoActual());
        problema.setProperty("estadoSolicitado", ex.getEstadoSolicitado());
        return problema;
    }

    /** 403: el usuario ve el recurso, pero no le toca hacer esa acción (RF-08). */
    @ExceptionHandler(AccionNoPermitidaException.class)
    public ProblemDetail accionNoPermitida(AccionNoPermitidaException ex) {
        // El mensaje lo escribe TicketService: es texto fijo.
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problema.setTitle("Acción no permitida");
        return problema;
    }

    /** 409: otro usuario modificó el mismo registro al mismo tiempo (@Version). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail conflictoDeVersion(OptimisticLockingFailureException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Otro usuario modificó este registro al mismo tiempo. Vuelve a consultarlo e intenta de nuevo.");
        problema.setTitle("Modificación concurrente");
        return problema;
    }
}
