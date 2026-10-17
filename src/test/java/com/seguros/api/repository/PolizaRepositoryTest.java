package com.seguros.api.repository;

import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests d'intégration de PolizaRepository sur PostgreSQL réel (seguros_test_db)")
class PolizaRepositoryTest {

    @Autowired
    private PolizaRepository polizaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente(
            "12345678Z",
            "Carlos",
            "García Ruiz",
            "carlos.garcia@example.es",
            "600123456",
            LocalDate.of(1990, 3, 15),
            "ES1234567890123456783456"
        );
        entityManager.persistAndFlush(cliente);
    }

    @Test
    @DisplayName("getNextNumeroSequence : génère des valeurs incrémentales uniques via la séquence PostgreSQL V2")
    void shouldGenerateSequentialNumbersFromPostgresSequence() {
        Long seq1 = polizaRepository.getNextNumeroSequence();
        Long seq2 = polizaRepository.getNextNumeroSequence();

        assertThat(seq1).isNotNull().isPositive();
        assertThat(seq2).isEqualTo(seq1 + 1);
    }

    @Test
    @DisplayName("findById avec @EntityGraph : charge la police et le client en une seule requête (pas de N+1)")
    void shouldFetchPolizaWithClienteEagerlyViaEntityGraph() {
        Poliza poliza = new Poliza(
            "POL-AUTO-2026-0001",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("450.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza);
        entityManager.clear(); // Détache toutes les entités du contexte de persistance

        Optional<Poliza> found = polizaRepository.findById(poliza.getId());
        assertThat(found).isPresent();

        Poliza p = found.get();
        // Vérifie que le proxy 'cliente' a bien été initialisé par le JOIN de @EntityGraph
        assertThat(Hibernate.isInitialized(p.getCliente())).isTrue();
        assertThat(p.getCliente().getNombre()).isEqualTo("Carlos");
        assertThat(p.getCliente().getDniNie()).isEqualTo("12345678Z");
    }

    @Test
    @DisplayName("findByClienteId avec JOIN FETCH : charge les polices d'un client avec leur entité client jointe")
    void shouldFindPolizasByClienteIdWithJoinFetch() {
        Poliza polizaAuto = new Poliza(
            "POL-AUTO-2026-0002",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("450.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        Poliza polizaHogar = new Poliza(
            "POL-HOGAR-2026-0001",
            TipoPoliza.HOGAR,
            EstadoPoliza.ACTIVA,
            new BigDecimal("320.00"),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2027, 2, 1),
            cliente
        );
        entityManager.persistAndFlush(polizaAuto);
        entityManager.persistAndFlush(polizaHogar);
        entityManager.clear();

        List<Poliza> polizas = polizaRepository.findByClienteId(cliente.getId());
        assertThat(polizas).hasSize(2);
        for (Poliza p : polizas) {
            assertThat(Hibernate.isInitialized(p.getCliente())).isTrue();
            assertThat(p.getCliente().getId()).isEqualTo(cliente.getId());
        }
    }

    @Test
    @DisplayName("existsByClienteIdAndTipoAndEstado : vérifie la règle 4.4.1 (au plus 1 police ACTIVA du même type)")
    void shouldCheckActivePolizaExistence() {
        Poliza poliza = new Poliza(
            "POL-AUTO-2026-0003",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("500.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza);

        // Une police AUTO ACTIVA existe déjà
        assertThat(polizaRepository.existsByClienteIdAndTipoAndEstado(cliente.getId(), TipoPoliza.AUTO, EstadoPoliza.ACTIVA)).isTrue();

        // Mais pas de police HOGAR ni VIDA ACTIVA
        assertThat(polizaRepository.existsByClienteIdAndTipoAndEstado(cliente.getId(), TipoPoliza.HOGAR, EstadoPoliza.ACTIVA)).isFalse();
        assertThat(polizaRepository.existsByClienteIdAndTipoAndEstado(cliente.getId(), TipoPoliza.VIDA, EstadoPoliza.ACTIVA)).isFalse();

        // Ni de police AUTO CANCELADA
        assertThat(polizaRepository.existsByClienteIdAndTipoAndEstado(cliente.getId(), TipoPoliza.AUTO, EstadoPoliza.CANCELADA)).isFalse();
    }

    @Test
    @DisplayName("Index partiel PostgreSQL (uq_polizas_activa_cliente_tipo) : interdit 2 polices ACTIVA du même type")
    void shouldRejectSecondActivePolizaOfSameTypeViaPartialUniqueIndex() {
        Poliza poliza1 = new Poliza(
            "POL-AUTO-2026-0010",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("400.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza1);

        Poliza polizaDoublon = new Poliza(
            "POL-AUTO-2026-0011",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA, // Même type AUTO et même état ACTIVA pour le même client !
            new BigDecimal("420.00"),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2027, 3, 1),
            cliente
        );

        // Doit lever une DataIntegrityViolationException au flush grâce à l'index partiel PostgreSQL V2
        assertThatThrownBy(() -> polizaRepository.saveAndFlush(polizaDoublon))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("uq_polizas_activa_cliente_tipo");
    }

    @Test
    @DisplayName("Index partiel PostgreSQL : autorise plusieurs polices CANCELADA du même type pour le même client")
    void shouldAllowMultipleCancelledPolizasOfSameType() {
        Poliza poliza1 = new Poliza(
            "POL-AUTO-2026-0020",
            TipoPoliza.AUTO,
            EstadoPoliza.CANCELADA,
            new BigDecimal("400.00"),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2025, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza1);

        Poliza poliza2 = new Poliza(
            "POL-AUTO-2026-0021",
            TipoPoliza.AUTO,
            EstadoPoliza.CANCELADA, // Deuxième police AUTO CANCELADA : autorisée !
            new BigDecimal("410.00"),
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2026, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza2);

        Poliza poliza3 = new Poliza(
            "POL-AUTO-2026-0022",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA, // Une seule police AUTO ACTIVA : autorisée !
            new BigDecimal("420.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza3);

        List<Poliza> list = polizaRepository.findByClienteId(cliente.getId());
        assertThat(list).hasSize(3);
    }
}
