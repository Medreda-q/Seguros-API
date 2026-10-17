-- ==============================================================================
-- V2__sequences_and_constraints.sql : Séquences PostgreSQL & Index Unique Partiel
-- SGBD : PostgreSQL 16
-- ==============================================================================

-- 1. Séquences pour numérotation sans condition de course (atomicité nextval)
-- Évite le pattern vulnérable SELECT COUNT(*)+1 ou MAX()+1 sujet aux race conditions
CREATE SEQUENCE IF NOT EXISTS seq_poliza_numero
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_siniestro_numero
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 2. Index unique partiel : au plus 1 police ACTIVA par (cliente_id, tipo)
-- Règle métier 4.4.1 : un client peut avoir plusieurs polices CANCELADA/SUSPENDIDA
-- du même type au fil du temps, mais UNE SEULE police ACTIVA simultanément.
-- L'index partiel PostgreSQL garantit cette règle au niveau SGBD même en cas de requêtes concurrentes.
CREATE UNIQUE INDEX IF NOT EXISTS uq_polizas_activa_cliente_tipo
    ON polizas (cliente_id, tipo)
    WHERE estado = 'ACTIVA';
