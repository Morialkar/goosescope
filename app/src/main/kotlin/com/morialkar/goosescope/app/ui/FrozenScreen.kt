package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.Detail
import com.morialkar.goosescope.app.ScopeState
import com.morialkar.goosescope.app.ScopeViewModel
import com.morialkar.goosescope.app.ui.theme.Tokens
import com.morialkar.goosescope.app.ui.theme.textColor
import com.morialkar.goosescope.app.ui.theme.toColor
import com.morialkar.goosescope.core.catalog.Lang
import com.morialkar.goosescope.core.compare.ComparisonText
import com.morialkar.goosescope.core.geometry.ViewToImageMapper
import kotlin.math.roundToInt

/** Image figée, réticule déplaçable au toucher et fiche détaillée (SPEC F6, F14, F16). */
@Composable
fun FrozenScreen(state: ScopeState.Frozen, lang: Lang, viewModel: ScopeViewModel) {
    val image = remember(state.image) { state.image.asImageBitmap() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Tokens.surface)) {
        val density = LocalDensity.current
        val viewW = constraints.maxWidth
        val viewH = constraints.maxHeight
        val landscape = maxWidth > maxHeight
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().testTag("frozenImage").pointerInput(viewW, viewH) {
                detectTapGestures { viewModel.onTap(it.x, it.y, viewW, viewH) }
            },
        )
        val (tx, ty) = ViewToImageMapper.toView(state.target, viewW, viewH, state.image.width, state.image.height)
        val half = with(density) { 28.dp.toPx() }
        Reticle(Modifier.offset { IntOffset((tx - half).roundToInt(), (ty - half).roundToInt()) }.testTag("reticle"))

        // La fiche ne couvre jamais le point visé (STANDARDS §4): elle se place du côté opposé.
        if (landscape) {
            val onRight = tx < viewW / 2f
            DetailSheet(
                state.detail, lang, viewModel,
                Modifier.align(if (onRight) Alignment.CenterEnd else Alignment.CenterStart).fillMaxHeight().width(380.dp),
            )
        } else {
            // Marge autour du centre: au départ le point visé est au centre et la fiche reste en bas.
            val atBottom = ty < viewH * 0.55f
            DetailSheet(
                state.detail, lang, viewModel,
                Modifier.align(if (atBottom) Alignment.BottomCenter else Alignment.TopCenter).fillMaxWidth()
                    .heightIn(max = maxHeight * 0.48f),
            )
        }
    }
}

@Composable
private fun DetailSheet(detail: Detail, lang: Lang, viewModel: ScopeViewModel, modifier: Modifier) {
    Column(
        modifier.background(Tokens.surface).safeDrawingPadding().padding(16.dp).testTag("detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (detail) {
                Detail.TooDark -> {
                    Label(stringResource(R.string.too_dark), Tokens.shadeNameSize, bold = true, modifier = Modifier.testTag("frozenTooDark"))
                    Label(stringResource(R.string.too_dark_frozen_hint))
                }
                is Detail.Color -> {
                    val baseName = viewModel.baseName(detail.base, lang)
                    val shadeName = detail.shade.name[lang]
                    Label(baseName, Tokens.labelSize, bold = true, modifier = Modifier.testTag("detailBase"))
                    Label(shadeName, Tokens.shadeNameSize, bold = true, modifier = Modifier.testTag("detailShade"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SwatchTile(stringResource(R.string.detail_measured), detail.measured, Modifier.weight(1f))
                        SwatchTile(shadeName, detail.shade.ref, Modifier.weight(1f))
                    }
                    Label(detail.shade.description[lang], modifier = Modifier.testTag("detailDescription"))
                    Label(
                        ComparisonText.render(detail.facts, shadeName, lang) { viewModel.baseName(it, lang) },
                        modifier = Modifier.testTag("detailComparison"),
                    )
                    Label(stringResource(R.string.detail_nature), Tokens.labelSize, bold = true)
                    Label(detail.shade.nature[lang].joinToString(" · "), modifier = Modifier.testTag("detailNature"))
                }
            }
            Label(stringResource(R.string.detail_tap_hint), Tokens.labelSize)
        }
        FlatButton(stringResource(R.string.resume), viewModel::resume, Modifier.fillMaxWidth().testTag("resume"))
    }
}

@Composable
private fun SwatchTile(label: String, color: com.morialkar.goosescope.core.color.Srgb8, modifier: Modifier) {
    Box(
        modifier.heightIn(min = 72.dp).background(color.toColor(), RoundedCornerShape(Tokens.corner)).padding(12.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Label(label, Tokens.labelSize, color.textColor(), bold = true)
    }
}
