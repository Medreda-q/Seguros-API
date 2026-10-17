package com.seguros.api.dto.poliza;

import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de réponse HTTP représentant une police d'assurance.
 *
 * Sécurité et Découplage :
 * - Ne renvoie JAMAIS l'entité JPA Cliente ni la collection Siniestro pour éliminer les boucles JSON
 *   et les LazyInitializationException hors session transactionnelle.
 * - Expose uniquement les informations scalaires nécessaires et le DNI/NIE masqué du titulaire.
 */
public record PolizaResponse(
    Long id,
    String numero,
    TipoPoliza tipo,
    EstadoPoliza estado,
    BigDecimal primaAnual,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Long clienteId,
    String clienteNombreCompleto,
    String clienteDniNieEnmascarado,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
