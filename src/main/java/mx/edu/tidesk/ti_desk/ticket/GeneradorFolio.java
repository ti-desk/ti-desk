package mx.edu.tidesk.ti_desk.ticket;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Genera folios con formato TID-AAAA-NNNNN usando la tabla folio_secuencia.
 * El INSERT ... ON CONFLICT ... RETURNING es una sola sentencia atómica, así
 * que dos tickets creados al mismo tiempo nunca reciben el mismo número.
 * El consecutivo reinicia cada año.
 */
@Component
public class GeneradorFolio {

    private static final String SIGUIENTE_NUMERO = """
            INSERT INTO folio_secuencia (anio, ultimo) VALUES (?, 1)
            ON CONFLICT (anio) DO UPDATE SET ultimo = folio_secuencia.ultimo + 1
            RETURNING ultimo
            """;

    private final JdbcTemplate jdbc;

    public GeneradorFolio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String siguiente(int anio) {
        Integer numero = jdbc.queryForObject(SIGUIENTE_NUMERO, Integer.class, anio);
        return formatear(anio, numero);
    }

    static String formatear(int anio, int numero) {
        return String.format("TID-%04d-%05d", anio, numero);
    }
}
