# Analyse d'impact — Goosescope iOS

Phase 1 AFTER (workflow Feature), 2026-10-24. Lecture seule. Références: SPEC-IOS.md (approuvé le
2026-10-24, NG5 remplacé par « pas de distribution publique iOS »), SPEC.md, ARCHITECTURE.md,
STANDARDS.md.

---

## 1. Constats

- **Cœur**: trois usages propres à la JVM dans `core/src/main`:
  `Srgb8.toHex` (`String.format`), `ColorMath.toOklch` (`Math.toDegrees`), `Catalog.loadBundled`
  (`getResourceAsStream`, `Charsets`). Côté tests, trois fichiers lisent des ressources
  (`CatalogTest`, `BaseClassifierTest`, `ShadeMatcherTest`). Tout le reste est du Kotlin commun.
- **SDK**: Xcode 27, SDK iOS 27. `LockedCameraCapture` (iOS 18+) fournit
  `LockedCameraCaptureExtension` et `LockedCameraCaptureUIScene<Content: View>`; AppIntents fournit
  `CameraCaptureIntent`. F33 est donc faisable: une extension qui affiche une interface SwiftUI
  par-dessus l'écran verrouillé, déclenchée par un contrôle.
- **Outils**: XcodeGen 2.46 déjà installé; aucun runtime de simulateur iOS; iphone nao appairé.

## 2. Choix techniques (alternatives et raisons)

### 2.1 Cœur multiplateforme
| Option | Pour | Contre |
|---|---|---|
| **Plugin Kotlin Multiplatform sur `:core`** (retenu) | Même code et mêmes tests; l'app Android consomme la cible JVM sans changement de code. | Réorganisation des dossiers (`src/commonMain`, `src/jvmMain`…). |
| Module KMP séparé qui dépend de `:core` | `:core` intact. | Impossible: `:core` JVM ne peut pas être compilé pour iOS. |

Cibles: `jvm` (Android et tests JVM), `iosArm64` (iPhone), `macosArm64` (exécuter les tests sur le
Mac, Q4). Pas de cible simulateur tant qu'aucun runtime n'est installé.

### 2.2 Catalogue hors JVM (Q3)
| Option | Pour | Contre |
|---|---|---|
| **Tâche Gradle qui génère `CatalogSource.kt` (le JSON en constante) à partir de `catalog.json`** (retenu) | Commun à toutes les cibles, aucun accès fichier à l'exécution; `catalog.json` reste la source lisible. | Fichier généré (dans `build/`, jamais commité). |
| Bibliothèque de ressources multiplateforme | Générique. | Nouvelle dépendance (D4), surdimensionnée. |
| Copier le JSON dans le bundle iOS et le passer au cœur | Simple côté iOS. | Deux chemins de chargement à tester; le cœur ne serait plus autonome. |

Même mécanisme pour les ressources de test (fixtures, `reference-colors.csv`): une tâche génère
`TestResources.kt`, et tous les tests passent en `commonTest`.

### 2.3 Projet Xcode
| Option | Pour | Contre |
|---|---|---|
| **XcodeGen (`ios/project.yml`)** (retenu) | Projet décrit en YAML lisible et relu; `.xcodeproj` régénéré, non commité. | Outil de développement à installer (déjà présent ici). |
| `.xcodeproj` maintenu à la main | Aucun outil. | Fichier opaque, conflits, quatre cibles à câbler. |

### 2.4 Intégration du cœur dans Xcode
XCFramework statique `GoosescopeCore` produit par Gradle (`assembleGoosescopeCoreXCFramework`),
référencé par l'app et par l'extension écran verrouillé. Le script de build iOS lance Gradle avant
`xcodebuild`.

### 2.5 Langue dans l'app (Q1)
| Option | Pour | Contre |
|---|---|---|
| **Textes iOS dans `en.lproj` et `fr.lproj`, lus depuis le bundle de la langue choisie dans l'app (préférence locale)** (retenu) | Format standard, outils Xcode; bascule immédiate sans redémarrage; anglais par défaut quelle que soit la langue d'iOS. | Petite couche `L10n` à écrire et tester. |
| Clé `AppleLanguages` | Une ligne. | Non documenté, exige un redémarrage de l'app. |
| Langue d'iOS (réglage par app dans Réglages) | Natif. | Contredit F17 (anglais par défaut, bascule dans l'app). |

Les textes du catalogue viennent déjà du cœur avec un `Lang` explicite.

### 2.6 Lecture des images caméra
`AVCaptureVideoDataOutput` en BGRA; un `PixelGrid` écrit en Swift lit le `CVPixelBuffer`
directement (implémentation Swift d'une interface Kotlin, sans copie). `ASSUMPTION`: le coût des
appels Swift → Kotlin par pixel (~15 000 par image pour la luminance moyenne) reste sous 2 ms;
mesuré en I4, sinon on ajoute au cœur une fonction qui prend un tableau d'octets.

### 2.7 Contrôle et écran verrouillé (Q2)
- Extension WidgetKit contenant un `ControlWidget` (Centre de contrôle, écran verrouillé, bouton
  Action).
- Extension `LockedCameraCapture` dont la scène réutilise les vues SwiftUI de l'app (mode direct,
  mode figé). Elle lie le même XCFramework.
- Une `CameraCaptureIntent` relie le contrôle à l'extension: verrouillé, iOS ouvre l'extension;
  déverrouillé, l'app.
- `ASSUMPTION`: l'extension n'écrit rien (NG3), donc `LockedCameraCaptureSession` sert seulement à
  ouvrir l'app (bouton « Ouvrir Goosescope » une fois déverrouillé), jamais à transmettre d'image.

### 2.8 Aucun réseau (Q5)
Vérification sur le binaire construit: `otool -L` ne doit lister ni `CFNetwork` ni `Network`, et
`nm -u` ne doit référencer aucun symbole `NSURLSession`, `nw_`, `CFSocket`, `getaddrinfo`.
Script `ios/tools/check-no-network.sh`, lancé par la tâche de vérification.

## 3. Modules touchés

| Module | Changement | Comportements Android à risque |
|---|---|---|
| `core/build.gradle.kts` | Plugin multiplateforme, cibles jvm/iosArm64/macosArm64, XCFramework, tâches de génération. | Tout l'Android dépend de `:core`: compilation et 23 tests instrumentés. |
| `core/src/main` → `commonMain` | Déplacement; 3 corrections JVM (`toHex`, degrés, chargement). | Format `#RRGGBB` majuscule (`Srgb8Test`), angle de teinte, chargement du catalogue. |
| `core/src/test` → `commonTest` | Déplacement; lecture des ressources par `TestResources`. | Aucun test ne doit disparaître: même nombre de tests avant/après. |
| `app/build.gradle.kts` | Dépendance inchangée (`project(":core")`), résolution de la variante JVM. | Build Android. |
| `ios/` (nouveau) | App SwiftUI, extensions, tests d'interface, `project.yml`, scripts. | Aucun. |
| `.gitignore` | `ios/*.xcodeproj`, `ios/build/`, profils. | Aucun. |
| `SPEC.md` | NG5 remplacé (approuvé). | Aucun. |
| `ARCHITECTURE.md`, `STANDARDS.md` | Sections iOS. | Aucun. |

Pas de migration de données (l'app n'en stocke pas, hormis la langue, gérée séparément sur chaque OS).

## 4. Règles ajoutées aux standards (à reporter dans STANDARDS.md)

- Le code Swift ne contient aucune règle de couleur: il appelle le cœur (F30).
- Swift: style Swift API Design Guidelines, SwiftUI, pas de dépendance tierce, pas de `!` forcé
  sauf invariant commenté.
- Mêmes jetons visuels (STANDARDS §4), déclarés une fois côté Swift (`Tokens.swift`).
- Toute nouvelle chaîne existe en EN et FR, vérifié par un test de parité côté iOS.

## 5. Risques

1. Compilation Kotlin/Native des noms de tests avec espaces: possible refus sur certaines cibles;
   repli: renommer les tests en camelCase (comportement inchangé).
2. Taille: le cœur est lié deux fois (app et extension); acceptable pour une installation directe.
3. L'extension écran verrouillé a une mémoire limitée; l'image figée (1280×720) doit y tenir.
4. Les tests d'interface sur appareil exigent que l'iPhone soit déverrouillé pendant l'exécution.
