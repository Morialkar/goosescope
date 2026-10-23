# Spécification — Goosescope

Statut: Phase 0 (Spécification), workflow Greenfield AFTER. Numérotation gelée: toute
exigence ajoutée après ce point prend le prochain numéro libre, jamais une insertion.
Aucun code, aucune configuration, aucune dépendance produite à ce stade.

Révision 2 (2026-10-23): décisions D1 à D7 tranchées par Naomi, ajout de la couche de
détail sur image figée (F13 à F19). Les points marqués `ASSUMPTION` restent à confirmer.

---

## 1. Problème

Une personne daltonienne ne peut pas toujours savoir, devant un objet, de quelle
couleur il est, et certaines paires de couleurs sont indiscernables. Demander à
quelqu'un n'est pas toujours possible ni souhaitable. Au-delà du simple nom, elle
n'a pas accès à la « sensation » d'une nuance (chaleureuse, pâle, terreuse…).

## 2. Utilisateurs

- **Utilisateur principal**: une personne daltonienne, seul utilisateur, usage personnel, sans profils.
  Pointe la caméra d'un appareil Samsung vers un objet pour lire le nom de sa couleur,
  et fige l'image pour en apprendre plus sur une nuance.
- **Utilisatrice secondaire**: Naomi, qui construit, installe, maintient l'app et
  valide les textes descriptifs.

## 3. Objectifs

- G1. Afficher en continu le nom de base de la couleur visée par la caméra.
- G2. Afficher aussi un aplat de cette couleur, pour confirmation visuelle.
- G3. Être lisible d'un coup d'œil: style « flat colors, easy to see, not too bright,
  not too dark ».
- G4. Fonctionner entièrement hors ligne, sans compte, sans transmission de données.
- G5. Sur image figée, donner une description riche de la nuance: nom précis,
  comparaison évocatrice, éléments de la nature qui partagent cette teinte.

## 4. Non-objectifs (explicites)

- NG1. Pas de correction / filtre daltonien de l'image (type EnChroma). On nomme, on
  ne transforme pas.
- NG2. Pas de reconnaissance d'objets (« c'est une pomme »).
- NG3. Pas d'enregistrement de photo, d'historique, de galerie ni de partage. L'image
  figée n'existe qu'en mémoire et disparaît à la reprise.
- NG4. Pas de publication sur le Play Store dans ce périmètre (installation directe).
- NG5. Pas de version iOS dans ce périmètre.
- NG6. Pas de calibration colorimétrique « de laboratoire »: l'objectif est un nom
  de couleur fiable pour un humain, pas une valeur colorimétrique certifiée.
- NG7. Pas de texte généré par IA ni par un service en ligne (D6).
- NG8. Pas de mode adapté à un type de daltonisme précis (D5).

## 5. Exigences fonctionnelles

- F1. Au lancement, l'app ouvre directement la vue caméra arrière plein écran.
- F2. En mode direct, un réticule fixe au centre de l'écran indique la zone visée.
- F3. La couleur visée est calculée sur une petite zone sous le réticule (moyenne
  de plusieurs pixels), pas sur un pixel unique, pour réduire le bruit.
- F4. En mode direct, l'app affiche le nom de base (couche 1, F13) en grand texte,
  et un aplat de la couleur mesurée.
- F5. Le résultat est lissé dans le temps pour ne pas clignoter entre deux noms
  quand la caméra bouge légèrement ou que la couleur est à une frontière.
- F6. Un bouton « figer » gèle l'image de la caméra (et non seulement le résultat).
  Sur l'image figée, l'app ouvre la fiche détaillée (F14) de la couleur visée.
  Un bouton « reprendre » revient au mode direct.
- F7. Si la permission caméra est refusée, l'app affiche un écran clair expliquant
  pourquoi elle en a besoin et comment l'accorder, sans planter.
- F8. Si la zone visée est trop sombre pour un résultat fiable, l'app le dit
  (« trop sombre ») au lieu d'inventer une couleur. Même règle sur l'image figée.
- F9. Noms de couleur en deux couches (D1, voir F13).
- F10. Langue anglais / français, réglable, anglais par défaut (D2, voir F17).
- F11. Visée: centre en direct, toucher n'importe quel point sur l'image figée
  (D3, voir F16).
- F12. Lampe torche activable depuis l'app en mode direct (A1, confirmée).
- F13. Deux couches de noms:
  - Couche 1: 11 couleurs de base — rouge, orange, jaune, vert, bleu, violet, rose,
    brun, gris, noir, blanc (EN: red, orange, yellow, green, blue, purple, pink,
    brown, gray, black, white).
  - Couche 2: liste étendue de 100 à 150 nuances (D7), chacune rattachée à
    exactement une couleur de base (ex. Pêche → Orange).
- F14. Fiche détaillée (sur image figée), contenant:
  - le nom de base (couche 1) et le nom de nuance (couche 2);
  - un aplat de la couleur mesurée, à côté d'un aplat de la nuance de référence;
  - un texte rédigé propre à la nuance (comparaison évocatrice, ex. « ressemble à
    celle d'une pêche »);
  - une phrase générée par règles qui compare la couleur mesurée à la nuance de
    référence (F15);
  - une liste d'éléments de la nature qui partagent cette teinte (F18).
- F15. Phrase de comparaison générée hors ligne (D6, hybride): à partir de l'écart
  entre la couleur mesurée et la nuance de référence, l'app dit quelle composante
  domine davantage (« le rouge y est légèrement plus présent »), si elle est plus
  claire/foncée, plus vive/pâle, et l'effet ressenti (plus chaleureuse / plus
  froide). Si l'écart est négligeable, la phrase dit que la couleur est très proche
  de la nuance de référence. Les seuils « légèrement / nettement » sont définis en
  Phase 1.
- F16. Sur l'image figée, toucher un point déplace le réticule à cet endroit et met à
  jour la fiche (couche 1, couche 2, comparaison, nature) pour ce point.
- F17. Réglage de langue EN / FR accessible depuis l'écran principal, anglais par
  défaut, mémorisé entre les lancements. Tous les textes, y compris les fiches de
  nuances et les éléments de la nature, existent dans les deux langues.
- F18. Chaque nuance de la couche 2 a au moins 3 éléments de la nature (fruits,
  fleurs, minéraux, animaux, paysages…) qui partagent cette teinte.
- F19. Cohérence des couches: la nuance affichée en couche 2 appartient toujours à
  la couleur de base affichée en couche 1. On n'affiche jamais « Orange → (nuance
  de Rose) ». `ASSUMPTION`: on détermine d'abord la couleur de base, puis la nuance
  la plus proche parmi celles de cette base.

## 6. Exigences non fonctionnelles

- NF1. Latence perçue entre le mouvement de la caméra et la mise à jour du nom:
  ≤ 300 ms.
- NF2. Fluidité de l'aperçu caméra: ≥ 24 fps sur l'appareil cible.
- NF3. Style visuel: aplats, aucun dégradé, aucune ombre portée décorative; fond
  d'interface en tons moyens (ni blanc pur ni noir pur); contraste du texte
  conforme WCAG AA (≥ 4.5:1) contre son fond, y compris sur un aplat de couleur
  mesurée (le texte bascule clair/sombre selon l'aplat).
- NF4. Lisible à bout de bras: nom de base ≥ 32 sp en mode direct; texte de fiche
  ≥ 18 sp.
- NF5. Orientation portrait et paysage supportées (A2, confirmée).
- NF6. Aucune permission autre que CAMERA (la torche en dépend).
- NF7. Démarrage à froid jusqu'à l'aperçu caméra: ≤ 2 s sur l'appareil cible.
- NF8. Ouverture de la fiche après « figer » ou après un toucher: ≤ 300 ms.
- NF9. Les données de nuances (noms, fiches, nature, deux langues) sont embarquées
  dans l'app et versionnées avec le code, dans un format vérifiable par tests.

## 7. Critères d'acceptation (vérifiables)

- AC1. Lancement sur la tablette cible → aperçu caméra plein écran avec réticule
  en ≤ 2 s (F1, F2, NF7).
- AC2. Viser des cibles de référence (pastilles imprimées ou écran affichant des
  couleurs connues) dans un éclairage intérieur normal → le nom de base affiché
  correspond au nom attendu pour ≥ 90 % des cibles. Le jeu de cibles couvre toutes
  les paires de confusion classiques (rouge/vert, vert/brun, orange/vert,
  bleu/violet, rose/gris, bleu/vert, jaune/rose) (F3, F4, D5).
- AC3. Tests unitaires de la classification couche 1: chaque couleur de base a au
  moins un cas nominal, un cas frontière et un cas d'erreur (entrée invalide) (F13).
- AC4. Caméra tenue immobile sur une couleur frontière → le nom ne change pas plus
  d'une fois par seconde (F5).
- AC5. « Figer » → l'image reste fixe quand on déplace l'appareil et la fiche
  s'ouvre; « reprendre » → retour au direct (F6, NF8).
- AC6. Permission refusée → écran explicatif, aucun crash (F7).
- AC7. Caméra couverte → message « trop sombre », pas de nom de couleur (F8).
- AC8. Captures d'écran sur émulateur au format de l'appareil de l'utilisateur principal validées visuellement par
  Naomi contre le prompt de style (NF3).
- AC9. Tests d'intégrité des données: le nombre de nuances est entre 100 et 150;
  chaque nuance a un nom EN et FR, un texte de fiche EN et FR, au moins 3 éléments
  de la nature dans chaque langue, une couleur de référence valide, et une couleur
  de base parente qui fait partie des 11; chaque couleur de base chromatique a au
  moins 5 nuances; la couleur de référence de chaque nuance est classée par la
  couche 1 dans sa propre base parente (F13, F17, F18, F19, NF9).
- AC10. Tests unitaires du générateur de comparaison (F15): cas nominal (écart net
  vers le rouge → mention du rouge et « plus chaleureuse »), cas frontière (écart
  négligeable → « très proche »), cas d'erreur (entrée invalide), dans les deux
  langues.
- AC11. Sur image figée, toucher trois points de couleurs différentes → le
  réticule se déplace et la fiche se met à jour à chaque fois (F16).
- AC12. Basculer la langue → tous les textes visibles changent; relancer l'app →
  la langue choisie est conservée; première installation → anglais (F17).
- AC13. Test de cohérence: pour un échantillon couvrant tout l'espace de couleurs,
  la nuance retournée appartient toujours à la base retournée (F19).
- AC14. Relecture par Naomi d'un échantillon de fiches (au moins une par couleur de
  base) avant distribution.

## 8. Contraintes

- **Appareil de l'utilisateur principal**: téléphone Samsung haut de gamme récent
  (précisé à l'approbation du plan, 2026-10-23). `ASSUMPTION`: Android 16 / API 36,
  écran ≈ 6,9 po, 1440×3120, ≈ 500 dpi.
- **Appareil de test physique**: Samsung Galaxy Tab S10 FE (SM-X528U), Android 16 /
  API 36, 1440×2304, connecté en ADB sans fil. Tests caméra réels sur cet appareil.
- **Vérification visuelle de l'interface**: émulateur Android configuré au format
  de l'appareil de l'utilisateur principal (écran, densité), en portrait et en paysage, en plus de la tablette.
  C'est ce rendu qui fait foi pour AC8, puisque c'est l'appareil réellement utilisé.
- **Plateforme**: Android uniquement (NG5).
- **Stockage**: seulement la préférence de langue. Aucune image stockée (NG3).
- **Confidentialité**: aucune image ne quitte l'appareil ni n'est écrite sur disque;
  aucune bibliothèque d'analytics, de pub ou de crash reporting réseau.
  `REQUIRES_REVIEW:` le code d'accès à la caméra (permission + flux d'images +
  image figée en mémoire) devra être relu par Naomi avant distribution.
- **Réseau**: aucune permission INTERNET dans le manifeste.
- **Dépendances interdites (D4)**: Google Play Services, ML Kit, Firebase, toute
  bibliothèque d'analytics, de pub ou nécessitant le réseau. Les bibliothèques
  AndroidX (ex. CameraX, Compose) sont permises.
- **Contenu**: les 100 à 150 fiches EN + FR sont rédigées par Claude puis relues
  par Naomi (AC14). `ASSUMPTION`: c'est Claude qui rédige le premier jet.
- **Distribution**: APK installé par ADB (NG4).
- **Pile technique**: tranchée en Phase 1 (alternatives à comparer: Kotlin natif +
  CameraX + Jetpack Compose; Capacitor comme Arquanzia; Flutter). Outillage
  présent: SDK Android (platform 37, build-tools 36), JDK fourni par Android Studio.

## 9. Décisions (tranchées le 2026-10-23)

- D1. Noms en deux couches: 11 couleurs de base, puis liste étendue (F13).
- D2. Bilingue EN / FR, réglable, anglais par défaut (F17).
- D3. Réticule au centre en direct; toucher sur image figée (F16).
- D4. Aucune dépendance réseau ni Google Play; AndroidX permis (§8).
- D5. Aucun type de daltonisme ciblé: les tests couvrent toutes les paires de
  confusion (AC2, NG8).
- D6. Descriptions hybrides hors ligne: fiche rédigée par nuance + phrase de
  comparaison générée par règles (F14, F15, NG7).
- D7. Liste étendue de 100 à 150 nuances (F13).

## 10. Hypothèses

- A1. Lampe torche incluse — confirmée (F12).
- A2. Portrait + paysage — confirmée (NF5).
- A3. Un seul utilisateur, sans profils — confirmée (§2).
- A4. La base est déterminée avant la nuance (F19) — à confirmer.
- A5. Claude rédige le premier jet des fiches, Naomi relit (§8) — à confirmer.

---

## 11. Amendement — révision 3 (approuvé par Naomi le 2026-10-23)

Origine: réponse de Naomi en Phase 1 (2026-10-23), « repo public GitHub où on pourra
héberger une petite page de téléchargement et d'explication bilingue avec gh pages ».
Cette section est en vigueur; elle prime sur NG4 et sur la ligne « Distribution » du §8.

- Modifie NG4: toujours pas de Play Store, mais l'APK est distribué publiquement
  via GitHub Releases, en plus de l'installation par ADB.
- F20. Une page web statique bilingue (EN par défaut, FR), hébergée sur GitHub
  Pages, explique l'app et donne le lien de téléchargement du dernier APK.
- F21. La page indique comment installer un APK hors Play Store et ce que l'app ne
  fait pas (aucune donnée envoyée, aucune image enregistrée).
- F22. L'APK publié est signé avec une clé de publication stable, conservée hors du
  dépôt. `REQUIRES_REVIEW:` gestion de la clé.
- F23. Le code source est public sur GitHub.
- AC15. La page s'affiche correctement sur téléphone et ordinateur, dans les deux
  langues, sans script tiers ni requête vers un autre domaine que GitHub.
- AC16. Un APK téléchargé depuis la page s'installe sur la tablette cible, et une
  version suivante s'installe par-dessus sans désinstaller.

---

## 12. Amendement — révision 4 (demandé et approuvé par Naomi le 2026-10-23)

- AC14 réalisé: Naomi a validé les 122 fiches (version 1) le 2026-10-23.
- Modifie F4: en mode direct, l'app affiche le nom de base **et** le nom de nuance, sur une
  ligne, au format « Rouge — Écarlate ». Quand la nuance porte le même nom que la base
  (« Rose », « Noir »…), seul le nom de base s'affiche.
- F24. La nuance affichée en direct est stabilisée comme le nom de base (F5): elle ne change
  que si une autre nuance de la même base devient nettement plus proche. Un changement de base
  change la nuance immédiatement.
- AC17. Caméra immobile sur une couleur à la frontière de deux nuances d'une même base → la
  nuance affichée ne clignote pas (test unitaire sur séquence synthétique); en direct, le texte
  affiché est « <base> — <nuance> » dans la langue de l'app (test instrumenté).
- Précision de présentation de F14 (Naomi, 2026-10-23): les éléments de la nature s'affichent en
  étiquettes; la fiche est animée (STANDARDS §4, « Mouvement »).
