package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8

/**
 * Présente une image brute de capteur comme si elle était redressée, sans copier de pixels.
 * [rotationDegrees] est la rotation horaire à appliquer à l'image brute (celle que donne CameraX).
 */
class RotatedGrid(private val raw: PixelGrid, private val rotationDegrees: Int) : PixelGrid {
    init {
        require(rotationDegrees in setOf(0, 90, 180, 270)) { "Rotation non gérée: $rotationDegrees" }
    }

    override val width: Int = if (rotationDegrees % 180 == 0) raw.width else raw.height
    override val height: Int = if (rotationDegrees % 180 == 0) raw.height else raw.width

    override fun rgbAt(x: Int, y: Int): Srgb8 = when (rotationDegrees) {
        0 -> raw.rgbAt(x, y)
        90 -> raw.rgbAt(y, raw.height - 1 - x)
        180 -> raw.rgbAt(raw.width - 1 - x, raw.height - 1 - y)
        else -> raw.rgbAt(raw.width - 1 - y, x)
    }
}
