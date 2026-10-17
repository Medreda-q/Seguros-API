package com.seguros.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-06-15T10:00:00Z"),
        ZoneId.of("Europe/Madrid")
    );

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(fixedClock);
    }

    @Test
    @DisplayName("500 Internal Server Error : traceId unique, message neutre, pas de stack trace")
    void shouldHandleGenericExceptionWithTraceIdAndNeutralMessage() {
        Exception unhandledException = new NullPointerException("Null reference inside internal database worker");

        ProblemDetail problem = exceptionHandler.handleGenericException(unhandledException);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Error interno del servidor");
        assertThat(problem.getDetail()).isEqualTo("Ha ocurrido un error interno en el servidor. Por favor, contacte con soporte técnico indicando el identificador de rastreo.");
        assertThat(problem.getProperties()).containsKey("traceId");
        assertThat(problem.getProperties().get("traceId")).isNotNull();
        assertThat(problem.getProperties()).containsKey("timestamp");
        // Vérifie qu'aucune trace ou message SQL/NPE n'a fuité dans ProblemDetail
        assertThat(problem.getDetail()).doesNotContain("NullPointerException");
        assertThat(problem.getDetail()).doesNotContain("Null reference");
    }

    @Test
    @DisplayName("409 Conflict : DataIntegrityViolationException non gérée")
    void shouldHandleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key violates unique constraint");

        ProblemDetail problem = exceptionHandler.handleDataIntegrityViolationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflicto de datos");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }
}
