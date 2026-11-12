package mx.edu.tidesk.ti_desk.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación en UTC. Se inyecta donde se necesite la hora actual
 * para que las pruebas puedan usar un tiempo fijo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
