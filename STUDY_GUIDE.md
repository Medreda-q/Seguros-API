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
- [`src/main/resources/application.yml`](file:///C:/dev/seguros-api/src/main/resources/application.yml) : Configuration partagée avec `spring.jpa.open-in-view=false`, `spring.jpa.hibernate.ddl-auto=validate`, Flyway activé et masquage des traces de pile d'erreur (`server.error.include-stacktrace=never`).
- [`src/main/resources/application-dev.yml`](file:///C:/dev/seguros-api/src/main/resources/application-dev.yml) & [`src/main/resources/application-test.yml`](file:///C:/dev/seguros-api/src/main/resources/application-test.yml) : Profils étanches avec parité stricte dev/test (100% PostgreSQL).
- [`src/main/resources/db/migration/V1__initial_schema.sql`](file:///C:/dev/seguros-api/src/main/resources/db/migration/V1__initial_schema.sql) : Script SQL DDL initial créant les tables `clientes`, `polizas` et `siniestros`.

---

### Étape 1.3 : Entités JPA & Modélisation du Domaine Métier

#### Ce que nous avons construit :
- [`src/main/java/com/seguros/api/entity/BaseEntity.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/BaseEntity.java) : Classe `@MappedSuperclass` factorisant `created_at` et `updated_at` en UTC.
- [`src/main/java/com/seguros/api/entity/Cliente.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Cliente.java) : Entité JPA du client avec `equals/hashCode` basés sur `instanceof` (compatible proxys Hibernate) et la clé métier `dniNie`.
- [`src/main/java/com/seguros/api/entity/Poliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Poliza.java) : Entité du contrat liée à `Cliente` avec `equals/hashCode` sur `numero`.
- [`src/main/java/com/seguros/api/entity/Siniestro.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/Siniestro.java) : Entité du sinistre liée à `Poliza` avec `equals/hashCode` sur `numero`.
- Énumérations typées : [`TipoPoliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/TipoPoliza.java), [`EstadoPoliza.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/EstadoPoliza.java), [`EstadoSiniestro.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/entity/enums/EstadoSiniestro.java).

#### Concepts Fondamentaux : La vérité sur le N+1, Open Session In View (OSIV) et les Proxys Hibernate
1. **Pourquoi `FetchType.LAZY` seul n'élimine PAS le problème N+1 :**
   - Mettre une relation en `LAZY` retarde simplement le chargement : au lieu de charger les enfants lors du `SELECT` initial, Hibernate crée un proxy non initialisé.
   - Si ton code itère ensuite sur une liste de 100 entités et appelle `poliza.getCliente().getNombre()`, Hibernate va exécuter **1 requête à chaque itération de la boucle**, soit 100 requêtes unitaires supplémentaires : c'est précisément le problème N+1 !
   - **Les vraies solutions :**
     - **`JOIN FETCH` en JPQL :** `SELECT p FROM Poliza p JOIN FETCH p.cliente WHERE p.estado = 'ACTIVA'`. Hibernate génère une seule requête SQL avec un `INNER JOIN`, récupérant d'un coup les polices et leurs clients.
     - **`@EntityGraph` de Spring Data JPA :** Permet de déclarer dynamiquement sur une méthode de repository quelles relations doivent être jointes pour une requête donnée, sans réécrire tout le JPQL.
2. **Pourquoi `spring.jpa.open-in-view=false` est obligatoire :**
   - Par défaut, Spring Boot active l'anti-pattern *Open Session in View* (OSIV), gardant la session Hibernate ouverte jusqu'au rendu de la vue ou la sérialisation JSON du contrôleur. Cela masque les problèmes N+1 en permettant des requêtes SQL tardives pendant la génération du JSON HTTP !
   - Avec `open-in-view=false`, la session se ferme dès que la méthode `@Transactional` du Service se termine. Si un développeur oublie un `JOIN FETCH`, Hibernate lève immédiatement une `LazyInitializationException` dans les tests, forçant une conception propre et performante.
3. **Proxys Hibernate & `instanceof` dans `equals()` :**
   - Lorsqu'une entité est chargée en `LAZY`, Hibernate ne renvoie pas une instance directe de `Cliente`, mais une sous-classe dynamique générée à l'exécution (un proxy ByteBuddy du type `Cliente$HibernateProxy$xxx`).
   - Si `equals()` vérifie `this.getClass() == o.getClass()`, la comparaison renverra **toujours `false`** ! L'utilisation de `!(o instanceof Cliente other)` et l'accès via le getter `other.getDniNie()` garantissent que la comparaison fonctionne parfaitement, qu'il s'agisse de l'objet réel ou de son proxy.

---

### Étape 1.4 : Validation Algorithmique DNI / NIE (Modulo 23)

#### Ce que nous avons construit :
- [`src/main/java/com/seguros/api/validator/ValidDniNie.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/ValidDniNie.java) : Annotation personnalisée Bean Validation (`jakarta.validation.Constraint`).
- [`src/main/java/com/seguros/api/validator/DniNieValidator.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/DniNieValidator.java) : Validateur exécutant l'algorithme officiel modulo 23 avec normalisation préalable (suppression des espaces/tirets et passage en majuscules).
- [`src/test/java/com/seguros/api/validator/DniNieValidatorTest.java`](file:///C:/dev/seguros-api/src/test/java/com/seguros/api/validator/DniNieValidatorTest.java) : 30 tests unitaires JUnit 5 couvrant tous les cas.

---

### Étape 1.5 : DTOs (Records Java), Protection Mass Assignment & Masquage PII

#### Ce que nous avons construit :
1. **Records Java pour les requêtes (séparation Create vs Update) :**
   - [`CreateClienteRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/CreateClienteRequest.java) : `@NotBlank`, `@ValidDniNie`, `@Email`, `@Past`, `@Pattern` téléphone espagnol (`^[6789][0-9]{8}$`), IBAN espagnol (`^ES[0-9]{22}$`).
   - [`UpdateClienteRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/UpdateClienteRequest.java) : Données de contact et IBAN (DNI et date de naissance immuables).
   - [`CreatePolizaRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/CreatePolizaRequest.java) : Validation de niveau classe `@ValidDateRange` garantissant `fechaFin > fechaInicio`.
   - [`UpdatePolizaEstadoRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/UpdatePolizaEstadoRequest.java) : Transition contrôlée d'état.
   - [`CreateSiniestroRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/CreateSiniestroRequest.java) : `@Size(10, 1000)`, `@PastOrPresent`, `BigDecimal` avec `@Positive` et `@Digits(integer=10, fraction=2)`.
   - [`UpdateSiniestroEstadoRequest.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/request/UpdateSiniestroEstadoRequest.java) : Avancement du statut du sinistre.
2. **Records Java pour les réponses & Masquage :**
   - [`ClienteResponse.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/response/ClienteResponse.java) : Masquage automatique du DNI (`*****678Z`) et de l'IBAN (`ES12 **** **** 3456`).
   - [`PolizaResponse.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/response/PolizaResponse.java) : Aplatissement du client (`clienteId`, `clienteNombreCompleto`).
   - [`SiniestroResponse.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/response/SiniestroResponse.java) : Aplatissement de la police (`polizaId`, `polizaNumero`).
3. **Composants d'infrastructure DTO :**
   - [`MaskingUtils.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/util/MaskingUtils.java) : Utilitaire centralisé de masquage sécurisé (PII / Banking).
   - [`ValidDateRange.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/ValidDateRange.java) & [`DateRangeValidator.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/DateRangeValidator.java) : Validation de période.
   - [`EntityDtoMapper.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/dto/mapper/EntityDtoMapper.java) : Mapping écrit à la main, pur et transparent (sans framework magique).
4. **Suite de tests unitaires :**
   - 76 tests unitaires JUnit 5 couvrant la validation des champs obligatoires, textes trop longs, formats invalides, inversion de dates, masquage et mapping.

---

### Concepts Fondamentaux de l'Étape 1.5

#### 1. Qu'est-ce que la faille de Mass Assignment (OWASP API3:2023 - Broken Object Property Level Authorization) ?
- **Le danger en français simple :**
  Imagine que ton contrôleur REST reçoive directement l'entité JPA `Poliza` dans le corps HTTP :
  ```java
  @PostMapping("/polizas")
  public Poliza createPoliza(@RequestBody Poliza poliza) { return repo.save(poliza); }
  ```
  Un attaquant observe la requête normale : `{"tipo": "AUTO", "clienteId": 1}`.
  Il décide alors d'injecter des propriétés internes dans son JSON :
  ```json
  {
    "tipo": "AUTO",
    "clienteId": 1,
    "primaAnual": 1.00,
    "estado": "ACTIVA",
    "numero": "POL-PIRATEE-001"
  }
  ```
  Si l'application désérialise aveuglément le JSON dans l'entité, l'attaquant **impose son propre tarif (1 € au lieu de 750 €)** ou change le statut de son contrat !
- **La protection par DTO (Data Transfer Object) :**
  Dans `CreatePolizaRequest`, les champs `id`, `estado`, `primaAnual`, `numero` et dates d'audit **n'existent tout simplement pas**.
  Même si l'attaquant les envoie dans son JSON, le désérialiseur Jackson les ignore royalement. C'est le serveur — et lui seul — qui calcule la prime et génère le numéro selon les règles métier.

#### 2. Pourquoi utiliser les `record` Java pour les DTOs ?
Introduits en Java 16, les `records` sont des conteneurs de données immuables :
- **Immuabilité garantie :** Tous les champs sont `private final`. Une fois instancié à la réception HTTP, le DTO ne peut plus être altéré par inadvertance.
- **Concision extrême :** Le compilateur génère automatiquement le constructeur canonique, les accesseurs (ex. `request.dniNie()`), `equals()`, `hashCode()` et `toString()`, évitant les annotations verbeuses type Lombok.
- **Sérialisation optimisée :** Jackson sait sérialiser et désérialiser nativement les records via leur constructeur canonique sans passer par des setters mutables.

#### 3. Masquage PII et RGPD (Privacy by Design)
- Un client ou un agent consultant une police n'a pas besoin de voir l'intégralité du compte bancaire ou du DNI.
- L'exposition complète de l'IBAN dans les réponses d'API enfreint le principe de minimisation des données (RGPD art. 5c).
- Le masquage (`ES12 **** **** 3456` et `*****678Z`) permet à l'utilisateur de vérifier qu'il s'agit du bon compte tout en neutralisant le risque de vol de données en cas d'interception ou de fuite d'écran.

---

### Questions d'entretien technique (Étape 1.5)

1. **Pourquoi sépare-t-on `CreateRequest` et `UpdateRequest` au lieu d'utiliser un seul DTO ?**
   - *Réponse :* Les règles de validation et le cycle de vie diffèrent : lors de la création d'un client, son DNI et sa date de naissance sont obligatoires ; lors d'une mise à jour de coordonnées, ces identifiants légaux sont immuables et ne doivent pas pouvoir être modifiés via l'API.

2. **Quelle est la différence entre `@NotNull`, `@NotEmpty` et `@NotBlank` ?**
   - *Réponse :*
     - `@NotNull` : Rejette uniquement `null` (accepte une chaîne vide `""` ou des espaces `" "`).
     - `@NotEmpty` : Rejette `null` et les chaînes de longueur 0 `""` (mais accepte des espaces `" "`).
     - `@NotBlank` : Le plus strict, rejette `null`, les chaînes vides `""` et les chaînes ne contenant que des espaces blancs `"   "`.

3. **Pourquoi applique-t-on `@Digits(integer = 10, fraction = 2)` en plus de `@Positive` sur un montant monétaire ?**
   - *Réponse :* `@Positive` vérifie que le nombre est $> 0$, mais n'empêche pas un utilisateur d'envoyer $12.34567$ ou un nombre avec 50 chiffres. `@Digits` impose la conformité comptable (2 décimales pour les centimes) et protège la base de données contre les dépassements de capacité sur les colonnes `NUMERIC(12, 2)`.

4. **Pourquoi préfère-t-on un mapping écrit à la main au démarrage d'un projet plutôt qu'un outil de reflection dynamique comme ModelMapper ?**
   - *Réponse :* Les frameworks dynamiques utilisent l'introspection (reflection), ce qui ralentit l'exécution, masque les erreurs à la compilation (découverte des bugs au runtime) et peut déclencher accidentellement le chargement paresseux de collections JPA entières. Un mapper écrit à la main est vérifié à la compilation, ultra-performant et totalement transparent.

5. **Comment valide-t-on une règle métier portant sur deux champs interdépendants (ex. date de fin postérieure à date de début) ?**
   - *Réponse :* Les annotations de champ standard (`@Future`, `@Past`) ne comparent les champs qu'à l'instant présent. Pour comparer deux champs entre eux, on crée une annotation de validation au niveau de la classe/record (`ElementType.TYPE`), où le validateur a accès à l'instance complète du DTO.

---

### Exercices pratiques pour toi

1. **Exercice 1 (Sécurité & Mass Assignment) :**
   - Si un attaquant envoie la requête suivante à `POST /api/v1/siniestros` :
     `{"polizaId": 1, "descripcion": "Accidente en parking", "fechaOcurrencia": "2026-05-10", "importeReclamado": 500.00, "estado": "APROBADO"}`
   - Explique pourquoi le champ `"estado": "APROBADO"` sera totalement inopérant et dans quel état réel le sinistre sera créé en base de données.
2. **Exercice 2 (Conception d'annotations) :**
   - Observe l'interface [`DateRangeAware.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/DateRangeAware.java) et [`DateRangeValidator.java`](file:///C:/dev/seguros-api/src/main/java/com/seguros/api/validator/DateRangeValidator.java).
   - Si nous devions plus tard ajouter un DTO `CampaignPromotionRequest` ayant aussi `fechaInicio` et `fechaFin`, que suffit-il de faire pour réutiliser la validation sans écrire une seule ligne de code de validation supplémentaire ?
