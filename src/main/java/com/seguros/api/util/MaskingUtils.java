package com.seguros.api.util;

import java.util.Locale;

/**
 * Utilitaire de masquage des données sensibles (PII - Personally Identifiable Information)
 * pour la conformité RGPD et la protection bancaire dans les DTOs de réponse HTTP.
 */
public final class MaskingUtils {

    private MaskingUtils() {
        // Empêche l'instanciation
    }

    /**
     * Masque un IBAN espagnol pour ne laisser visibles que le code pays et les 4 derniers chiffres.
     * Exemple : "ES1234567890123456783456" -> "ES12 **** **** 3456"
     *
     * @param iban IBAN brut
     * @return IBAN masqué ou null si l'entrée est null
     */
    public static String maskIban(String iban) {
        if (iban == null) {
            return null;
        }

        String clean = iban.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
        if (clean.length() < 8) {
            return "****";
        }

        String prefix = clean.substring(0, 4); // ex: "ES12"
        String suffix = clean.substring(clean.length() - 4); // ex: "3456"
        return prefix + " **** **** " + suffix;
    }

    /**
     * Masque un numéro d'identification espagnol (DNI ou NIE) en préservant les 4 derniers caractères.
     * Exemple DNI : "12345678Z" -> "*****678Z"
     * Exemple NIE : "Y1234567X" -> "*****567X"
     *
     * @param dniNie DNI ou NIE brut
     * @return Identifiant masqué ou null si l'entrée est null
     */
    public static String maskDniNie(String dniNie) {
        if (dniNie == null) {
            return null;
        }

        String clean = dniNie.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
        if (clean.length() <= 4) {
            return "****";
        }

        int visibleCount = 4;
        int maskCount = clean.length() - visibleCount;
        return "*".repeat(maskCount) + clean.substring(maskCount);
    }
}
