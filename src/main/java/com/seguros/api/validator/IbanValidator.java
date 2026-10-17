package com.seguros.api.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validateur Bean Validation pour l'IBAN espagnol.
 *
 * Spécification espagnole :
 * - Commence obligatoirement par "ES" (insensible à la casse à la saisie, normalisé en majuscules).
 * - Suivi exactement de 2 chiffres de contrôle et de 20 chiffres de compte (22 chiffres au total).
 * - Longueur totale : 24 caractères.
 *
 * Conformément aux règles Bean Validation, null est considéré comme valide pour déléguer l'obligation à @NotBlank.
 */
public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    private static final Pattern SPANISH_IBAN_PATTERN = Pattern.compile("^ES[0-9]{22}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // Normalisation : suppression des espaces et tirets fréquents lors de la saisie
        String normalized = value.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return false;
        }

        return SPANISH_IBAN_PATTERN.matcher(normalized).matches();
    }
}
