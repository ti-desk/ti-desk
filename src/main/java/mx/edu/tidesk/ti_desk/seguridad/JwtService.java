package mx.edu.tidesk.ti_desk.seguridad;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y valida tokens JWT (ADR-003).
 * El token solo lleva el id del usuario y su expiración: el rol y el estado
 * "activo" se leen de la base de datos en cada petición.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits para HS256

    private final SecretKey key;
    private final Duration vigencia;
    private final Clock reloj;

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long minutos) {
        this(secret, minutos, Clock.systemUTC());
    }

    // Constructor para pruebas: permite controlar el reloj.
    JwtService(String secret, long minutos, Clock reloj) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofMinutes(minutos);
        this.reloj = reloj;
    }

    public String generarToken(Long usuarioId) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida firma y expiración y devuelve el id del usuario.
     * @throws JwtException si el token es inválido, está alterado o expiró
     */
    public Long extraerUsuarioId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new JwtException("Token con identificador inválido", e);
        }
    }
}