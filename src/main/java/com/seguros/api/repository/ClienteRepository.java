package com.seguros.api.repository;

import com.seguros.api.entity.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Cliente.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Recherche un client par son DNI/NIE unique.
     *
     * @param dniNie Identifiant légal espagnol normalisé
     * @return Optional contenant le client s'il existe
     */
    Optional<Cliente> findByDniNie(String dniNie);

    /**
     * Vérifie si un client existe avec ce DNI/NIE.
     *
     * @param dniNie Identifiant légal
     * @return true si le DNI/NIE existe déjà
     */
    boolean existsByDniNie(String dniNie);

    /**
     * Vérifie si un AUTRE client possède déjà ce DNI/NIE (évite les collisions lors d'un update).
     *
     * @param id Identifiant du client en cours de mise à jour
     * @param dniNie Identifiant légal cible
     * @return true si un autre enregistrement avec ce DNI/NIE existe déjà
     */
    boolean existsByIdNotAndDniNie(Long id, String dniNie);

    /**
     * Recherche un client par son ID en chargeant également ses polices (évite le lazy loading hors session).
     *
     * @param id Identifiant du client
     * @return Optional contenant le client avec sa collection de polices initialisée
     */
    @EntityGraph(attributePaths = {"polizas"})
    Optional<Cliente> findWithPolizasById(Long id);
}
