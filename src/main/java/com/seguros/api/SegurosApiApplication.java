package com.seguros.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application SegurosAPI.
 * Démarre le contexte Spring Boot, déclenche les migrations Flyway
 * et valide le schéma Hibernate sur PostgreSQL.
 */
@SpringBootApplication
public class SegurosApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SegurosApiApplication.class, args);
    }
}
