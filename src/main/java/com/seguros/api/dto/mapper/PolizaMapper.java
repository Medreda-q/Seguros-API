package com.seguros.api.dto.mapper;

import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaResponse;
import com.seguros.api.dto.poliza.PolizaUpdateRequest;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.util.MaskingUtils;

import java.math.BigDecimal;

/**
 * Mapper manuel pour la conversion bidirectionnelle entre l'entité JPA Poliza et ses DTOs.
 */
public final class PolizaMapper {

    private PolizaMapper() {
    }

    /**
     * Convertit une requête de création en entité JPA Poliza.
     * Les valeurs serveur (numéro généré, prime actuarielle calculée, état ACTIVA initial) sont injectées ici.
     */
    public static Poliza toEntity(PolizaCreateRequest request, Cliente cliente, String numero, BigDecimal primaAnual) {
        if (request == null || cliente == null) {
            return null;
        }

        Poliza poliza = new Poliza();
        poliza.setNumero(numero);
        poliza.setTipo(request.tipo());
        poliza.setEstado(EstadoPoliza.ACTIVA);
        poliza.setPrimaAnual(primaAnual);
        poliza.setFechaInicio(request.fechaInicio());
        poliza.setFechaFin(request.fechaFin());
        poliza.setCliente(cliente);
        return poliza;
    }

    /**
     * Met à jour les paramètres éditables de la police.
     */
    public static void updateEntity(Poliza poliza, PolizaUpdateRequest request) {
        if (poliza == null || request == null) {
            return;
        }

        poliza.setTipo(request.tipo());
        poliza.setFechaInicio(request.fechaInicio());
        poliza.setFechaFin(request.fechaFin());
    }

    /**
     * Convertit l'entité Poliza en DTO de réponse, en extrayant les informations scalaires du client
     * sans renvoyer l'objet entité JPA.
     */
    public static PolizaResponse toResponse(Poliza poliza) {
        if (poliza == null) {
            return null;
        }

        Long clienteId = null;
        String clienteNombreCompleto = null;
        String clienteDniNieEnmascarado = null;

        if (poliza.getCliente() != null) {
            clienteId = poliza.getCliente().getId();
            clienteNombreCompleto = poliza.getCliente().getNombre() + " " + poliza.getCliente().getApellidos();
            clienteDniNieEnmascarado = MaskingUtils.maskDniNie(poliza.getCliente().getDniNie());
        }

        return new PolizaResponse(
            poliza.getId(),
            poliza.getNumero(),
            poliza.getTipo(),
            poliza.getEstado(),
            poliza.getPrimaAnual(),
            poliza.getFechaInicio(),
            poliza.getFechaFin(),
            clienteId,
            clienteNombreCompleto,
            clienteDniNieEnmascarado,
            poliza.getCreatedAt(),
            poliza.getUpdatedAt()
        );
    }
}
