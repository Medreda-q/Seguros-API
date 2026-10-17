package com.seguros.api.service;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.dto.mapper.ClienteMapper;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.repository.ClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Service métier pour la gestion des clients assurés.
 * Respecte la politique transactionnelle stricte :
 * - @Transactional(readOnly = true) au niveau classe
 * - @Transactional explicite sur les opérations d'écriture
 * - Mapping DTO au sein de la transaction
 * - Dépendance Clock injectée
 */
@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final Clock clock;

    public ClienteService(ClienteRepository clienteRepository, Clock clock) {
        this.clienteRepository = clienteRepository;
        this.clock = clock;
    }

    /**
     * Enregistre un nouveau client après vérification des règles de majorité et d'unicité légale.
     */
    @Transactional
    public ClienteResponse createCliente(ClienteCreateRequest request) {
        LocalDate hoy = LocalDate.now(clock);

        // Règle métier : Client majeur (>= 18 ans)
        if (Period.between(request.fechaNacimiento(), hoy).getYears() < 18) {
            throw new BusinessRuleException("El cliente debe tener al menos 18 años para darse de alta");
        }

        String normalizedDniNie = request.dniNie().replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
        if (clienteRepository.existsByDniNie(normalizedDniNie)) {
            throw new BusinessConflictException("Ya existe un cliente registrado con el DNI/NIE " + request.dniNie());
        }

        Cliente cliente = ClienteMapper.toEntity(request);
        try {
            Cliente saved = clienteRepository.save(cliente);
            return ClienteMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException("Ya existe un cliente registrado con el DNI/NIE " + request.dniNie(), ex);
        }
    }

    /**
     * Recherche un client par son identifiant unique.
     */
    public ClienteResponse getClienteById(Long id) {
        return clienteRepository.findById(id)
            .map(ClienteMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    /**
     * Retourne la liste paginée des clients.
     */
    public Page<ClienteResponse> getClientes(Pageable pageable) {
        return clienteRepository.findAll(pageable)
            .map(ClienteMapper::toResponse);
    }

    /**
     * Retourne la liste complète des clients (non paginée).
     */
    public List<ClienteResponse> getAllClientes() {
        return clienteRepository.findAll().stream()
            .map(ClienteMapper::toResponse)
            .toList();
    }

    /**
     * Met à jour les coordonnées de contact d'un client.
     */
    @Transactional
    public ClienteResponse updateCliente(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        ClienteMapper.updateEntity(cliente, request);
        Cliente updated = clienteRepository.save(cliente);
        return ClienteMapper.toResponse(updated);
    }

    /**
     * Supprime un client si et seulement s'il ne possède aucune police en état ACTIVA.
     */
    @Transactional
    public void deleteCliente(Long id) {
        Cliente cliente = clienteRepository.findWithPolizasById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        boolean tienePolizasActivas = cliente.getPolizas().stream()
            .anyMatch(p -> p.getEstado() == EstadoPoliza.ACTIVA);

        if (tienePolizasActivas) {
            throw new BusinessConflictException("No se puede eliminar el cliente porque tiene pólizas activas asociadas");
        }

        clienteRepository.delete(cliente);
    }
}
