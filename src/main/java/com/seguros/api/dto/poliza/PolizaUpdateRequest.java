package com.seguros.api.dto.poliza;

import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.validator.DateRangeAware;
import com.seguros.api.validator.ValidDateRange;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de mise à jour des paramètres contractuels d'une police (PUT /api/v1/polizas/{id}).
 *
 * Protection Mass Assignment :
 * Ne contient ni id, ni numero, ni estado (géré via PATCH dédié), ni primaAnual, ni dates d'audit.
 */
@ValidDateRange
public record PolizaUpdateRequest(

    @NotNull(message = "El tipo de póliza es obligatorio")
    TipoPoliza tipo,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate fechaInicio,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDate fechaFin
) implements DateRangeAware {
}
