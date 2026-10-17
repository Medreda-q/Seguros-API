package com.seguros.api.service;

import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitaires du service actuariel de calcul de prime (CalculadoraPrimaService)")
class CalculadoraPrimaServiceTest {

    private CalculadoraPrimaService calculadoraPrimaService;

    @BeforeEach
    void setUp() {
        calculadoraPrimaService = new CalculadoraPrimaService();
    }

    @Nested
    @DisplayName("Tests des cas limites obligatoires (Boundary Testing - Règle 4.1.C)")
    class BoundaryTests {

        private final LocalDate fechaInicio = LocalDate.of(2026, 6, 1);

        @Test
        @DisplayName("AUTO - 24 ans (frontière supérieure jeune < 25) : facteur 1.50 -> 750,00 €")
        void shouldCalculateAutoYoungDriverBoundary() {
            // Né le 2002-06-01 -> a exactement 24 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(2002, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("750.00"));
        }

        @Test
        @DisplayName("AUTO - 25 ans (frontière inférieure standard >= 25) : facteur 1.00 -> 500,00 €")
        void shouldCalculateAutoStandardLowerBoundary() {
            // Né le 2001-06-01 -> a exactement 25 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(2001, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("AUTO - 65 ans (frontière supérieure standard <= 65) : facteur 1.00 -> 500,00 €")
        void shouldCalculateAutoStandardUpperBoundary() {
            // Né le 1961-06-01 -> a exactement 65 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1961, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("AUTO - 66 ans (frontière inférieure senior > 65) : facteur 1.20 -> 600,00 €")
        void shouldCalculateAutoSeniorBoundary() {
            // Né le 1960-06-01 -> a exactement 66 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1960, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("VIDA - 34 ans (frontière supérieure jeune < 35) : facteur 1.00 -> 200,00 €")
        void shouldCalculateVidaYoungBoundary() {
            // Né le 1992-06-01 -> a exactement 34 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1992, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.VIDA, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("VIDA - 35 ans (frontière inférieure intermédiaire >= 35) : facteur 1.30 -> 260,00 €")
        void shouldCalculateVidaIntermediateLowerBoundary() {
            // Né le 1991-06-01 -> a exactement 35 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1991, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.VIDA, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("260.00"));
        }

        @Test
        @DisplayName("VIDA - 55 ans (frontière supérieure intermédiaire <= 55) : facteur 1.30 -> 260,00 €")
        void shouldCalculateVidaIntermediateUpperBoundary() {
            // Né le 1971-06-01 -> a exactement 55 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1971, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.VIDA, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("260.00"));
        }

        @Test
        @DisplayName("VIDA - 56 ans (frontière inférieure senior > 55) : facteur 1.80 -> 360,00 €")
        void shouldCalculateVidaSeniorBoundary() {
            // Né le 1970-06-01 -> a exactement 56 ans le 2026-06-01
            LocalDate fechaNacimiento = LocalDate.of(1970, 6, 1);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.VIDA, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("360.00"));
        }

        @ParameterizedTest(name = "HOGAR - Âge {0} ans : toujours 300,00 €")
        @CsvSource({"18", "25", "45", "65", "80"})
        @DisplayName("HOGAR - Indépendant de l'âge : toujours tarif fixe 300,00 €")
        void shouldCalculateHogarFlatRateRegardlessOfAge(int edad) {
            LocalDate fechaNacimiento = fechaInicio.minusYears(edad);

            BigDecimal prima = calculadoraPrimaService.calcularPrima(TipoPoliza.HOGAR, fechaNacimiento, fechaInicio);

            assertThat(prima).isEqualByComparingTo(new BigDecimal("300.00"));
        }
    }

    @Nested
    @DisplayName("Validation des entrées et cas d'erreur")
    class ValidationTests {

        private final LocalDate fechaValida = LocalDate.of(2026, 1, 1);

        @Test
        @DisplayName("Rejette un type de police nul")
        void shouldRejectNullTipo() {
            assertThatThrownBy(() -> calculadoraPrimaService.calcularPrima(null, fechaValida, fechaValida))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejette une date de naissance nulle")
        void shouldRejectNullFechaNacimiento() {
            assertThatThrownBy(() -> calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, null, fechaValida))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejette une date de début nulle")
        void shouldRejectNullFechaInicio() {
            assertThatThrownBy(() -> calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, fechaValida, null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejette une date de naissance postérieure à la date de début")
        void shouldRejectBirthDateAfterStartDate() {
            LocalDate birth = LocalDate.of(2026, 5, 1);
            LocalDate start = LocalDate.of(2026, 1, 1);

            assertThatThrownBy(() -> calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, birth, start))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("La fecha de nacimiento no puede ser posterior a la fecha de inicio");
        }
    }
}
