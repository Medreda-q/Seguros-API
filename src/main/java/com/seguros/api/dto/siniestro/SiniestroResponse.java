package com.seguros.api.dto.siniestro;

import com.seguros.api.entity.enums.EstadoSiniestro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de réponse HTTP représentant un sinistre.
 *
 * Sécurité et Découplage :
 * Ne renvoie JAMAIS l'entité JPA Poliza directement.
 */
public record SiniestroResponse(
    Long id,
    String numero,
    Long polizaId,
    String polizaNumero,
    String descripcion,
    LocalDate fechaOcurrencia,
    BigDecimal importeReclamado,
    EstadoSiniestro estado,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
