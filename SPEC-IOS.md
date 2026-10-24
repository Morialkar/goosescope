# Spécification — Goosescope iOS (delta)

Statut: Phase 0 (Scoping), workflow Feature AFTER, demandé par Naomi le 2026-10-24.
Lecture seule: aucun code, aucune configuration. Ce document s'ajoute à SPEC.md (révisions 2 à 6),
qui reste la référence; il n'en reprend que ce qui change pour iOS. Numérotation: on continue celle
de SPEC.md (F29+, NG10+, AC21+).

Décisions de Naomi (2026-10-24): cœur Kotlin partagé (Kotlin Multiplatform) et interface native
SwiftUI; installation directe seulement; tests sur « iphone nao » (iPhone 13 Pro); équivalent iOS
de la tuile inclus, donc iOS 18 minimum.

---

## 1. Conflit avec la spec approuvée (AFTER règle 7)

- **NG5** (« Pas de version iOS ») est levé par cette demande. Proposition: NG5 devient « Pas de
  distribution publique de la version iOS (ni App Store ni TestFlight) dans ce périmètre ».
- Le module `:core` doit devenir multiplateforme: c'est une modification d'architecture de l'app
  Android existante (analyse d'impact en Phase 1).

## 2. Objectifs

- G6. Offrir sur iPhone la même app que sur Android: mode direct, mode figé, fiche détaillée,
  langues, trop sombre, lampe, raccourci sans déverrouiller.
- G7. Une seule source pour la logique de couleur et le contenu (ancres, 122 fiches, seuils,
  comparaisons): un réglage corrige les deux apps, les mêmes tests les protègent.

## 3. Non-objectifs

- NG10. Pas de distribution publique iOS (voir NG5 modifié); le site ne mentionne pas iOS.
- NG11. Pas d'app iPad dédiée ni de Mac Catalyst (l'app iPhone peut tourner sur iPad en mode
  compatibilité, sans optimisation).
- NG12. Pas de widget iOS (même raison que NG9).
- Tous les non-objectifs de SPEC.md restent valables (pas d'IA, pas d'enregistrement d'image,
  pas de réseau…).

## 4. Exigences

### 4.1 Parité

- F29. L'app iOS respecte F1 à F26 de SPEC.md, avec le même comportement, les mêmes textes (EN/FR)
  et le même style visuel (STANDARDS §4), adaptés aux conventions iOS quand une règle Android n'a pas
  d'équivalent:
  - F7 (permission refusée) → bouton vers la page de l'app dans Réglages iOS;
  - F26 (retour Android) → geste de retour / bouton « Retour à la caméra »;
  - F17 (langue réglable, anglais par défaut, mémorisée) → réglage dans l'app, indépendant de la
    langue d'iOS. `ASSUMPTION`: iOS ne permet pas de changer la langue d'une app depuis l'app
    elle-même; l'app gérera donc ses propres textes. Mécanisme tranché en Phase 1.

### 4.2 Propres à iOS

- F30. Le cœur partagé (`:core`) est compilé pour iOS par Kotlin Multiplatform et utilisé tel quel
  par l'app iOS; aucune règle de couleur n'est réécrite en Swift.
- F31. Caméra par AVFoundation, interface en SwiftUI.
- F32. Contrôle iOS 18 « Goosescope » pour le Centre de contrôle et l'écran verrouillé, assignable
  au bouton Action: il ouvre le mode direct (équivalent de F27).
- F33. Depuis l'écran verrouillé, le contrôle ouvre Goosescope sans déverrouiller (équivalent de
  F28). `ASSUMPTION` forte: iOS ne permet cela qu'aux apps de type caméra, par une extension
  « Locked Camera Capture » (iOS 18) qui tourne séparément de l'app. Faisabilité et coût à confirmer
  en Phase 1; si l'extension s'avère impossible, `DECISION_REQUIRED` entre « déverrouillage
  demandé » et « pas de contrôle sur l'écran verrouillé ».
  `REQUIRES_REVIEW:` affichage sur l'écran verrouillé.

## 5. Exigences non fonctionnelles

- NF10. iOS 18 minimum (exigé par F32), iPhone.
- NF11. Mêmes cibles que NF1 à NF8 (latence ≤ 300 ms, démarrage ≤ 2 s…), mesurées sur iphone nao.
- NF12. Aucun accès réseau: iOS n'a pas de permission réseau à retirer, donc la garantie passe par
  l'absence de tout appel réseau dans le code et les dépendances (vérification définie en Phase 1).
- NF13. Seule autorisation demandée: la caméra (`NSCameraUsageDescription`, texte EN/FR).

## 6. Contraintes

- **Identifiant**: `com.morialkar.goosescope` (le même que sur Android).
- **Signature**: équipe de développement de Naomi, passée en ligne de commande comme pour
  Arquanzia; le dépôt public ne contient ni identifiant d'équipe ni profil.
- **Appareil de test**: iphone nao, iPhone 13 Pro (identifiant d'appareil hors du dépôt).
- **Outillage**: Xcode 27 présent; aucun runtime de simulateur iOS installé.
  `ASSUMPTION`: les tests du cœur partagé tourneront sur une cible macOS (arm64) du Mac plutôt que
  sur un simulateur, pour éviter un téléchargement de plusieurs Go; à confirmer en Phase 1.
- **Dépôt**: même dépôt public; dossier `ios/` pour l'app iOS. Mêmes règles d'anonymat
  (STANDARDS §8) et de secrets: aucun profil ni certificat versionné.
- **Dépendances**: Kotlin Multiplatform et kotlinx.serialization (déjà permises), frameworks Apple
  du système. Aucune dépendance Swift tierce sans `DECISION_REQUIRED`.

## 7. Critères d'acceptation

- AC21. Les tests existants du cœur passent sur la JVM **et** sur la cible Apple choisie (même code,
  mêmes résultats), et l'app Android passe encore tous ses tests (non-régression).
- AC22. L'app iOS s'installe sur iphone nao, s'ouvre en ≤ 2 s sur la caméra avec le réticule.
- AC23. Sur une même couleur de référence, Android et iOS affichent la même base et la même nuance
  (test sur les couleurs du jeu de référence, côté iOS).
- AC24. Mode figé, fiche, fiche réductible, toucher pour viser, trop sombre, langue EN/FR (anglais
  par défaut, mémorisée), permission refusée: même comportement qu'AC5 à AC12 et AC17, AC18,
  vérifié par tests d'interface sur iphone nao.
- AC25. Contrôle iOS: ajouté au Centre de contrôle, il ouvre le mode direct; depuis l'écran
  verrouillé, il ouvre Goosescope sans déverrouiller (F33), ou le comportement retenu en Phase 1.
- AC26. Aucune référence à une API réseau dans le binaire iOS (vérification définie en Phase 1).
- AC27. Validation visuelle par Naomi de captures sur iphone nao (portrait, paysage, EN, FR, fiche).

## 8. Questions ouvertes pour la Phase 1

1. Mécanisme de langue dans l'app (F29, F17).
2. Faisabilité de l'extension Locked Camera Capture et partage du cœur avec elle (F33).
3. Passage de `:core` en multiplateforme: chargement de `catalog.json` hors JVM, impact sur l'app
   Android.
4. Cible Apple pour exécuter les tests du cœur (macOS arm64 ou simulateur).
5. Vérification « aucun réseau » côté iOS (NF12, AC26).
