package com.seguros.api.dto;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
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

@DisplayName("Tests unitaires de validation Bean Validation sur les DTOs Cliente")
class ClienteDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("ClienteCreateRequest valide ne doit générer aucune violation")
    void shouldPassValidationWhenClienteCreateRequestIsValid() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "12345678Z",
            "Carlos",
            "García Lorca",
            "carlos.garcia@example.es",
            "612345678",
            LocalDate.of(1990, 5, 15),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ClienteCreateRequest avec NIE valide doit être accepté")
    void shouldPassValidationWhenNieIsValid() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "Y1234567X",
            "Jean",
            "Dupont",
            "jean.dupont@example.es",
            "712345678",
            LocalDate.of(1985, 10, 20),
            "ES9876543210987654321098"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("DNI invalide (fausse lettre de contrôle) doit générer une violation")
    void shouldFailWhenDniChecksumIsInvalid() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "12345678A", // A est incorrect pour 12345678 (reste 14 -> Z)
            "Carlos",
            "García",
            "carlos@example.es",
            "612345678",
            LocalDate.of(1990, 5, 15),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dniNie");
    }

    @Test
    @DisplayName("DNI manquant ou vide doit être rejeté par @NotBlank")
    void shouldFailWhenDniIsBlank() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "   ",
            "Carlos",
            "García",
            "carlos@example.es",
            "612345678",
            LocalDate.of(1990, 5, 15),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dniNie"));
    }

    @Test
    @DisplayName("Email malformé doit être rejeté par @Email")
    void shouldFailWhenEmailIsInvalid() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "12345678Z",
            "Carlos",
            "García",
            "carlos-sin-arroba.com",
            "612345678",
            LocalDate.of(1990, 5, 15),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    @DisplayName("FechaNacimiento dans le futur doit être rejetée par @Past")
    void shouldFailWhenFechaNacimientoIsFuture() {
        ClienteCreateRequest request = new ClienteCreateRequest(
            "12345678Z",
            "Carlos",
            "García",
            "carlos@example.es",
            "612345678",
            LocalDate.now().plusDays(1),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("fechaNacimiento");
    }

    @Test
    @DisplayName("Textes dépassant la taille maximale autorisée doivent générer des violations")
    void shouldFailWhenTextFieldsExceedMaxSize() {
        String longText = "A".repeat(101);
        String longEmail = "A".repeat(145) + "@ex.es"; // 151 chars

        ClienteCreateRequest request = new ClienteCreateRequest(
            "12345678901", // 11 chars (> 10)
            longText,
            longText,
            longEmail,
            "612345678",
            LocalDate.of(1990, 1, 1),
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dniNie"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nombre"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("apellidos"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("IBAN non espagnol ou ne comportant pas 22 chiffres doit être rejeté")
    void shouldFailWhenIbanIsNotValidSpanishIban() {
        // IBAN français FR...
        ClienteCreateRequest requestFrench = new ClienteCreateRequest(
            "12345678Z",
            "Carlos",
            "García",
            "carlos@example.es",
            "612345678",
            LocalDate.of(1990, 1, 1),
            "FR7630006000011234567890189"
        );
        assertThat(validator.validate(requestFrench)).anyMatch(v -> v.getPropertyPath().toString().equals("iban"));

        // IBAN espagnol avec trop peu de chiffres
        ClienteCreateRequest requestShort = new ClienteCreateRequest(
            "12345678Z",
            "Carlos",
            "García",
            "carlos@example.es",
            "612345678",
            LocalDate.of(1990, 1, 1),
            "ES12345678"
        );
        assertThat(validator.validate(requestShort)).anyMatch(v -> v.getPropertyPath().toString().equals("iban"));
    }

    @Test
    @DisplayName("ClienteUpdateRequest valide ne doit générer aucune violation")
    void shouldPassValidationWhenClienteUpdateRequestIsValid() {
        ClienteUpdateRequest request = new ClienteUpdateRequest(
            "Carlos",
            "García Moderno",
            "nuevo.email@example.es",
            "688776655",
            "ES1234567890123456783456"
        );

        Set<ConstraintViolation<ClienteUpdateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
