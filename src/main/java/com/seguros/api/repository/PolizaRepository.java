package com.seguros.api.repository;

import com.seguros.api.entity.Poliza;
import com.seguros.api.entity.enums.EstadoPoliza;
import com.seguros.api.entity.enums.TipoPoliza;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Poliza.
 * Gère le chargement optimisé (@EntityGraph / JOIN FETCH) pour éliminer les requêtes N+1
 * et la génération atomique de numéros via séquence PostgreSQL.
 */
@Repository
public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    /**
     * Recherche une police par son identifiant en chargeant immédiatement son client.
     * Utilise @EntityGraph pour réaliser un LEFT OUTER JOIN, évitant le problème N+1
     * lorsque le client rattaché est accédé hors session ou pour mapper la réponse.
     */
    @Override
    @EntityGraph(attributePaths = {"cliente"})
    Optional<Poliza> findById(Long id);

    /**
     * Recherche une police par son numéro unique de contrat (ex: POL-AUTO-2026-0001).
     */
    @EntityGraph(attributePaths = {"cliente"})
    Optional<Poliza> findByNumero(String numero);

    /**
     * Recherche toutes les polices d'un client donné avec JOIN FETCH pour charger le client en une seule requête.
     */
    @Query("SELECT p FROM Poliza p JOIN FETCH p.cliente WHERE p.cliente.id = :clienteId ORDER BY p.createdAt DESC")
    List<Poliza> findByClienteId(@Param("clienteId") Long clienteId);

    /**
     * Règle métier 4.4.1 : Vérifie si le client possède déjà une police active d'un type donné.
     * Cette vérification applicative précède l'index partiel PostgreSQL (uq_polizas_activa_cliente_tipo).
     *
     * @param clienteId ID du client
     * @param tipo Type de police (AUTO, HOGAR, VIDA)
     * @param estado État de la police (ACTIVA)
     * @return true si une police active du même type existe déjà pour ce client
     */
    boolean existsByClienteIdAndTipoAndEstado(Long clienteId, TipoPoliza tipo, EstadoPoliza estado);

    /**
     * Obtient la valeur suivante de la séquence PostgreSQL 'seq_poliza_numero'.
     * Opération atomique gérée nativement par le moteur PostgreSQL, immunisée contre les conditions de course.
     *
     * @return Prochaine valeur séquentielle
     */
    @Query(value = "SELECT nextval('seq_poliza_numero')", nativeQuery = true)
    Long getNextNumeroSequence();
}
