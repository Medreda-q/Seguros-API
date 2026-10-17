package com.seguros.api.controller;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des clients assurés (/api/v1/clientes).
 * Délégué entièrement au service sans aucune logique métier dans la couche de transport.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Operaciones de gestión y consulta de clientes asegurados")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de un nuevo cliente asegurado", description = "Valida DNI/NIE módulo 23 único, IBAN español y mayoría de edad")
    public ClienteResponse createCliente(@Valid @RequestBody ClienteCreateRequest request) {
        return clienteService.createCliente(request);
    }

    @GetMapping
    @Operation(summary = "Listado paginado de clientes", description = "Devuelve el listado paginado ordenado por fecha de creación descendente")
    public Page<ClienteResponse> getClientes(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return clienteService.getClientes(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un cliente por ID", description = "Devuelve la ficha completa del cliente con DNI/NIE e IBAN enmascarados")
    public ClienteResponse getClienteById(@PathVariable Long id) {
        return clienteService.getClienteById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificación de datos de contacto del cliente", description = "Actualiza nombre, apellidos, teléfono, email o IBAN")
    public ClienteResponse updateCliente(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        return clienteService.updateCliente(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja de un cliente", description = "Elimina el cliente siempre que no posea pólizas en estado ACTIVA")
    public void deleteCliente(@PathVariable Long id) {
        clienteService.deleteCliente(id);
    }
}
