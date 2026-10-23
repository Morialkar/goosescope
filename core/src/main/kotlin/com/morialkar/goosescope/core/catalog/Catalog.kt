package com.morialkar.goosescope.core.catalog

import com.morialkar.goosescope.core.CATALOG_FORMAT_VERSION
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Srgb8
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Catalogue des couleurs de base et des nuances. Le chargement vérifie la structure;
 * les règles de quantité sont vérifiées par le test du catalogue livré (PLAN T03, T09).
 */
class Catalog private constructor(val bases: List<BaseEntry>, val shades: List<Shade>) {

    fun base(color: BaseColor): BaseEntry = bases.first { it.color == color }

    fun shadesOf(color: BaseColor): List<Shade> = shades.filter { it.base == color }

    companion object {
        private const val MIN_NATURE = 3
        private val ID = Regex("^[a-z][a-z0-9_]*$")
        private val HEX = Regex("^#[0-9A-Fa-f]{6}$")
        private val json = Json { ignoreUnknownKeys = false }

        fun loadBundled(): Catalog {
            val stream = Catalog::class.java.getResourceAsStream("/catalog.json")
                ?: throw CatalogException(listOf("catalog.json introuvable dans les ressources"))
            return load(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }

        fun load(text: String): Catalog {
            val raw = try {
                json.decodeFromString(CatalogJson.serializer(), text)
            } catch (e: SerializationException) {
                throw CatalogException(listOf("JSON illisible: ${e.message}"))
            } catch (e: IllegalArgumentException) {
                throw CatalogException(listOf("JSON illisible: ${e.message}"))
            }
            val violations = validate(raw)
            if (violations.isNotEmpty()) throw CatalogException(violations)
            return convert(raw)
        }

        private fun validate(raw: CatalogJson): List<String> = buildList {
            if (raw.version != CATALOG_FORMAT_VERSION) add("version ${raw.version}, attendue $CATALOG_FORMAT_VERSION")

            val baseIds = raw.bases.map { it.id }
            baseIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.forEach { add("base en double: $it") }
            baseIds.filter { BaseColor.fromId(it) == null }.forEach { add("base inconnue: $it") }
            BaseColor.entries.filter { it.id !in baseIds }.forEach { add("base manquante: ${it.id}") }
            raw.bases.forEach { b ->
                checkText("base ${b.id}: name", b.name)
                if (b.anchors.isEmpty()) add("base ${b.id}: aucune ancre")
                b.anchors.filterNot { HEX.matches(it) }.forEach { add("base ${b.id}: ancre invalide \"$it\"") }
            }

            raw.shades.map { it.id }.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
                .forEach { add("nuance en double: $it") }
            raw.shades.forEach { s ->
                val where = "nuance ${s.id}"
                if (!ID.matches(s.id)) add("$where: id doit être en snake_case ASCII")
                if (BaseColor.fromId(s.base) == null) add("$where: base inconnue \"${s.base}\"")
                if (!HEX.matches(s.ref)) add("$where: ref invalide \"${s.ref}\"")
                checkText("$where: name", s.name)
                checkText("$where: description", s.description)
                checkList("$where: nature", s.nature)
            }
        }

        private fun MutableList<String>.checkText(where: String, t: TextJson) {
            if (t.en.isBlank()) add("$where: texte EN vide")
            if (t.fr.isBlank()) add("$where: texte FR vide")
        }

        private fun MutableList<String>.checkList(where: String, l: ListJson) {
            if (l.en.size < MIN_NATURE || l.en.any { it.isBlank() }) add("$where: au moins $MIN_NATURE éléments EN non vides")
            if (l.fr.size < MIN_NATURE || l.fr.any { it.isBlank() }) add("$where: au moins $MIN_NATURE éléments FR non vides")
        }

        private fun convert(raw: CatalogJson): Catalog {
            val bases = BaseColor.entries.map { color ->
                val b = raw.bases.first { it.id == color.id }
                BaseEntry(color, Localized(b.name.en, b.name.fr), b.anchors.map { hex ->
                    val c = Srgb8.fromHex(hex)
                    Anchor(c, ColorMath.toOklab(c))
                })
            }
            val shades = raw.shades.map { s ->
                val ref = Srgb8.fromHex(s.ref)
                Shade(
                    id = s.id,
                    // Déjà vérifiée par validate().
                    base = checkNotNull(BaseColor.fromId(s.base)),
                    ref = ref,
                    refLab = ColorMath.toOklab(ref),
                    name = Localized(s.name.en, s.name.fr),
                    description = Localized(s.description.en, s.description.fr),
                    nature = Localized(s.nature.en, s.nature.fr),
                )
            }
            return Catalog(bases, shades)
        }
    }
}
