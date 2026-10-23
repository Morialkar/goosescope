package com.morialkar.goosescope.app.camera

import android.graphics.Bitmap
import com.morialkar.goosescope.core.color.Srgb8
import com.morialkar.goosescope.core.sample.PixelGrid

/** Pixels d'une image figée, copiés une fois pour que chaque toucher soit instantané (NF8). */
class BitmapPixelGrid(bitmap: Bitmap) : PixelGrid {
    override val width = bitmap.width
    override val height = bitmap.height
    private val pixels = IntArray(width * height).also { bitmap.getPixels(it, 0, width, 0, 0, width, height) }

    override fun rgbAt(x: Int, y: Int): Srgb8 {
        val p = pixels[y * width + x]
        return Srgb8((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)
    }
}
