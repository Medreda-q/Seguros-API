# AGENTS.md - SegurosAPI Project Context & Agent Memory

> **Note pour l'agent IA :** Ce document est la source de vérité absolue sur l'architecture, les règles de travail, les contraintes techniques et l'avancement du projet. Lis-le au début de chaque nouvelle conversation pour reprendre immédiatement le travail sans poser de questions redondantes.

---

## 1. Profil du Projet & Collaboration
- **Projet :** SegurosAPI — API REST sécurisée de gestion d'assurances en Espagne (Auto, Hogar, Vida).
- **Utilisateur :** Étudiant en cybersécurité avec bases en Java.
- **Rôle de l'agent :** Mentor en pair programming, bienveillant, pédagogique et rigoureux sur les bonnes pratiques et la sécurité applicative.
- **Langue des échanges :** **Français** simple et clair pour toutes les explications et réponses.
- **Langue du domaine métier :** **Espagnol** pour la documentation métier (`docs/documento_funcional.md`), les commentaires de code et les libellés de domaine.

---

## 2. Règles d'Or Impératives (Ne jamais enfreindre)
1. **PAS DE `git push`** : L'agent fait les commits locaux, **mais l'utilisateur effectue lui-même les `git push`** (*"tu commits, je pousse"*).
2. **JAMAIS de suppression sauvage** : Ne jamais supprimer un fichier sans le lister préalablement à l'utilisateur et obtenir sa confirmation.
3. **Arrêt après chaque étape** : Ne jamais enchaîner plusieurs étapes d'un coup. S'arrêter à la fin de l'étape demandée et attendre la validation explicite.
4. **Un commit par étape** : Chaque étape validée produit un commit Git clair et documenté.
5. **Zéro base H2 ou base embarquée** : Parité stricte entre dev et test. Tous les tests tournent contre le vrai conteneur PostgreSQL (`seguros_test_db`). Utiliser systématiquement `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`.
6. **Pas de MapStruct ni d'automapping opaque** : Tous les mappers sont écrits à la main dans `com.seguros.api.dto.mapper`.
7. **`java.time.Clock` injecté obligatoirement** : Ne JAMAIS utiliser `LocalDate.now()` directement. Toujours injecter un bean `Clock` configuré sur `ZoneId.of("Europe/Madrid")` pour garantir des tests déterministes.
8. **Gestion monétaire stricte** : `BigDecimal` avec `RoundingMode.HALF_UP` et échelle à 2 décimales pour toute somme d'argent. Ne jamais utiliser `float` ou `double`.
9. **JPA & Hibernate Proxy-safe & Contraintes DB** :
   - `spring.jpa.open-in-view=false` dans toute l'application.
   - `spring.jpa.hibernate.ddl-auto=validate` (aucune création dynamique de schéma par Hibernate, Flyway fait foi).
   - Les méthodes `equals()` et `hashCode()` des entités doivent utiliser `if (!(o instanceof Entity other)) return false;` (compatible avec les proxys Hibernate) et se baser sur une clé métier unique (`dniNie`, `numero`).
   - **Tests de contraintes DB (`UNIQUE`, index partiel)** : Toujours utiliser `repository.saveAndFlush()` et attendre `DataIntegrityViolationException`. `save()` seul retarde l'envoi SQL (write-behind), et `TestEntityManager.persistAndFlush()` by-passerait la traduction Spring Data en exposant l'exception brute Hibernate `ConstraintViolationException`.

---

## 3. Stack Technique & Environnement
- **Java :** 17 (OpenJDK).
- **Framework :** Spring Boot 3.3.4 (Spring Data JPA, Bean Validation, Spring Web).
- **Build Tool :** Maven Wrapper (`.\mvnw.cmd`).
- **Base de données :** PostgreSQL 16 Alpine via Docker Compose (`docker compose up -d`).
  - Port : `5432`
  - Base dev : `seguros_db` (user: `seguros_admin`, pass: `seguros_secret`)
  - Base test : `seguros_test_db` (user: `seguros_admin`, pass: `seguros_secret`)
- **Migrations :** Flyway (`db/migration/V1__initial_schema.sql`, `V2__sequences_and_constraints.sql`).

---

## 4. Règles Métier Clés (Spécifications fonctionnelles)

### 4.1. Protection Mass Assignment & PII (Étape 1.5)
- Les DTOs d'entrée (`*CreateRequest`, `*UpdateRequest`) sont des **records Java** et ne contiennent JAMAIS : `id`, `estado`, `primaAnual`, `numero`, ni dates d'audit (`createdAt`, `updatedAt`). Ces champs sont calculés/imposés par le serveur.
- Les DTOs de sortie (`*Response`) ne renvoient JAMAIS les entités JPA directement et masquent les données sensibles PII :
  - DNI/NIE masqué : `*****678Z`
  - IBAN masqué : `ES12 **** **** 3456`

### 4.2. Règle 4.4.1 : Unicité d'une police ACTIVA par type et par client
- Un client ne peut avoir qu'au maximum **UNE SEULE** police `ACTIVA` pour un même type (`AUTO`, `HOGAR`, `VIDA`).
- **Double protection :**
  1. Vérification applicative en service : `polizaRepository.existsByClienteIdAndTipoAndEstado(...)`.
  2. Index partiel unique PostgreSQL (Migration V2) pour éliminer les race conditions :
     ```sql
     CREATE UNIQUE INDEX uq_polizas_activa_cliente_tipo ON polizas (cliente_id, tipo) WHERE estado = 'ACTIVA';
     ```
  3. L'exception `DataIntegrityViolationException` levée en cas de collision concurrente doit être traduite en HTTP `409 Conflict`.

### 4.3. Règle 4.4.2 : Date de début de police
- `fechaInicio >= LocalDate.now(clock)` : aujourd'hui est autorisé, date passée refusée (HTTP `400 Bad Request`).
- `fechaFin > fechaInicio`.

### 4.4. Règle 4.5 : Ordre strict des vérifications pour déclarer un sinistre
Toute déclaration de sinistre doit respecter scrupuleusement l'ordre de validation suivant :
1. La police existe en base -> Sinon **404 Not Found**.
2. La police est à l'état `ACTIVA` -> Sinon **409 Conflict** (*"Solo se pueden registrar siniestros en pólizas activas"*).
3. La police n'est pas expirée (`fechaFin >= LocalDate.now(clock)`) -> Sinon **409 Conflict** (*"La póliza ha expirado"*).
4. `fechaOcurrencia` est comprise dans la période de validité `[fechaInicio, fechaFin]` -> Sinon **400 Bad Request**.
5. `fechaOcurrencia <= LocalDate.now(clock)` (pas de sinistre dans le futur) -> Sinon **400 Bad Request**.

### 4.5. Numérotation atomique (Migration V2)
- Formats : `POL-{TIPO}-{ANIO}-{0001}` et `SIN-{ANIO}-{0001}`.
- Générés obligatoirement via les séquences PostgreSQL `seq_poliza_numero` et `seq_siniestro_numero` (`SELECT nextval(...)`), évitant toute condition de course.

---

## 5. État d'Avancement des Étapes

| Étape | Statut | Description | Commit |
| :--- | :---: | :--- | :--- |
| **1.1** | ✅ Fait | Spécification fonctionnelle (`documento_funcional.md`) & registre `BUGS.md` | `ab758ef` |
| **1.2** | ✅ Fait | Infrastructure Docker Compose, profils Spring Boot `dev`/`test`, Flyway V1 | `d144c52` |
| **1.3** | ✅ Fait | Entités JPA (`Cliente`, `Poliza`, `Siniestro`, `BaseEntity`), enums | `19ba8bf` |
| **1.4** | ✅ Fait | Validateur DNI/NIE (algorithme modulo 23, support NIE X/Y/Z), 30 tests JUnit 5 | `cef44fa` |
| **1.5** | ✅ Fait | DTOs records Java, mappers manuels, masquage PII, validation Bean | `54bf0e8` |
| **1.6** | ✅ Fait | Repositories JPA (`@EntityGraph`, `JOIN FETCH`), Flyway V2 (séquences + index partiel), tests `@DataJpaTest` sur PostgreSQL réel (73 tests passés) | `eb0d259` |
| **1.7** | ✅ Fait | Services métier (`ClienteService`, `PolizaService`, `SiniestroService`, `CalculadoraPrimaService`), bean `Clock` injecté, transactions `@Transactional`, séquences, 131 tests unitaires et intégration passés | `db961e8` |
| **1.8** | ⏳ À faire | Contrôleurs REST, ProblemDetail RFC 7807, Swagger/OpenAPI | - |
| **1.9** | ⏳ À faire | Tests d'intégration de bout en bout (`@SpringBootTest`, MockMvc) & Postman | - |
| **1.10**| ⏳ À faire | Documentation finale (`README.md`, `STUDY_GUIDE.md`) | - |

---

## 6. Commandes Utiles
- Lancer les conteneurs : `docker compose up -d`
- Vérifier les conteneurs : `docker ps`
- Exécuter tous les tests : `.\mvnw.cmd test`
- Démarrer l'application en dev : `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`
