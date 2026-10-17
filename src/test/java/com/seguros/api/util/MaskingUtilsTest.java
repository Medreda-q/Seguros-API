package com.seguros.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires de l'utilitaire de masquage MaskingUtils")
class MaskingUtilsTest {

    @Test
    @DisplayName("Masquage conforme de l'IBAN selon l'exemple exigé (ES12 **** **** 3456)")
    void shouldMaskIbanCorrectly() {
        String iban = "ES1234567890123456783456";
        String masked = MaskingUtils.maskIban(iban);
        assertThat(masked).isEqualTo("ES12 **** **** 3456");
    }

    @Test
    @DisplayName("Masquage de l'IBAN avec espaces à l'entrée")
    void shouldMaskIbanWithSpaces() {
        String iban = "ES12 3456 7890 1234 5678 3456";
        String masked = MaskingUtils.maskIban(iban);
        assertThat(masked).isEqualTo("ES12 **** **** 3456");
    }

    @Test
    @DisplayName("Masquage du DNI (conserve les 4 derniers caractères)")
    void shouldMaskDniCorrectly() {
        String dni = "12345678Z";
        String masked = MaskingUtils.maskDniNie(dni);
        assertThat(masked).isEqualTo("*****678Z");
    }

    @Test
    @DisplayName("Masquage du NIE")
    void shouldMaskNieCorrectly() {
        String nie = "Y1234567X";
        String masked = MaskingUtils.maskDniNie(nie);
        assertThat(masked).isEqualTo("*****567X");
    }

    @Test
    @DisplayName("Gestion sécurisée des valeurs null")
    void shouldHandleNullValuesSafely() {
        assertThat(MaskingUtils.maskIban(null)).isNull();
        assertThat(MaskingUtils.maskDniNie(null)).isNull();
    }
}
