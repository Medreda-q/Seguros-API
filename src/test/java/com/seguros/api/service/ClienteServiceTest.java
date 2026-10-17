package com.seguros.api.service;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires de ClienteService (Mockito)")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    // Horloge figée au 15 juin 2026 10:00:00 Europe/Madrid
    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-06-15T10:00:00Z"),
        ZoneId.of("Europe/Madrid")
    );
    private final LocalDate hoy = LocalDate.of(2026, 6, 15);

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository, fixedClock);
    }

    @Nested
    @DisplayName("Tests de création d'un client (createCliente)")
    class CreateClienteTests {

        @Test
        @DisplayName("Succès : enregistre un nouveau client adulte")
        void shouldCreateClienteSuccessfully() {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Laura",
                "Martínez",
                "laura@example.es",
                "+34612345678",
                LocalDate.of(1995, 6, 10), // 31 ans
                "ES1234567890123456789012"
            );

            when(clienteRepository.existsByDniNie("12345678Z")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
                Cliente c = invocation.getArgument(0);
                c.setId(1L);
                return c;
            });

            ClienteResponse res = clienteService.createCliente(request);

            assertThat(res).isNotNull();
            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.nombre()).isEqualTo("Laura");
            assertThat(res.dniNieEnmascarado()).isEqualTo("*****678Z");
            assertThat(res.ibanEnmascarado()).isEqualTo("ES12 **** **** 9012");
        }

        @Test
        @DisplayName("Échec 400 : Client mineur (< 18 ans)")
        void shouldRejectUnderageClient() {
            // Né le 2008-06-16 -> aura 18 ans demain, a encore 17 ans aujourd'hui (2026-06-15)
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Menor",
                "Gómez",
                "menor@example.es",
                "+34612345678",
                LocalDate.of(2008, 6, 16),
                "ES1234567890123456789012"
            );

            assertThatThrownBy(() -> clienteService.createCliente(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("El cliente debe tener al menos 18 años para darse de alta");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Échec 409 : DNI/NIE déjà existant (vérification applicative)")
        void shouldThrowConflictWhenDniAlreadyExists() {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Duplicado",
                "Gómez",
                "dup@example.es",
                "+34612345678",
                LocalDate.of(1990, 1, 1),
                "ES1234567890123456789012"
            );

            when(clienteRepository.existsByDniNie("12345678Z")).thenReturn(true);

            assertThatThrownBy(() -> clienteService.createCliente(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Ya existe un cliente registrado con el DNI/NIE 12345678Z");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Échec 409 : DNI/NIE collision concurrente (catch DataIntegrityViolationException)")
        void shouldCatchDataIntegrityViolationOnSave() {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Concurrente",
                "Gómez",
                "conc@example.es",
                "+34612345678",
                LocalDate.of(1990, 1, 1),
                "ES1234567890123456789012"
            );

            when(clienteRepository.existsByDniNie("12345678Z")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates unique constraint uq_clientes_dni_nie"));

            assertThatThrownBy(() -> clienteService.createCliente(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("Ya existe un cliente registrado con el DNI/NIE 12345678Z");
        }
    }

    @Nested
    @DisplayName("Tests de suppression d'un client (deleteCliente)")
    class DeleteClienteTests {

        @Test
        @DisplayName("Échec 409 : Suppression refusée si le client a au moins une police ACTIVA")
        void shouldRejectDeletionWhenClientHasActivePolizas() {
            Cliente cliente = new Cliente("12345678Z", "Juan", "Pérez", "juan@test.es", "+34600112233", LocalDate.of(1985, 2, 2), "ES1234567890123456789012");
            cliente.setId(5L);

            Poliza polizaActiva = new Poliza("POL-AUTO-2026-0001", TipoPoliza.AUTO, EstadoPoliza.ACTIVA, new BigDecimal("500.00"), hoy, hoy.plusYears(1), cliente);
            cliente.getPolizas().add(polizaActiva);

            when(clienteRepository.findWithPolizasById(5L)).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> clienteService.deleteCliente(5L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("No se puede eliminar el cliente porque tiene pólizas activas asociadas");

            verify(clienteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Succès : Suppression autorisée si le client n'a que des polices CANCELADA ou aucune")
        void shouldAllowDeletionWhenClientHasNoActivePolizas() {
            Cliente cliente = new Cliente("12345678Z", "Juan", "Pérez", "juan@test.es", "+34600112233", LocalDate.of(1985, 2, 2), "ES1234567890123456789012");
            cliente.setId(5L);

            Poliza polizaCancelada = new Poliza("POL-AUTO-2026-0001", TipoPoliza.AUTO, EstadoPoliza.CANCELADA, new BigDecimal("500.00"), hoy.minusYears(1), hoy, cliente);
            cliente.getPolizas().add(polizaCancelada);

            when(clienteRepository.findWithPolizasById(5L)).thenReturn(Optional.of(cliente));

            clienteService.deleteCliente(5L);

            verify(clienteRepository).delete(cliente);
        }

        @Test
        @DisplayName("Échec 404 : Client introuvable pour la suppression")
        void shouldThrowNotFoundOnDeleteWhenClienteDoesNotExist() {
            when(clienteRepository.findWithPolizasById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.deleteCliente(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: 99");
        }
    }

    @Nested
    @DisplayName("Tests de consultation et modification")
    class GetAndUpdateTests {

        @Test
        @DisplayName("getClienteById : succès")
        void shouldReturnClienteById() {
            Cliente cliente = new Cliente("12345678Z", "Ana", "López", "ana@test.es", "+34600112233", LocalDate.of(1990, 5, 5), "ES1234567890123456789012");
            cliente.setId(1L);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            ClienteResponse res = clienteService.getClienteById(1L);

            assertThat(res).isNotNull();
            assertThat(res.id()).isEqualTo(1L);
            assertThat(res.nombre()).isEqualTo("Ana");
        }

        @Test
        @DisplayName("getClienteById : 404 si introuvable")
        void shouldThrowNotFoundWhenClienteDoesNotExist() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.getClienteById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: 99");
        }

        @Test
        @DisplayName("updateCliente : succès")
        void shouldUpdateClienteSuccessfully() {
            Cliente cliente = new Cliente("12345678Z", "Ana", "López", "ana@test.es", "+34600112233", LocalDate.of(1990, 5, 5), "ES1234567890123456789012");
            cliente.setId(1L);

            ClienteUpdateRequest updateReq = new ClienteUpdateRequest(
                "Ana María",
                "López Gil",
                "anamaria@test.es",
                "+34699887766",
                "ES9876543210987654321098"
            );

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

            ClienteResponse res = clienteService.updateCliente(1L, updateReq);

            assertThat(res.nombre()).isEqualTo("Ana María");
            assertThat(res.email()).isEqualTo("anamaria@test.es");
        }
    }
}
