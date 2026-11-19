package mx.edu.tidesk.ti_desk.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import mx.edu.tidesk.ti_desk.usuario.Rol;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Para pruebas de controladores: deja autenticado a un usuario con el id y el
 * rol indicados, igual que lo haría el filtro JWT con un token válido.
 *
 * <pre>
 * &#64;Test
 * &#64;WithMockCustomUser(id = 5, rol = Rol.TECNICO)
 * void elTecnicoVeSusTickets() { ... }
 * </pre>
 *
 * Sin parámetros equivale al usuario 1 con rol USUARIO.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    long id() default 1L;

    Rol rol() default Rol.USUARIO;
}