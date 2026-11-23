package mx.edu.tidesk.ti_desk.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void clockUsaUtc() {
        Clock clock = new ClockConfig().clock();
        assertEquals(ZoneOffset.UTC, clock.getZone());
    }
}