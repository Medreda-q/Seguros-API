package com.seguros.api.dto.poliza;

import com.seguros.api.entity.enums.EstadoPoliza;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la transition d'état contrôlée d'une police (PATCH /api/v1/polizas/{id}/estado).
 */
public record PolizaEstadoUpdateRequest(

    @NotNull(message = "El nuevo estado de la póliza es obligatorio")
    EstadoPoliza nuevoEstado
) {
}
