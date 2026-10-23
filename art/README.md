# Icône Goosescope

- `goosescope-icon.svg`: source (grille d'icône adaptative Android 108×108, zone sûre = cercle de
  rayon 33). Les drawables `app/src/main/res/drawable/ic_launcher_*.xml` en sont la traduction.
- Dessin à plat d'une oie blanche, bec orange; l'œil reprend le réticule de l'app.

## Couleurs et daltonisme

L'app est faite pour une personne daltonienne: le premier jet (bec #E8781E sur disque vert
#5F7A5A) tombait à **1,29:1** entre bec et disque en vision protan, donc indiscernables.
Palette retenue après simulation (Machado et al. 2009, sévérité 1,0) des visions protan, deutan
et tritan, contraste WCAG minimal sur les quatre visions:

| Élément | Couleur | Contraste minimal |
|---|---|---|
| Fond | #5A6370 | — |
| Disque | #264034 | 1,84:1 avec le fond |
| Bec | #FFA94D | 5,03:1 avec le disque, 2,74:1 avec le fond |
| Mandibule | #E07F2E | 3,17:1 avec le disque |
| Cou et tête | #F4F2EC | 9,75:1 avec le disque |

`icon-colorblind-check.png`: rendu en vision normale, protan, deutan et tritan.
