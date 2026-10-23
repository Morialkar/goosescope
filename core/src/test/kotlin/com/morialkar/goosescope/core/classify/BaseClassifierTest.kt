package com.morialkar.goosescope.core.classify

import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Srgb8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaseClassifierTest {
    private val catalog = Catalog.loadBundled()
    private val classifier = BaseClassifier(catalog)

    private data class Ref(val hex: String, val expected: BaseColor, val kind: String, val source: String)

    private val refs: List<Ref> = checkNotNull(javaClass.getResourceAsStream("/reference-colors.csv"))
        .bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map { line ->
            val (hex, expected, kind, source) = line.split(",", limit = 4)
            Ref(hex, checkNotNull(BaseColor.fromId(expected)) { expected }, kind, source)
        }

    private fun classify(hex: String) = classifier.classify(ColorMath.toOklab(Srgb8.fromHex(hex))).base

    @Test
    fun `classifies every typical reference color`() {
        val typical = refs.filter { it.kind == "typical" }
        val wrong = typical.filter { classify(it.hex) != it.expected }.map { "${it.hex} ${it.source} -> ${classify(it.hex)}" }
        assertTrue(wrong.isEmpty(), "Mal classées: $wrong")
        // Chaque base a au moins un cas typique (AC3, cas nominal).
        assertEquals(BaseColor.entries.toSet(), typical.map { it.expected }.toSet())
    }

    @Test
    fun `classifies at least 90 percent of frontier colors`() {
        val frontier = refs.filter { it.kind == "frontier" }
        val ok = frontier.count { classify(it.hex) == it.expected }
        assertTrue(ok >= 0.9 * frontier.size, "$ok/${frontier.size} cas frontière")
    }

    @Test
    fun `covers the classic confusion pairs`() {
        // AC2: chaque paire de confusion a au moins une couleur de chaque côté dans le jeu de référence.
        val pairs = listOf(
            BaseColor.RED to BaseColor.GREEN, BaseColor.GREEN to BaseColor.BROWN, BaseColor.ORANGE to BaseColor.GREEN,
            BaseColor.BLUE to BaseColor.PURPLE, BaseColor.PINK to BaseColor.GRAY, BaseColor.BLUE to BaseColor.GREEN,
            BaseColor.YELLOW to BaseColor.PINK,
        )
        val present = refs.map { it.expected }.toSet()
        pairs.forEach { (a, b) -> assertTrue(a in present && b in present, "$a/$b") }
    }

    @Test
    fun `dark brown is not black and warm gray is not brown`() {
        assertEquals(BaseColor.BROWN, classify("#3B2A1A"))
        assertEquals(BaseColor.BLACK, classify("#141414"))
        assertEquals(BaseColor.GRAY, classify("#8A8580"))
        assertEquals(BaseColor.PINK, classify("#FFB6C1"))
    }

    @Test
    fun `every base has between 3 and 10 anchors`() {
        catalog.bases.forEach { assertTrue(it.anchors.size in 3..10, "${it.color}: ${it.anchors.size} ancres") }
    }

    @Test
    fun `distance to the winning base is the match distance`() {
        val c = ColorMath.toOklab(Srgb8.fromHex("#FFCBA4"))
        val match = classifier.classify(c)
        assertEquals(match.distance, classifier.distanceTo(match.base, c))
        BaseColor.entries.forEach { assertTrue(classifier.distanceTo(it, c) >= match.distance) }
    }
}
