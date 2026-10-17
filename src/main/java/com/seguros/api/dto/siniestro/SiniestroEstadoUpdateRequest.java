package com.seguros.api.dto.siniestro;

import com.seguros.api.entity.enums.EstadoSiniestro;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour l'avancement d'état contrôlé d'un sinistre (PATCH /api/v1/siniestros/{id}/estado).
 */
public record SiniestroEstadoUpdateRequest(

    @NotNull(message = "El nuevo estado del siniestro es obligatorio")
    EstadoSiniestro nuevoEstado
) {
}
