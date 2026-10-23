package com.morialkar.goosescope.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// STANDARDS §3: les deux langues ont exactement les mêmes clés de texte.
class StringsParityTest {
    private fun keys(dir: String): Set<String> {
        val file = File("src/main/res/$dir/strings.xml")
        assertTrue("introuvable: ${file.absolutePath}", file.exists())
        return Regex("""<string name="([^"]+)"""").findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun english_and_french_have_the_same_keys() {
        val en = keys("values")
        val fr = keys("values-fr")
        assertEquals("Clés manquantes en FR: ${en - fr}; en EN: ${fr - en}", en, fr)
    }

    @Test
    fun parity_check_detects_a_missing_key() {
        val en = setOf("a", "b")
        val fr = setOf("a")
        assertEquals(setOf("b"), en - fr)
    }
}
