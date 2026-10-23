package com.morialkar.goosescope.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morialkar.goosescope.core.color.Contrast
import com.morialkar.goosescope.core.color.Srgb8

// Jetons visuels: seule source de couleurs de l'interface (STANDARDS §4).
object Tokens {
    val surface = Color(0xFF3F4752)
    val surfaceRaised = Color(0xFF545D69)
    val textOnSurface = Color(0xFFF4F2EC)
    /** Texte secondaire (étiquettes); 6,8:1 sur `surface`, 4,8:1 sur `surfaceRaised`, vérifié dans ContrastTest. */
    val textMuted = Color(0xFFD8DBDF)
    val reticleOuter = Color(0xFFF4F2EC)
    val reticleInner = Color(0xFF1F2328)

    val corner = 12.dp
    val baseNameSize = 40.sp
    val shadeNameSize = 30.sp
    val bodySize = 18.sp
    val labelSize = 15.sp
    val minTouch = 48.dp
}

fun Srgb8.toColor(): Color = Color(r, g, b)

/** Texte noir ou blanc purs sur un aplat mesuré (STANDARDS §4). */
fun Srgb8.textColor(): Color = Contrast.textColorOn(this).toColor()
