package com.morialkar.goosescope.core.color

/** Couleur sRGB 8 bits par canal, telle qu'elle sort de la caméra ou du catalogue. */
data class Srgb8(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) { "Canal hors de 0..255: ($r, $g, $b)" }
    }

    fun toHex(): String = "#%02X%02X%02X".format(r, g, b)

    companion object {
        val BLACK = Srgb8(0, 0, 0)
        val WHITE = Srgb8(255, 255, 255)

        private val HEX = Regex("^#[0-9A-Fa-f]{6}$")

        fun fromHex(hex: String): Srgb8 {
            require(HEX.matches(hex)) { "Couleur attendue au format #RRGGBB: \"$hex\"" }
            val v = hex.substring(1).toInt(16)
            return Srgb8((v shr 16) and 0xFF, (v shr 8) and 0xFF, v and 0xFF)
        }
    }
}

/** sRGB en lumière linéaire, 0..1 par canal. Les moyennes de pixels se font ici (ARCHITECTURE §2.1). */
data class LinearRgb(val r: Double, val g: Double, val b: Double) {
    init {
        require(r.isFinite() && g.isFinite() && b.isFinite()) { "Canal non fini: ($r, $g, $b)" }
    }
}
