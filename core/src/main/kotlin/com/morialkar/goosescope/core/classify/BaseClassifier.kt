package com.morialkar.goosescope.core.classify

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab

data class BaseMatch(val base: BaseColor, val distance: Double)

/**
 * Couche 1 (SPEC F13, A4): la base est celle de l'ancre la plus proche en OKLab.
 * Les ancres vivent dans catalog.json pour être réglées sans toucher au code (ARCHITECTURE §3.2).
 */
class BaseClassifier(private val catalog: Catalog) {

    fun classify(c: Oklab): BaseMatch =
        catalog.bases.map { BaseMatch(it.color, distanceTo(it.color, c)) }.minBy { it.distance }

    /** Distance à l'ancre la plus proche d'une base donnée; sert à l'hystérésis du lissage (§3.5). */
    fun distanceTo(base: BaseColor, c: Oklab): Double =
        catalog.base(base).anchors.minOf { ColorMath.distance(it.lab, c) }
}
