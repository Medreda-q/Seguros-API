# STUDY GUIDE - SegurosAPI

Ce guide d'étude accompagne chaque étape du projet. Il est conçu pour consolider tes connaissances en ingénierie logicielle backend Java et en développement sécurisé.

---

## Phase 1 : Core API Fonctionnelle

### Étape 1.1 : Spécification Fonctionnelle & Précision Monétaire

#### Ce que nous avons construit :
- [`docs/documento_funcional.md`](file:///C:/dev/seguros-api/docs/documento_funcional.md) : Cahier des charges fonctionnel complet en espagnol définissant le périmètre de la Phase 1 (Cliente, Poliza, Siniestro), les règles de tarification de la prime annuelle avec tests aux limites, les matrices de transition d'état et le catalogue d'endpoints REST.
- [`BUGS.md`](file:///C:/dev/seguros-api/BUGS.md) : Registre officiel des anomalies pour documenter méthodiquement chaque bogue (date, environnement, symptôme, cause racine, correction).
- [`.gitignore`](file:///C:/dev/seguros-api/.gitignore) : Filtrage strict des fichiers générés (`target/`), configurations locales sensibles (`.env`, `application-local.yml`) et métadonnées d'IDE (Eclipse, VS Code, IntelliJ).

#### Concept Fondamental : Pourquoi `BigDecimal` et jamais `double` / `float` pour la monnaie ?
En Java (et dans la plupart des langages), les types primitifs `float` et `double` sont conformes à la norme **IEEE 754** (représentation en virgule flottante binaire).
1. **Le problème de la base 2 vs base 10 :** Dans notre système décimal (base 10), une fraction comme $1/10 = 0.1$ s'écrit de manière finie. Mais en binaire (base 2), les puissances négatives sont $1/2 = 0.5$, $1/4 = 0.25$, $1/8 = 0.125$, etc. Le nombre décimal `0.1` ne peut **jamais** être représenté exactement par une somme finie de puissances de 2 : c'est un nombre périodique infini en binaire (`0.0001100110011...`). L'ordinateur est donc obligé de tronquer la valeur :
   ```java
   double a = 0.1;
   double b = 0.2;
   System.out.println(a + b); // Affiche 0.30000000000000004 !
   ```
2. **L'impact en milieu financier et assurantiel :**
   - **Accumulation d'erreurs :** Sur des millions de calculs de primes, d'intérêts ou d'indemnisations de sinistres, ces millièmes de centimes s'accumulent et créent des déséquilibres comptables majeurs.
   - **Échec des tests d'égalité :** Une condition telle que `if (montantPaye == montantReclame)` échouera aléatoirement à cause d'un reliquat de $0.0000000000000001$.
   - **Responsabilité légale :** Les organismes de régulation financière (Banque d'Espagne, ACPR, BCE) exigent une comptabilité déterministe et exacte au centime près.
3. **La solution : `BigDecimal` :** `BigDecimal` représente un nombre décimal sous forme d'un entier non borné (`BigInteger`) accompagné d'une échelle (`scale`, le nombre de décimales) : $\text{Valeur} = \text{unscaledValue} \times 10^{-\text{scale}}$.
   - `BigDecimal("500.00")` représente exactement $50000 \times 10^{-2}$.
   - `RoundingMode.HALF_UP` applique la règle d'arrondi commerciale classique ("arrondi au plus proche, ou au supérieur si la fraction suivante est $\ge 0.5$").
   > **Règle d'or :** Instanciez toujours `BigDecimal` à partir d'un `String` ou via `BigDecimal.valueOf(...)`, jamais via `new BigDecimal(double)`. Exemple : `new BigDecimal(0.1)` produira `0.1000000000000000055511151231257827021181583404541015625`.

---

### Étape 1.2 : Infrastructure Docker, Profils Spring, Schéma Flyway & Build Maven

#### Ce que nous avons construit :
- [`pom.xml`](file:///C:/dev/seguros-api/pom.xml) : Configuration Maven complète sous Java 17 avec Spring Boot 3.3.4, Spring Data JPA, Jakarta Bean Validation, pilote PostgreSQL, Flyway Core + PostgreSQL extension, SpringDoc OpenAPI (Swagger UI) et Testcontainers.
- [`docker-compose.yml`](file:///C:/dev/seguros-api/docker-compose.yml) & [`docker/init-databases.sql`](file:///C:/dev/seguros-api/docker/init-databases.sql) : Conteneur PostgreSQL 16 provisionnant automatiquement deux bases distinctes :
  - `seguros_db` : Base de données du profil `dev`.
  - `seguros_test_db` : Base de données dédiée aux tests d'intégration du profil `test`.
- [`src/main/resources/application.yml`](file:///C:/dev/seguros-api/src/main/resources/application.yml) : Configuration partagée avec `spring.jpa.hibernate.ddl-auto=validate`, migration Flyway automatique et masquage des traces de pile d'erreur (`server.error.include-stacktrace=never`).
- [`src/main/resources/application-dev.yml`](file:///C:/dev/seguros-api/src/main/resources/application-dev.yml) & [`src/main/resources/application-test.yml`](file:///C:/dev/seguros-api/src/main/resources/application-test.yml) : Profils étanches avec parité stricte dev/test (100% PostgreSQL).
- [`src/main/resources/db/migration/V1__initial_schema.sql`](file:///C:/dev/seguros-api/src/main/resources/db/migration/V1__initial_schema.sql) : Script SQL DDL initial créant les tables `clientes`, `polizas` et `siniestros` avec contraintes d'intégrité référentielle (`FOREIGN KEY ... ON DELETE RESTRICT`), contraintes d'unicité, contraintes de validation SQL (`CHECK`) et index de recherche.
- [`mvnw.cmd`](file:///C:/dev/seguros-api/mvnw.cmd) & [`mvnw`](file:///C:/dev/seguros-api/mvnw) : Wrapper Maven autonome préconfiguré avec détection automatique du JDK Temurin 17.

---

### Étape 1.3 : Entités JPA & Modélisation du Domaine Métier

#### Ce que nous avons construit :
- [`src/main/java/com/seguros/api/entity/BaseEntity.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/BaseEntity.java) : Classe `@MappedSuperclass` factorisant `created_at` et `updated_at` en UTC via `@PrePersist` et `@PreUpdate`.
- [`src/main/java/com/seguros/api/entity/Cliente.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Cliente.java) : Entité JPA du client (tomador) avec relation `@OneToMany` vers ses polices et égalité sur clé métier `dniNie`.
- [`src/main/java/com/seguros/api/entity/Poliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Poliza.java) : Entité du contrat d'assurance liée à `Cliente` par `@ManyToOne(fetch = FetchType.LAZY)`.
- [`src/main/java/com/seguros/api/entity/Siniestro.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Siniestro.java) : Entité de déclaration de sinistre liée à `Poliza` par `@ManyToOne(fetch = FetchType.LAZY)`.
- Énumérations typées sous `entity.enums` :
  - [`TipoPoliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/TipoPoliza.java) (`AUTO`, `HOGAR`, `VIDA`).
  - [`EstadoPoliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/EstadoPoliza.java) (`ACTIVA`, `SUSPENDIDA`, `CANCELADA`).
  - [`EstadoSiniestro.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/EstadoSiniestro.java) (`ABIERTO`, `EN_REVISION`, `APROBADO`, `RECHAZADO`).

---

### Concepts Fondamentaux de l'Étape 1.3

#### 1. Le piège du chargement : `FetchType.LAZY` vs `FetchType.EAGER`
- **Le problème :** En JPA, la spécification définit que les annotations `@ManyToOne` et `@OneToOne` ont par défaut une stratégie de chargement `EAGER` (avide/immédiate).
- **Conséquence dramatique (Le problème N+1 requêtes) :** Si tu exécutes une requête pour lister 100 sinistres, Hibernate va exécuter 1 requête pour récupérer les sinistres, puis 100 requêtes SQL supplémentaires pour récupérer les polices rattachées, et potentiellement 100 autres pour les clients !
- **La solution défensive :** Déclarer explicitement `fetch = FetchType.LAZY` sur **toutes** les relations `@ManyToOne`. Les données associées ne seront chargées que si le code appelle explicitement le getter dans une transaction active, ou lors d'une requête optimisée avec `JOIN FETCH`.

#### 2. Sécurité de typage : `@Enumerated(EnumType.STRING)`
- **Pourquoi pas le mode par défaut (`EnumType.ORDINAL`) ?** Par défaut, JPA stocke les énumérations sous forme d'entiers correspondant à leur position dans la déclaration Java (0, 1, 2...). Si plus tard un développeur insère un nouvel état (ex. `EN_ATTENTE`) au milieu de l'enum, tous les numéros sont décalés en base de données, corrompant silencieusement l'historique de production !
- **Avantage de `EnumType.STRING` :** La valeur textuelle exacte (`ACTIVA`, `SUSPENDIDA`) est enregistrée dans PostgreSQL, assurant la résilience du schéma et une parfaite lisibilité lors des audits directs sous DBeaver.

#### 3. Égalité et identité JPA : Le piège de `equals` et `hashCode`
- Utiliser l'`id` autogénéré dans `equals()` pose un problème majeur : avant que l'entité ne soit sauvegardée (`repository.save()`), son `id` est `null`. Si tu ajoutes deux nouvelles entités dans un `Set` Java, elles seront considérées comme identiques car toutes deux ont `id == null` !
- La bonne pratique consiste à baser `equals()` et `hashCode()` sur une **clé métier unique et immuable** (Business Key) : le `dniNie` pour un client, et le numéro de référence (`numero`) pour une police ou un sinistre.

---

### Questions d'entretien technique (Étape 1.3)

1. **Qu'est-ce que le problème des requêtes N+1 et comment le détecte-t-on ?**
   - *Réponse :* C'est une anomalie de performance où l'ORM exécute 1 requête initiale pour obtenir N enregistrements parents, puis N requêtes unitaires supplémentaires pour récupérer leurs enfants associés (généralement causé par un `FetchType.EAGER`). On le détecte en observant les logs SQL (`show-sql: true` ou via un profiler de requêtes) où une avalanche de requêtes `SELECT ... WHERE id = ?` apparaît.

2. **Quelle est l'utilité de l'annotation `@MappedSuperclass` par rapport à une entité classique `@Entity` ?**
   - *Réponse :* Une classe annotée `@MappedSuperclass` (comme notre `BaseEntity`) ne correspond pas à une table physique en base de données. Elle permet d'hériter des attributs communs (ici `createdAt`, `updatedAt`) et de leurs règles de cycle de vie (`@PrePersist`) sans créer de table dédiée ni de jointure d'héritage complexe.

3. **Pourquoi est-il risqué de générer automatiquement `equals()` et `hashCode()` avec tous les attributs dans une entité JPA ?**
   - *Réponse :* Cela force l'évaluation des collections et des relations paresseuses (`LAZY`), ce qui peut déclencher un chargement intempestif de graphes d'objets entiers ou lever une `LazyInitializationException` si la session Hibernate est fermée. De plus, cela peut provoquer des boucles infinies (`StackOverflowError`) en cas de relation bidirectionnelle.

4. **Quelle est la différence entre `CascadeType.REMOVE` et la clause SQL `ON DELETE CASCADE` ?**
   - *Réponse :* `CascadeType.REMOVE` est géré côté Java par Hibernate, qui charge d'abord chaque entité enfant en mémoire avant de déclencher des `DELETE` individuels. `ON DELETE CASCADE` est géré directement par le moteur de base de données PostgreSQL lors de la suppression de la ligne parente. Dans notre API, nous rejetons les deux au profit de `RESTRICT` pour protéger l'intégrité de l'historique assurantiel.

5. **Pourquoi utilise-t-on `OffsetDateTime` plutôt que `LocalDateTime` pour les horodatages techniques ?**
   - *Réponse :* `LocalDateTime` ne contient aucune information de décalage horaire ou de fuseau. En environnement distribué ou lors d'audits de sécurité, `OffsetDateTime` avec `ZoneOffset.UTC` garantit un enregistrement universel et non ambigu de l'instant exact d'un événement, insensible aux changements d'heure saisonniers.

---

### Exercices pratiques pour toi

1. **Exercice 1 (Analyse de la relation Poliza -> Cliente) :**
   - Regarde l'attribut `cliente` dans [`Poliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Poliza.java).
   - Pourquoi avons-nous combiné `@ManyToOne(fetch = FetchType.LAZY, optional = false)` avec `@JoinColumn(name = "cliente_id", nullable = false)` ? Que se passe-t-il si un développeur essaie d'instancier une police sans lui rattacher de client ?
2. **Exercice 2 (Conception d'audit) :**
   - Dans [`BaseEntity.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/BaseEntity.java), pourquoi le champ `createdAt` a-t-il l'attribut `updatable = false` ? Quel principe de sécurité cela protège-t-il ?
