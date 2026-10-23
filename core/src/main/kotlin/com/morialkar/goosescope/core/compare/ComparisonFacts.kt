package com.morialkar.goosescope.core.compare

import com.morialkar.goosescope.core.catalog.BaseColor

enum class Closeness { VERY_CLOSE, SLIGHT, CLEAR }
enum class LightnessShift { LIGHTER, DARKER, SAME }
enum class ChromaShift { MORE_VIVID, PALER, SAME }
enum class TemperatureShift { WARMER, COOLER, SAME }

/** Écart entre la couleur mesurée et la nuance de référence, sans texte (ARCHITECTURE §3.6). */
data class ComparisonFacts(
    val closeness: Closeness,
    val hueShiftToward: BaseColor?,
    val lightness: LightnessShift,
    val chroma: ChromaShift,
    val temperature: TemperatureShift,
)
