package com.seguros.api.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("Tests d'intégration de bout en bout sur PostgreSQL réel (seguros_test_db, sans mocks)")
class SegurosApiFullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Flux nominal complet : créer client -> créer police -> déclarer sinistre avec masquage PII et montants exacts")
    void shouldExecuteFullLifecycleFlowAndMaskSensitiveData() throws Exception {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaFinPoliza = hoy.plusYears(1);

        // 1. Création du client via POST /api/v1/clientes
        String clientePayload = """
            {
              "dniNie": "12345678Z",
              "nombre": "Carlos",
              "apellidos": "Santana Ruiz",
              "email": "carlos.santana@example.com",
              "telefono": "654987321",
              "fechaNacimiento": "1990-06-15",
              "iban": "ES1234567890123456783456"
            }
            """;

        MvcResult clienteResult = mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(clientePayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dniNieEnmascarado", is("*****678Z")))
            .andExpect(jsonPath("$.ibanEnmascarado", is("ES12 **** **** 3456")))
            .andExpect(jsonPath("$.nombre", is("Carlos")))
            .andReturn();

        Number clienteIdNumber = JsonPath.read(clienteResult.getResponse().getContentAsString(), "$.id");
        Long clienteId = clienteIdNumber.longValue();

        // 2. Création de la police via POST /api/v1/polizas
        String polizaPayload = String.format("""
            {
              "clienteId": %d,
              "tipo": "AUTO",
              "fechaInicio": "%s",
              "fechaFin": "%s"
            }
            """, clienteId, hoy, fechaFinPoliza);

        MvcResult polizaResult = mockMvc.perform(post("/api/v1/polizas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(polizaPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").isString())
            .andExpect(jsonPath("$.tipo", is("AUTO")))
            .andExpect(jsonPath("$.estado", is("ACTIVA")))
            .andExpect(jsonPath("$.primaAnual", is(500.00)))
            .andExpect(jsonPath("$.clienteDniNieEnmascarado", is("*****678Z")))
            .andReturn();

        Number polizaIdNumber = JsonPath.read(polizaResult.getResponse().getContentAsString(), "$.id");
        Long polizaId = polizaIdNumber.longValue();

        // 3. Déclaration d'un sinistre via POST /api/v1/siniestros
        String siniestroPayload = String.format("""
            {
              "polizaId": %d,
              "descripcion": "Golpe leve en paragolpes trasero durante maniobra",
              "fechaOcurrencia": "%s",
              "importeReclamado": 150.00
            }
            """, polizaId, hoy);

        mockMvc.perform(post("/api/v1/siniestros")
                .contentType(MediaType.APPLICATION_JSON)
                .content(siniestroPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero").isString())
            .andExpect(jsonPath("$.polizaId", is(polizaId.intValue())))
            .andExpect(jsonPath("$.importeReclamado", is(150.00)))
            .andExpect(jsonPath("$.estado", is("ABIERTO")));
    }

    @Test
    @DisplayName("Doit renvoyer HTTP 400 Bad Request (ProblemDetail) quand le JSON est malformé ou illisible (au lieu d'un 500)")
    void shouldReturn400BadRequestWhenPayloadIsMalformedOrUnreadable() throws Exception {
        // Envoi d'un payload avec du JSON mal formé (guillemets non fermés ou syntaxe corrompue)
        String malformedJson = "{ \"dniNie\": \"12345678Z\", \"nombre\": malformed }";

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Cuerpo de solicitud inválido")))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.detail", is("El cuerpo de la solicitud no es legible o contiene un formato JSON inválido")))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
