package com.seguros.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionnaire global des exceptions applicatives et techniques (RFC 7807 ProblemDetail).
 *
 * Règles d'or :
 * - timestamp généré obligatoirement à partir du bean Clock injecté.
 * - traceId unique (UUID) généré et renvoyé pour les erreurs 500, loggué côté serveur avec la trace complète.
 * - Zéro fuite de stack trace ou de messages SQL dans les réponses HTTP.
 * - Format standardisé avec la liste 'errors' pour les erreurs de validation Bean (400).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Entité non trouvée -> HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso no encontrado");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        return problem;
    }

    /**
     * Conflit métier (règle 4.4.1 doublon police active, police non active en sinistre, client avec polices actives) -> HTTP 409 Conflict.
     */
    @ExceptionHandler(BusinessConflictException.class)
    public ProblemDetail handleBusinessConflictException(BusinessConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflicto de negocio");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        return problem;
    }

    /**
     * Violation d'une règle de gestion (dates, machine à états, majorité) -> HTTP 400 Bad Request.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Violación de regla de negocio");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        return problem;
    }

    /**
     * Erreurs de validation des DTOs (@Valid) -> HTTP 400 Bad Request avec tableau 'errors'.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errorDetails = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> err = new HashMap<>();
            err.put("campo", fieldError.getField());
            err.put("mensaje", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valor inválido");
            errorDetails.add(err);
        }

        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            Map<String, String> err = new HashMap<>();
            err.put("campo", globalError.getObjectName());
            err.put("mensaje", globalError.getDefaultMessage() != null ? globalError.getDefaultMessage() : "Objeto inválido");
            errorDetails.add(err);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene parámetros inválidos");
        problem.setTitle("Error de validación");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        problem.setProperty("errors", errorDetails);
        return problem;
    }

    /**
     * Violation d'intégrité de contrainte base de données concurrente non interceptée -> HTTP 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos en base de datos: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Conflicto de integridad de datos en el servidor"
        );
        problem.setTitle("Conflicto de datos");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        return problem;
    }

    /**
     * Exception générique inattendue -> HTTP 500 Internal Server Error (Message neutre + traceId).
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Error interno del servidor no controlado [traceId={}]: ", traceId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ha ocurrido un error interno en el servidor. Por favor, contacte con soporte técnico indicando el identificador de rastreo."
        );
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", OffsetDateTime.now(clock));
        problem.setProperty("traceId", traceId);
        return problem;
    }
}
