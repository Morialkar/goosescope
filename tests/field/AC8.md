# AC8 — validation visuelle (format de l'appareil de l'utilisateur principal)

- Émulateur `Goosescope_S26U`: 1440×3120, densité 600 (≈ 384 dp de large, comme un Galaxy
  Ultra récent), image système API 37, caméra arrière en scène virtuelle.
- 2026-10-23, captures dans `screenshots/`:
  - `live-portrait-en.png`: mode direct, « Gray » sur le damier gris/noir du téléviseur virtuel.
  - `frozen-portrait-en.png`, `frozen-portrait-fr.png`: fiche « Brown → Coffee » sur le mur en bois.
  - `frozen-landscape-fr.png`, `live-landscape-fr.png`: paysage, panneau à droite.
  - `permission-denied-en.png`: écran de permission refusée.
- Démarrage à froid sur l'émulateur: 4967 ms (premier lancement après installation, émulateur
  logiciel; la mesure qui fait foi pour NF7 est celle de la tablette, AC1).
- Limite observée: une image figée en portrait puis affichée en paysage est rognée en haut et en
  bas; le point visé peut sortir de la zone visible (le réticule disparaît). Backlog.
- 2026-10-23 (après révision 4 et fiche animée): `live-portrait-en.png` (« Gray — Charcoal »),
  `frozen-portrait-en.png`, `frozen-portrait-fr.png` refaites. Corrigés au passage: fiche coupée au
  milieu de la comparaison sur téléphone (hauteur 62 %, bandeau 112 dp, indication déplacée dans la
  zone qui défile), marge de barre d'état inutile au-dessus de la fiche, fiche qui changeait de côté
  en paysage au moment de figer.
- Tablette, tissu violet réel: direct « Purple — Plum », figé « Purple → Plum », toucher plus bas
  « Purple → Grape ».
- **En attente**: validation de Naomi contre le prompt de style.
