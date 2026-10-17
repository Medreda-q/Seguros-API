package com.seguros.api.repository;

import com.seguros.api.entity.Cliente;
import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.Siniestro;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.EstadoSiniestro;
import com.seguros.api.entity.enums.TipoPoliza;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests d'intégration de SiniestroRepository sur PostgreSQL réel (seguros_test_db)")
class SiniestroRepositoryTest {

    @Autowired
    private SiniestroRepository siniestroRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente;
    private Poliza poliza;

    @BeforeEach
    void setUp() {
        cliente = new Cliente(
            "12345678Z",
            "Lucía",
            "Navarro Sánchez",
            "lucia.navarro@example.es",
            "633445566",
            LocalDate.of(1985, 8, 25),
            "ES1234567890123456783456"
        );
        entityManager.persistAndFlush(cliente);

        poliza = new Poliza(
            "POL-AUTO-2026-0001",
            TipoPoliza.AUTO,
            EstadoPoliza.ACTIVA,
            new BigDecimal("600.00"),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            cliente
        );
        entityManager.persistAndFlush(poliza);
    }

    @Test
    @DisplayName("getNextNumeroSequence : génère des valeurs incrémentales uniques via la séquence PostgreSQL V2")
    void shouldGenerateSequentialNumbersFromPostgresSequence() {
        Long seq1 = siniestroRepository.getNextNumeroSequence();
        Long seq2 = siniestroRepository.getNextNumeroSequence();

        assertThat(seq1).isNotNull().isPositive();
        assertThat(seq2).isEqualTo(seq1 + 1);
    }

    @Test
    @DisplayName("findById avec @EntityGraph : charge le sinistre, la police et le client sans requête N+1")
    void shouldFetchSiniestroWithPolizaAndClienteEagerly() {
        Siniestro siniestro = new Siniestro(
            "SIN-2026-0001",
            poliza,
            "Colisión trasera leve en semáforo.",
            LocalDate.of(2026, 3, 10),
            new BigDecimal("750.50"),
            EstadoSiniestro.ABIERTO
        );
        entityManager.persistAndFlush(siniestro);
        entityManager.clear();

        Optional<Siniestro> found = siniestroRepository.findById(siniestro.getId());
        assertThat(found).isPresent();

        Siniestro s = found.get();
        // Vérifie que poliza et poliza.cliente sont tous les deux initialisés par le JOIN de @EntityGraph
        assertThat(Hibernate.isInitialized(s.getPoliza())).isTrue();
        assertThat(Hibernate.isInitialized(s.getPoliza().getCliente())).isTrue();
        assertThat(s.getPoliza().getNumero()).isEqualTo("POL-AUTO-2026-0001");
        assertThat(s.getPoliza().getCliente().getNombre()).isEqualTo("Lucía");
    }

    @Test
    @DisplayName("findByPolizaId avec JOIN FETCH : charge tous les sinistres d'une police avec police et client")
    void shouldFindSiniestrosByPolizaIdWithJoinFetch() {
        Siniestro s1 = new Siniestro(
            "SIN-2026-0002",
            poliza,
            "Impacto contra retrovisor estacionado.",
            LocalDate.of(2026, 4, 1),
            new BigDecimal("180.00"),
            EstadoSiniestro.ABIERTO
        );
        Siniestro s2 = new Siniestro(
            "SIN-2026-0003",
            poliza,
            "Rotura de faro antiniebla.",
            LocalDate.of(2026, 5, 2),
            new BigDecimal("220.00"),
            EstadoSiniestro.EN_REVISION
        );
        entityManager.persistAndFlush(s1);
        entityManager.persistAndFlush(s2);
        entityManager.clear();

        List<Siniestro> list = siniestroRepository.findByPolizaId(poliza.getId());
        assertThat(list).hasSize(2);
        for (Siniestro s : list) {
            assertThat(Hibernate.isInitialized(s.getPoliza())).isTrue();
            assertThat(Hibernate.isInitialized(s.getPoliza().getCliente())).isTrue();
        }
    }
}
