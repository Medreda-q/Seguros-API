package com.seguros.api.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du validateur algorithmique DNI / NIE (módulo 23).
 */
class DniNieValidatorTest {

    private DniNieValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DniNieValidator();
    }

    @Test
    @DisplayName("null doit être considéré comme valide (délégué à @NotNull/@NotBlank)")
    void shouldConsiderNullAsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest(name = "DNI valide : {0}")
    @ValueSource(strings = {
            "12345678Z",
            "00000000T",
            "99999999R"
    })
    @DisplayName("DNI valides selon l'algorithme modulo 23")
    void shouldValidateValidDni(String dni) {
        assertThat(validator.isValid(dni, null)).isTrue();
    }

    @ParameterizedTest(name = "NIE valide : {0}")
    @ValueSource(strings = {
            "X1234567L", // X -> 01234567 % 23 = 19 (L)
            "Y1234567X", // Y -> 11234567 % 23 = 10 (X)
            "Z1234567R"  // Z -> 21234567 % 23 = 1 (R)
    })
    @DisplayName("NIE valides avec préfixes X, Y et Z")
    void shouldValidateValidNie(String nie) {
        assertThat(validator.isValid(nie, null)).isTrue();
    }

    @ParameterizedTest(name = "Format normalisé valide : ''{0}''")
    @ValueSource(strings = {
            "  12345678z  ",
            "12345678-z",
            "12345678-Z",
            "  x1234567l  ",
            "X-1234567-L",
            "x-1234567-l"
    })
    @DisplayName("Tolérance des minuscules, espaces et tirets avant validation")
    void shouldNormalizeAndValidate(String rawValue) {
        assertThat(validator.isValid(rawValue, null)).isTrue();
    }

    @ParameterizedTest(name = "Lettre erronée : {0}")
    @ValueSource(strings = {
            "12345678A", // Pour 12345678 la lettre correcte est Z
            "12345678B",
            "X1234567A"  // Pour X1234567 la lettre correcte est L
    })
    @DisplayName("DNI/NIE rejetés si la lettre de contrôle est fausse")
    void shouldRejectInvalidControlLetter(String invalidValue) {
        assertThat(validator.isValid(invalidValue, null)).isFalse();
    }

    @ParameterizedTest(name = "Longueur erronée : {0}")
    @ValueSource(strings = {
            "1234Z",
            "1234567Z",
            "123456789Z",
            "X123456L",
            "X12345678L"
    })
    @DisplayName("DNI/NIE rejetés si la longueur est incorrecte (trop court ou trop long)")
    void shouldRejectInvalidLength(String invalidLength) {
        assertThat(validator.isValid(invalidLength, null)).isFalse();
    }

    @ParameterizedTest(name = "Caractères interdits : {0}")
    @ValueSource(strings = {
            "1234A678Z", // Lettre dans la partie numérique
            "A1234567L", // Préfixe invalide (A au lieu de X, Y ou Z)
            "123456789", // Pas de lettre de contrôle
            "12345678@", // Caractère spécial
            "X12345678"  // Pas de lettre finale
    })
    @DisplayName("DNI/NIE rejetés en cas de caractères non autorisés ou préfixe NIE inconnu")
    void shouldRejectForbiddenCharacters(String invalidChars) {
        assertThat(validator.isValid(invalidChars, null)).isFalse();
    }

    @ParameterizedTest(name = "Chaîne vide ou blanche : ''{0}''")
    @ValueSource(strings = {
            "",
            "   ",
            "-",
            "   -   "
    })
    @DisplayName("Chaînes vides, blanches ou réduites à des tirets rejetées")
    void shouldRejectEmptyOrBlank(String blankValue) {
        assertThat(validator.isValid(blankValue, null)).isFalse();
    }
}
