// REQUIRES_REVIEW: accès à la caméra et flux d'images (SPEC §8, ARCHITECTURE §6).
// Aucune image n'est écrite ni envoyée: chaque image d'analyse est lue puis fermée.
package com.morialkar.goosescope.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morialkar.goosescope.core.color.Srgb8
import com.morialkar.goosescope.core.sample.PixelGrid
import com.morialkar.goosescope.core.sample.RotatedGrid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

class CameraXFrameSource(private val context: Context) : FrameSource {
    private val torchAvailable = MutableStateFlow(false)
    override val hasTorch: StateFlow<Boolean> = torchAvailable
    private var camera: Camera? = null

    @Composable
    override fun Preview(modifier: Modifier, onFrame: (Frame) -> Unit, onError: (Throwable) -> Unit) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentOnFrame = rememberUpdatedState(onFrame)
        val currentOnError = rememberUpdatedState(onError)
        val previewView = remember {
            PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        }
        AndroidView(factory = { previewView }, modifier = modifier)

        DisposableEffect(lifecycleOwner) {
            val executor = Executors.newSingleThreadExecutor()
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    // Même rapport 16:9 pour l'aperçu et l'analyse: l'image figée a le même cadrage que le direct.
                    val selector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build()
                    val preview = Preview.Builder().setResolutionSelector(selector).build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                    ),
                                )
                                .build(),
                        )
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { proxy ->
                        try {
                            currentOnFrame.value(ProxyFrame(proxy))
                        } finally {
                            proxy.close()
                        }
                    }
                    provider.unbindAll()
                    val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    camera = bound
                    torchAvailable.value = bound.cameraInfo.hasFlashUnit()
                } catch (e: Exception) {
                    // Caméra absente ou occupée: l'interface passe à l'état d'erreur (ARCHITECTURE §5).
                    currentOnError.value(e)
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                camera = null
                if (providerFuture.isDone) providerFuture.get().unbindAll()
                executor.shutdown()
            }
        }
    }

    override fun setTorch(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }
}

private class ProxyFrame(private val proxy: ImageProxy) : Frame {
    override val grid: PixelGrid = RotatedGrid(ProxyPixelGrid(proxy), proxy.imageInfo.rotationDegrees)
    override val timestampMs: Long = proxy.imageInfo.timestamp / 1_000_000

    override fun snapshot(): Bitmap {
        val raw = proxy.toBitmap()
        val rotation = proxy.imageInfo.rotationDegrees
        if (rotation == 0) return raw
        val m = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also { if (it !== raw) raw.recycle() }
    }
}

/** Lecture directe du tampon RGBA_8888 de CameraX, sans copie. */
private class ProxyPixelGrid(proxy: ImageProxy) : PixelGrid {
    private val plane = proxy.planes[0]
    private val buffer = plane.buffer
    private val rowStride = plane.rowStride
    private val pixelStride = plane.pixelStride
    override val width = proxy.width
    override val height = proxy.height

    override fun rgbAt(x: Int, y: Int): Srgb8 {
        val i = y * rowStride + x * pixelStride
        return Srgb8(buffer.get(i).toInt() and 0xFF, buffer.get(i + 1).toInt() and 0xFF, buffer.get(i + 2).toInt() and 0xFF)
    }
}
