package com.morialkar.goosescope.app.camera

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.morialkar.goosescope.core.sample.PixelGrid
import kotlinx.coroutines.flow.StateFlow

/** Une image d'analyse. Valide seulement pendant l'appel à `onFrame`: ne pas la garder. */
interface Frame {
    /** Pixels redressés. */
    val grid: PixelGrid
    val timestampMs: Long

    /** Copie redressée de l'image, en mémoire seulement (SPEC NG3). */
    fun snapshot(): Bitmap
}

/**
 * Source d'images de la caméra (ARCHITECTURE §2). L'aperçu tourne tant que [Preview] est composé:
 * le retirer de la composition (mode figé) arrête la caméra.
 */
interface FrameSource {
    val hasTorch: StateFlow<Boolean>

    @Composable
    fun Preview(modifier: Modifier, onFrame: (Frame) -> Unit, onError: (Throwable) -> Unit)

    fun setTorch(on: Boolean)
}
