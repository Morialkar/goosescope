package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.color.Srgb8

/** Image en lecture seule, déjà redressée. Implémentée côté app sur les images CameraX. */
interface PixelGrid {
    val width: Int
    val height: Int
    fun rgbAt(x: Int, y: Int): Srgb8
}

data class Point(val x: Int, val y: Int)
