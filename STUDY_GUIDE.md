# STUDY GUIDE - SegurosAPI

Ce guide d'étude accompagne chaque étape du projet. Il est conçu pour consolider tes connaissances en ingénierie logicielle backend Java et en développement sécurisé.

---

## Phase 1 : Core API Fonctionnelle

### Étape 1.1 : Spécification Fonctionnelle & Précision Monétaire

#### Ce que nous avons construit :
- [`docs/documento_funcional.md`](file:///C:/dev/seguros-api/docs/documento_funcional.md) : Cahier des charges fonctionnel complet en espagnol définissant le périmètre de la Phase 1 (Cliente, Poliza, Siniestro), les règles de tarification de la prime annuelle avec tests aux limites, les matrices de transition d'état et le catalogue d'endpoints REST.
- [`BUGS.md`](file:///C:/dev/seguros-api/BUGS.md) : Registre officiel des anomalies pour documenter méthodiquement chaque bogue (date, environnement, symptôme, cause racine, correction).
- [`.gitignore`](file:///C:/dev/seguros-api/.gitignore) : Filtrage strict des fichiers générés (`target/`), configurations locales sensibles (`.env`, `application-local.yml`) et métadonnées d'IDE (Eclipse, VS Code, IntelliJ).

---

### Concept Fondamental : Pourquoi `BigDecimal` et jamais `double` / `float` pour la monnaie ?

En Java (et dans la plupart des langages), les types primitifs `float` et `double` sont conformes à la norme **IEEE 754** (représentation en virgule flottante binaire).

#### 1. Le problème de la base 2 vs base 10
Dans notre système décimal (base 10), une fraction comme $1/10 = 0.1$ s'écrit de manière finie. Mais en binaire (base 2), les puissances négatives sont $1/2 = 0.5$, $1/4 = 0.25$, $1/8 = 0.125$, etc. 
Le nombre décimal `0.1` ne peut **jamais** être représenté exactement par une somme finie de puissances de 2 : c'est un nombre périodique infini en binaire (`0.0001100110011...`).
L'ordinateur est donc obligé de tronquer la valeur :
```java
double a = 0.1;
double b = 0.2;
System.out.println(a + b); // Affiche 0.30000000000000004 !
```

#### 2. L'impact en milieu financier et assurantiel
- **Accumulation d'erreurs :** Sur des millions de calculs de primes, d'intérêts ou d'indemnisations de sinistres, ces millièmes de centimes s'accumulent et créent des déséquilibres comptables majeurs.
- **Échec des tests d'égalité :** Une condition telle que `if (montantPaye == montantReclame)` échouera aléatoirement à cause d'un reliquat de $0.0000000000000001$.
- **Responsabilité légale :** Les organismes de régulation financière (Banque d'Espagne, ACPR, BCE) exigent une comptabilité déterministe et exacte au centime près.

#### 3. La solution : `BigDecimal`
`BigDecimal` représente un nombre décimal sous forme d'un entier non borné (`BigInteger`) accompagné d'une échelle (`scale`, le nombre de décimales) :
$$\text{Valeur} = \text{unscaledValue} \times 10^{-\text{scale}}$$
- `BigDecimal("500.00")` représente exactement $50000 \times 10^{-2}$.
- `RoundingMode.HALF_UP` applique la règle d'arrondi commerciale classique ("arrondi au plus proche, ou au supérieur si la fraction suivante est $\ge 0.5$").
> **Règle d'or :** Instanciez toujours `BigDecimal` à partir d'un `String` ou via `BigDecimal.valueOf(...)`, jamais via `new BigDecimal(double)`. Exemple : `new BigDecimal(0.1)` produira `0.1000000000000000055511151231257827021181583404541015625`.

---

### Questions d'entretien technique (Junior Backend & Sécurité)

1. **Pourquoi utilise-t-on `Period.between(date1, date2).getYears()` plutôt que de diviser les millisecondes par 365.25 ?**
   - *Réponse :* Les années ont une durée variable (années bissextiles) et les fuseaux horaires comportent des ajustements d'heure d'été/hiver. L'API `java.time` gère précisément le calendrier grégorien sans dérive de calcul calendaire.

2. **Qu'est-ce qu'une faille de type Mass Assignment et comment les DTOs la neutralisent-ils ?**
   - *Réponse :* Si un contrôleur reçoit directement une entité JPA, un attaquant peut ajouter des champs inattendus dans le JSON (ex: `{"id": 1, "estado": "APROBADO"}`). Les DTOs (Data Transfer Objects) n'exposent que les champs strictement autorisés à la saisie, ignorant toute charge utile excédentaire.

3. **Quelle est la différence entre `ddl-auto=update` et `ddl-auto=validate` dans Hibernate ?**
   - *Réponse :* `update` modifie automatiquement les tables existantes (dangereux en prod car non audité et risque de blocage de verrous). `validate` vérifie uniquement au démarrage que le schéma existant (créé par Flyway) correspond exactement aux entités Java, et lève une exception s'il y a un décalage.

4. **Pourquoi sépare-t-on les fichiers `.env` et `application-local.yml` du dépôt Git ?**
   - *Réponse :* Pour éviter la divulgation d'identifiants sensibles (mots de passe BDD, clés secrètes d'API, secrets JWT) dans l'historique Git (OWASP A07 - Identification and Authentication Failures).

5. **Pourquoi la validation d'entrées (ex: format DNI/NIE) doit-elle s'effectuer côté serveur avec Bean Validation alors que le frontend peut déjà la faire ?**
   - *Réponse :* Les validations côté client ne sont que de l'ergonomie utilisateur et sont trivialement contournées via des outils comme curl, Postman ou Burp Suite. La sécurité applicative impose de considérer toute entrée réseau comme potentiellement hostile (principe de défense en profondeur).

---

### Exercices pratiques pour toi

1. **Exercice 1 (Calcul mental & Algorithme DNI) :**
   - Prends le DNI fictif suivant : `12345678`.
   - Effectue l'opération $12345678 \pmod{23}$.
   - À l'aide de la chaîne `TRWAGMYFPDXBNJZSQVHLCKE` (index de 0 à 22), trouve la lettre officielle de contrôle.
2. **Exercice 2 (Cas limites) :**
   - Si un client est né le `2001-10-18` et que la police d'assurance prend effet le `2026-10-17` : quel est son âge exact selon `Period.between` ?
   - Quel facteur pour une police AUTO s'applique (1.50 ou 1.00) ? Que se passe-t-il le lendemain (le `2026-10-18`) ?
