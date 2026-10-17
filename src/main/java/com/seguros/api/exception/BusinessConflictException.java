package com.seguros.api.exception;

/**
 * Exception levée en cas de conflit avec l'état actuel de la ressource ou d'une contrainte d'unicité métier/base.
 * Correspond au statut HTTP 409 Conflict.
 * Exemples :
 * - Règle 4.4.1 : Tentative de souscrire une deuxième police active du même type.
 * - Règle 4.5.1 : Déclaration d'un sinistre sur une police SUSPENDIDA ou CANCELADA.
 * - Règle 4.5.2 : Déclaration d'un sinistre sur une police expirée.
 * - Suppression d'un client avec des polices actives rattachées.
 */
public class BusinessConflictException extends RuntimeException {

    public BusinessConflictException(String message) {
        super(message);
    }

    public BusinessConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
