# Plan — Goosescope

Produit en Phase 2 AFTER (Greenfield). Aucun code produit à ce stade.
Plan approuvé par Naomi le 2026-10-23.
Références: SPEC.md (révisions 2 + 3), ARCHITECTURE.md, STANDARDS.md (Phase 1
approuvée le 2026-10-23).

Chaque tâche est autonome: une session neuve peut l'exécuter en lisant cette fiche
et les trois documents de référence. Une tâche n'est terminée qu'après exécution
**affichée** de sa commande de vérification. Un commit par tâche, sur `main`,
message `Tnn: <objectif>` (STANDARDS §8). Le projet reste compilable et testable
après chaque tâche.

Convention de commande: depuis `~/Developer/goosescope`, avec
`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` et
`~/Library/Android/sdk/platform-tools` dans le `PATH`. La tablette est l'appareil
ADB `SM-X528U` (sans fil).

---

## Vue d'ensemble

| Tâche | Objectif | Dépend de |
|---|---|---|
| T01 | Squelette du dépôt et de la construction | — |
| T02 | Conversions de couleur et contraste | T01 |
| T03 | Chargement et validation du catalogue | T02 |
| T04 | Couche 1: classification de base (ancres) | T03 |
| T05 | Échantillonnage, obscurité, toucher → pixel | T02 |
| T06 | Lissage et hystérésis | T04 |
| T07 | Moteur et textes de comparaison | T04 |
| T08 | Couche 2: `ShadeMatcher` | T03, T04 |
| T09 | Contenu: 100 à 150 fiches EN/FR | T08 |
| T10 | App: mode direct (caméra, permission, torche) | T05, T06, T09 |
| T11 | App: mode figé, fiche, toucher | T07, T10 |
| T12 | App: langue EN/FR | T11 |
| T13 | Calibration terrain sur la tablette | T12 |
| T14 | Vérification visuelle sur émulateur | T13 |
| T15 | Signature de publication et GitHub Release | T14 |
| T16 | Page GitHub Pages bilingue | T15 |
| — | Phase finale: revue adversariale | T16 |

Parallélisable si voulu: T05 en parallèle de T03/T04; T07 en parallèle de T06/T08.

---

## T01 — Squelette du dépôt et de la construction

- **Objectif**: un dépôt git avec deux modules Gradle qui compilent, un test
  unitaire qui passe, et une app vide installable sur la tablette.
- **Exigences couvertes**: NF6, F23, STANDARDS §6 à §8, ARCHITECTURE §1.6, §8.
- **Fichiers**: `.gitignore`, `LICENSE` (MIT), `README.md` (court, bilingue,
  renvoie aux documents), `settings.gradle.kts`, `build.gradle.kts`,
  `gradle.properties`, `gradle/libs.versions.toml`, wrapper Gradle,
  `core/build.gradle.kts`, `core/src/main/kotlin/.../core/Version.kt`,
  `core/src/test/kotlin/.../core/SmokeTest.kt`, `app/build.gradle.kts`,
  `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/.../app/MainActivity.kt`,
  `app/src/main/res/values/strings.xml`, `app/src/main/res/values-fr/strings.xml`,
  tâche de vérification `verifyNoInternetPermission`.
- **Contexte**: `:core` est un module Kotlin/JVM sans SDK Android. `:app` en Compose,
  minSdk 33, targetSdk 36, compileSdk = la plateforme installée la plus récente
  (37). Versions d'AGP/Kotlin/Compose/CameraX figées dans le catalogue; si AGP
  refuse le JDK 25 d'Android Studio, documenter et utiliser un JDK 21 (décision à
  remonter si un téléchargement est nécessaire). `allowBackup=false`. Seule
  permission: `CAMERA`. Dépôt GitHub public `Morialkar/goosescope` créé avec `gh` et
  premier push (F23).
- **Critères d'acceptation**: `./gradlew :core:test` passe; l'APK debug s'installe
  et s'ouvre sur la tablette avec un écran « Goosescope » aux jetons de STANDARDS §4;
  `verifyNoInternetPermission` passe; le dépôt public existe sans secret.
- **Cas de test**: nominal — `SmokeTest` passe; frontière — manifeste fusionné ne
  contient que `CAMERA`; erreur — ajouter temporairement `INTERNET` fait échouer
  `verifyNoInternetPermission` (montré, puis retiré).
- **Vérification**:
  `./gradlew :core:test :app:verifyNoInternetPermission :app:installDebug && adb shell am start -W -n com.morialkar.goosescope/.app.MainActivity`

## T02 — Conversions de couleur et contraste

- **Objectif**: `Srgb8`, `Oklab`, `Oklch`, `ColorMath`, `Contrast`, analyse `#RRGGBB`.
- **Exigences**: ARCHITECTURE §3.1, §3.7, §4.2; NF3; STANDARDS §4.
- **Fichiers**: `core/src/main/kotlin/.../core/color/{Srgb8,Oklab,ColorMath,Contrast,Hex}.kt`,
  tests associés.
- **Contexte**: matrices OKLab de Björn Ottosson; courbe sRGB standard; `toSrgb8`
  borne au gamut. `Contrast.textColorOn` retourne `#000000` ou `#FFFFFF` (STANDARDS §4).
- **Critères**: valeurs de référence d'Ottosson retrouvées à 1e-3 près; aller-retour
  sRGB→OKLab→sRGB exact à ±1 sur 4096 couleurs; contraste ≥ 4,5 pour toute couleur
  d'une grille 16³.
- **Cas de test**: nominal — blanc → L≈1, a≈b≈0; rouge pur → valeurs de référence;
  frontière — gris moyen (pire cas de contraste ≈ 4,58); teinte à 0°/360°; erreur —
  `Srgb8(256,0,0)`, `Srgb8(-1,0,0)`, `"#12345"`, `"#GGGGGG"` → `IllegalArgumentException`.
- **Dépendances**: T01. **Comportements à risque**: aucun (nouveau code).
- **Vérification**: `./gradlew :core:test --tests '*color*'`

## T03 — Chargement et validation du catalogue

- **Objectif**: `Catalog.load(json)` et `Catalog.loadBundled()` avec validation
  **structurelle**; `catalog.json` initial avec les 11 bases (noms EN/FR, ancres
  provisoires) et une liste de nuances vide.
- **Exigences**: F13, F17, F18, NF9; ARCHITECTURE §1.3, §4.1, §5.
- **Fichiers**: `core/.../core/catalog/{Catalog,CatalogModel,CatalogException}.kt`,
  `core/src/main/resources/catalog.json`, `core/src/test/resources/fixtures/*.json`, tests.
- **Contexte**: `kotlinx.serialization`. La validation au chargement couvre la
  **structure** (ids uniques en `snake_case`, `base` parmi les 11, `#RRGGBB`,
  champs EN et FR non vides, `nature` ≥ 3 par langue). Les règles de **quantité**
  (100–150 nuances, ≥ 5 par base chromatique) sont vérifiées par le test d'intégrité
  du catalogue livré, ajouté en T09. `CatalogException` liste toutes les violations.
- **Critères**: le catalogue livré se charge; chaque fixture invalide est refusée
  avec la violation attendue dans le message.
- **Cas de test**: nominal — fixture valide de 3 nuances; frontière — nuance avec
  exactement 3 éléments de nature; nom avec accents (« Pêche »); erreur — base
  inconnue, id dupliqué, FR manquant, hex invalide, JSON mal formé, plusieurs
  violations à la fois (toutes listées).
- **Dépendances**: T02.
- **Vérification**: `./gradlew :core:test --tests '*catalog*'`

## T04 — Couche 1: classification de base

- **Objectif**: `BaseClassifier` par plus proche ancre, ancres réglées dans
  `catalog.json`, jeu de couleurs de référence.
- **Exigences**: F13, A4, AC3, AC2 (version synthétique); ARCHITECTURE §3.2.
- **Fichiers**: `core/.../core/classify/BaseClassifier.kt`, `catalog.json` (ancres),
  `core/src/test/resources/reference-colors.csv` (hex, base attendue, source),
  tests.
- **Contexte**: 3 à 10 ancres par base. Le jeu de référence comprend, pour chaque
  base, des cas typiques et des cas frontière, et couvre les paires de confusion
  de AC2 (rouge/vert, vert/brun, orange/vert, bleu/violet, rose/gris, bleu/vert,
  jaune/rose). Les cas frontière documentent l'attendu choisi (ex. `#808000`
  olive → brun ou vert: choix écrit dans le CSV, `ASSUMPTION` signalée à Naomi).
- **Critères**: 100 % du jeu de référence typique correctement classé; ≥ 90 % des
  cas frontière; aucune base sans ancre.
- **Cas de test**: nominal — un cas typique par base (11); frontière — brun foncé vs
  noir, gris chaud vs brun, rose pâle vs blanc, orange foncé vs brun; erreur —
  catalogue sans ancre pour une base → `CatalogException`.
- **Dépendances**: T03.
- **Vérification**: `./gradlew :core:test --tests '*classify*'`

## T05 — Échantillonnage, obscurité, toucher → pixel

- **Objectif**: `PixelGrid`, `Sampler.sample`, `Sampler.meanLuminance`,
  `DarknessGate`, `ViewToImageMapper`, `Tuning.kt` (constantes nommées).
- **Exigences**: F3, F8, F16; ARCHITECTURE §3.4, §3.8, §5; STANDARDS §1.3.
- **Fichiers**: `core/.../core/sample/{PixelGrid,Sampler,DarknessGate}.kt`,
  `core/.../core/geometry/ViewToImageMapper.kt`, `core/.../core/Tuning.kt`, tests
  avec `PixelGrid` synthétiques.
- **Contexte**: moyenne en lumière linéaire; zone rognée aux bords;
  `ViewToImageMapper` suppose un rognage centré (`Crop`/`FILL_CENTER`).
- **Critères**: moitié noir/moitié blanc → moyenne linéaire (pas `#808080`);
  aucun accès hors limites.
- **Cas de test**: nominal — grille uniforme → sa couleur; toucher au centre → centre
  image; frontière — échantillon au coin (0,0); vue plus large que l'image et plus
  haute que l'image; erreur — `sideFraction` 0 ou > 1 → exception; toucher dans la
  zone rognée → `null`.
- **Dépendances**: T02.
- **Vérification**: `./gradlew :core:test --tests '*sample*' --tests '*geometry*'`

## T06 — Lissage et hystérésis

- **Objectif**: `Smoother` (moyenne exponentielle + marge de distance + délai).
- **Exigences**: F5, NF1, AC4; ARCHITECTURE §3.5.
- **Fichiers**: `core/.../core/smooth/Smoother.kt`, tests; constantes dans `Tuning.kt`.
- **Critères**: séquence synthétique oscillant à une frontière pendant 5 s à 30 i/s
  → ≤ 5 changements de nom; changement franc → nouveau nom en ≤ 300 ms simulées.
- **Cas de test**: nominal — couleur stable → nom stable dès la 1re lecture;
  frontière — oscillation (ci-dessus); changement franc; erreur — horodatage qui
  recule → `IllegalArgumentException`; `reset()` efface l'état.
- **Dépendances**: T04.
- **Vérification**: `./gradlew :core:test --tests '*smooth*'`

## T07 — Moteur et textes de comparaison

- **Objectif**: `ComparisonEngine` (faits) et `ComparisonText` (EN, FR).
- **Exigences**: F15, AC10; ARCHITECTURE §3.6; STANDARDS §1.4.
- **Fichiers**: `core/.../core/compare/{ComparisonFacts,ComparisonEngine,ComparisonText}.kt`, tests.
- **Contexte**: sujet toujours « cette teinte » / « this shade »; accord féminin en
  FR. Nom de la base voisine traduit via le catalogue.
- **Critères**: AC10 dans les deux langues; toute combinaison de faits produit une
  phrase non vide sans placeholder résiduel.
- **Cas de test**: nominal — pêche décalée vers le rouge → mention du rouge +
  « plus chaleureuse » / « warmer »; frontière — écart négligeable → « très
  proche »; décalage de teinte avec chroma quasi nulle → pas de mention de teinte;
  erreur — couleur `NaN` → exception; exhaustif — toutes les combinaisons d'énumérations rendues.
- **Dépendances**: T04.
- **Vérification**: `./gradlew :core:test --tests '*compare*'`

## T08 — Couche 2: `ShadeMatcher`

- **Objectif**: plus proche nuance au sein de la base retenue.
- **Exigences**: F13, F19, A4; ARCHITECTURE §3.3.
- **Fichiers**: `core/.../core/classify/ShadeMatcher.kt`, tests sur fixture.
- **Critères**: la nuance retournée appartient toujours à la base demandée.
- **Cas de test**: nominal — couleur égale à une référence → cette nuance;
  frontière — couleur plus proche d'une nuance d'une autre base → nuance de la base
  demandée quand même; erreur — base sans nuance → `IllegalStateException` explicite.
- **Dépendances**: T03, T04.
- **Vérification**: `./gradlew :core:test --tests '*ShadeMatcher*'`

## T09 — Contenu: 100 à 150 fiches EN/FR

- **Objectif**: remplir `catalog.json` (A5: premier jet par Claude) et ajouter les
  tests d'intégrité du catalogue livré.
- **Exigences**: F13, F14, F17, F18, F19, AC9, AC13, AC14 (préparation);
  STANDARDS §1.4.
- **Fichiers**: `core/src/main/resources/catalog.json`,
  `core/src/test/.../BundledCatalogTest.kt`, `docs/shade-review.html` (page de
  relecture générée par `tools/shade_review.py`, publiée en artifact à la demande de
  Naomi: chaque fiche avec un aplat de sa couleur, approbation ou signalement).
- **Contexte**: environ 10 à 15 nuances par base chromatique, 2 à 4 par base neutre.
  Couleurs de référence inspirées de références publiques de noms de couleur, mais
  textes originaux. Chaque fiche: comparaison évocatrice, ≥ 3 éléments de la nature.
- **Critères**: AC9 et AC13 passent; `docs/shade-review.html` généré et publié.
- **Cas de test**: nominal — AC9 complet; frontière — chaque référence classée dans
  sa base parente (sinon ajuster ancre ou référence, jamais retirer le test);
  AC13 sur une grille 32³; erreur — couvert par T03 (fixtures invalides).
- **Dépendances**: T08.
- **Vérification**: `./gradlew :core:test`

## T10 — App: mode direct

- **Objectif**: caméra en direct avec réticule, nom de base, aplat, torche,
  « trop sombre », permission, portrait et paysage.
- **Exigences**: F1–F5, F7, F8, F12, NF1–NF5, NF7, AC1, AC4, AC6, AC7;
  ARCHITECTURE §2.1, §4.3, §5, §6.
- **Fichiers**: `app/.../app/camera/{FrameSource,CameraXFrameSource,BitmapPixelGrid}.kt`,
  `app/.../app/ScopeViewModel.kt`, `app/.../app/ui/{LiveScreen,PermissionScreen,Reticle}.kt`,
  `app/.../app/ui/theme/Tokens.kt`, `strings.xml` (EN/FR),
  `app/src/androidTest/.../{FakeFrameSource,LiveScreenTest,PermissionTest}.kt`.
- **Contexte**: `REQUIRES_REVIEW:` en tête de `AndroidManifest.xml`,
  `CameraXFrameSource.kt`, `PermissionScreen.kt` et de la partie permission du
  ViewModel. CameraX `ImageAnalysis` en RGBA_8888, `KEEP_ONLY_LATEST`, 1280×720.
  Torche masquée si absente.
- **Critères**: tests instrumentés verts sur la tablette; AC1, AC4, AC7 vérifiés à
  la main et consignés dans `tests/field/`.
- **Cas de test**: nominal — `FakeFrameSource` émet du bleu → « Blue » affiché;
  frontière — image très sombre → « Too dark »; aucune image encore → réticule seul;
  rotation → état conservé; erreur — permission refusée → écran explicatif (AC6);
  caméra indisponible → état `Failure`.
- **Comportements à risque**: aucun existant côté app; le cœur n'est pas modifié.
- **Dépendances**: T05, T06, T09.
- **Vérification**:
  `./gradlew :core:test :app:connectedDebugAndroidTest :app:installDebug && adb shell am start -W -n com.morialkar.goosescope/.app.MainActivity`

## T11 — App: mode figé, fiche, toucher

- **Objectif**: figer l'image, afficher la fiche détaillée, toucher pour viser,
  reprendre.
- **Exigences**: F6, F14, F15, F16, NF8, AC5, AC11; ARCHITECTURE §2.2.
- **Fichiers**: `app/.../app/ui/{FrozenScreen,DetailSheet}.kt`, `ScopeViewModel.kt`,
  `strings.xml`, `app/src/androidTest/.../{FreezeTest,TapTest}.kt`.
- **Contexte**: `REQUIRES_REVIEW:` le chemin de l'image figée (copie en mémoire,
  libération). La fiche ne masque jamais le point visé (STANDARDS §4).
- **Critères**: AC5 et AC11 automatisés verts; ouverture de fiche ≤ 300 ms mesurée.
- **Cas de test**: nominal — figer sur du orange pâle → « Orange » / « Peach » +
  comparaison + nature; frontière — toucher au bord de l'image; toucher une zone
  sombre → « Too dark »; erreur — toucher dans la zone rognée → ignoré.
- **Comportements à risque**: mode direct (T10) — les tests de T10 doivent rester verts.
- **Dépendances**: T07, T10.
- **Vérification**: `./gradlew :core:test :app:connectedDebugAndroidTest`

## T12 — App: langue EN/FR

- **Objectif**: bascule de langue, anglais par défaut, mémorisée par le système.
- **Exigences**: F10, F17, AC12; ARCHITECTURE §1.4; STANDARDS §3.
- **Fichiers**: `app/src/main/res/xml/locales_config.xml`, manifeste,
  `app/.../app/ui/LanguageToggle.kt`, `ScopeViewModel.kt`,
  `core/src/test/.../StringsParityTest.kt` ou équivalent côté app,
  `app/src/androidTest/.../LanguageTest.kt`.
- **Critères**: AC12 vert; clés identiques dans les deux `strings.xml`.
- **Cas de test**: nominal — basculer FR → textes FR, fiche FR; frontière — système
  en français à la première installation → app en anglais; relance → langue
  conservée; erreur — clé manquante en FR → test de parité échoue (démontré).
- **Comportements à risque**: T10, T11 — leurs tests restent verts dans les deux langues.
- **Dépendances**: T11.
- **Vérification**: `./gradlew :core:test :app:connectedDebugAndroidTest`

## T13 — Calibration terrain sur la tablette

- **Objectif**: mesurer AC1, AC2, AC4, AC7 sur la vraie tablette et ajuster les
  seuils de `Tuning.kt` et les ancres.
- **Exigences**: AC1, AC2, AC4, AC7, NF1, NF2, NF7; STANDARDS §1.3.
- **Fichiers**: `tests/field/{AC1,AC2,AC4,AC7}.md`, `tests/field/targets.html`
  (planche de cibles affichable sur un 2e écran), `Tuning.kt`, `catalog.json`.
- **Contexte**: la planche couvre toutes les paires de confusion. Chaque
  ajustement est consigné avec la mesure qui le justifie. Tâche en partie
  manuelle: Naomi tient la tablette ou la pose devant la planche; Claude lit les
  résultats par capture d'écran ADB.
- **Critères**: AC2 ≥ 90 %; AC1 ≤ 2 s; AC4 ≤ 1 changement/s; AC7 conforme;
  `:core:test` toujours vert après ajustement.
- **Cas de test**: nominal/frontière/erreur = les cibles de la planche, la frontière
  choisie pour AC4, la caméra couverte.
- **Dépendances**: T12.
- **Vérification**: `./gradlew :core:test` + rapports `tests/field/*.md` remplis.

## T14 — Vérification visuelle sur émulateur

- **Objectif**: captures d'écran pour valider le style (AC8).
- **Exigences**: NF3, NF4, NF5, AC8.
- **Fichiers**: `tests/field/AC8.md`, `tests/field/screenshots/*.png`.
- **Contexte**: l'utilisateur principal a un téléphone Samsung haut de gamme récent:
  créer un AVD à ce format (profil matériel personnalisé 1440×3120, ≈ 500 dpi, image système API 36 ou 37
  disponible); caméra de l'émulateur en scène virtuelle. La tablette reste
  l'appareil de test caméra, l'émulateur fait foi pour le rendu sur l'appareil de
  l'utilisateur principal.
  Captures: direct portrait et paysage, fiche portrait et paysage, EN et FR,
  écran de permission, « trop sombre ».
- **Critères**: Naomi valide les captures contre le prompt de style.
- **Dépendances**: T13.
- **Vérification**: `adb -e exec-out screencap -p > tests/field/screenshots/<nom>.png`
  pour chaque écran, puis validation humaine.

## T15 — Signature de publication et GitHub Release

- **Objectif**: APK release signé, publié en GitHub Release.
- **Exigences**: F22, AC16, NG4 amendé.
- **Fichiers**: `app/build.gradle.kts` (config de signature lue depuis
  `~/.gradle/gradle.properties`, jamais depuis le dépôt), `docs/RELEASING.md`.
- **Contexte**: `REQUIRES_REVIEW:` gestion de la clé. **Naomi génère elle-même** la
  clé avec `keytool` et choisit les mots de passe (Claude ne saisit pas de mot de
  passe); Claude fournit la commande et la configuration Gradle. `versionCode`
  incrémenté à chaque publication.
- **Critères**: AC16 — APK v1 téléchargé depuis la Release s'installe sur la
  tablette; un APK v2 de test s'installe par-dessus sans désinstaller.
- **Cas de test**: nominal — installation v1; frontière — mise à jour v1→v2;
  erreur — construction release sans les propriétés de signature → échec clair,
  pas d'APK non signé publié.
- **Dépendances**: T14.
- **Vérification**:
  `./gradlew :app:assembleRelease && apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk && gh release view`

## T16 — Page GitHub Pages bilingue

- **Objectif**: page statique EN/FR d'explication et de téléchargement.
- **Exigences**: F20, F21, AC15; ARCHITECTURE §9; STANDARDS §4.
- **Fichiers**: `site/index.html`, `site/style.css`, `site/screenshots/*`,
  configuration Pages (`gh api` ou workflow de déploiement depuis `site/`).
- **Contexte**: aucun script tiers, aucune police externe, aucun analytics; bascule
  EN/FR en HTML/CSS/JS local; lien vers la dernière Release
  (`/releases/latest/download/goosescope.apk`). Captures de T14 réutilisées.
- **Critères**: AC15 — affichage correct sur téléphone et ordinateur dans les deux
  langues, aucune requête vers un autre domaine que GitHub (vérifié dans le
  navigateur intégré, onglet réseau).
- **Cas de test**: nominal — page EN; frontière — largeur 360 px; bascule FR;
  erreur — lien de téléchargement mort détecté (vérification HTTP du lien).
- **Dépendances**: T15.
- **Vérification**: `curl -sI https://morialkar.github.io/goosescope/` (200) +
  `curl -sIL <lien APK>` (200) + contrôle visuel dans le navigateur intégré.

## Phase finale — Revue adversariale

Lecture seule. Grille de qualité (OUI/NON justifié par élément), marqueurs non
résolus (`ASSUMPTION`, `REQUIRES_REVIEW`, `UNCLEAR`), un verdict: READY,
READY WITH EXPLICITLY ACCEPTED RISKS, ou NOT READY.

---

## Backlog (hors périmètre, AFTER règle 8)

- Version iOS (NG5).
- Mode adapté à un type de daltonisme connu (NG8).
- Publication Play Store (NG4).
