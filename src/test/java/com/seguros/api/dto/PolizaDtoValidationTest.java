package com.seguros.api.dto;

import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaUpdateRequest;
import com.seguros.api.entity.enums.TipoPoliza;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires de validation Bean Validation sur les DTOs Poliza")
class PolizaDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("PolizaCreateRequest valide ne doit générer aucune violation")
    void shouldPassValidationWhenPolizaCreateRequestIsValid() {
        PolizaCreateRequest request = new PolizaCreateRequest(
            1L,
            TipoPoliza.AUTO,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1)
        );

        Set<ConstraintViolation<PolizaCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PolizaCreateRequest avec champs obligatoires null doit être rejeté")
    void shouldFailWhenMandatoryFieldsAreNull() {
        PolizaCreateRequest request = new PolizaCreateRequest(
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<PolizaCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(4);
    }

    @Test
    @DisplayName("fechaFin antérieure à fechaInicio doit être rejetée par @ValidDateRange")
    void shouldFailWhenFechaFinIsBeforeFechaInicio() {
        PolizaCreateRequest request = new PolizaCreateRequest(
            1L,
            TipoPoliza.HOGAR,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 5, 1) // Antérieure !
        );

        Set<ConstraintViolation<PolizaCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("estrictamente posterior");
    }

    @Test
    @DisplayName("fechaFin égale à fechaInicio doit être rejetée par @ValidDateRange")
    void shouldFailWhenFechaFinIsEqualToFechaInicio() {
        LocalDate sameDate = LocalDate.of(2026, 6, 1);
        PolizaCreateRequest request = new PolizaCreateRequest(
            1L,
            TipoPoliza.VIDA,
            sameDate,
            sameDate
        );

        Set<ConstraintViolation<PolizaCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("PolizaUpdateRequest avec période incohérente doit être rejeté")
    void shouldFailWhenPolizaUpdateRequestHasInvalidPeriod() {
        PolizaUpdateRequest request = new PolizaUpdateRequest(
            TipoPoliza.AUTO,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 9, 1)
        );

        Set<ConstraintViolation<PolizaUpdateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }
}
