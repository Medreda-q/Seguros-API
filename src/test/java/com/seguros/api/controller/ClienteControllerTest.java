package com.seguros.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seguros.api.config.ClockConfig;
import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.exception.BusinessConflictException;
import com.seguros.api.exception.BusinessRuleException;
import com.seguros.api.exception.GlobalExceptionHandler;
import com.seguros.api.exception.ResourceNotFoundException;
import com.seguros.api.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import({GlobalExceptionHandler.class, ClockConfig.class})
@DisplayName("Tests @WebMvcTest pour ClienteController")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteService clienteService;

    private final ClienteResponse sampleResponse = new ClienteResponse(
        1L,
        "12345678Z",
        "*****678Z",
        "Carlos",
        "García Lorca",
        "carlos@example.es",
        "+34612345678",
        LocalDate.of(1990, 5, 20),
        "ES1234567890123456789012",
        "ES12 **** **** 9012",
        OffsetDateTime.now(),
        OffsetDateTime.now()
    );

    @Nested
    @DisplayName("POST /api/v1/clientes")
    class CreateClienteTests {

        @Test
        @DisplayName("201 Created : Création avec succès, DNI et IBAN masqués dans la réponse")
        void shouldCreateClienteAndReturn201WithMaskedData() throws Exception {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Carlos",
                "García Lorca",
                "carlos@example.es",
                "+34612345678",
                LocalDate.of(1990, 5, 20),
                "ES1234567890123456789012"
            );

            when(clienteService.createCliente(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos"))
                .andExpect(jsonPath("$.dniNieEnmascarado").value("*****678Z"))
                .andExpect(jsonPath("$.ibanEnmascarado").value("ES12 **** **** 9012"))
                .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("400 Bad Request : Échec Bean Validation avec tableau 'errors'")
        void shouldReturn400WithErrorsListOnInvalidPayload() throws Exception {
            // DNI invalide, email invalide, nom vide
            ClienteCreateRequest invalidRequest = new ClienteCreateRequest(
                "INVALID_DNI",
                "",
                "García",
                "email-invalide",
                "123",
                LocalDate.of(2010, 1, 1),
                "IBAN_FAUX"
            );

            mockMvc.perform(post("/api/v1/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Error de validación"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("409 Conflict : Doublon DNI/NIE renvoyé par le service métier")
        void shouldReturn409ConflictOnDuplicateDni() throws Exception {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Carlos",
                "García Lorca",
                "carlos@example.es",
                "+34612345678",
                LocalDate.of(1990, 5, 20),
                "ES1234567890123456789012"
            );

            when(clienteService.createCliente(any()))
                .thenThrow(new BusinessConflictException("Ya existe un cliente registrado con el DNI/NIE 12345678Z"));

            mockMvc.perform(post("/api/v1/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de negocio"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("Ya existe un cliente")))
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("400 Bad Request : Client mineur rejeté par règle métier")
        void shouldReturn400BadRequestWhenUnderage() throws Exception {
            ClienteCreateRequest request = new ClienteCreateRequest(
                "12345678Z",
                "Menor",
                "Gómez",
                "menor@example.es",
                "+34612345678",
                LocalDate.of(2010, 1, 1),
                "ES1234567890123456789012"
            );

            when(clienteService.createCliente(any()))
                .thenThrow(new BusinessRuleException("El cliente debe tener al menos 18 años para darse de alta"));

            mockMvc.perform(post("/api/v1/clientes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Violación de regla de negocio"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("El cliente debe tener al menos 18 años")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clientes")
    class GetClientesTests {

        @Test
        @DisplayName("200 OK : Retourne la page de clients")
        void shouldReturnPagedClientes() throws Exception {
            when(clienteService.getClientes(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse)));

            mockMvc.perform(get("/api/v1/clientes")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nombre").value("Carlos"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /api/v1/clientes/{id} : 200 OK quand trouvé")
        void shouldReturnClienteById() throws Exception {
            when(clienteService.getClienteById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dniNieEnmascarado").value("*****678Z"));
        }

        @Test
        @DisplayName("GET /api/v1/clientes/{id} : 404 Not Found si inexistant")
        void shouldReturn404WhenClienteNotFound() throws Exception {
            when(clienteService.getClienteById(99L))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));

            mockMvc.perform(get("/api/v1/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cliente no encontrado con id: 99"))
                .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/clientes/{id}")
    class UpdateClienteTests {

        @Test
        @DisplayName("200 OK : Mise à jour réussie")
        void shouldUpdateCliente() throws Exception {
            ClienteUpdateRequest request = new ClienteUpdateRequest(
                "Carlos Alberto",
                "García Lorca",
                "carlos.nuevo@example.es",
                "+34699887766",
                "ES9876543210987654321098"
            );

            when(clienteService.updateCliente(eq(1L), any())).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/clientes/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("404 Not Found : Client introuvable à la mise à jour")
        void shouldReturn404OnUpdateNonExistent() throws Exception {
            ClienteUpdateRequest request = new ClienteUpdateRequest(
                "Carlos Alberto",
                "García Lorca",
                "carlos.nuevo@example.es",
                "+34699887766",
                "ES9876543210987654321098"
            );

            when(clienteService.updateCliente(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));

            mockMvc.perform(put("/api/v1/clientes/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("409 Conflict : Collision de données uniques lors de la mise à jour")
        void shouldReturn409OnConflictDuringUpdate() throws Exception {
            ClienteUpdateRequest request = new ClienteUpdateRequest(
                "Carlos Alberto",
                "García Lorca",
                "carlos.nuevo@example.es",
                "+34699887766",
                "ES9876543210987654321098"
            );

            when(clienteService.updateCliente(eq(1L), any()))
                .thenThrow(new BusinessConflictException("Conflicto con datos existentes de otro cliente"));

            mockMvc.perform(put("/api/v1/clientes/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/clientes/{id}")
    class DeleteClienteTests {

        @Test
        @DisplayName("204 No Content : Suppression autorisée sans polices actives")
        void shouldDeleteClienteAndReturn204() throws Exception {
            doNothing().when(clienteService).deleteCliente(1L);

            mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("409 Conflict : Suppression refusée si le client a des polices ACTIVA")
        void shouldReturn409WhenDeletingClientWithActivePolicies() throws Exception {
            doThrow(new BusinessConflictException("No se puede eliminar el cliente porque tiene pólizas activas asociadas"))
                .when(clienteService).deleteCliente(1L);

            mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("tiene pólizas activas asociadas")));
        }

        @Test
        @DisplayName("404 Not Found : Client introuvable à supprimer")
        void shouldReturn404WhenDeletingNonExistentClient() throws Exception {
            doThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"))
                .when(clienteService).deleteCliente(99L);

            mockMvc.perform(delete("/api/v1/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }
    }
}
