package com.seguros.api.repository;

import com.seguros.api.entity.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Tests d'intégration de ClienteRepository sur PostgreSQL réel (seguros_test_db)")
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Cliente cliente1;

    @BeforeEach
    void setUp() {
        cliente1 = new Cliente(
            "12345678Z",
            "María",
            "López García",
            "maria.lopez@example.es",
            "611223344",
            LocalDate.of(1988, 5, 20),
            "ES1234567890123456783456"
        );
        entityManager.persistAndFlush(cliente1);
    }

    @Test
    @DisplayName("findByDniNie : retrouve un client existant par son identifiant unique")
    void shouldFindClienteByDniNie() {
        Optional<Cliente> found = clienteRepository.findByDniNie("12345678Z");
        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("María");
        assertThat(found.get().getEmail()).isEqualTo("maria.lopez@example.es");
    }

    @Test
    @DisplayName("findByDniNie : renvoie vide pour un identifiant inexistant")
    void shouldReturnEmptyWhenDniNieNotFound() {
        Optional<Cliente> found = clienteRepository.findByDniNie("87654321X");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByDniNie : renvoie true si le client existe, false sinon")
    void shouldCheckIfExistsByDniNie() {
        assertThat(clienteRepository.existsByDniNie("12345678Z")).isTrue();
        assertThat(clienteRepository.existsByDniNie("00000000T")).isFalse();
    }

    @Test
    @DisplayName("existsByIdNotAndDniNie : protège contre les collisions lors des mises à jour")
    void shouldCheckCollisionForUpdate() {
        Cliente cliente2 = new Cliente(
            "87654321X",
            "Javier",
            "Ruiz Martín",
            "javier.ruiz@example.es",
            "622334455",
            LocalDate.of(1995, 11, 14),
            "ES9876543210987654321098"
        );
        entityManager.persistAndFlush(cliente2);

        // Si cliente2 tente d'adopter le DNI de cliente1 -> collision détectée (true)
        assertThat(clienteRepository.existsByIdNotAndDniNie(cliente2.getId(), "12345678Z")).isTrue();

        // Si cliente1 garde son propre DNI -> pas de collision (false)
        assertThat(clienteRepository.existsByIdNotAndDniNie(cliente1.getId(), "12345678Z")).isFalse();

        // Si un DNI libre est utilisé -> pas de collision (false)
        assertThat(clienteRepository.existsByIdNotAndDniNie(cliente2.getId(), "99999999R")).isFalse();
    }

    @Test
    @DisplayName("Contrainte UNIQUE DNI : lève DataIntegrityViolationException en cas de doublon en base")
    void shouldFailWhenDuplicateDniIsPersisted() {
        Cliente doublon = new Cliente(
            "12345678Z", // Même DNI que cliente1
            "Autre",
            "Personne",
            "autre@example.es",
            "600000000",
            LocalDate.of(1990, 1, 1),
            "ES0000000000000000000000"
        );

        assertThatThrownBy(() -> {
            clienteRepository.saveAndFlush(doublon);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
