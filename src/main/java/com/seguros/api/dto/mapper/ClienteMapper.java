package com.seguros.api.dto.mapper;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.entity.Cliente;
import com.seguros.api.util.MaskingUtils;

import java.util.Locale;

/**
 * Mapper manuel pour la conversion bidirectionnelle entre l'entité JPA Cliente et ses DTOs.
 * Écrit entièrement à la main sans framework de génération de code (ex: MapStruct)
 * pour offrir une visibilité pédagogique totale sur chaque champ.
 */
public final class ClienteMapper {

    private ClienteMapper() {
    }

    /**
     * Convertit une requête de création en nouvelle instance d'entité JPA Cliente.
     */
    public static Cliente toEntity(ClienteCreateRequest request) {
        if (request == null) {
            return null;
        }

        Cliente cliente = new Cliente();
        // Normalisation canonique du DNI/NIE et de l'IBAN à la persistance
        cliente.setDniNie(request.dniNie().replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT));
        cliente.setNombre(request.nombre().trim());
        cliente.setApellidos(request.apellidos().trim());
        cliente.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        cliente.setTelefono(request.telefono().replaceAll("[\\s-]+", ""));
        cliente.setFechaNacimiento(request.fechaNacimiento());
        cliente.setIban(request.iban().replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT));
        return cliente;
    }

    /**
     * Met à jour une entité existante à partir du DTO de modification.
     * Note : DNI/NIE et fechaNacimiento ne sont pas modifiables.
     */
    public static void updateEntity(Cliente cliente, ClienteUpdateRequest request) {
        if (cliente == null || request == null) {
            return;
        }

        cliente.setNombre(request.nombre().trim());
        cliente.setApellidos(request.apellidos().trim());
        cliente.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        cliente.setTelefono(request.telefono().replaceAll("[\\s-]+", ""));
        cliente.setIban(request.iban().replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT));
    }

    /**
     * Convertit une entité JPA Cliente en DTO de réponse avec masquage automatique des données sensibles.
     */
    public static ClienteResponse toResponse(Cliente cliente) {
        if (cliente == null) {
            return null;
        }

        return new ClienteResponse(
            cliente.getId(),
            MaskingUtils.maskDniNie(cliente.getDniNie()),
            cliente.getNombre(),
            cliente.getApellidos(),
            cliente.getEmail(),
            cliente.getTelefono(),
            cliente.getFechaNacimiento(),
            MaskingUtils.maskIban(cliente.getIban()),
            cliente.getCreatedAt(),
            cliente.getUpdatedAt()
        );
    }
}
