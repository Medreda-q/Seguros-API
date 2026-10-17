-- Initialisation des bases de données pour SegurosAPI
-- Script exécuté automatiquement lors du premier démarrage du conteneur PostgreSQL

-- Création de la base de données dédiée aux tests d'intégration (parité stricte dev/test)
SELECT 'CREATE DATABASE seguros_test_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'seguros_test_db')\gexec

GRANT ALL PRIVILEGES ON DATABASE seguros_test_db TO seguros_admin;
