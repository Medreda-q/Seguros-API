package com.seguros.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seguros.api.config.ClockConfig;
import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroEstadoUpdateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.entity.enums.EstadoSiniestro;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.GlobalExceptionHandler;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.service.SiniestroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiniestroController.class)
@Import({GlobalExceptionHandler.class, ClockConfig.class})
@DisplayName("Tests @WebMvcTest pour SiniestroController")
class SiniestroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SiniestroService siniestroService;

    private final SiniestroResponse sampleResponse = new SiniestroResponse(
        50L,
        "SIN-2026-0001",
        100L,
        "POL-AUTO-2026-0001",
        "Colisión trasera en cruce semafórico con daños en carrocería",
        LocalDate.of(2026, 6, 10),
        new BigDecimal("1200.50"), // Montant sérialisé avec précision
        EstadoSiniestro.ABIERTO,
        OffsetDateTime.now(),
        OffsetDateTime.now()
    );

    @Nested
    @DisplayName("POST /api/v1/siniestros")
    class CreateSiniestroTests {

        @Test
        @DisplayName("201 Created : Déclaration réussie avec montant 1200.50 et état ABIERTO")
        void shouldCreateSiniestroAndReturn201WithExactAmount() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                100L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2026, 6, 10),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.numero").value("SIN-2026-0001"))
                .andExpect(jsonPath("$.importeReclamado").value(1200.50))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.polizaNumero").value("POL-AUTO-2026-0001"));
        }

        @Test
        @DisplayName("400 Bad Request : Échec de validation du DTO (champs nuls ou description trop courte)")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            SiniestroCreateRequest invalid = new SiniestroCreateRequest(
                null,
                "Court", // < 10 caractères
                null,
                new BigDecimal("-50.00") // négatif
            );

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("404 Not Found (Règle 4.5 Étape 1) : Police introuvable")
        void shouldReturn404WhenPolizaDoesNotExist() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                999L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2026, 6, 10),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any()))
                .thenThrow(new ResourceNotFoundException("Póliza no encontrada con id: 999"));

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Póliza no encontrada con id: 999"));
        }

        @Test
        @DisplayName("409 Conflict (Règle 4.5 Étape 2) : Police non ACTIVA (SUSPENDIDA/CANCELADA)")
        void shouldReturn409WhenPolizaIsNotActive() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                100L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2026, 6, 10),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any()))
                .thenThrow(new BusinessConflictException("No se puede declarar un siniestro sobre una póliza SUSPENDIDA"));

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de negocio"))
                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("409 Conflict (Règle 4.5 Étape 3) : Police expirée")
        void shouldReturn409WhenPolizaIsExpired() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                100L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2026, 6, 10),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any()))
                .thenThrow(new BusinessConflictException("No se puede declarar un siniestro sobre una póliza expirada"));

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("400 Bad Request (Règle 4.5 Étape 4) : Date d'occurrence hors période de couverture")
        void shouldReturn400WhenFechaOcurrenciaOutOfRange() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                100L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2025, 12, 31),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any()))
                .thenThrow(new BusinessRuleException("La fecha de ocurrencia no está dentro del período de vigencia de la póliza"));

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("no está dentro del período de vigencia")));
        }

        @Test
        @DisplayName("400 Bad Request (Règle 4.5 Étape 5) : Date d'occurrence dans le futur")
        void shouldReturn400WhenFechaOcurrenciaIsInTheFuture() throws Exception {
            SiniestroCreateRequest request = new SiniestroCreateRequest(
                100L,
                "Colisión trasera en cruce semafórico con daños en carrocería",
                LocalDate.of(2026, 6, 16),
                new BigDecimal("1200.50")
            );

            when(siniestroService.createSiniestro(any()))
                .thenThrow(new BusinessRuleException("La fecha de ocurrencia no puede ser una fecha futura."));

            mockMvc.perform(post("/api/v1/siniestros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/siniestros")
    class GetSiniestrosTests {

        @Test
        @DisplayName("GET /api/v1/siniestros/{id} : 200 OK")
        void shouldReturnSiniestroById() throws Exception {
            when(siniestroService.getSiniestroById(50L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/siniestros/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.importeReclamado").value(1200.50));
        }

        @Test
        @DisplayName("GET /api/v1/siniestros/{id} : 404 Not Found")
        void shouldReturn404WhenSiniestroNotFound() throws Exception {
            when(siniestroService.getSiniestroById(999L))
                .thenThrow(new ResourceNotFoundException("Siniestro no encontrado con id: 999"));

            mockMvc.perform(get("/api/v1/siniestros/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("GET /api/v1/siniestros/numero/{numero} : 200 OK")
        void shouldReturnSiniestroByNumero() throws Exception {
            when(siniestroService.getSiniestroByNumero("SIN-2026-0001")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/siniestros/numero/SIN-2026-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("SIN-2026-0001"));
        }

        @Test
        @DisplayName("GET /api/v1/siniestros/poliza/{polizaId} : 200 OK liste non paginée")
        void shouldReturnSiniestrosListForPoliza() throws Exception {
            when(siniestroService.getSiniestrosByPolizaId(100L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/siniestros/poliza/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value("SIN-2026-0001"));
        }

        @Test
        @DisplayName("GET /api/v1/siniestros/poliza/{polizaId} : 404 si la police n'existe pas")
        void shouldReturn404WhenPolizaNotFoundForSiniestrosQuery() throws Exception {
            when(siniestroService.getSiniestrosByPolizaId(999L))
                .thenThrow(new ResourceNotFoundException("Póliza no encontrada con id: 999"));

            mockMvc.perform(get("/api/v1/siniestros/poliza/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/siniestros/{id}/estado")
    class UpdateEstadoTests {

        @Test
        @DisplayName("200 OK : Avancement d'état réussi")
        void shouldUpdateSiniestroEstado() throws Exception {
            SiniestroEstadoUpdateRequest request = new SiniestroEstadoUpdateRequest(EstadoSiniestro.EN_REVISION);
            SiniestroResponse updated = new SiniestroResponse(
                50L, "SIN-2026-0001", 100L, "POL-AUTO-2026-0001",
                "Colisión trasera", LocalDate.of(2026, 6, 10), new BigDecimal("1200.50"),
                EstadoSiniestro.EN_REVISION, OffsetDateTime.now(), OffsetDateTime.now()
            );

            when(siniestroService.updateEstadoSiniestro(eq(50L), any())).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/siniestros/50/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        }

        @Test
        @DisplayName("400 Bad Request : Transition d'état invalide (ex: depuis un état terminal)")
        void shouldReturn400WhenTransitionIsInvalid() throws Exception {
            SiniestroEstadoUpdateRequest request = new SiniestroEstadoUpdateRequest(EstadoSiniestro.EN_REVISION);

            when(siniestroService.updateEstadoSiniestro(eq(50L), any()))
                .thenThrow(new BusinessRuleException("No se puede modificar un siniestro en estado terminal"));

            mockMvc.perform(patch("/api/v1/siniestros/50/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("estado terminal")));
        }
    }
}
