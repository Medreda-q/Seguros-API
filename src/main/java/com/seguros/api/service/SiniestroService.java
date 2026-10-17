package com.seguros.api.service;

import com.seguros.api.dto.mapper.SiniestroMapper;
import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroEstadoUpdateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.Siniestro;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.EstadoSiniestro;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.repository.PolizaRepository;
import com.seguros.api.repository.SiniestroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Service métier pour la déclaration et le cycle de vie des sinistres.
 *
 * Applique strictement l'ordre d'évaluation des règles de la section 4.5 du document fonctionnel :
 * 1. Existence de la police (404)
 * 2. État ACTIVA de la police (409)
 * 3. Police non expirée à la date courante (409)
 * 4. Date d'occurrence dans la période de validité [fechaInicio, fechaFin] (400)
 * 5. Date d'occurrence non future (400)
 *
 * Utilise la séquence PostgreSQL 'seq_siniestro_numero' et applique la machine à états de la section 4.3.
 */
@Service
@Transactional(readOnly = true)
public class SiniestroService {

    private final SiniestroRepository siniestroRepository;
    private final PolizaRepository polizaRepository;
    private final Clock clock;

    public SiniestroService(SiniestroRepository siniestroRepository,
                            PolizaRepository polizaRepository,
                            Clock clock) {
        this.siniestroRepository = siniestroRepository;
        this.polizaRepository = polizaRepository;
        this.clock = clock;
    }

    /**
     * Déclare un nouveau sinistre en respectant l'ordre chronologique et logique d'évaluation des règles.
     */
    @Transactional
    public SiniestroResponse createSiniestro(SiniestroCreateRequest request) {
        LocalDate hoy = LocalDate.now(clock);

        // Étape 1 : La police existe en base de données -> Sinon 404 Not Found
        Poliza poliza = polizaRepository.findById(request.polizaId())
            .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada con id: " + request.polizaId()));

        // Étape 2 : La police est à l'état ACTIVA -> Sinon 409 Conflict
        if (poliza.getEstado() == EstadoPoliza.SUSPENDIDA) {
            throw new BusinessConflictException("No se puede declarar un siniestro sobre una póliza SUSPENDIDA. Es necesario regularizar el pago primero.");
        } else if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessConflictException("No se puede declarar un siniestro sobre una póliza CANCELADA.");
        } else if (poliza.getEstado() != EstadoPoliza.ACTIVA) {
            throw new BusinessConflictException("Solo se pueden registrar siniestros en pólizas activas (estado actual: " + poliza.getEstado() + ")");
        }

        // Étape 3 : La police n'est pas expirée (fechaFin >= hoy) -> Sinon 409 Conflict
        if (poliza.getFechaFin().isBefore(hoy)) {
            throw new BusinessConflictException("No se puede declarar un siniestro sobre una póliza expirada (vigencia finalizada el "
                + poliza.getFechaFin() + ").");
        }

        // Étape 4 : fechaOcurrencia est comprise dans [fechaInicio, fechaFin] -> Sinon 400 Bad Request
        if (request.fechaOcurrencia().isBefore(poliza.getFechaInicio()) || request.fechaOcurrencia().isAfter(poliza.getFechaFin())) {
            throw new BusinessRuleException("La fecha de ocurrencia (" + request.fechaOcurrencia()
                + ") no está dentro del período de vigencia de la póliza (" + poliza.getFechaInicio() + " – " + poliza.getFechaFin() + ").");
        }

        // Étape 5 : fechaOcurrencia <= hoy (pas de sinistre futur) -> Sinon 400 Bad Request
        if (request.fechaOcurrencia().isAfter(hoy)) {
            throw new BusinessRuleException("La fecha de ocurrencia no puede ser una fecha futura.");
        }

        // Génération du numéro via séquence PostgreSQL native
        Long seqVal = siniestroRepository.getNextNumeroSequence();
        String numero = String.format("SIN-%d-%04d", hoy.getYear(), seqVal);

        Siniestro siniestro = SiniestroMapper.toEntity(request, poliza, numero);
        Siniestro saved = siniestroRepository.save(siniestro);
        return SiniestroMapper.toResponse(saved);
    }

    /**
     * Recherche un sinistre par ID.
     */
    public SiniestroResponse getSiniestroById(Long id) {
        return siniestroRepository.findById(id)
            .map(SiniestroMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Siniestro no encontrado con id: " + id));
    }

    /**
     * Recherche un sinistre par numéro unique.
     */
    public SiniestroResponse getSiniestroByNumero(String numero) {
        return siniestroRepository.findByNumero(numero)
            .map(SiniestroMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Siniestro no encontrado con número: " + numero));
    }

    /**
     * Récupère tous les sinistres d'une police donnée.
     */
    public List<SiniestroResponse> getSiniestrosByPolizaId(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw new ResourceNotFoundException("Póliza no encontrada con id: " + polizaId);
        }
        return siniestroRepository.findByPolizaId(polizaId).stream()
            .map(SiniestroMapper::toResponse)
            .toList();
    }

    /**
     * Fait avancer le cycle de vie du sinistre selon la machine à états de la section 4.3 :
     * ABIERTO -> EN_REVISION -> APROBADO / RECHAZADO (terminaux).
     */
    @Transactional
    public SiniestroResponse updateEstadoSiniestro(Long id, SiniestroEstadoUpdateRequest request) {
        Siniestro siniestro = siniestroRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Siniestro no encontrado con id: " + id));

        EstadoSiniestro actual = siniestro.getEstado();
        EstadoSiniestro nuevo = request.nuevoEstado();

        if (actual == nuevo) {
            return SiniestroMapper.toResponse(siniestro);
        }

        // Vérification de la machine à états
        switch (actual) {
            case APROBADO, RECHAZADO -> throw new BusinessRuleException("No se puede modificar un siniestro en estado terminal: " + actual);
            case ABIERTO -> {
                if (nuevo != EstadoSiniestro.EN_REVISION) {
                    throw new BusinessRuleException("Desde el estado ABIERTO solo se puede pasar a EN_REVISION");
                }
            }
            case EN_REVISION -> {
                if (nuevo != EstadoSiniestro.APROBADO && nuevo != EstadoSiniestro.RECHAZADO) {
                    throw new BusinessRuleException("Desde el estado EN_REVISION solo se puede pasar a APROBADO o RECHAZADO");
                }
            }
        }

        siniestro.setEstado(nuevo);
        Siniestro updated = siniestroRepository.save(siniestro);
        return SiniestroMapper.toResponse(updated);
    }
}
