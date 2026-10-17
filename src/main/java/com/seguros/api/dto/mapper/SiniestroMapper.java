package com.seguros.api.dto.mapper;

import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.dto.siniestro.SiniestroUpdateRequest;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.Siniestro;
import com.seguros.api.entity.enums.EstadoSiniestro;

/**
 * Mapper manuel pour la conversion bidirectionnelle entre l'entité JPA Siniestro et ses DTOs.
 */
public final class SiniestroMapper {

    private SiniestroMapper() {
    }

    /**
     * Convertit une requête de création en entité JPA Siniestro.
     * Le numéro généré et l'état initial ABIERTO sont imposés par le serveur.
     */
    public static Siniestro toEntity(SiniestroCreateRequest request, Poliza poliza, String numero) {
        if (request == null || poliza == null) {
            return null;
        }

        Siniestro siniestro = new Siniestro();
        siniestro.setNumero(numero);
        siniestro.setPoliza(poliza);
        siniestro.setDescripcion(request.descripcion().trim());
        siniestro.setFechaOcurrencia(request.fechaOcurrencia());
        siniestro.setImporteReclamado(request.importeReclamado());
        siniestro.setEstado(EstadoSiniestro.ABIERTO);
        return siniestro;
    }

    /**
     * Met à jour les éléments déclaratifs du sinistre.
     */
    public static void updateEntity(Siniestro siniestro, SiniestroUpdateRequest request) {
        if (siniestro == null || request == null) {
            return;
        }

        siniestro.setDescripcion(request.descripcion().trim());
        siniestro.setFechaOcurrencia(request.fechaOcurrencia());
        siniestro.setImporteReclamado(request.importeReclamado());
    }

    /**
     * Convertit l'entité JPA Siniestro en DTO de réponse HTTP.
     */
    public static SiniestroResponse toResponse(Siniestro siniestro) {
        if (siniestro == null) {
            return null;
        }

        Long polizaId = null;
        String polizaNumero = null;

        if (siniestro.getPoliza() != null) {
            polizaId = siniestro.getPoliza().getId();
            polizaNumero = siniestro.getPoliza().getNumero();
        }

        return new SiniestroResponse(
            siniestro.getId(),
            siniestro.getNumero(),
            polizaId,
            polizaNumero,
            siniestro.getDescripcion(),
            siniestro.getFechaOcurrencia(),
            siniestro.getImporteReclamado(),
            siniestro.getEstado(),
            siniestro.getCreatedAt(),
            siniestro.getUpdatedAt()
        );
    }
}
