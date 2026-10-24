# Plan — Goosescope iOS

Phase 2 AFTER (workflow Feature), 2026-10-24. Références: SPEC-IOS.md, IMPACT-IOS.md. Même règles
que PLAN.md: une tâche = un commit sur `main`, vérification affichée avant de dire « fait », tests
avec le code. Naomi a demandé d'enchaîner les tâches sauf décision.

Commandes: depuis `~/Developer/goosescope`, `JAVA_HOME` = JDK d'Android Studio. Signature iOS:
`DEVELOPMENT_TEAM` passé en ligne de commande (jamais dans le dépôt). Appareil: iphone nao.

| Tâche | Objectif | Dépend de |
|---|---|---|
| I1 | `:core` multiplateforme, tests sur JVM et macOS, Android intact | — |
| I2 | XCFramework `GoosescopeCore` et projet iOS minimal installé sur iphone nao | I1 |
| I3 | Mode direct iOS (caméra, réticule, base — nuance, trop sombre, lampe, permission) | I2 |
| I4 | Mode figé, fiche animée, fiche réductible, toucher | I3 |
| I5 | Langue EN/FR dans l'app | I4 |
| I6 | Tests d'interface iOS sur iphone nao (caméra factice) | I5 |
| I7 | Contrôle iOS et extension écran verrouillé | I6 |
| I8 | Vérification « aucun réseau », captures, validation visuelle | I7 |
| — | Revue adversariale | I8 |

## I1 — Cœur multiplateforme
- **Exigences**: F30, AC21. **Fichiers**: `core/build.gradle.kts`, `core/src/{commonMain,commonTest}`,
  tâches `generateCatalogSource` et `generateTestResources`, `gradle/libs.versions.toml`.
- **Contexte**: IMPACT §2.1, §2.2. Corriger `toHex`, degrés, chargement du catalogue.
- **Critères**: même nombre de tests qu'avant (comptés avant/après), tous verts sur `jvmTest` et
  `macosArm64Test`; Android: tests unitaires et 23 tests instrumentés verts sur la tablette.
- **Cas**: nominal = suite complète; frontière = `toHex` sur 0 et 255; erreur = catalogue invalide
  toujours refusé sur les deux cibles.
- **Comportements à risque**: tout l'Android (IMPACT §3).
- **Vérification**: `./gradlew :core:allTests :app:testDebugUnitTest :app:connectedDebugAndroidTest`

## I2 — XCFramework et squelette iOS
- **Exigences**: F31, NF10, NF13, AC22 (partiel). **Fichiers**: `ios/project.yml`,
  `ios/Goosescope/{GoosescopeApp,Tokens}.swift`, `Info.plist` (texte caméra EN/FR), `ios/build.sh`.
- **Critères**: l'app s'installe sur iphone nao et affiche un écran aux jetons qui appelle le cœur
  (nom de la base d'une couleur fixe, preuve que l'interop marche).
- **Vérification**: `ios/build.sh install` (Gradle → XCFramework → XcodeGen → xcodebuild →
  devicectl install/launch).

## I3 — Mode direct
- **Exigences**: F1–F5, F7, F8, F12, F24, F29. **Fichiers**: `CameraSource.swift`,
  `PixelBufferGrid.swift`, `ScopeModel.swift`, `LiveView.swift`, `PermissionView.swift`.
- **Critères**: sur iphone nao, la caméra affiche « base — nuance » qui suit la couleur visée;
  « trop sombre » caméra couverte; lampe; écran de permission refusée. Coût de lecture des pixels
  mesuré (IMPACT §2.6).
- **Vérification**: build + install + captures `xcrun devicectl` / observation; tests en I6.

## I4 — Mode figé et fiche
- **Exigences**: F6, F14–F16, F25, F26. **Fichiers**: `FrozenView.swift`, `DetailSheet.swift`.
- **Critères**: parité visuelle et comportementale avec Android (fiche animée, étiquettes, bandeau,
  barre compacte, réticule qui glisse).

## I5 — Langue
- **Exigences**: F17, F29. **Fichiers**: `L10n.swift`, `en.lproj`, `fr.lproj`.
- **Critères**: anglais au premier lancement même si iOS est en français; bascule immédiate; choix
  conservé après relance; test de parité des clés.

## I6 — Tests d'interface
- **Exigences**: AC23, AC24. **Fichiers**: cible `GoosescopeUITests`, caméra factice activée par un
  argument de lancement.
- **Critères**: équivalents iOS des tests instrumentés Android, verts sur iphone nao.
- **Vérification**: `ios/build.sh test` (`xcodebuild test` sur iphone nao).

## I7 — Contrôle et écran verrouillé
- **Exigences**: F32, F33, AC25. **Fichiers**: extensions `GoosescopeControls`,
  `GoosescopeLockedCapture`, `OpenScopeIntent.swift`. `REQUIRES_REVIEW`.
- **Critères**: contrôle ajouté par Naomi au Centre de contrôle → ouvre le mode direct; iPhone
  verrouillé → l'extension s'affiche sans déverrouiller.

## I8 — Aucun réseau, captures, validation
- **Exigences**: NF12, AC26, AC27. **Fichiers**: `ios/tools/check-no-network.sh`,
  `tests/field/ios/*.png`, `tests/field/AC26-AC27.md`.
- **Critères**: script vert sur le binaire; Naomi valide les captures.
