package com.morialkar.goosescope.app

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.morialkar.goosescope.app.camera.BitmapPixelGrid
import com.morialkar.goosescope.app.camera.Frame
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.catalog.CatalogException
import com.morialkar.goosescope.core.catalog.Lang
import com.morialkar.goosescope.core.catalog.Shade
import com.morialkar.goosescope.core.classify.BaseClassifier
import com.morialkar.goosescope.core.classify.ShadeMatcher
import com.morialkar.goosescope.core.color.ColorMath
import com.morialkar.goosescope.core.color.Srgb8
import com.morialkar.goosescope.core.compare.ComparisonEngine
import com.morialkar.goosescope.core.compare.ComparisonFacts
import com.morialkar.goosescope.core.geometry.ViewToImageMapper
import com.morialkar.goosescope.core.sample.DarknessGate
import com.morialkar.goosescope.core.sample.PixelGrid
import com.morialkar.goosescope.core.sample.Point
import com.morialkar.goosescope.core.sample.Sampler
import com.morialkar.goosescope.core.smooth.ShadeStabilizer
import com.morialkar.goosescope.core.smooth.Smoother
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

enum class FailureReason { CATALOG, CAMERA }

sealed interface LiveReading {
    data object Warming : LiveReading
    data object TooDark : LiveReading
    data class Color(val base: BaseColor, val shade: Shade, val swatch: Srgb8) : LiveReading
}

sealed interface Detail {
    data object TooDark : Detail
    data class Color(val base: BaseColor, val shade: Shade, val measured: Srgb8, val facts: ComparisonFacts) : Detail
}

// ARCHITECTURE §4.3.
sealed interface ScopeState {
    data object NeedsPermission : ScopeState
    data object PermissionDenied : ScopeState
    data class Live(val reading: LiveReading, val torchOn: Boolean) : ScopeState
    data class Frozen(val image: Bitmap, val target: Point, val detail: Detail) : ScopeState
    data class Failure(val reason: FailureReason) : ScopeState
}

/** État de l'app. `onFrame` est appelé sur le fil d'analyse de la caméra; le reste sur le fil principal. */
class ScopeViewModel(loadCatalog: () -> Catalog) : ViewModel() {

    private val catalog: Catalog? = try {
        loadCatalog()
    } catch (e: CatalogException) {
        null
    }

    fun baseName(base: BaseColor, lang: Lang): String = catalog?.base(base)?.name?.get(lang) ?: base.id

    private val classifier = catalog?.let { BaseClassifier(it) }
    private val matcher = catalog?.let { ShadeMatcher(it) }
    private val smoother = classifier?.let { Smoother(it) }
    private val shadeStabilizer = matcher?.let { ShadeStabilizer(it) }
    private val gate = DarknessGate()
    private val freezeRequested = AtomicBoolean(false)
    private var frozenGrid: PixelGrid? = null
    private var frozenTooDark = false

    private val mutableState = MutableStateFlow<ScopeState>(
        if (catalog == null) ScopeState.Failure(FailureReason.CATALOG) else ScopeState.NeedsPermission,
    )
    val state: StateFlow<ScopeState> = mutableState

    // REQUIRES_REVIEW: suite donnée à la permission caméra (F7).
    fun onPermissionResult(granted: Boolean) {
        mutableState.update { current ->
            when {
                current is ScopeState.Failure && current.reason == FailureReason.CATALOG -> current
                granted && (current is ScopeState.NeedsPermission || current is ScopeState.PermissionDenied) ->
                    ScopeState.Live(LiveReading.Warming, torchOn = false)
                granted -> current
                else -> ScopeState.PermissionDenied
            }
        }
    }

    fun onCameraError() {
        mutableState.value = ScopeState.Failure(FailureReason.CAMERA)
    }

    fun retryCamera() {
        if (mutableState.value == ScopeState.Failure(FailureReason.CAMERA)) {
            smoother?.reset()
        shadeStabilizer?.reset()
            mutableState.value = ScopeState.Live(LiveReading.Warming, torchOn = false)
        }
    }

    fun toggleTorch() {
        mutableState.update { if (it is ScopeState.Live) it.copy(torchOn = !it.torchOn) else it }
    }

    fun onFrame(frame: Frame) {
        val live = mutableState.value as? ScopeState.Live ?: return
        if (freezeRequested.getAndSet(false)) {
            freeze(frame.snapshot())
            return
        }
        val reading = if (gate.isTooDark(Sampler.meanLuminance(frame.grid))) {
            smoother?.reset()
        shadeStabilizer?.reset()
            LiveReading.TooDark
        } else {
            val s = checkNotNull(smoother).push(ColorMath.toOklab(Sampler.sampleCenter(frame.grid)), frame.timestampMs)
            LiveReading.Color(s.base, checkNotNull(shadeStabilizer).update(s.color, s.base), ColorMath.toSrgb8(s.color))
        }
        mutableState.update { if (it is ScopeState.Live) ScopeState.Live(reading, live.torchOn) else it }
    }

    /** Fige à la prochaine image d'analyse (F6). */
    fun requestFreeze() {
        if (mutableState.value is ScopeState.Live) freezeRequested.set(true)
    }

    // REQUIRES_REVIEW: l'image figée vit en mémoire seulement et est libérée à la reprise (NG3).
    private fun freeze(image: Bitmap) {
        val grid = BitmapPixelGrid(image)
        frozenGrid = grid
        frozenTooDark = gate.isTooDark(Sampler.meanLuminance(grid))
        val center = Point(grid.width / 2, grid.height / 2)
        mutableState.value = ScopeState.Frozen(image, center, detailAt(grid, center))
    }

    /** Toucher sur l'image figée (F16). Ignoré hors de la vue. */
    fun onTap(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val frozen = mutableState.value as? ScopeState.Frozen ?: return
        val grid = frozenGrid ?: return
        val point = ViewToImageMapper.map(x, y, viewWidth, viewHeight, grid.width, grid.height) ?: return
        mutableState.value = frozen.copy(target = point, detail = detailAt(grid, point))
    }

    fun resume() {
        if (mutableState.value !is ScopeState.Frozen) return
        // Pas de recycle(): Compose peut encore dessiner l'image pendant la transition; le GC la libère.
        frozenGrid = null
        smoother?.reset()
        shadeStabilizer?.reset()
        mutableState.value = ScopeState.Live(LiveReading.Warming, torchOn = false)
    }

    private fun detailAt(grid: PixelGrid, point: Point): Detail {
        if (frozenTooDark) return Detail.TooDark
        val measured = Sampler.sample(grid, point)
        val lab = ColorMath.toOklab(measured)
        val base = checkNotNull(classifier).classify(lab).base
        val shade = checkNotNull(matcher).match(lab, base)
        return Detail.Color(base, shade, measured, ComparisonEngine.compare(lab, shade.refLab))
    }
}
