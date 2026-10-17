# Registro de Errores y Bugs (BUGS.md)

Este documento registra de forma cronológica cada error o comportamiento anómalo detectado durante el desarrollo y las pruebas (ambientes dev y test), identificando el síntoma, la causa raíz y la solución implementada.

| Fecha (YYYY-MM-DD) | Entorno (dev / test) | Síntoma observado | Causa raíz | Solución aplicada |
| :--- | :--- | :--- | :--- | :--- |
| 2026-10-17 | test | Fallo en test unitario JUnit `shouldValidateValidNie` para `Y1234567S` y `Z1234567B` | El algoritmo del validador de producción (`DniNieValidator`) era 100% correcto. El error residía exclusivamente en los valores esperados del test unitario debido a un cálculo aritmético manual erróneo del módulo 23 ($11234567 \pmod{23} = 10 \rightarrow \text{'X'}$ y no 'S'; $21234567 \pmod{23} = 1 \rightarrow \text{'R'}$ y no 'B'). | Corregir los casos de prueba en `DniNieValidatorTest` con los valores oficiales calculados por el algoritmo: `Y1234567X` y `Z1234567R`. |
| 2026-10-17 | dev | `docker compose up -d` échouait avec `CommandNotFoundException` : Docker Desktop n'était pas installé sur le poste de travail. | Absence de Docker Desktop et de son daemon sous Windows / WSL2. | **Résolu :** Installation de Docker Desktop 29.8.2 avec backend WSL2. Exécution réussie de `docker compose up -d` (conteneur `seguros-postgres` démarré et healthy). Démarrage de l'application validé sous profil `dev` avec migration Flyway V1 appliquée avec succès et Hibernate `ddl-auto=validate` sans aucune anomalie. |
