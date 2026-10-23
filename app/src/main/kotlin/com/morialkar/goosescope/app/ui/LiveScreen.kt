package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.LiveReading
import com.morialkar.goosescope.app.camera.Frame
import com.morialkar.goosescope.app.camera.FrameSource
import com.morialkar.goosescope.app.ui.theme.Tokens
import com.morialkar.goosescope.app.ui.theme.textColor
import com.morialkar.goosescope.app.ui.theme.toColor

@Composable
fun LiveScreen(
    reading: LiveReading,
    torchOn: Boolean,
    hasTorch: Boolean,
    source: FrameSource,
    baseName: (com.morialkar.goosescope.core.catalog.BaseColor) -> String,
    onFrame: (Frame) -> Unit,
    onCameraError: (Throwable) -> Unit,
    onFreeze: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLanguage: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Tokens.surface)) {
        val landscape = maxWidth > maxHeight
        Box(Modifier.fillMaxSize()) {
            source.Preview(Modifier.fillMaxSize(), onFrame, onCameraError)
            Reticle(Modifier.align(Alignment.Center).testTag("reticle"))
        }
        val panel = @Composable { modifier: Modifier ->
            InfoPanel(reading, torchOn, hasTorch, baseName, onFreeze, onToggleTorch, onToggleLanguage, landscape, modifier)
        }
        if (landscape) {
            panel(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(340.dp))
        } else {
            panel(Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
    }
}

@Composable
private fun InfoPanel(
    reading: LiveReading,
    torchOn: Boolean,
    hasTorch: Boolean,
    baseName: (com.morialkar.goosescope.core.catalog.BaseColor) -> String,
    onFreeze: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLanguage: () -> Unit,
    landscape: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier.background(Tokens.surface).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, if (landscape) Alignment.CenterVertically else Alignment.Top),
    ) {
        val swatchModifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
        when (reading) {
            is LiveReading.Color -> Box(
                swatchModifier.background(reading.swatch.toColor(), RoundedCornerShape(Tokens.corner)).padding(16.dp).testTag("swatch"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Label(baseName(reading.base), Tokens.baseNameSize, reading.swatch.textColor(), bold = true, modifier = Modifier.testTag("baseName"))
            }
            LiveReading.TooDark -> Column(
                swatchModifier.background(Tokens.surfaceRaised, RoundedCornerShape(Tokens.corner)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Label(stringResource(R.string.too_dark), Tokens.baseNameSize, bold = true, modifier = Modifier.testTag("tooDark"))
                Label(stringResource(R.string.too_dark_hint))
            }
            LiveReading.Warming -> Box(
                swatchModifier.background(Tokens.surfaceRaised, RoundedCornerShape(Tokens.corner)).padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Label(stringResource(R.string.warming), Tokens.bodySize, modifier = Modifier.testTag("warming"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FlatButton(stringResource(R.string.freeze), onFreeze, Modifier.weight(1f).testTag("freeze"))
            if (hasTorch) {
                FlatButton(
                    stringResource(if (torchOn) R.string.torch_off else R.string.torch_on),
                    onToggleTorch,
                    Modifier.testTag("torch"),
                )
            }
            // Le bouton affiche l'autre langue, dans cette langue: on la reconnaît même sans lire la langue actuelle.
            FlatButton(stringResource(R.string.other_language), onToggleLanguage, Modifier.testTag("language"))
        }
    }
}
