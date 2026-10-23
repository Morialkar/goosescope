# AC2 — précision sur cibles réelles

Statut: en cours. La séance complète avec la planche (`targets.html`, 49 cibles) reste à faire.

## Observations terrain

- 2026-10-23, tablette SM-X528U, tissu violet terne, éclairage intérieur:
  - Mode direct: « Purple — Plum » (correct).
  - Mode figé, point au centre: pixels mesurés #695086 (OKLCh C = 0,089, h = 305°) classés
    **Gray → Charcoal** (incorrect). Cause: l'ancre grise #606060 était plus proche en distance
    OKLab que les ancres violettes, toutes très saturées.
  - Correctif: seuil de chroma `Tuning.NEUTRAL_MAX_CHROMA = 0,06` (au-delà, jamais gris/noir/blanc)
    et deux ancres violettes ternes (#725684, #8A7AA0). Jeu de référence: 94/94 inchangé, 122/122
    nuances toujours classées dans leur base; les 4 pixels du tissu sont maintenant « Purple ».
