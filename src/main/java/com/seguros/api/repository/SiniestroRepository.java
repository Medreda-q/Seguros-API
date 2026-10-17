package com.seguros.api.repository;

import com.seguros.api.entity.Siniestro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Siniestro.
 * Gère le chargement optimisé avec @EntityGraph et JOIN FETCH sur poliza et poliza.cliente
 * pour éliminer les requêtes N+1 et la génération de numéros atomiques par séquence.
 */
@Repository
public interface SiniestroRepository extends JpaRepository<Siniestro, Long> {

    /**
     * Recherche un sinistre par ID en chargeant immédiatement la police et le client.
     * @EntityGraph effectue un JOIN sur poliza et poliza.cliente.
     */
    @Override
    @EntityGraph(attributePaths = {"poliza", "poliza.cliente"})
    Optional<Siniestro> findById(Long id);

    /**
     * Recherche un sinistre par son numéro unique (ex: SIN-2026-0001).
     */
    @EntityGraph(attributePaths = {"poliza", "poliza.cliente"})
    Optional<Siniestro> findByNumero(String numero);

    /**
     * Recherche tous les sinistres déclarés pour une police donnée.
     * Utilise JOIN FETCH sur la police et son client pour éviter les requêtes N+1 lors du mapping.
     */
    @Query("SELECT s FROM Siniestro s JOIN FETCH s.poliza p JOIN FETCH p.cliente WHERE s.poliza.id = :polizaId ORDER BY s.fechaOcurrencia DESC")
    List<Siniestro> findByPolizaId(@Param("polizaId") Long polizaId);

    /**
     * Obtient la valeur suivante de la séquence PostgreSQL 'seq_siniestro_numero'.
     * Opération atomique native PostgreSQL.
     *
     * @return Prochaine valeur séquentielle
     */
    @Query(value = "SELECT nextval('seq_siniestro_numero')", nativeQuery = true)
    Long getNextNumeroSequence();
}
