package com.seguros.api.exception;

/**
 * Exception levée lorsqu'une entité recherchée n'existe pas en base de données.
 * Correspond au statut HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
