package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.morialkar.goosescope.app.ui.theme.Tokens

/** Anneau clair doublé d'un anneau sombre: lisible sur n'importe quelle image (STANDARDS §4). */
@Composable
fun Reticle(modifier: Modifier = Modifier) {
    Canvas(modifier.size(56.dp)) {
        val r = size.minDimension / 2
        drawCircle(Tokens.reticleInner, radius = r - 3.dp.toPx(), style = Stroke(3.dp.toPx()))
        drawCircle(Tokens.reticleOuter, radius = r - 6.dp.toPx(), style = Stroke(3.dp.toPx()))
        drawCircle(Tokens.reticleInner, radius = r - 9.dp.toPx(), style = Stroke(2.dp.toPx()))
    }
}
