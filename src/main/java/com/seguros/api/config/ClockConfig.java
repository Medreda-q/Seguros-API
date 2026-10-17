package com.seguros.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Configuration de l'horloge système injectée et du support web Spring Data.
 * Impose le fuseau horaire 'Europe/Madrid' pour garantir la cohérence temporelle
 * de l'API espagnole et permettre un bouchonnage (mocking) déterministe lors des tests.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ClockConfig {

    public static final ZoneId ZONE_MADRID = ZoneId.of("Europe/Madrid");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE_MADRID);
    }
}
