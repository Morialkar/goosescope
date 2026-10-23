package com.morialkar.goosescope.core

// Seuils de l'algorithme, en un seul endroit (STANDARDS §1.3). Valeurs de départ (ASSUMPTION)
// à calibrer sur la tablette en T13; noter la mesure qui justifie tout changement.
object Tuning {
    /** Côté du carré échantillonné, en fraction de la plus petite dimension de l'image (ARCHITECTURE §2.1). */
    const val SAMPLE_SIDE_FRACTION = 0.02

    /** Luminance relative moyenne de toute l'image sous laquelle la scène est « trop sombre » (§3.4). */
    const val TOO_DARK_MEAN_LUMINANCE = 0.02

    /** Pas de sous-échantillonnage pour la luminance moyenne: 1 pixel sur 8 dans chaque axe suffit. */
    const val LUMINANCE_STEP = 8

    /** Poids de la nouvelle mesure dans la moyenne exponentielle OKLab (§3.5). */
    const val SMOOTHING_ALPHA = 0.35

    /** Avance minimale, en distance OKLab, qu'une autre base doit prendre pour remplacer l'actuelle (§3.5). */
    const val HYSTERESIS_MARGIN = 0.015

    /** Durée minimale pendant laquelle une nouvelle base doit gagner avant d'être affichée (§3.5). */
    const val HYSTERESIS_DWELL_MS = 250L

    // Comparaison (§3.6).
    const val COMPARE_VERY_CLOSE = 0.02
    const val COMPARE_SLIGHT = 0.06
    const val COMPARE_MIN_CHROMA = 0.03
    const val COMPARE_HUE_DEG = 6.0
    const val COMPARE_LIGHTNESS = 0.03
    const val COMPARE_CHROMA = 0.02
    const val WARM_POLE_DEG = 50.0
    const val COOL_POLE_DEG = 230.0
}
