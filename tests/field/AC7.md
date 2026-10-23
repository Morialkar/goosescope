# AC7 — caméra couverte

- 2026-10-23, tablette posée caméra contre la table (T10): l'app affiche « Trop sombre » et
  l'indication d'ajouter de la lumière, sans nom de couleur. **Conforme.**
- Test automatisé complémentaire: `LiveScreenTest.says_too_dark_when_the_camera_is_covered`,
  `black_object_in_a_lit_scene_is_black_not_too_dark`.
- Reste à faire en T13: objet noir bien éclairé sur la vraie caméra (doit dire « Noir »).
