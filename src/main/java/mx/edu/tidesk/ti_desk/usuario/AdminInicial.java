package mx.edu.tidesk.ti_desk.usuario;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario ADMIN al arrancar, a partir de las variables de entorno
 * ADMIN_EMAIL y ADMIN_PASSWORD. El registro público siempre crea rol USUARIO,
 * así que sin esto nadie podría administrar el sistema (RF-03).
 * Si el correo ya existe no se modifica: nunca se sobrescribe una cuenta.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInicial.class);
    private static final int MIN_CARACTERES = 8;
    private static final int MAX_BYTES_BCRYPT = 72;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String contrasena;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                        @Value("${app.admin.email:}") String correo,
                        @Value("${app.admin.password:}") String contrasena) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        this.contrasena = contrasena == null ? "" : contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo.isEmpty() || contrasena.isEmpty()) {
            LOG.info("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea administrador inicial");
            return;
        }
        if (contrasena.length() < MIN_CARACTERES
                || contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_BCRYPT) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD debe tener entre " + MIN_CARACTERES + " y "
                            + MAX_BYTES_BCRYPT + " caracteres");
        }
        if (usuarios.findByEmail(correo).isPresent()) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setEmail(correo);
        admin.setPasswordHash(passwordEncoder.encode(contrasena));
        admin.setRol(Rol.ADMIN);
        usuarios.save(admin);
        // Solo se registra el correo; la contraseña nunca va a los logs.
        LOG.info("Administrador inicial creado: {}", correo);
    }
}