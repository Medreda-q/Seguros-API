package com.seguros.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuration de l'horloge système injectée.
 * Impose le fuseau horaire 'Europe/Madrid' pour garantir la cohérence temporelle
 * de l'API espagnole et permettre un bouchonnage (mocking) déterministe lors des tests.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE_MADRID = ZoneId.of("Europe/Madrid");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE_MADRID);
    }
}
