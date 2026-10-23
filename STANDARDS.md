# Standards de projet — Goosescope

Produit en Phase 1 AFTER. Gouverne tout le code écrit à partir de la Phase 3.
Références: SPEC.md (révision 2), ARCHITECTURE.md.

---

## 1. Règles non mécanisables (revue humaine)

### 1.1 Le cœur reste pur
`:core` n'importe jamais `android.*` ni `androidx.*`. Toute logique de couleur,
de lissage, de comparaison ou de géométrie y vit. `:app` branche, n'interprète pas:
si une condition sur une couleur apparaît dans `:app`, elle est au mauvais endroit.
(Le compilateur l'impose en partie: `:core` est un module JVM sans SDK Android.)

### 1.2 Aucune image ne sort de la mémoire
Jamais d'écriture d'image (fichier, cache, MediaStore, presse-papiers, log). Jamais
de valeur de pixel dans les logs de production. L'image figée est libérée à la
reprise.

### 1.3 Les seuils sont des données nommées
Tout seuil de l'architecture (§3.4 à §3.6) est une constante nommée dans un seul
fichier `Tuning.kt` de `:core`, avec un commentaire qui renvoie à la section de
l'architecture. Pas de nombre magique dans les algorithmes. Un seuil recalibré sur
la tablette se change à cet endroit, et le changement est noté dans le message de
commit avec la mesure qui le justifie.

### 1.4 Ton des fiches
Les fiches s'adressent à quelqu'un qui n'a peut-être jamais perçu cette teinte. Elles
transmettent ce que la couleur **fait ressentir**, pas seulement à quoi elle ressemble
(révision demandée par Naomi le 2026-10-23, les premières versions étant trop
génériques):
- une émotion ou une ambiance (calme, joyeuse, sérieuse, nostalgique…);
- au moins une sensation d'un autre sens: température, toucher, goût, odeur, son;
- une situation concrète où on la ressent (un matin d'hiver, une cuisine un dimanche).
Deux ou trois phrases courtes, vocabulaire courant, sans s'appuyer seulement sur
d'autres noms de couleur. FR et EN disent la même chose, mais chaque langue est
rédigée naturellement, pas traduite mot à mot.

---

## 2. Langage et style

- Kotlin, style officiel (`kotlin.code.style=official`), indentation 4 espaces.
- Paquets: `com.morialkar.goosescope.core` et `com.morialkar.goosescope.app`.
  `ASSUMPTION`: préfixe repris d'Arquanzia (`com.morialkar.*`).
- Identifiants de code en anglais; commentaires et documents en français.
- Commentaires: expliquent **pourquoi**, jamais **quoi**. Chaque algorithme renvoie
  à sa section d'ARCHITECTURE.md (ex. `// ARCHITECTURE §3.5`).
- Interdits: `!!`, `lateinit` hors injection de test, `GlobalScope`, `runBlocking`
  dans `:app`, attraper `Exception` sans la relancer ni la convertir en état d'erreur.
- Immutabilité par défaut: `val`, `data class`, collections en lecture seule.
- Pas d'injection de dépendances par framework: construction manuelle dans
  `MainActivity` / une petite `ViewModelProvider.Factory` (l'app a un écran).

## 3. Textes et langues

- Tout texte d'interface est dans `res/values/strings.xml` (EN, défaut) et
  `res/values-fr/strings.xml`. Aucun texte affiché en dur dans le code Kotlin.
- Les textes de nuances et de comparaison viennent de `:core` (`catalog.json`,
  `ComparisonText`), qui reçoit la langue en paramètre explicite (`Lang`).
- Un test vérifie que les deux `strings.xml` ont exactement les mêmes clés.

## 4. Système visuel (NF3, NF4)

« Just flat colors, easy to see, not too bright, not too dark. »

- Aplats uniquement: pas de dégradé, pas d'ombre (`elevation = 0`), pas de flou.
  Coins arrondis modérés (12 dp) acceptés.
- Jetons de couleur (seule source de couleurs de l'interface, `ui/theme/Tokens.kt`):

| Jeton | Valeur | Usage | Contraste avec `textOnSurface` |
|---|---|---|---|
| `surface` | `#3F4752` | panneaux, fond de fiche | 8,4:1 |
| `surfaceRaised` | `#545D69` | boutons, puces | 6,0:1 |
| `textOnSurface` | `#F4F2EC` | texte sur surfaces | — |
| `reticle` | `#F4F2EC` contour + `#1F2328` contour intérieur | lisible sur toute image | — |

- Texte posé **sur un aplat de couleur mesurée**: noir `#000000` ou blanc
  `#FFFFFF`, choisi par `Contrast.textColorOn`. Raison: c'est la seule paire qui
  garantit ≥ 4,5:1 sur **toute** couleur (pire cas 4,58:1). Une paire adoucie
  (ex. `#F4F2EC` / `#1F2328`) descend à 3,8:1 sur les tons moyens. Le test de
  contraste (ARCHITECTURE §3.7) protège ce choix.
- Tailles: nom de base en direct ≥ 32 sp (cible 40 sp), nom de nuance ≥ 28 sp,
  texte de fiche ≥ 18 sp, boutons ≥ 48 dp de cible tactile.
- Mise en page adaptative: portrait → bandeau d'info en bas; paysage → panneau à
  droite. La fiche ne couvre jamais le point visé sur l'image figée.

## 5. Tests

- Toute modification de comportement arrive avec ses tests dans le même commit
  (AFTER règle 4): cas nominal, au moins un cas frontière, au moins un cas d'erreur.
- `:core`: JUnit 5 + `kotlin.test`. Nom des tests en phrase:
  `` `peach shifted toward red reads as warmer`() ``.
- `:app`: tests Compose instrumentés avec `FakeFrameSource`; jamais de dépendance à
  la vraie caméra dans un test automatisé.
- Tests manuels: protocole et résultat consignés dans `tests/field/<AC>.md`
  (date, appareil, éclairage, résultat).
- Une tâche n'est terminée qu'après exécution affichée de sa commande de
  vérification (AFTER règle 5).

## 6. Dépendances

- Permises: Kotlin stdlib, `kotlinx.serialization`, `kotlinx.coroutines`,
  bibliothèques AndroidX (CameraX, Compose, Lifecycle, Activity), JUnit 5,
  bibliothèques de test AndroidX.
- Interdites (D4): Google Play Services, ML Kit, Firebase, analytics, pub, crash
  reporting réseau, toute bibliothèque qui exige le réseau.
- Toute nouvelle dépendance hors de la liste « permises » est une `DECISION_REQUIRED`.
- Versions figées dans `gradle/libs.versions.toml`; pas de version dynamique (`+`).

## 7. Sécurité

- Manifeste: `CAMERA` est la seule permission. Un test (ou une tâche Gradle de
  vérification) échoue si `INTERNET` apparaît dans le manifeste fusionné.
- `REQUIRES_REVIEW:` en commentaire en tête de chaque fichier listé dans
  ARCHITECTURE §6.
- `android:allowBackup="false"`.

## 8. Gestion de version

- Dépôt git dans `~/Developer/goosescope`, commits directement sur `main`, auteur
  `Claude <noreply@anthropic.com>` via `git -c user.name=… -c user.email=…`
  (même pratique qu'Arquanzia).
- Dépôt distant **public** `Morialkar/goosescope` sur GitHub (D10). Comme il est
  public: aucun secret commité (keystore, mots de passe, `local.properties`), et
  rien de personnel sur l'utilisateur principal (documents anonymisés le
  2026-10-23 à la demande de Naomi).
- Licence: MIT (décision de Naomi, 2026-10-23), fichier `LICENSE` à la racine.
- Un commit par tâche du plan, message qui cite l'identifiant de tâche.
- `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `*.apk` ignorés.
