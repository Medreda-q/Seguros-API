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
- [`src/test/java/com/seguros/api/validator/DniNieValidatorTest.java`](file:///C:/dev/seguros-api/src/test/java/com/seguros/api/validator/DniNieValidatorTest.java) : 30 tests unitaires JUnit 5 couvrant tous les cas (valides, invalides, minuscules, espaces, tirets, longueurs erronées, caractères interdits et valeur `null`).

---

### Concepts Fondamentaux de l'Étape 1.4

#### 1. L'Algorithme Modulo 23 en français simple
En Espagne, le Ministère de l'Intérieur utilise un système de clé de contrôle mathématique pour vérifier qu'un numéro de document d'identité n'a pas été falsifié ou mal recopié.

La table officielle comporte **23 lettres** (les lettres I, Ñ, O, U sont exclues pour éviter les confusions visuelles avec 1, N, 0 ou V) :
```text
Index :  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22
Lettre : T  R  W  A  G  M  Y  F  P  D  X  B  N  J  Z  S  Q  V  H  L  C  K  E
```

##### A) Cas du DNI (Citoyens espagnols) :
- Le format se compose de **8 chiffres suivis d'1 lettre**.
- On prend le nombre formé par les 8 chiffres et on calcule le reste de sa division euclidienne par 23 :
  $$\text{reste} = \text{nombre} \pmod{23}$$
- La lettre située à l'index `reste` dans la table doit correspondre exactement à la lettre du DNI.
- *Exemple :* Pour le DNI `12345678Z` :
  $$12345678 = 23 \times 536768 + 14$$
  Le reste est **14**. À l'index 14 de la table se trouve la lettre **Z**. Le DNI est donc valide.

##### B) Cas du NIE (Résidents étrangers) :
- Le format se compose d'une **lettre initiale ('X', 'Y' ou 'Z') suivie de 7 chiffres et 1 lettre de contrôle**.
- Pour pouvoir appliquer la même formule mathématique, on remplace la lettre initiale par un chiffre :
  - **`X` devient `0`**
  - **`Y` devient `1`**
  - **`Z` devient `2`**
- On obtient alors un nombre de 8 chiffres, et on applique exactement le même calcul modulo 23.
- *Exemple 1 :* Pour le NIE `X1234567L` :
  - `X` devient `0` $\rightarrow$ le nombre est `01234567` (soit $1234567$).
  - $1234567 \pmod{23} = 19$. À l'index 19 se trouve la lettre **L**. Valide !
- *Exemple 2 :* Pour le NIE `Y1234567X` :
  - `Y` devient `1` $\rightarrow$ le nombre est `11234567`.
  - $11234567 \pmod{23} = 10$. À l'index 10 se trouve la lettre **X**. Valide !
- *Exemple 3 :* Pour le NIE `Z1234567R` :
  - `Z` devient `2` $\rightarrow$ le nombre est `21234567`.
  - $21234567 \pmod{23} = 1$. À l'index 1 se trouve la lettre **R**. Valide !

#### 2. Pourquoi `null` est considéré valide dans le validateur ?
C'est une règle d'or de la spécification **Jakarta Bean Validation** :
- **Principe de responsabilité unique (SRP) :** Un validateur de format ne doit vérifier que le format. La vérification de présence obligatoire incombe à `@NotNull` ou `@NotBlank`.
- **Composabilité :** Si un champ devient un jour optionnel dans un DTO (ex: mise à jour partielle où l'utilisateur ne transmet pas le DNI), l'annotation `@ValidDniNie` seule n'empêchera pas la requête d'aboutir si la valeur est absente (`null`).
- Pour rendre le champ obligatoire ET valide, on combine simplement les deux annotations sur le DTO :
  ```java
  @NotBlank(message = "El DNI/NIE es obligatorio")
  @ValidDniNie
  private String dniNie;
  ```

---

### Questions d'entretien technique (Étape 1.4)

1. **Comment fonctionne le cycle de vie d'un `ConstraintValidator` dans Spring Boot ?**
   - *Réponse :* Le conteneur Bean Validation instancie le validateur lors de la première validation. S'il s'agit d'un composant géré par Spring (`@Component`), Spring injecte automatiquement ses dépendances. La méthode `isValid(value, context)` est ensuite invoquée à chaque validation de champ annoté.

2. **Pourquoi doit-on normaliser les entrées utilisateur (suppression d'espaces/tirets, passage en majuscules) avant de valider l'algorithme ?**
   - *Réponse :* Les utilisateurs peuvent saisir des formats variés (`12345678-Z`, `12345678 z`, `x1234567l`). La normalisation évite des faux négatifs d'ergonomie tout en garantissant que la valeur qui atteindra la base de données est propre, uniforme et canonique.

3. **Quelle est la différence entre une validation par Regex et une validation algorithmique par clé de contrôle ?**
   - *Réponse :* Une Regex valide uniquement la syntaxe et la structure (ex: 8 chiffres et 1 lettre), mais accepterait n'importe quelle fausse lettre (comme `12345678A`). La clé de contrôle calcule la relation arithmétique exacte entre les chiffres et la lettre, détectant les erreurs de frappe et les faux identifiants.

4. **Pourquoi les lettres I, Ñ, O et U sont-elles absentes de la table de contrôle DNI espagnole ?**
   - *Réponse :* Pour des raisons de lisibilité optique et de sécurité : le 'I' ressemble au chiffre 1, le 'O' ressemble au chiffre 0, le 'U' se confond avec le 'V', et le 'Ñ' n'existe pas dans tous les claviers internationaux ou systèmes informatiques anciens.

5. **Où doit se situer l'appel de Bean Validation dans une architecture REST sécurisée ?**
   - *Réponse :* Dans le contrôleur REST, via l'annotation `@Valid` sur le `@RequestBody` du DTO. Cela permet d'intercepter et de rejeter les charges utiles malformées dès la couche d'exposition HTTP (erreur 400 Bad Request) avant d'engager des transactions ou des ressources en base de données.

---

### Exercices pratiques pour toi

1. **Exercice 1 (Calcul manuel) :**
   - Soit un citoyen étranger titulaire du NIE commençant par la lettre **Y** et suivi des chiffres `9876543`.
   - Remplace la lettre Y par son équivalent numérique.
   - Calcule le reste de la division par 23 et déduis-en la lettre de contrôle officielle.
2. **Exercice 2 (Sécurité des entrées) :**
   - Pourquoi le validateur utilise-t-il `Locale.ROOT` lors de l'appel à `.toUpperCase(Locale.ROOT)` plutôt que le simple `.toUpperCase()` sans paramètre ? Quel bug de sécurité historique (notamment avec la langue turque et la lettre 'i') cela prévient-il ?
