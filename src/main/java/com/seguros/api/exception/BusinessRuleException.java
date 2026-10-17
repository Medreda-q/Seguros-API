package com.seguros.api.exception;

/**
 * Exception levée en cas de violation d'une règle de gestion ou de validation temporelle/état.
 * Correspond au statut HTTP 400 Bad Request.
 * Exemples :
 * - Règle 4.4.2 : Date de début de police dans le passé.
 * - Règle 4.5.3 : Date d'occurrence hors de la période de validité de la police.
 * - Règle 4.5.4 : Date d'occurrence dans le futur.
 * - Transition d'état non autorisée selon la matrice métier.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
