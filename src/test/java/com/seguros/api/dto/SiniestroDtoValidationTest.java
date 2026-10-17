package com.seguros.api.dto;

import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires de validation Bean Validation sur les DTOs Siniestro")
class SiniestroDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("SiniestroCreateRequest valide ne doit générer aucune violation")
    void shouldPassValidationWhenSiniestroCreateRequestIsValid() {
        SiniestroCreateRequest request = new SiniestroCreateRequest(
            10L,
            "Colisión trasera en cruce con daños en parachoques.",
            LocalDate.now().minusDays(2),
            new BigDecimal("1250.50")
        );

        Set<ConstraintViolation<SiniestroCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Description trop courte (< 10 caractères) doit être rejetée")
    void shouldFailWhenDescripcionIsTooShort() {
        SiniestroCreateRequest request = new SiniestroCreateRequest(
            10L,
            "Golpe", // 5 chars
            LocalDate.now(),
            new BigDecimal("500.00")
        );

        Set<ConstraintViolation<SiniestroCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("descripcion"));
    }

    @Test
    @DisplayName("Description trop longue (> 1000 caractères) doit être rejetée")
    void shouldFailWhenDescripcionIsTooLong() {
        SiniestroCreateRequest request = new SiniestroCreateRequest(
            10L,
            "A".repeat(1001),
            LocalDate.now(),
            new BigDecimal("500.00")
        );

        Set<ConstraintViolation<SiniestroCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("descripcion"));
    }

    @Test
    @DisplayName("Importe non strictement positif (zéro ou négatif) doit être rejeté par @Positive")
    void shouldFailWhenImporteIsNotPositive() {
        SiniestroCreateRequest requestZero = new SiniestroCreateRequest(
            10L,
            "Daños por agua en cocina.",
            LocalDate.now(),
            BigDecimal.ZERO
        );
        assertThat(validator.validate(requestZero)).anyMatch(v -> v.getPropertyPath().toString().equals("importeReclamado"));

        SiniestroCreateRequest requestNegative = new SiniestroCreateRequest(
            10L,
            "Daños por agua en cocina.",
            LocalDate.now(),
            new BigDecimal("-150.00")
        );
        assertThat(validator.validate(requestNegative)).anyMatch(v -> v.getPropertyPath().toString().equals("importeReclamado"));
    }

    @Test
    @DisplayName("Importe avec plus de 2 décimales doit être rejeté par @Digits")
    void shouldFailWhenImporteHasTooManyFractionalDigits() {
        SiniestroCreateRequest request = new SiniestroCreateRequest(
            10L,
            "Daños causados por granizo en luna delantera.",
            LocalDate.now(),
            new BigDecimal("450.999") // 3 décimales
        );

        Set<ConstraintViolation<SiniestroCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("importeReclamado"));
    }

    @Test
    @DisplayName("FechaOcurrencia dans le futur doit être rejetée par @PastOrPresent")
    void shouldFailWhenFechaOcurrenciaIsFuture() {
        SiniestroCreateRequest request = new SiniestroCreateRequest(
            10L,
            "Daños causados por filtración.",
            LocalDate.now().plusDays(2),
            new BigDecimal("300.00")
        );

        Set<ConstraintViolation<SiniestroCreateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fechaOcurrencia"));
    }

    @Test
    @DisplayName("SiniestroUpdateRequest valide ne doit générer aucune violation")
    void shouldPassValidationWhenSiniestroUpdateRequestIsValid() {
        SiniestroUpdateRequest request = new SiniestroUpdateRequest(
            "Ampliación del informe pericial con factura de taller.",
            LocalDate.now().minusDays(1),
            new BigDecimal("1800.00")
        );

        Set<ConstraintViolation<SiniestroUpdateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
