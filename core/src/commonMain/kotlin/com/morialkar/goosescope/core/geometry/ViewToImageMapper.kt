package com.morialkar.goosescope.core.geometry

import com.morialkar.goosescope.core.sample.Point
import kotlin.math.roundToInt

/**
 * Correspondance entre la vue et l'image affichée en rognage centré (Crop / FILL_CENTER),
 * pour viser au toucher sur l'image figée (SPEC F16, ARCHITECTURE §3.8).
 */
object ViewToImageMapper {

    /** Point image sous un toucher, ou null si le toucher est hors de la vue. */
    fun map(tapX: Float, tapY: Float, viewW: Int, viewH: Int, imageW: Int, imageH: Int): Point? {
        require(viewW > 0 && viewH > 0 && imageW > 0 && imageH > 0) { "Dimensions nulles ou négatives" }
        if (tapX < 0 || tapY < 0 || tapX >= viewW || tapY >= viewH) return null
        val scale = maxOf(viewW.toFloat() / imageW, viewH.toFloat() / imageH)
        val offsetX = (viewW - imageW * scale) / 2
        val offsetY = (viewH - imageH * scale) / 2
        val x = ((tapX - offsetX) / scale).toInt().coerceIn(0, imageW - 1)
        val y = ((tapY - offsetY) / scale).toInt().coerceIn(0, imageH - 1)
        return Point(x, y)
    }

    /** Inverse: position dans la vue d'un point image, pour dessiner le réticule sur l'image figée. */
    fun toView(point: Point, viewW: Int, viewH: Int, imageW: Int, imageH: Int): Pair<Float, Float> {
        require(viewW > 0 && viewH > 0 && imageW > 0 && imageH > 0) { "Dimensions nulles ou négatives" }
        val scale = maxOf(viewW.toFloat() / imageW, viewH.toFloat() / imageH)
        val offsetX = (viewW - imageW * scale) / 2
        val offsetY = (viewH - imageH * scale) / 2
        return Pair(offsetX + (point.x + 0.5f) * scale, offsetY + (point.y + 0.5f) * scale)
    }
}
