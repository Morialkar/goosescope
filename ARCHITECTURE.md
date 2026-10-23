# Architecture — Goosescope

Produit en Phase 1 AFTER (Greenfield). Aucun code produit à ce stade.
Référence: SPEC.md révision 2 (approuvée le 2026-10-23). Les exigences sont citées
par leur numéro gelé (F, NF, AC, D, A).

Décisions tranchées par Naomi le 2026-10-23: D8 (Kotlin + CameraX + Compose),
D9 (minSdk 33), texte sur aplat en noir/blanc purs (STANDARDS §4), dépôt git
public sur GitHub (D10, voir §9). Les valeurs marquées `ASSUMPTION` sont des points de départ à calibrer sur
la tablette.

---

## 1. Choix techniques

### 1.1 Pile applicative — D8, tranchée: Kotlin natif

| Option | Pour | Contre |
|---|---|---|
| **Kotlin natif + CameraX + Jetpack Compose** (recommandé) | Accès direct aux images caméra sur un fil d'analyse, ce qui tient NF1/NF2 sans effort. CameraX est AndroidX, donc conforme à D4. Torche, rotation, cycle de vie gérés par CameraX. Le cœur couleur peut être un module Kotlin pur testable sur le Mac sans appareil. | Ne se transpose pas à iOS (hors périmètre, NG5). |
| Capacitor (comme Arquanzia) | Réutilise TypeScript et l'outillage d'Arquanzia. Le cœur couleur serait du TS testable avec Vitest. | Les images caméra passent par `getUserMedia` dans une WebView puis une relecture de canvas: latence et coût CPU plus élevés (NF1, NF2), permission caméra à doubler (Android + WebView). Les plugins caméra Capacitor visent la photo, pas l'analyse en continu. |
| Flutter | Plugin `camera` avec flux d'images, un seul code pour deux OS. | Outillage absent du Mac, Dart nouveau dans l'écosystème de Naomi, le multiplateforme n'est pas un objectif. |
| React Native + VisionCamera | Bons « frame processors ». | Pile lourde (JS + natif + worklets) pour une app d'un écran. |

Décision (D8): Kotlin natif + CameraX + Compose.

### 1.2 Espace de couleur

| Option | Pour | Contre |
|---|---|---|
| **OKLab / OKLCh** (recommandé) | Perceptuellement uniforme: une distance euclidienne correspond bien à une différence perçue. Calcul en quelques multiplications. La forme LCh (clarté, chroma, teinte) donne directement « plus clair », « plus vif », « vers le rouge » pour F15. | Moins connu que CIELAB. |
| CIELAB + CIEDE2000 | Standard de l'industrie. | CIEDE2000 est lourd et sert surtout à comparer des couleurs très proches; CIELAB seul est moins uniforme dans les bleus. |
| HSV / HSL | Simple, familier. | Pas perceptuel: un jaune et un bleu à la même « valeur » n'ont pas la même clarté perçue, d'où des erreurs sur clair/foncé et sur brun. |

Choix: OKLab pour les distances, OKLCh pour les comparaisons.

### 1.3 Format des données de nuances

| Option | Pour | Contre |
|---|---|---|
| **JSON embarqué + tests d'intégrité** (recommandé) | Lisible et modifiable par Naomi sans connaître Kotlin (relecture AC14). Validé par AC9 à chaque build. | Erreurs détectées au test, pas à la compilation. |
| Code Kotlin (liste de data classes) | Vérifié par le compilateur. | 150 fiches × 2 langues dans du code: difficile à relire et à corriger. |

Choix: un fichier `catalog.json` dans les ressources du module cœur, lu avec
`kotlinx.serialization` (bibliothèque JetBrains hors ligne, conforme à D4).

### 1.4 Langue de l'app

| Option | Pour | Contre |
|---|---|---|
| **Langue par app d'Android (`LocaleManager`, API 33+)** (recommandé) | Le système mémorise le choix lui-même (F17, AC12): aucun stockage à écrire. Les textes d'interface viennent des ressources `values/` et `values-fr/`. Le réglage apparaît aussi dans les paramètres Android de l'app. | Exige minSdk 33 (voir §1.5). |
| Préférence maison (DataStore) + contexte recréé | Fonctionne sur d'anciens Android. | Plus de code et un stockage à gérer pour rien sur la cible. |

Anglais par défaut (F17): si `applicationLocales` est vide au lancement, l'app le
fixe à `en`. Conséquence assumée: si l'utilisateur remet « langue du système » dans les
paramètres Android, l'app repasse à l'anglais.

### 1.5 Version minimale d'Android — D9, tranchée: minSdk 33

- Décision (D9): **minSdk 33** (Android 13), targetSdk 36. La tablette est en API 36,
  l'émulateur en API 37. Permet §1.4 sans bibliothèque AppCompat.
- Alternative: minSdk 26 avec AppCompat pour la langue, si l'app doit un jour
  tourner sur un vieil appareil.

### 1.6 Construction et tests

- Gradle (Kotlin DSL) avec catalogue de versions, wrapper commité. Versions exactes
  d'AGP, Kotlin, CameraX et Compose figées en Phase 3 selon ce qui fonctionne avec le
  JDK d'Android Studio (OpenJDK 25). Risque à vérifier au premier build: compatibilité
  AGP / JDK 25; repli possible sur un JDK 21.
- Module cœur: tests JUnit 5 + `kotlin.test`, exécutés sur le Mac (`./gradlew :core:test`).
- Module app: tests Compose instrumentés exécutés sur la tablette
  (`./gradlew :app:connectedDebugAndroidTest`), avec une source d'images factice.

---

## 2. Frontières du système

```
┌───────────────────────── :app (Android) ─────────────────────────┐
│                                                                   │
│  CameraXFrameSource ──┐                       ┌── Compose UI      │
│  (CameraX, torche)    │   FrameSource         │   LiveScreen      │
│                       ├──────────────► ScopeViewModel ─► FrozenScreen │
│  FakeFrameSource ─────┘   (interface)   (StateFlow)   PermissionScreen │
│  (tests)                       │                      DetailSheet │
│                                │                                  │
└────────────────────────────────┼──────────────────────────────────┘
                                 │ PixelGrid, Point
┌────────────────────────────────▼──── :core (Kotlin pur, JVM) ─────┐
│  Sampler → ColorMath (sRGB ↔ linéaire ↔ OKLab/OKLCh)              │
│  BaseClassifier ── ShadeMatcher ── Catalog (catalog.json)         │
│  Smoother (lissage + hystérésis)  DarknessGate                    │
│  ComparisonEngine → ComparisonText (EN, FR)                       │
│  ViewToImageMapper   Contrast (WCAG)                              │
└───────────────────────────────────────────────────────────────────┘
```

Règle de frontière: `:core` n'importe rien d'`android.*` ni d'`androidx.*`. Toute la
logique décrite dans la spec (classification, nuances, comparaison, lissage,
obscurité, contraste, correspondance toucher→pixel) vit dans `:core` et est testée
sur le Mac. `:app` ne fait que brancher la caméra, l'état et l'affichage.

### 2.1 Flux en mode direct (F1 à F5, F8, F12)

1. CameraX lie deux cas d'usage au cycle de vie: `Preview` (affichage, `PreviewView`
   en `FILL_CENTER`) et `ImageAnalysis` en `OUTPUT_IMAGE_FORMAT_RGBA_8888`, stratégie
   `KEEP_ONLY_LATEST`. CameraX fait la conversion YUV→RGBA; on n'écrit pas ce calcul.
   `ASSUMPTION`: résolution d'analyse 1280×720, suffisante pour échantillonner.
2. `CameraXFrameSource` enveloppe chaque image dans un `PixelGrid` (lecture seule,
   dimensions déjà redressées selon la rotation) et l'émet.
3. Le ViewModel appelle `Sampler.sampleCenter(grid)`: moyenne en lumière linéaire
   d'un carré centré de côté 2 % de la plus petite dimension (F3).
   La moyenne se fait en linéaire, pas en sRGB, sinon les mélanges tirent vers le
   foncé.
4. `DarknessGate` décide si le résultat est fiable (F8, §3.4).
5. `Smoother` lisse l'OKLab et stabilise le nom (F5, §3.5).
6. L'état `Live` est publié; Compose affiche nom de base + aplat (F4).

Le centre de l'image d'analyse correspond au centre de l'aperçu: les deux sont
centrés et `FILL_CENTER` rogne symétriquement.

### 2.2 Flux en mode figé (F6, F14 à F16)

1. Appui sur « figer »: le ViewModel garde la dernière image d'analyse, copiée en
   `Bitmap` redressé, en mémoire seulement (NG3). L'analyse caméra est suspendue.
2. L'écran figé affiche ce `Bitmap` avec le même rognage que l'aperçu (`Crop`
   centré), pour qu'il ait l'air identique au direct.
3. Le point visé part du centre. Un toucher est converti en coordonnées de l'image
   par `ViewToImageMapper` (fonction pure, tient compte du rognage) (F16).
4. Pour ce point: échantillonnage, `DarknessGate`, `BaseClassifier`, `ShadeMatcher`,
   `ComparisonEngine`. Pas de lissage: l'image est fixe.
5. La fiche (`DetailSheet`) s'affiche par-dessus l'image (F14).
6. « Reprendre »: le `Bitmap` est libéré, l'analyse reprend.

---

## 3. Algorithmes du cœur

### 3.1 Conversion

`Srgb8(r, g, b)` (0–255) → linéaire (courbe sRGB standard) → OKLab (matrices de
Björn Ottosson) → OKLCh (`C = √(a²+b²)`, `h = atan2(b, a)` en degrés 0–360).

### 3.2 Couche 1: `BaseClassifier` (F13)

Plus proche voisin sur des **ancres** (A4: la base est déterminée en premier).
Chaque couleur de base a de 3 à 10 ancres OKLab dans `catalog.json` (ex. brun:
plusieurs bruns clairs et foncés). La base retenue est celle de l'ancre la plus
proche en distance OKLab.

Alternative écartée: règles fixes sur LCh (« si C < x alors gris »). Elles
deviennent vite un empilement de cas (brun = orange foncé, rose = rouge clair peu
saturé…) difficile à régler. Les ancres se règlent en déplaçant des points de
données, et chaque ajustement est couvert par AC2/AC3.

### 3.3 Couche 2: `ShadeMatcher` (F13, F19)

Parmi les nuances dont `base` = base retenue, la plus proche en distance OKLab de
leur couleur de référence. F19 est donc garanti par construction; AC9 vérifie en
plus que chaque couleur de référence est bien classée dans sa propre base, et AC13
le vérifie sur un échantillonnage de tout l'espace.

### 3.4 Obscurité: `DarknessGate` (F8)

Difficulté: un objet noir et une scène trop sombre donnent tous deux une zone
sombre. Règle:
- « Trop sombre » si la luminance moyenne de **toute l'image** est sous un seuil
  `ASSUMPTION` (Y < 0,02) — la caméra n'arrive plus à exposer.
- Sinon, une zone sombre au réticule est classée normalement (souvent « noir »).

L'exposition automatique de la caméra relève les scènes sombres, ce qui rend ce
seuil empirique: il sera calibré sur la tablette, caméra couverte (AC7) puis devant
un objet noir bien éclairé.

### 3.5 Lissage: `Smoother` (F5, NF1, AC4)

- Moyenne exponentielle sur l'OKLab mesuré, `α = 0,35` par image d'analyse
  `ASSUMPTION`.
- Hystérésis sur le nom: on change de base seulement si la distance à la meilleure
  ancre de la nouvelle base est plus petite d'au moins `δ = 0,015` que celle de la
  base actuelle, **et** que cette nouvelle base gagne depuis au moins 150 ms
  (250 ms prévus au départ donnaient 396 ms de latence au test de T06, au-delà de NF1).
  Le seuil de distance empêche le clignotement à une frontière; le délai court
  garde la latence sous 300 ms pour un vrai changement.

### 3.6 Comparaison: `ComparisonEngine` + `ComparisonText` (F15)

Deux étapes séparées pour que la logique soit testable sans dépendre de la langue:

1. `ComparisonEngine.compare(measured, reference): ComparisonFacts` (sans texte):
   - `closeness`: `VERY_CLOSE` si ΔE_OK < 0,02, sinon `SLIGHT` (< 0,06) ou `CLEAR`.
   - `hueShiftToward`: si les deux chromas > 0,03 et |Δh| > 6°, la couleur de base
     chromatique voisine dans le sens du décalage sur le cercle des teintes
     (ex. Pêche décalée vers h plus petit → `RED`). Sinon `null`.
   - `lightness`: `LIGHTER` / `DARKER` / `SAME` (seuil |ΔL| > 0,03).
   - `chroma`: `MORE_VIVID` / `PALER` / `SAME` (seuil |ΔC| > 0,02).
   - `temperature`: `WARMER` / `COOLER` / `SAME`, selon que la teinte se rapproche
     du pôle chaud (h ≈ 40°, entre rouge et orange) ou de son opposé froid (h ≈ 220°)
     de plus de 6°. (50° prévus au départ rendaient une pêche tirant vers le rouge
     « plus froide », corrigé en T07.)
   Tous ces seuils sont `ASSUMPTION`, calibrés en Phase 3.
2. `ComparisonText.render(facts, shade, lang): String`: gabarits par langue.
   Exemple FR: « Le rouge y est légèrement plus présent, ce qui la rend plus
   chaleureuse. » Le sujet est toujours « cette teinte » (féminin), ce qui fixe
   l'accord des adjectifs en français.

### 3.7 Contraste: `Contrast` (NF3)

`contrastRatio(a, b)` selon WCAG 2.x. `textColorOn(swatch)` retourne le texte clair
ou sombre du thème qui offre le meilleur rapport. Un test parcourt toutes les
couleurs de référence du catalogue et un échantillon de l'espace sRGB pour vérifier
≥ 4,5:1 (voir STANDARDS §4 pour le choix des deux couleurs de texte).

### 3.8 Correspondance toucher → pixel: `ViewToImageMapper` (F16)

Fonction pure: dimensions de la vue, dimensions de l'image, mode de rognage centré →
point image. Retourne `null` si le toucher tombe hors de l'image visible.

---

## 4. Schémas de données

### 4.1 `catalog.json` (`:core/src/main/resources/`)

```json
{
  "version": 1,
  "bases": [
    {
      "id": "orange",
      "name": { "en": "Orange", "fr": "Orange" },
      "anchors": ["#FF8000", "#E07020", "#FFA040"]
    }
  ],
  "shades": [
    {
      "id": "peach",
      "base": "orange",
      "ref": "#FFCBA4",
      "name": { "en": "Peach", "fr": "Pêche" },
      "description": {
        "en": "A soft, pale orange, like the skin of a ripe peach…",
        "fr": "Un orange doux et pâle, comme la peau d'une pêche mûre…"
      },
      "nature": {
        "en": ["ripe peach skin", "apricot flesh", "dawn sky"],
        "fr": ["peau de pêche mûre", "chair d'abricot", "ciel à l'aube"]
      }
    }
  ]
}
```

Règles (vérifiées par AC9): `id` en `snake_case` ASCII, uniques; `base` ∈ les 11 ids
de base (`red, orange, yellow, green, blue, purple, pink, brown, gray, black, white`);
couleurs en `#RRGGBB`; tous les champs textuels présents en `en` et `fr`, non vides;
`nature` ≥ 3 entrées par langue; 100 ≤ nombre de nuances ≤ 150; ≥ 5 nuances par base
chromatique (les bases neutres gris/noir/blanc en ont ≥ 2).

### 4.2 Types du cœur (signatures)

```kotlin
data class Srgb8(val r: Int, val g: Int, val b: Int)          // 0..255, sinon IllegalArgumentException
data class Oklab(val l: Double, val a: Double, val b: Double)
data class Oklch(val l: Double, val c: Double, val hDeg: Double)
enum class BaseColor { RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK, BROWN, GRAY, BLACK, WHITE }
enum class Lang { EN, FR }

interface PixelGrid { val width: Int; val height: Int; fun rgbAt(x: Int, y: Int): Srgb8 }
data class Point(val x: Int, val y: Int)

object ColorMath {
    fun toOklab(c: Srgb8): Oklab
    fun toOklch(c: Oklab): Oklch
    fun toSrgb8(c: Oklab): Srgb8                               // borné au gamut sRGB
    fun distance(a: Oklab, b: Oklab): Double
}

object Sampler {
    fun sample(grid: PixelGrid, center: Point, sideFraction: Double = 0.02): Srgb8
    fun meanLuminance(grid: PixelGrid, step: Int = 8): Double
}

class Catalog private constructor(...) {
    val bases: List<BaseEntry>; val shades: List<Shade>
    companion object { fun load(json: String): Catalog; fun loadBundled(): Catalog }
}

class BaseClassifier(catalog: Catalog) {
    fun classify(c: Oklab): BaseMatch                          // base + distance
}
class ShadeMatcher(catalog: Catalog) {
    fun match(c: Oklab, base: BaseColor): Shade
}
class DarknessGate(threshold: Double) { fun isTooDark(meanLuminance: Double): Boolean }
class Smoother(classifier: BaseClassifier, alpha: Double, margin: Double, dwellMs: Long) {
    fun push(c: Oklab, timestampMs: Long): SmoothedReading     // base stable + couleur lissée
    fun reset()
}
object ComparisonEngine { fun compare(measured: Oklab, reference: Oklab): ComparisonFacts }
object ComparisonText { fun render(facts: ComparisonFacts, lang: Lang): String }
object Contrast { fun ratio(a: Srgb8, b: Srgb8): Double; fun textColorOn(bg: Srgb8): Srgb8 }
object ViewToImageMapper {
    fun map(tapX: Float, tapY: Float, viewW: Int, viewH: Int, imageW: Int, imageH: Int): Point?
}
```

### 4.3 État de l'interface (`:app`)

```kotlin
sealed interface ScopeState {
    data object NeedsPermission : ScopeState
    data object PermissionDenied : ScopeState                  // F7
    data class Live(val reading: LiveReading, val torchOn: Boolean) : ScopeState
    data class Frozen(val image: Bitmap, val target: Point, val detail: Detail) : ScopeState
    data class Failure(val message: String) : ScopeState       // catalogue illisible, caméra indisponible
}
sealed interface LiveReading { data object TooDark; data object Warming; data class Color(val base: BaseColor, val swatch: Srgb8) }
sealed interface Detail { data object TooDark; data class Color(val base: BaseColor, val shade: Shade, val measured: Srgb8, val comparison: String) }
```

---

## 5. Contrats d'erreur

| Situation | Contrat | Exigence |
|---|---|---|
| `Srgb8` hors 0–255, couleur `#` mal formée, `sideFraction` hors ]0, 1] | `IllegalArgumentException` avec message explicite. Erreur de programmation: jamais attrapée silencieusement. | AC3, AC10 |
| `catalog.json` invalide | `Catalog.load` lève `CatalogException` qui liste **toutes** les violations. En pratique bloqué par AC9 au build; à l'exécution → état `Failure`, écran d'erreur, pas de crash. | AC9 |
| Point d'échantillonnage près d'un bord | La zone est rognée aux limites de l'image; jamais d'accès hors limites. | F16 |
| Toucher hors de l'image visible | `ViewToImageMapper` retourne `null`; le toucher est ignoré. | F16 |
| Permission caméra refusée | État `PermissionDenied`: écran explicatif + bouton vers les paramètres de l'app. | F7, AC6 |
| Caméra indisponible (occupée, absente) | État `Failure` avec message lisible; bouton « réessayer ». | — |
| Scène trop sombre | `LiveReading.TooDark` / `Detail.TooDark`: message, pas de nom. | F8, AC7 |
| Pas encore d'image reçue | `LiveReading.Warming`: réticule seul, pas de nom. | — |
| Torche absente sur l'appareil | Bouton torche masqué (`cameraInfo.hasFlashUnit()`). | F12 |

---

## 6. Sécurité et confidentialité

`REQUIRES_REVIEW:` les fichiers suivants touchent à une permission et à des images
de l'environnement de l'utilisateur; relecture humaine obligatoire avant toute
distribution (§8
de la spec):
- `AndroidManifest.xml` (une seule permission: `CAMERA`; pas d'`INTERNET`).
- `CameraXFrameSource.kt` (flux d'images).
- `PermissionScreen.kt` et la gestion de la permission dans le ViewModel.
- Le chemin de l'image figée (copie en `Bitmap`, libération à la reprise).

Garanties: aucune écriture d'image sur disque, aucun accès réseau possible
(permission absente), pas de sauvegarde Android de données (`allowBackup=false`;
seule la langue existe et elle est gérée par le système).

---

## 7. Stratégie de test (correspondance AC)

| AC | Où | Comment |
|---|---|---|
| AC1, NF7 | Tablette | Mesure manuelle chronométrée + `adb shell am start -W` (temps rapporté). |
| AC2 | Tablette | Protocole manuel: planche de cibles affichée sur un 2e écran ou imprimée, couvrant les paires de confusion; résultat noté dans `tests/field/AC2.md`. |
| AC3, AC10, AC13 | `:core:test` | Unitaires JVM. |
| AC4 | Tablette + `:core:test` | Test unitaire du `Smoother` sur une séquence synthétique qui oscille à une frontière; puis vérification manuelle. |
| AC5, AC11, AC12, AC6 | `:app:connectedDebugAndroidTest` | Tests Compose avec `FakeFrameSource` (images synthétiques), exécutés sur la tablette. AC6 via `adb shell pm revoke`. |
| AC7 | Tablette | Caméra couverte, manuel. |
| AC8 | Émulateur | Captures d'écran (portrait, paysage, FR, EN, fiche) validées par Naomi. |
| AC9 | `:core:test` | Tests d'intégrité de `catalog.json`. |
| AC14 | Humain | Naomi relit un échantillon de fiches. |
| NF3 | `:core:test` | Test de contraste sur tout le catalogue. |

---

## 8. Arborescence prévue

```
goosescope/
├── AFTER.md  SPEC.md  ARCHITECTURE.md  STANDARDS.md  PLAN.md (Phase 2)
├── settings.gradle.kts  build.gradle.kts  gradle/libs.versions.toml  gradlew
├── core/
│   ├── build.gradle.kts
│   └── src/{main,test}/kotlin/com/morialkar/goosescope/core/...
│       src/main/resources/catalog.json
├── app/
│   ├── build.gradle.kts
│   └── src/main/{AndroidManifest.xml, kotlin/.../app/, res/values, res/values-fr, res/xml/locales_config.xml}
│       src/androidTest/kotlin/...
└── tests/field/   (protocoles et résultats des tests manuels)
```

---

## 9. Dépôt public et page GitHub Pages (D10)

Naomi a demandé un dépôt GitHub **public** et une petite page bilingue de
téléchargement et d'explication hébergée sur GitHub Pages.

Conflits avec la spec, tranchés par Naomi le 2026-10-23 (amendement SPEC révision 3 approuvé):
- NG4 et SPEC §8 « Distribution: APK installé par ADB ». Une page de
  téléchargement publie l'APK à tout le monde. Résolu par SPEC F20 à F23
  (révision 3).
- Un APK téléchargeable doit être signé avec une clé de publication stable (sinon
  chaque mise à jour exige de désinstaller). `REQUIRES_REVIEW:` la clé (keystore)
  et ses mots de passe sont des secrets: jamais dans le dépôt public, conservés par
  Naomi hors du dépôt, signature faite en local.
- Le public visé n'est plus seulement l'utilisateur principal: les fiches deviennent du contenu
  publié; la relecture AC14 prend plus de poids.

Architecture retenue:
- Dossier `site/` dans le dépôt: une page HTML statique, sans framework ni
  script tiers, sans analytics (cohérent avec G4 et D4), EN par défaut avec bascule
  FR, mêmes jetons visuels que l'app (STANDARDS §4).
- Publication par GitHub Pages depuis `site/` sur `main`.
- L'APK signé est attaché à une GitHub Release (`gh release create`); la page
  pointe vers le dernier APK de Releases, l'APK n'est jamais commité.
- La page explique: ce que fait l'app, comment installer un APK hors Play Store
  (sources inconnues), ce que l'app ne fait pas (aucune donnée envoyée, aucune image
  enregistrée).
