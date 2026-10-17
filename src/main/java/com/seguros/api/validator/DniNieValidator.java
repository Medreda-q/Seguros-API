package com.seguros.api.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validateur algorithmique de conformité DNI et NIE selon la spécification espagnole (módulo 23).
 *
 * Algorithme :
 * 1. Table de correspondance des 23 lettres : "TRWAGMYFPDXBNJZSQVHLCKE".
 * 2. DNI : 8 chiffres suivis d'1 lettre de contrôle calculée par : chiffres % 23.
 * 3. NIE : commence par X, Y ou Z, suivi de 7 chiffres et 1 lettre de contrôle.
 *    X est substitué par 0, Y par 1, Z par 2, puis on applique l'algorithme modulo 23 sur les 8 chiffres résultants.
 *
 * Conformément aux spécifications Bean Validation, la valeur 'null' est considérée comme valide
 * pour respecter la séparation des responsabilités (à combiner avec @NotBlank/@NotNull sur les DTOs).
 */
public class DniNieValidator implements ConstraintValidator<ValidDniNie, String> {

    // Table de correspondance officielle (indices 0 à 22)
    private static final String LETRAS_CONTROL = "TRWAGMYFPDXBNJZSQVHLCKE";

    // Format DNI : 8 chiffres + 1 lettre
    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$");

    // Format NIE : lettre initiale (X, Y ou Z) + 7 chiffres + 1 lettre de contrôle
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ][0-9]{7}[A-Z]$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // En Bean Validation, null est considéré valide pour déléguer la présence à @NotNull/@NotBlank
        if (value == null) {
            return true;
        }

        // Normalisation : suppression des espaces et tirets, passage en majuscules
        String normalized = value.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return false;
        }

        if (DNI_PATTERN.matcher(normalized).matches()) {
            return validateDni(normalized);
        } else if (NIE_PATTERN.matcher(normalized).matches()) {
            return validateNie(normalized);
        }

        return false;
    }

    private boolean validateDni(String dni) {
        String numericPart = dni.substring(0, 8);
        char expectedLetter = dni.charAt(8);
        return checkModulo23(numericPart, expectedLetter);
    }

    private boolean validateNie(String nie) {
        char firstChar = nie.charAt(0);
        String prefixDigit;
        switch (firstChar) {
            case 'X':
                prefixDigit = "0";
                break;
            case 'Y':
                prefixDigit = "1";
                break;
            case 'Z':
                prefixDigit = "2";
                break;
            default:
                return false;
        }

        // Concaténation du chiffre de substitution avec les 7 chiffres suivants
        String numericPart = prefixDigit + nie.substring(1, 8);
        char expectedLetter = nie.charAt(8);
        return checkModulo23(numericPart, expectedLetter);
    }

    private boolean checkModulo23(String numericPart, char expectedLetter) {
        try {
            int number = Integer.parseInt(numericPart);
            int remainder = number % 23;
            char calculatedLetter = LETRAS_CONTROL.charAt(remainder);
            return calculatedLetter == expectedLetter;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
