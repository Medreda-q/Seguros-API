package com.seguros.api.service;

import com.seguros.api.dto.mapper.PolizaMapper;
import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaEstadoUpdateRequest;
import com.seguros.api.dto.poliza.PolizaResponse;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.repository.ClienteRepository;
import com.seguros.api.repository.PolizaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Service métier pour l'émission et la gestion du cycle de vie des polices d'assurance.
 *
 * Implémente :
 * - Règle 4.4.1 : Unicité d'une police ACTIVA par type et par client (vérification applicative + catch DataIntegrityViolationException -> 409).
 * - Règle 4.4.2 : Date de début non antérieure à la date du jour (LocalDate.now(clock)).
 * - Tarification actuarielle deterministe en BigDecimal avec CalculadoraPrimaService.
 * - Génération de numéros atomiques par séquence PostgreSQL (POL-{TIPO}-{ANIO}-{0001}).
 * - Matrice stricte des transitions d'états (ACTIVA -> SUSPENDIDA/CANCELADA, SUSPENDIDA -> ACTIVA/CANCELADA).
 */
@Service
@Transactional(readOnly = true)
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final ClienteRepository clienteRepository;
    private final CalculadoraPrimaService calculadoraPrimaService;
    private final Clock clock;

    public PolizaService(PolizaRepository polizaRepository,
                         ClienteRepository clienteRepository,
                         CalculadoraPrimaService calculadoraPrimaService,
                         Clock clock) {
        this.polizaRepository = polizaRepository;
        this.clienteRepository = clienteRepository;
        this.calculadoraPrimaService = calculadoraPrimaService;
        this.clock = clock;
    }

    /**
     * Émet une nouvelle police d'assurance.
     */
    @Transactional
    public PolizaResponse createPoliza(PolizaCreateRequest request) {
        LocalDate hoy = LocalDate.now(clock);

        // 1. Vérification d'existence du client
        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + request.clienteId()));

        // 2. Règle 4.4.2 : fechaInicio >= hoy
        if (request.fechaInicio().isBefore(hoy)) {
            throw new BusinessRuleException("La fecha de inicio de la póliza no puede ser anterior a la fecha de hoy (" + hoy + ")");
        }

        // Cohérence de la plage de dates
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new BusinessRuleException("La fecha de fin debe ser estrictamente posterior a la fecha de inicio");
        }

        // 3. Règle 4.4.1 (Vérification applicative préalable)
        if (polizaRepository.existsByClienteIdAndTipoAndEstado(cliente.getId(), request.tipo(), EstadoPoliza.ACTIVA)) {
            throw new BusinessConflictException("El cliente ya tiene una póliza ACTIVA de tipo " + request.tipo()
                + ". Cancele o suspenda la póliza existente antes de contratar una nueva.");
        }

        // 4. Calcul actuariel de la prime annuelle
        BigDecimal primaAnual = calculadoraPrimaService.calcularPrima(
            request.tipo(),
            cliente.getFechaNacimiento(),
            request.fechaInicio()
        );

        // 5. Génération du numéro unique via séquence PostgreSQL (atome garanti sans collision)
        Long seqVal = polizaRepository.getNextNumeroSequence();
        String numero = String.format("POL-%s-%d-%04d", request.tipo().name(), hoy.getYear(), seqVal);

        // 6. Instanciation de l'entité
        Poliza poliza = PolizaMapper.toEntity(request, cliente, numero, primaAnual);

        // 7. Persistance et protection contre les courses critiques via l'index partiel PostgreSQL
        try {
            Poliza saved = polizaRepository.saveAndFlush(poliza);
            return PolizaMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException("El cliente ya tiene una póliza ACTIVA de tipo " + request.tipo()
                + ". Cancele o suspenda la póliza existente antes de contratar una nueva.", ex);
        }
    }

    /**
     * Recherche une police par son identifiant unique.
     */
    public PolizaResponse getPolizaById(Long id) {
        return polizaRepository.findById(id)
            .map(PolizaMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada con id: " + id));
    }

    /**
     * Recherche une police par son numéro de contrat.
     */
    public PolizaResponse getPolizaByNumero(String numero) {
        return polizaRepository.findByNumero(numero)
            .map(PolizaMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada con número: " + numero));
    }

    /**
     * Liste les polices rattachées à un client.
     */
    public List<PolizaResponse> getPolizasByClienteId(Long clienteId) {
        // Vérifie d'abord que le client existe pour lever 404 si besoin
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId);
        }
        return polizaRepository.findByClienteId(clienteId).stream()
            .map(PolizaMapper::toResponse)
            .toList();
    }

    /**
     * Effectue une transition d'état contrôlée sur une police selon la matrice de la section 4.2.
     */
    @Transactional
    public PolizaResponse updateEstadoPoliza(Long id, PolizaEstadoUpdateRequest request) {
        Poliza poliza = polizaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada con id: " + id));

        EstadoPoliza estadoActual = poliza.getEstado();
        EstadoPoliza nuevoEstado = request.nuevoEstado();

        if (estadoActual == nuevoEstado) {
            return PolizaMapper.toResponse(poliza);
        }

        // Matrice de transitions :
        // ACTIVA -> SUSPENDIDA, CANCELADA
        // SUSPENDIDA -> ACTIVA, CANCELADA
        // CANCELADA -> Terminal (aucune transition autorisée)
        switch (estadoActual) {
            case CANCELADA -> throw new BusinessRuleException("No se puede cambiar el estado de una póliza CANCELADA (estado terminal)");
            case ACTIVA -> {
                if (nuevoEstado != EstadoPoliza.SUSPENDIDA && nuevoEstado != EstadoPoliza.CANCELADA) {
                    throw new BusinessRuleException("Transición no permitida: de " + estadoActual + " a " + nuevoEstado);
                }
            }
            case SUSPENDIDA -> {
                if (nuevoEstado == EstadoPoliza.ACTIVA) {
                    // Revérifier qu'aucune autre police active du même type n'a été créée entre temps
                    if (polizaRepository.existsByClienteIdAndTipoAndEstado(poliza.getCliente().getId(), poliza.getTipo(), EstadoPoliza.ACTIVA)) {
                        throw new BusinessConflictException("El cliente ya tiene otra póliza ACTIVA de tipo " + poliza.getTipo());
                    }
                } else if (nuevoEstado != EstadoPoliza.CANCELADA) {
                    throw new BusinessRuleException("Transición no permitida: de " + estadoActual + " a " + nuevoEstado);
                }
            }
        }

        poliza.setEstado(nuevoEstado);
        try {
            Poliza updated = polizaRepository.saveAndFlush(poliza);
            return PolizaMapper.toResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException("El cliente ya tiene una póliza ACTIVA de tipo " + poliza.getTipo(), ex);
        }
    }
}
