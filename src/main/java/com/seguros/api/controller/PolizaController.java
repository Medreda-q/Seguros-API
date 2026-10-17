package com.seguros.api.controller;

import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaEstadoUpdateRequest;
import com.seguros.api.dto.poliza.PolizaResponse;
import com.seguros.api.service.PolizaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion et l'émission des polices d'assurance (/api/v1/polizas).
 */
@RestController
@RequestMapping("/api/v1/polizas")
@Tag(name = "Pólizas", description = "Operaciones de emisión, consulta y control de ciclo de vida de pólizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emisión de una nueva póliza de seguros", description = "Calcula la prima actuarial y genera el identificador POL-{TIPO}-{ANIO}-{0001}")
    public PolizaResponse createPoliza(@Valid @RequestBody PolizaCreateRequest request) {
        return polizaService.createPoliza(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una póliza por ID", description = "Consulta la póliza por su identificador primario numérico")
    public PolizaResponse getPolizaById(@PathVariable Long id) {
        return polizaService.getPolizaById(id);
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Detalle de una póliza por su número legible", description = "Consulta la póliza por su identificador de póliza (ej: POL-AUTO-2026-0001)")
    public PolizaResponse getPolizaByNumero(@PathVariable String numero) {
        return polizaService.getPolizaByNumero(numero);
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listado de pólizas de un cliente", description = "Recupera todas las pólizas contratadas por un cliente específico (404 si el cliente no existe)")
    public List<PolizaResponse> getPolizasByClienteId(@PathVariable Long clienteId) {
        return polizaService.getPolizasByClienteId(clienteId);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambio de estado de una póliza", description = "Transición controlada entre ACTIVA, SUSPENDIDA y CANCELADA")
    public PolizaResponse updateEstadoPoliza(
        @PathVariable Long id,
        @Valid @RequestBody PolizaEstadoUpdateRequest request
    ) {
        return polizaService.updateEstadoPoliza(id, request);
    }
}
