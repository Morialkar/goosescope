package com.morialkar.goosescope.core.compare

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Lang

/**
 * Phrase de comparaison (SPEC F15). Le sujet est toujours « cette teinte » / « this shade »,
 * féminin en français, ce qui fixe l'accord des adjectifs (ARCHITECTURE §3.6).
 */
object ComparisonText {

    fun render(facts: ComparisonFacts, shadeName: String, lang: Lang, baseName: (BaseColor) -> String): String =
        when (lang) {
            Lang.EN -> english(facts, shadeName, baseName)
            Lang.FR -> french(facts, shadeName, baseName)
        }

    private fun english(f: ComparisonFacts, shade: String, baseName: (BaseColor) -> String): String {
        if (f.closeness == Closeness.VERY_CLOSE) return "This shade is very close to $shade."
        val clauses = buildList {
            f.hueShiftToward?.let {
                val amount = if (f.closeness == Closeness.CLEAR) "clearly" else "slightly"
                add("has $amount more ${baseName(it).lowercase()}")
            }
            when (f.lightness) {
                LightnessShift.LIGHTER -> add("is lighter")
                LightnessShift.DARKER -> add("is darker")
                LightnessShift.SAME -> Unit
            }
            when (f.chroma) {
                ChromaShift.MORE_VIVID -> add("is more vivid")
                ChromaShift.PALER -> add("is paler")
                ChromaShift.SAME -> Unit
            }
            when (f.temperature) {
                TemperatureShift.WARMER -> add("feels warmer")
                TemperatureShift.COOLER -> add("feels cooler")
                TemperatureShift.SAME -> Unit
            }
        }
        if (clauses.isEmpty()) return "This shade is close to $shade."
        return "Compared with $shade, this shade ${join(clauses, "and")}."
    }

    private fun french(f: ComparisonFacts, shade: String, baseName: (BaseColor) -> String): String {
        if (f.closeness == Closeness.VERY_CLOSE) return "Cette teinte est très proche de la nuance $shade."
        val clauses = buildList {
            f.hueShiftToward?.let {
                val amount = if (f.closeness == Closeness.CLEAR) "nettement" else "légèrement"
                val name = baseName(it).lowercase()
                val de = if (name.first() in "aeiouyhéèê") "d'" else "de "
                add("contient $amount plus $de$name")
            }
            when (f.lightness) {
                LightnessShift.LIGHTER -> add("est plus claire")
                LightnessShift.DARKER -> add("est plus foncée")
                LightnessShift.SAME -> Unit
            }
            when (f.chroma) {
                ChromaShift.MORE_VIVID -> add("est plus vive")
                ChromaShift.PALER -> add("est plus pâle")
                ChromaShift.SAME -> Unit
            }
            when (f.temperature) {
                TemperatureShift.WARMER -> add("paraît plus chaleureuse")
                TemperatureShift.COOLER -> add("paraît plus froide")
                TemperatureShift.SAME -> Unit
            }
        }
        if (clauses.isEmpty()) return "Cette teinte est proche de la nuance $shade."
        return "Par rapport à la nuance $shade, cette teinte ${join(clauses, "et")}."
    }

    private fun join(parts: List<String>, and: String): String =
        if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " $and " + parts.last()
}
