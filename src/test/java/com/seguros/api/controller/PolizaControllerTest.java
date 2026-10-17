package com.seguros.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seguros.api.config.ClockConfig;
import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaEstadoUpdateRequest;
import com.seguros.api.dto.poliza.PolizaResponse;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.GlobalExceptionHandler;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.service.PolizaService;
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

@WebMvcTest(PolizaController.class)
@Import({GlobalExceptionHandler.class, ClockConfig.class})
@DisplayName("Tests @WebMvcTest pour PolizaController")
class PolizaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolizaService polizaService;

    private final PolizaResponse sampleResponse = new PolizaResponse(
        100L,
        "POL-AUTO-2026-0001",
        TipoPoliza.AUTO,
        EstadoPoliza.ACTIVA,
        new BigDecimal("500.00"), // Sérialisation exacte exigée : 500.00
        LocalDate.of(2026, 6, 15),
        LocalDate.of(2027, 6, 15),
        10L,
        "Carlos García Lorca",
        "*****678Z", // DNI masqué exigé
        OffsetDateTime.now(),
        OffsetDateTime.now()
    );

    @Nested
    @DisplayName("POST /api/v1/polizas")
    class CreatePolizaTests {

        @Test
        @DisplayName("201 Created : Émission réussie avec montant 500.00 et DNI masqué")
        void shouldCreatePolizaAndReturn201WithExactAmountAndMaskedDni() throws Exception {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2027, 6, 15)
            );

            when(polizaService.createPoliza(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/polizas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.numero").value("POL-AUTO-2026-0001"))
                .andExpect(jsonPath("$.primaAnual").value(500.00)) // Exactitude du montant
                .andExpect(jsonPath("$.clienteDniNieEnmascarado").value("*****678Z"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
        }

        @Test
        @DisplayName("400 Bad Request : Échec de validation du DTO (champs nuls ou dates incohérentes)")
        void shouldReturn400WhenPayloadFailsValidation() throws Exception {
            // fechaFin avant fechaInicio
            PolizaCreateRequest invalid = new PolizaCreateRequest(
                null, // @NotNull violé
                null, // @NotNull violé
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 1, 1) // @ValidDateRange violé
            );

            mockMvc.perform(post("/api/v1/polizas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("400 Bad Request : Règle métier 4.4.2 (date de début dans le passé)")
        void shouldReturn400WhenFechaInicioInThePast() throws Exception {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 1)
            );

            when(polizaService.createPoliza(any()))
                .thenThrow(new BusinessRuleException("La fecha de inicio de la póliza no puede ser anterior a la fecha de hoy"));

            mockMvc.perform(post("/api/v1/polizas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Violación de regla de negocio"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("no puede ser anterior a la fecha de hoy")));
        }

        @Test
        @DisplayName("409 Conflict : Règle métier 4.4.1 (police active du même type déjà existante)")
        void shouldReturn409WhenActivePolizaAlreadyExists() throws Exception {
            PolizaCreateRequest request = new PolizaCreateRequest(
                10L,
                TipoPoliza.AUTO,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2027, 6, 15)
            );

            when(polizaService.createPoliza(any()))
                .thenThrow(new BusinessConflictException("El cliente ya tiene una póliza ACTIVA de tipo AUTO"));

            mockMvc.perform(post("/api/v1/polizas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de negocio"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("ya tiene una póliza ACTIVA")));
        }

        @Test
        @DisplayName("404 Not Found : Client inexistant lors de l'émission")
        void shouldReturn404WhenClienteNotFound() throws Exception {
            PolizaCreateRequest request = new PolizaCreateRequest(
                99L,
                TipoPoliza.AUTO,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2027, 6, 15)
            );

            when(polizaService.createPoliza(any()))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));

            mockMvc.perform(post("/api/v1/polizas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/polizas")
    class GetPolizasTests {

        @Test
        @DisplayName("GET /api/v1/polizas/{id} : 200 OK")
        void shouldReturnPolizaById() throws Exception {
            when(polizaService.getPolizaById(100L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/polizas/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.primaAnual").value(500.00))
                .andExpect(jsonPath("$.clienteDniNieEnmascarado").value("*****678Z"));
        }

        @Test
        @DisplayName("GET /api/v1/polizas/{id} : 404 Not Found")
        void shouldReturn404WhenPolizaNotFound() throws Exception {
            when(polizaService.getPolizaById(999L))
                .thenThrow(new ResourceNotFoundException("Póliza no encontrada con id: 999"));

            mockMvc.perform(get("/api/v1/polizas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("GET /api/v1/polizas/numero/{numero} : 200 OK")
        void shouldReturnPolizaByNumero() throws Exception {
            when(polizaService.getPolizaByNumero("POL-AUTO-2026-0001")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/polizas/numero/POL-AUTO-2026-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("POL-AUTO-2026-0001"));
        }

        @Test
        @DisplayName("GET /api/v1/polizas/cliente/{clienteId} : 200 OK liste non paginée")
        void shouldReturnPolizasListForClient() throws Exception {
            when(polizaService.getPolizasByClienteId(10L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/polizas/cliente/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value("POL-AUTO-2026-0001"));
        }

        @Test
        @DisplayName("Point 5 : GET /polizas/cliente/{id} renvoie 404 si le client n'existe pas")
        void shouldReturn404WhenClientDoesNotExistForPolizasQuery() throws Exception {
            when(polizaService.getPolizasByClienteId(99L))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/polizas/cliente/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cliente no encontrado con id: 99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/polizas/{id}/estado")
    class UpdateEstadoTests {

        @Test
        @DisplayName("200 OK : Transition d'état réussie")
        void shouldUpdatePolizaEstado() throws Exception {
            PolizaEstadoUpdateRequest request = new PolizaEstadoUpdateRequest(EstadoPoliza.SUSPENDIDA);
            PolizaResponse updatedResponse = new PolizaResponse(
                100L, "POL-AUTO-2026-0001", TipoPoliza.AUTO, EstadoPoliza.SUSPENDIDA,
                new BigDecimal("500.00"), LocalDate.of(2026, 6, 15), LocalDate.of(2027, 6, 15),
                10L, "Carlos", "*****678Z", OffsetDateTime.now(), OffsetDateTime.now()
            );

            when(polizaService.updateEstadoPoliza(eq(100L), any())).thenReturn(updatedResponse);

            mockMvc.perform(patch("/api/v1/polizas/100/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
        }

        @Test
        @DisplayName("400 Bad Request : Transition non autorisée (ex: police CANCELADA)")
        void shouldReturn400WhenTransitionIsInvalid() throws Exception {
            PolizaEstadoUpdateRequest request = new PolizaEstadoUpdateRequest(EstadoPoliza.ACTIVA);

            when(polizaService.updateEstadoPoliza(eq(100L), any()))
                .thenThrow(new BusinessRuleException("No se puede cambiar el estado de una póliza CANCELADA (estado terminal)"));

            mockMvc.perform(patch("/api/v1/polizas/100/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("estado terminal")));
        }

        @Test
        @DisplayName("409 Conflict : Reactivation vers ACTIVA en conflit avec une autre police active")
        void shouldReturn409WhenReactivationConflictsWithAnotherActivePoliza() throws Exception {
            PolizaEstadoUpdateRequest request = new PolizaEstadoUpdateRequest(EstadoPoliza.ACTIVA);

            when(polizaService.updateEstadoPoliza(eq(100L), any()))
                .thenThrow(new BusinessConflictException("El cliente ya tiene otra póliza ACTIVA de tipo AUTO"));

            mockMvc.perform(patch("/api/v1/polizas/100/estado")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        }
    }
}
