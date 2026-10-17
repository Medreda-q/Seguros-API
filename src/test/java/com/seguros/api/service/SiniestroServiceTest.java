package com.seguros.api.service;

import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroEstadoUpdateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.Siniestro;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.EstadoSiniestro;
import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.repository.PolizaRepository;
import com.seguros.api.repository.SiniestroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires de SiniestroService (Mockito)")
class SiniestroServiceTest {

    @Mock
    private SiniestroRepository siniestroRepository;

    @Mock
    private PolizaRepository polizaRepository;

    // Horloge figée au 15 juin 2026 10:00:00 Europe/Madrid
    private final Clock fixedClock = Clock.fixed(
        Instant.parse("2026-06-15T10:00:00Z"),
        ZoneId.of("Europe/Madrid")
    );
    private final LocalDate hoy = LocalDate.of(2026, 6, 15);

    private SiniestroService siniestroService;

    private Poliza polizaMock;

    @BeforeEach
    void setUp() {
        siniestroService = new SiniestroService(
            siniestroRepository,
            polizaRepository,
            fixedClock
        );

        Cliente cliente = new Cliente(
            "12345678Z",
            "Elena",
            "Naranjo",
            "elena@example.es",
            "+34611223344",
            LocalDate.of(1988, 3, 10),
            "ES1234567890123456789012"
        );
        cliente.setId(10L);

        polizaMock = new Poliza(
            "POL-AUTO-2026-0001",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("500.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            cliente
        );
        polizaMock.setId(1L);
    }

    @Nested
    @DisplayName("Ordre strict des vérifications pour la déclaration de sinistre (Règle 4.5)")
    class DeclaracionSiniestroStepByStepTests {

        @Test
        @DisplayName("Étape 1 : Police introuvable -> 404 Not Found")
        void step1_shouldThrowNotFoundWhenPolizaDoesNotExist() {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                99L,
                "Colisión trasera en cruce semafórico",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1200.00")
            );

            when(polizaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Póliza no encontrada con id: 99");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 2a : Police SUSPENDIDA -> 409 Conflict avec message de paiement")
        void step2_shouldThrowConflictWhenPolizaIsSuspendida() {
            polizaMock.setEstado(EstadoPoliza.SUSPENDIDA);
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Golpe en aparcamiento comunitario",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("600.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("No se puede declarar un siniestro sobre una póliza SUSPENDIDA. Es necesario regularizar el pago primero.");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 2b : Police CANCELADA -> 409 Conflict")
        void step2_shouldThrowConflictWhenPolizaIsCancelada() {
            polizaMock.setEstado(EstadoPoliza.CANCELADA);
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Rotura de luna delantera en autovía",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("350.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("No se puede declarar un siniestro sobre una póliza CANCELADA.");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 3 : Police expirée (fechaFin < hoy) -> 409 Conflict")
        void step3_shouldThrowConflictWhenPolizaIsExpired() {
            // Police active mais expirée le 1er juin 2026 (aujourd'hui = 15 juin 2026)
            polizaMock.setFechaFin(LocalDate.of(2026, 6, 1));
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Avería de motor",
                LocalDate.of(2026, 5, 20),
                new BigDecimal("1500.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("No se puede declarar un siniestro sobre una póliza expirada (vigencia finalizada el 2026-06-01).");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 4a : fechaOcurrencia antérieure à fechaInicio de la police -> 400 Bad Request")
        void step4_shouldThrowBadRequestWhenFechaOcurrenciaBeforeFechaInicio() {
            // fechaInicio = 2026-01-01, occurrence = 2025-12-31
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Impacto con columna de garaje",
                LocalDate.of(2025, 12, 31),
                new BigDecimal("800.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está dentro del período de vigencia de la póliza");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 4b : fechaOcurrencia postérieure à fechaFin de la police -> 400 Bad Request")
        void step4_shouldThrowBadRequestWhenFechaOcurrenciaAfterFechaFin() {
            // Police jusqu'au 2026-12-31 mais occurrence demandée en 2027
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Daños por granizo",
                LocalDate.of(2027, 1, 15),
                new BigDecimal("900.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está dentro del período de vigencia de la póliza");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Étape 5 : fechaOcurrencia dans le futur par rapport à hoy -> 400 Bad Request")
        void step5_shouldThrowBadRequestWhenFechaOcurrenciaIsInTheFuture() {
            // aujourd'hui = 2026-06-15, occurrence = 2026-06-16 (dans la vigencia mais dans le futur !)
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Accidente simulado en fecha futura",
                LocalDate.of(2026, 6, 16),
                new BigDecimal("400.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));

            assertThatThrownBy(() -> siniestroService.createSiniestro(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("La fecha de ocurrencia no puede ser una fecha futura.");

            verify(siniestroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cas nominal : Toutes les vérifications passent -> création avec SIN-2026-0001 et état ABIERTO")
        void shouldCreateSiniestroSuccessfullyWhenAllValidationsPass() {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                1L,
                "Golpe lateral en rotonda con daños materiales",
                LocalDate.of(2026, 6, 10),
                new BigDecimal("750.00")
            );

            when(polizaRepository.findById(1L)).thenReturn(Optional.of(polizaMock));
            when(siniestroRepository.getNextNumeroSequence()).thenReturn(1L);
            when(siniestroRepository.save(any(Siniestro.class))).thenAnswer(invocation -> {
                Siniestro s = invocation.getArgument(0);
                s.setId(50L);
                return s;
            });

            SiniestroResponse response = siniestroService.createSiniestro(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(50L);
            assertThat(response.numero()).isEqualTo("SIN-2026-0001");
            assertThat(response.estado()).isEqualTo(EstadoSiniestro.ABIERTO);
            assertThat(response.polizaId()).isEqualTo(1L);
            assertThat(response.polizaNumero()).isEqualTo("POL-AUTO-2026-0001");
            assertThat(response.importeReclamado()).isEqualByComparingTo(new BigDecimal("750.00"));

            ArgumentCaptor<Siniestro> captor = ArgumentCaptor.forClass(Siniestro.class);
            verify(siniestroRepository).save(captor.capture());
            assertThat(captor.getValue().getNumero()).isEqualTo("SIN-2026-0001");
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSiniestro.ABIERTO);
        }
    }

    @Nested
    @DisplayName("Machine à états des sinistres (Section 4.3)")
    class StateMachineTests {

        private Siniestro siniestroMock;

        @BeforeEach
        void initSiniestro() {
            siniestroMock = new Siniestro(
                "SIN-2026-0001",
                polizaMock,
                "Daños materiales en carrocería",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000.00"),
                EstadoSiniestro.ABIERTO
            );
            siniestroMock.setId(10L);
        }

        @Test
        @DisplayName("Transition ABIERTO vers EN_REVISION : succès")
        void shouldTransitionFromAbiertoToEnRevision() {
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(siniestroMock));
            when(siniestroRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SiniestroResponse res = siniestroService.updateEstadoSiniestro(10L, new SiniestroEstadoUpdateRequest(EstadoSiniestro.EN_REVISION));

            assertThat(res.estado()).isEqualTo(EstadoSiniestro.EN_REVISION);
        }

        @Test
        @DisplayName("Transition EN_REVISION vers APROBADO : succès")
        void shouldTransitionFromEnRevisionToAprobado() {
            siniestroMock.setEstado(EstadoSiniestro.EN_REVISION);
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(siniestroMock));
            when(siniestroRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SiniestroResponse res = siniestroService.updateEstadoSiniestro(10L, new SiniestroEstadoUpdateRequest(EstadoSiniestro.APROBADO));

            assertThat(res.estado()).isEqualTo(EstadoSiniestro.APROBADO);
        }

        @Test
        @DisplayName("Transition EN_REVISION vers RECHAZADO : succès")
        void shouldTransitionFromEnRevisionToRechazado() {
            siniestroMock.setEstado(EstadoSiniestro.EN_REVISION);
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(siniestroMock));
            when(siniestroRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SiniestroResponse res = siniestroService.updateEstadoSiniestro(10L, new SiniestroEstadoUpdateRequest(EstadoSiniestro.RECHAZADO));

            assertThat(res.estado()).isEqualTo(EstadoSiniestro.RECHAZADO);
        }

        @Test
        @DisplayName("Rejet : saut direct de ABIERTO vers APROBADO interdit")
        void shouldRejectTransitionDirectlyFromAbiertoToAprobado() {
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(siniestroMock));

            assertThatThrownBy(() -> siniestroService.updateEstadoSiniestro(10L, new SiniestroEstadoUpdateRequest(EstadoSiniestro.APROBADO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Desde el estado ABIERTO solo se puede pasar a EN_REVISION");
        }

        @Test
        @DisplayName("Rejet : état terminal APROBADO non modifiable")
        void shouldRejectTransitionFromTerminalAprobado() {
            siniestroMock.setEstado(EstadoSiniestro.APROBADO);
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(siniestroMock));

            assertThatThrownBy(() -> siniestroService.updateEstadoSiniestro(10L, new SiniestroEstadoUpdateRequest(EstadoSiniestro.EN_REVISION)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se puede modificar un siniestro en estado terminal");
        }
    }

    @Nested
    @DisplayName("Tests de consultation (getSiniestroById, getSiniestrosByPolizaId)")
    class RetrievalTests {

        @Test
        @DisplayName("getSiniestroById : succès")
        void shouldReturnSiniestroById() {
            Siniestro s = new Siniestro("SIN-2026-0001", polizaMock, "Golpe", LocalDate.of(2026, 5, 1), new BigDecimal("500.00"), EstadoSiniestro.ABIERTO);
            s.setId(10L);
            when(siniestroRepository.findById(10L)).thenReturn(Optional.of(s));

            SiniestroResponse res = siniestroService.getSiniestroById(10L);

            assertThat(res).isNotNull();
            assertThat(res.numero()).isEqualTo("SIN-2026-0001");
        }

        @Test
        @DisplayName("getSiniestroById : 404 si introuvable")
        void shouldThrowNotFoundWhenSiniestroDoesNotExist() {
            when(siniestroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> siniestroService.getSiniestroById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Siniestro no encontrado con id: 99");
        }

        @Test
        @DisplayName("getSiniestrosByPolizaId : 404 si police introuvable")
        void shouldThrowNotFoundWhenPolizaDoesNotExistForSiniestros() {
            when(polizaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> siniestroService.getSiniestrosByPolizaId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Póliza no encontrada con id: 99");
        }
    }
}
