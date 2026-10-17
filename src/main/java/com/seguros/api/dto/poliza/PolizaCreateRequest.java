package com.seguros.api.dto.poliza;

import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.validator.DateRangeAware;
import com.seguros.api.validator.ValidDateRange;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de création d'une police d'assurance (POST /api/v1/polizas).
 *
 * Protection Mass Assignment :
 * Ne contient JAMAIS :
 * - id : clé primaire générée par la base.
 * - numero : identifiant métier généré automatiquement par le serveur (ex: POL-AUTO-2026-0001).
 * - estado : fixé automatiquement à ACTIVA à l'émission.
 * - primaAnual : calculée exclusivement par le moteur actuariel côté serveur (formule métier).
 * - dates d'audit : gérées automatiquement par JPA.
 */
@ValidDateRange
public record PolizaCreateRequest(

    @NotNull(message = "El ID del cliente es obligatorio")
    Long clienteId,

    @NotNull(message = "El tipo de póliza es obligatorio")
    TipoPoliza tipo,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate fechaInicio,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDate fechaFin
) implements DateRangeAware {
}
