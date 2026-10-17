package com.seguros.api.dto.mapper;

import com.seguros.api.dto.cliente.ClienteCreateRequest;
import com.seguros.api.dto.cliente.ClienteResponse;
import com.seguros.api.dto.cliente.ClienteUpdateRequest;
import com.seguros.api.dto.poliza.PolizaCreateRequest;
import com.seguros.api.dto.poliza.PolizaResponse;
import com.seguros.api.dto.poliza.PolizaUpdateRequest;
import com.seguros.api.dto.siniestro.SiniestroCreateRequest;
import com.seguros.api.dto.siniestro.SiniestroResponse;
import com.seguros.api.dto.siniestro.SiniestroUpdateRequest;
import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.Siniestro;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.EstadoSiniestro;
import com.seguros.api.entity.enums.TipoPoliza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires des mappers manuels (ClienteMapper, PolizaMapper, SiniestroMapper)")
class MapperTest {

    @Test
    @DisplayName("ClienteMapper : conversion Request -> Entity et Entity -> Response avec masquage")
    void shouldMapClienteCorrectly() {
        ClienteCreateRequest createReq = new ClienteCreateRequest(
            "12345678Z",
            "Elena",
            "Naranjo",
            "elena@example.es",
            "600112233",
            LocalDate.of(1992, 4, 10),
            "ES1234567890123456783456"
        );

        Cliente entity = ClienteMapper.toEntity(createReq);
        assertThat(entity.getDniNie()).isEqualTo("12345678Z");
        assertThat(entity.getNombre()).isEqualTo("Elena");

        // Simulation ID et audit
        entity.setId(42L);

        ClienteResponse response = ClienteMapper.toResponse(entity);
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.dniNieEnmascarado()).isEqualTo("*****678Z");
        assertThat(response.ibanEnmascarado()).isEqualTo("ES12 **** **** 3456");

        // Test update
        ClienteUpdateRequest updateReq = new ClienteUpdateRequest(
            "Elena Sofía",
            "Naranjo Pérez",
            "elena.nueva@example.es",
            "699887766",
            "ES9988776655443322110099"
        );
        ClienteMapper.updateEntity(entity, updateReq);
        assertThat(entity.getNombre()).isEqualTo("Elena Sofía");
        assertThat(entity.getEmail()).isEqualTo("elena.nueva@example.es");
    }

    @Test
    @DisplayName("PolizaMapper : conversion sans fuite d'entité JPA Cliente")
    void shouldMapPolizaCorrectly() {
        Cliente cliente = new Cliente("12345678Z", "Carlos", "García", "carlos@example.es", "612345678", LocalDate.of(1990, 1, 1), "ES1234567890123456783456");
        cliente.setId(10L);

        PolizaCreateRequest createReq = new PolizaCreateRequest(
            10L,
            TipoPoliza.AUTO,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1)
        );

        Poliza entity = PolizaMapper.toEntity(createReq, cliente, "POL-AUTO-2026-0001", new BigDecimal("500.00"));
        entity.setId(100L);

        PolizaResponse response = PolizaMapper.toResponse(entity);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.numero()).isEqualTo("POL-AUTO-2026-0001");
        assertThat(response.clienteId()).isEqualTo(10L);
        assertThat(response.clienteNombreCompleto()).isEqualTo("Carlos García");
        assertThat(response.clienteDniNieEnmascarado()).isEqualTo("*****678Z");
        assertThat(response.estado()).isEqualTo(EstadoPoliza.ACTIVA);

        // Update
        PolizaUpdateRequest updateReq = new PolizaUpdateRequest(
            TipoPoliza.HOGAR,
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2027, 2, 1)
        );
        PolizaMapper.updateEntity(entity, updateReq);
        assertThat(entity.getTipo()).isEqualTo(TipoPoliza.HOGAR);
    }

    @Test
    @DisplayName("SiniestroMapper : conversion sans fuite d'entité JPA Poliza")
    void shouldMapSiniestroCorrectly() {
        Poliza poliza = new Poliza();
        poliza.setId(200L);
        poliza.setNumero("POL-AUTO-2026-0001");

        SiniestroCreateRequest createReq = new SiniestroCreateRequest(
            200L,
            "Rotura de luna delantera en autovía por impacto de gravilla.",
            LocalDate.of(2026, 3, 15),
            new BigDecimal("350.00")
        );

        Siniestro entity = SiniestroMapper.toEntity(createReq, poliza, "SIN-2026-0001");
        entity.setId(500L);

        SiniestroResponse response = SiniestroMapper.toResponse(entity);
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.numero()).isEqualTo("SIN-2026-0001");
        assertThat(response.polizaId()).isEqualTo(200L);
        assertThat(response.polizaNumero()).isEqualTo("POL-AUTO-2026-0001");
        assertThat(response.estado()).isEqualTo(EstadoSiniestro.ABIERTO);

        // Update
        SiniestroUpdateRequest updateReq = new SiniestroUpdateRequest(
            "Rotura de luna delantera y sustitución de escobillas limpiaparabrisas.",
            LocalDate.of(2026, 3, 15),
            new BigDecimal("420.00")
        );
        SiniestroMapper.updateEntity(entity, updateReq);
        assertThat(entity.getImporteReclamado()).isEqualTo(new BigDecimal("420.00"));
    }
}
