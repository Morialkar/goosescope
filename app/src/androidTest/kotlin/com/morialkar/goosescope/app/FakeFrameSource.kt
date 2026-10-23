package com.morialkar.goosescope.app

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.morialkar.goosescope.app.camera.Frame
import com.morialkar.goosescope.app.camera.FrameSource
import com.morialkar.goosescope.core.color.Srgb8
import com.morialkar.goosescope.core.sample.PixelGrid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Source d'images synthétiques: aucun test automatisé ne dépend de la vraie caméra (STANDARDS §5). */
class FakeFrameSource(torch: Boolean = true) : FrameSource {
    override val hasTorch: StateFlow<Boolean> = MutableStateFlow(torch)
    @Volatile private var sink: ((Frame) -> Unit)? = null
    @Volatile private var errorSink: ((Throwable) -> Unit)? = null
    @Volatile var torchOn = false
        private set
    private var clock = 0L

    val isPreviewing: Boolean get() = sink != null

    @Composable
    override fun Preview(modifier: Modifier, onFrame: (Frame) -> Unit, onError: (Throwable) -> Unit) {
        val frame = rememberUpdatedState(onFrame)
        val error = rememberUpdatedState(onError)
        Box(modifier.background(Color.DarkGray))
        DisposableEffect(Unit) {
            sink = { frame.value(it) }
            errorSink = { error.value(it) }
            onDispose { sink = null; errorSink = null }
        }
    }

    override fun setTorch(on: Boolean) { torchOn = on }

    /** Émet une image dont chaque pixel vient de [pixel]; renvoie false si l'aperçu n'est pas affiché. */
    fun emit(width: Int = 320, height: Int = 180, pixel: (Int, Int) -> Srgb8): Boolean {
        val s = sink ?: return false
        clock += 33
        s(GridFrame(width, height, pixel, clock))
        return true
    }

    fun emitColor(c: Srgb8, times: Int = 1) = repeat(times) { emit { _, _ -> c } }

    fun fail(e: Throwable = IllegalStateException("caméra absente")) { errorSink?.invoke(e) }
}

private class GridFrame(w: Int, h: Int, val pixel: (Int, Int) -> Srgb8, override val timestampMs: Long) : Frame {
    override val grid = object : PixelGrid {
        override val width = w
        override val height = h
        override fun rgbAt(x: Int, y: Int) = pixel(x, y)
    }

    override fun snapshot(): Bitmap {
        val bmp = Bitmap.createBitmap(grid.width, grid.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until grid.height) for (x in 0 until grid.width) {
            val c = pixel(x, y)
            bmp.setPixel(x, y, android.graphics.Color.rgb(c.r, c.g, c.b))
        }
        return bmp
    }
}
