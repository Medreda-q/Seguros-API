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

### Concepts Fondamentaux de l'Étape 1.2

#### 1. Parité Dev / Test : Le piège de la base en mémoire H2
Pourquoi avoir banni H2 au profit d'un vrai PostgreSQL pour les tests ?
- **Différences de dialecte SQL :** H2 et PostgreSQL n'ont pas les mêmes fonctions natives (ex: gestion des types JSONB, séquences IDENTITY, fonctions de date `INTERVAL`, contraintes partielles).
- **Sensibilité à la casse et verrous :** La gestion des transactions, des niveaux d'isolation (`READ COMMITTED` vs `SERIALIZABLE`) et du verrouillage pessimiste (`SELECT ... FOR UPDATE`) diffère radicalement entre un moteur en mémoire monothreadé comme H2 et un SGBD distribué comme PostgreSQL.
- **Principe des 12-Factor Apps :** Le principe de *Dev/Prod Parity* stipule que les environnements de développement, de test et de production doivent utiliser des dépendances sous-jacentes strictement identiques pour éliminer la classe d'anomalies "ça marchait en local avec H2, mais ça crashe en recette sur PostgreSQL".

#### 2. Sécurité & Intégrité : `ddl-auto=validate` vs `ddl-auto=update`
Dans Hibernate / JPA :
- `ddl-auto=create` ou `update` délègue à l'ORM le droit de générer ou modifier les tables. En environnement professionnel ou certifié sécurité, **ce mode est proscrit** :
  - Il ne supprime jamais les colonnes orphelines (il accumule la dette technique).
  - Il génère des contraintes et des index avec des noms aléatoires (`UK_xxxxxxxxxxx`).
  - Il peut bloquer la base en posant des verrous exclusifs sans préavis.
- `ddl-auto=validate` inverse le rapport de force : **Flyway est l'unique source de vérité du schéma**. Au démarrage de Spring Boot :
  1. Flyway applique les scripts de migration (`V1__...`, `V2__...`).
  2. Hibernate inspecte les tables physiques et les compare aux métadonnées des classes Java `@Entity`.
  3. Si la moindre colonne, type ou contrainte ne correspond pas exactement, l'application refuse de démarrer avec une `SchemaManagementException`.

---

### Questions d'entretien technique (Étape 1.2)

1. **À quoi sert la table `flyway_schema_history` générée par Flyway ?**
   - *Réponse :* Cette table stocke la somme de contrôle (checksum SHA-256), la version, la date d'exécution et le statut de chaque script de migration appliqué. Elle garantit qu'un script déjà exécuté n'a pas été altéré a posteriori et empêche l'exécution de migrations dans le désordre.

2. **Pourquoi l'option `server.error.include-stacktrace: never` est-elle une recommandation de sécurité OWASP ?**
   - *Réponse :* Afficher la stack trace dans une réponse HTTP 500 divulgue des informations architecturales sensibles (OWASP A05 - Security Misconfiguration / Information Disclosure) : versions exactes des librairies, noms de packages internes, structures de requêtes SQL ou chemins de fichiers sur le serveur, ce qui facilite grandement le travail d'un attaquant.

3. **Quelle est l'utilité des scripts Maven Wrapper (`mvnw` / `mvnw.cmd`) dans un projet d'équipe ?**
   - *Réponse :* Ils permettent à n'importe quel développeur ou serveur d'intégration continue (CI/CD) de compiler le projet sans installer Maven au préalable sur le système hôte, garantissant que tout le monde utilise exactement la même version de Maven (ici 3.9.9).

4. **Pourquoi avons-nous configuré `ON DELETE RESTRICT` sur nos clés étrangères plutôt que `CASCADE` ?**
   - *Réponse :* En domaine bancaire et d'assurance, la suppression en cascade est dangereuse : supprimer un client par erreur détruirait l'historique de ses polices et de ses sinistres. `RESTRICT` empêche formellement la suppression d'un client tant qu'il possède des polices rattachées, préservant l'intégrité référentielle et les pistes d'audit.

5. **Pourquoi la colonne de montant est-elle typée `NUMERIC(12, 2)` sous PostgreSQL et non `FLOAT` ou `REAL` ?**
   - *Réponse :* En SQL, `NUMERIC` (ou `DECIMAL`) est un type à virgule fixe à précision exacte, mappé nativement vers `java.math.BigDecimal`. Les types `FLOAT` et `REAL` sont approximatifs et soufriraient des mêmes erreurs d'arrondi binaire que `double` en Java.

---

### Exercices pratiques pour toi

1. **Exercice 1 (Inspection Flyway) :**
   - Ouvre le fichier [`V1__initial_schema.sql`](file:///C:/dev/seguros-api/src/main/resources/db/migration/V1__initial_schema.sql).
   - Identifie les 3 contraintes de vérification (`CHECK`) appliquées sur la table `polizas` et explique le rôle défensif de chacune en cas d'insertion SQL directe frauduleuse.
2. **Exercice 2 (Parité et isolation) :**
   - Pourquoi est-il indispensable que la base de test `seguros_test_db` soit distincte de la base de développement `seguros_db` lorsque les développeurs lancent la commande `mvn test` ? Quels risques ferait courir une base partagée unique ?
