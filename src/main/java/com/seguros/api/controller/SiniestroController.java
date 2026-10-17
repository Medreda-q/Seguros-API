package com.seguros.api.controller;

import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroEstadoUpdateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la déclaration et le cycle de vie des sinistres (/api/v1/siniestros).
 */
@RestController
@RequestMapping("/api/v1/siniestros")
@Tag(name = "Siniestros", description = "Operaciones de declaración, seguimiento y tramitación de siniestros")
public class SiniestroController {

    private final SiniestroService siniestroService;

    public SiniestroController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Declaración de un nuevo siniestro", description = "Valida las 5 reglas de negocio de la póliza y genera expediente SIN-{ANIO}-{0001}")
    public SiniestroResponse createSiniestro(@Valid @RequestBody SiniestroCreateRequest request) {
        return siniestroService.createSiniestro(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un siniestro por ID", description = "Consulta un siniestro por su identificador primario numérico")
    public SiniestroResponse getSiniestroById(@PathVariable Long id) {
        return siniestroService.getSiniestroById(id);
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Detalle de un siniestro por número de expediente", description = "Consulta un siniestro por su identificador único (ej: SIN-2026-0001)")
    public SiniestroResponse getSiniestroByNumero(@PathVariable String numero) {
        return siniestroService.getSiniestroByNumero(numero);
    }

    @GetMapping("/poliza/{polizaId}")
    @Operation(summary = "Listado de siniestros de una póliza", description = "Recupera todos los expedientes de siniestros de una póliza (404 si la póliza no existe)")
    public List<SiniestroResponse> getSiniestrosByPolizaId(@PathVariable Long polizaId) {
        return siniestroService.getSiniestrosByPolizaId(polizaId);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Avance de estado de un siniestro", description = "Flujo unidireccional: ABIERTO -> EN_REVISION -> APROBADO / RECHAZADO")
    public SiniestroResponse updateEstadoSiniestro(
        @PathVariable Long id,
        @Valid @RequestBody SiniestroEstadoUpdateRequest request
    ) {
        return siniestroService.updateEstadoSiniestro(id, request);
    }
}
