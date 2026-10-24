package com.morialkar.goosescope.core.classify

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.catalog.Shade
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Oklab

/**
 * Couche 2 (SPEC F13, F19, A4): nuance la plus proche parmi celles de la base déjà retenue.
 * Chercher seulement dans cette base garantit que la nuance affichée lui appartient toujours.
 */
class ShadeMatcher(catalog: Catalog) {
    private val byBase: Map<BaseColor, List<Shade>> = catalog.shades.groupBy { it.base }

    fun match(c: Oklab, base: BaseColor): Shade {
        val candidates = byBase[base]
        check(!candidates.isNullOrEmpty()) { "Aucune nuance pour la base ${base.id} dans le catalogue" }
        return candidates.minBy { ColorMath.distance(it.refLab, c) }
    }
}
