package com.seguros.api.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires de PolizaService (Mockito)")
class PolizaServiceTest {

    @Mock
    private PolizaRepository polizaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CalculadoraPrimaService calculadoraPrimaService;

    // Horloge figée au 15 juin 2026 10:00:00 Europe/Madrid
    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-06-15T10:00:00Z"),
        ZoneId.of("Europe/Madrid")
    );
    private final LocalDate hoy = LocalDate.of(2026, 6, 15);

    private PolizaService polizaService;

    private Cliente clienteMock;

    @BeforeEach
    void setUp() {
        polizaService = new PolizaService(
            polizaRepository,
            clienteRepository,
            calculadoraPrimaService,
            fixedClock
        );

        clienteMock = new Cliente(
            "12345678Z",
            "Carlos",
            "García Lorca",
            "carlos@example.es",
            "+34612345678",
            LocalDate.of(1990, 5, 20),
            "ES1234567890123456789012"
        );
        clienteMock.setId(10L);
    }

    @Nested
    @DisplayName("Tests de création d'une police (createPoliza)")
    class CreatePolizaTests {

        @Test
        @DisplayName("Succès : émet la police, calcule la prime et génère le numéro séquentiel avec padding à 4 chiffres")
        void shouldCreatePolizaSuccessfully() {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                hoy,
                hoy.plusYears(1)
            );

            when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
            when(polizaRepository.existsByClienteIdAndTipoAndEstado(10L, TipoPoliza.AUTO, EstadoPoliza.ACTIVA))
                .thenReturn(false);
            when(calculadoraPrimaService.calcularPrima(TipoPoliza.AUTO, clienteMock.getFechaNacimiento(), hoy))
                .thenReturn(new BigDecimal("500.00"));
            when(polizaRepository.getNextNumeroSequence()).thenReturn(7L);

            when(polizaRepository.saveAndFlush(any(Poliza.class))).thenAnswer(invocation -> {
                Poliza p = invocation.getArgument(0);
                p.setId(100L);
                return p;
            });

            PolizaResponse response = polizaService.createPoliza(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.numero()).isEqualTo("POL-AUTO-2026-0007"); // Padding 4 chiffres
            assertThat(response.tipo()).isEqualTo(TipoPoliza.AUTO);
            assertThat(response.estado()).isEqualTo(EstadoPoliza.ACTIVA);
            assertThat(response.primaAnual()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(response.clienteId()).isEqualTo(10L);

            ArgumentCaptor<Poliza> captor = ArgumentCaptor.forClass(Poliza.class);
            verify(polizaRepository).saveAndFlush(captor.capture());
            Poliza savedPoliza = captor.getValue();
            assertThat(savedPoliza.getNumero()).isEqualTo("POL-AUTO-2026-0007");
            assertThat(savedPoliza.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        }

        @Test
        @DisplayName("Échec 404 : Client introuvable")
        void shouldThrowNotFoundWhenClienteDoesNotExist() {
            PolizaCreateRequest request = new PolizaCreateRequest(
                99L,
                TipoPoliza.HOGAR,
                hoy,
                hoy.plusYears(1)
            );

            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> polizaService.createPoliza(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: 99");

            verify(polizaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Échec 400 (Règle 4.4.2) : fechaInicio dans le passé par rapport à LocalDate.now(clock)")
        void shouldThrowBadRequestWhenFechaInicioIsInThePast() {
            LocalDate fechaPasada = hoy.minusDays(1);
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                fechaPasada,
                hoy.plusYears(1)
            );

            when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

            assertThatThrownBy(() -> polizaService.createPoliza(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("La fecha de inicio de la póliza no puede ser anterior a la fecha de hoy");

            verify(polizaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Échec 400 : fechaFin <= fechaInicio")
        void shouldThrowBadRequestWhenFechaFinIsNotAfterFechaInicio() {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                hoy,
                hoy // Même date
            );

            when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

            assertThatThrownBy(() -> polizaService.createPoliza(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("La fecha de fin debe ser estrictamente posterior a la fecha de inicio");
        }

        @Test
        @DisplayName("Échec 409 (Règle 4.4.1) : Le client a déjà une police ACTIVA du même type (détecté par le service)")
        void shouldThrowConflictWhenActivePolizaAlreadyExistsInServiceCheck() {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                hoy,
                hoy.plusYears(1)
            );

            when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
            when(polizaRepository.existsByClienteIdAndTipoAndEstado(10L, TipoPoliza.AUTO, EstadoPoliza.ACTIVA))
                .thenReturn(true);

            assertThatThrownBy(() -> polizaService.createPoliza(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("El cliente ya tiene una póliza ACTIVA de tipo AUTO");

            verify(polizaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Échec 409 (Règle 4.4.1 course critique) : catch DataIntegrityViolationException lors du saveAndFlush -> 409")
        void shouldCatchDataIntegrityViolationExceptionAndThrowConflict() {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                hoy,
                hoy.plusYears(1)
            );

            when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
            when(polizaRepository.existsByClienteIdAndTipoAndEstado(10L, TipoPoliza.AUTO, EstadoPoliza.ACTIVA))
                .thenReturn(false); // Pas de conflit vu lors du check initial (race condition)
            when(calculadoraPrimaService.calcularPrima(any(), any(), any()))
                .thenReturn(new BigDecimal("500.00"));
            when(polizaRepository.getNextNumeroSequence()).thenReturn(1L);

            // Simule l'index partiel PostgreSQL uq_polizas_activa_cliente_tipo qui rejette la concurrence
            when(polizaRepository.saveAndFlush(any(Poliza.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates unique constraint uq_polizas_activa_cliente_tipo"));

            assertThatThrownBy(() -> polizaService.createPoliza(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("El cliente ya tiene una póliza ACTIVA de tipo AUTO");
        }
    }

    @Nested
    @DisplayName("Tests de transition d'état d'une police (updateEstadoPoliza)")
    class UpdateEstadoTests {

        private Poliza polizaActiva;

        @BeforeEach
        void initPoliza() {
            polizaActiva = new Poliza(
                "POL-AUTO-2026-0001",
                TipoPoliza.AUTO,
                EstadoPoliza.ACTIVA,
                new BigDecimal("500.00"),
                hoy,
                hoy.plusYears(1),
                clienteMock
            );
            polizaActiva.setId(1L);
        }

        @Test
        @DisplayName("Succès : ACTIVA vers SUSPENDIDA")
        void shouldTransitionFromActivaToSuspendida() {
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaActiva));
            when(polizaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

            PolizaResponse res = polizaService.updateEstadoPoliza(1L, new PolizaEstadoUpdateRequest(EstadoPoliza.SUSPENDIDA));

            assertThat(res.estado()).isEqualTo(EstadoPoliza.SUSPENDIDA);
        }

        @Test
        @DisplayName("Succès : ACTIVA vers CANCELADA")
        void shouldTransitionFromActivaToCancelada() {
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaActiva));
            when(polizaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

            PolizaResponse res = polizaService.updateEstadoPoliza(1L, new PolizaEstadoUpdateRequest(EstadoPoliza.CANCELADA));

            assertThat(res.estado()).isEqualTo(EstadoPoliza.CANCELADA);
        }

        @Test
        @DisplayName("Succès : SUSPENDIDA vers ACTIVA si aucune autre police active du même type")
        void shouldTransitionFromSuspendidaToActivaWhenNoCollision() {
            polizaActiva.setEstado(EstadoPoliza.SUSPENDIDA);
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaActiva));
            when(polizaRepository.existsByClienteIdAndTipoAndEstado(10L, TipoPoliza.AUTO, EstadoPoliza.ACTIVA)).thenReturn(false);
            when(polizaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

            PolizaResponse res = polizaService.updateEstadoPoliza(1L, new PolizaEstadoUpdateRequest(EstadoPoliza.ACTIVA));

            assertThat(res.estado()).isEqualTo(EstadoPoliza.ACTIVA);
        }

        @Test
        @DisplayName("Échec 409 : SUSPENDIDA vers ACTIVA refusé si une autre police active existe")
        void shouldRejectTransitionToActivaIfAnotherActivePolizaExists() {
            polizaActiva.setEstado(EstadoPoliza.SUSPENDIDA);
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaActiva));
            when(polizaRepository.existsByClienteIdAndTipoAndEstado(10L, TipoPoliza.AUTO, EstadoPoliza.ACTIVA)).thenReturn(true);

            assertThatThrownBy(() -> polizaService.updateEstadoPoliza(1L, new PolizaEstadoUpdateRequest(EstadoPoliza.ACTIVA)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("El cliente ya tiene otra póliza ACTIVA de tipo AUTO");
        }

        @Test
        @DisplayName("Échec 400 : CANCELADA est un état terminal (aucune transition permise)")
        void shouldRejectAnyTransitionFromCancelada() {
            polizaActiva.setEstado(EstadoPoliza.CANCELADA);
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaActiva));

            assertThatThrownBy(() -> polizaService.updateEstadoPoliza(1L, new PolizaEstadoUpdateRequest(EstadoPoliza.ACTIVA)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se puede cambiar el estado de una póliza CANCELADA");
        }
    }

    @Nested
    @DisplayName("Tests de consultation (getPolizaById, getPolizasByClienteId)")
    class RetrievalTests {

        @Test
        @DisplayName("getPolizaById : succès")
        void shouldReturnPolizaById() {
            Poliza p = new Poliza("POL-AUTO-2026-0001", TipoPoliza.AUTO, EstadoPoliza.ACTIVA, new BigDecimal("500.00"), hoy, hoy.plusYears(1), clienteMock);
            p.setId(1L);
            when(polizaRepository.findById(1L)).thenReturn(Optional.of(p));

            PolizaResponse res = polizaService.getPolizaById(1L);

            assertThat(res).isNotNull();
            assertThat(res.numero()).isEqualTo("POL-AUTO-2026-0001");
        }

        @Test
        @DisplayName("getPolizaById : 404 si introuvable")
        void shouldThrowNotFoundWhenPolizaDoesNotExist() {
            when(polizaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> polizaService.getPolizaById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Póliza no encontrada con id: 99");
        }

        @Test
        @DisplayName("getPolizasByClienteId : succès")
        void shouldReturnPolizasByClienteId() {
            Poliza p = new Poliza("POL-AUTO-2026-0001", TipoPoliza.AUTO, EstadoPoliza.ACTIVA, new BigDecimal("500.00"), hoy, hoy.plusYears(1), clienteMock);
            when(clienteRepository.existsById(10L)).thenReturn(true);
            when(polizaRepository.findByClienteId(10L)).thenReturn(List.of(p));

            List<PolizaResponse> result = polizaService.getPolizasByClienteId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).numero()).isEqualTo("POL-AUTO-2026-0001");
        }

        @Test
        @DisplayName("getPolizasByClienteId : 404 si client inexistant")
        void shouldThrowNotFoundWhenQueryingPolizasForNonExistentCliente() {
            when(clienteRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> polizaService.getPolizasByClienteId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: 99");
        }
    }
}
