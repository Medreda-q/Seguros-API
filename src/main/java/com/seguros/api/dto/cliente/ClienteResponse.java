package com.seguros.api.dto.cliente;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de réponse HTTP représentant un client.
 *
 * Sécurité et Encapsulation :
 * - Ne renvoie JAMAIS l'entité JPA Cliente directement pour éviter les fuites de structure interne.
 * - Fournit les représentations masquées de l'IBAN et du DNI/NIE pour protéger la vie privée des assurés.
 */
public record ClienteResponse(
    Long id,
    String dniNieEnmascarado,
    String nombre,
    String apellidos,
    String email,
    String telefono,
    LocalDate fechaNacimiento,
    String ibanEnmascarado,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
