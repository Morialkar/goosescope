package com.morialkar.goosescope.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.Detail
import com.morialkar.goosescope.app.ScopeState
import com.morialkar.goosescope.app.ScopeViewModel
import com.morialkar.goosescope.app.ui.theme.Tokens
import com.morialkar.goosescope.app.ui.theme.textColor
import com.morialkar.goosescope.app.ui.theme.toColor
import com.morialkar.goosescope.core.catalog.Lang
import com.morialkar.goosescope.core.color.Srgb8
import com.morialkar.goosescope.core.compare.ComparisonText
import com.morialkar.goosescope.core.geometry.ViewToImageMapper
import kotlinx.coroutines.delay
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
        // Le réticule glisse jusqu'au nouveau point plutôt que d'y sauter: on voit d'où vient la mesure.
        val reticleOffset by animateIntOffsetAsState(
            IntOffset((tx - half).roundToInt(), (ty - half).roundToInt()),
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "reticle",
        )
        Reticle(Modifier.offset { reticleOffset }.testTag("reticle"))

        // La fiche entre en glissant, et ne couvre jamais le point visé (STANDARDS §4).
        val entered = remember { MutableTransitionState(false).apply { targetState = true } }
        if (landscape) {
            // Même marge qu'en portrait: au centre, la fiche reste du même côté que le panneau du direct.
            val onRight = tx < viewW * 0.55f
            AnimatedVisibility(
                entered,
                Modifier.align(if (onRight) Alignment.CenterEnd else Alignment.CenterStart),
                enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 6 },
            ) {
                DetailSheet(
                    state.detail, lang, viewModel, Modifier.fillMaxHeight().width(400.dp),
                    WindowInsetsSides.Vertical + if (onRight) WindowInsetsSides.End else WindowInsetsSides.Start,
                )
            }
        } else {
            // Marge autour du centre: au départ le point visé est au centre et la fiche reste en bas.
            val atBottom = ty < viewH * 0.55f
            AnimatedVisibility(
                entered,
                Modifier.align(if (atBottom) Alignment.BottomCenter else Alignment.TopCenter),
                enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { if (atBottom) it / 3 else -it / 3 },
            ) {
                DetailSheet(
                    state.detail, lang, viewModel, Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.62f),
                    WindowInsetsSides.Horizontal + if (atBottom) WindowInsetsSides.Bottom else WindowInsetsSides.Top,
                )
            }
        }
    }
}

@Composable
private fun DetailSheet(detail: Detail, lang: Lang, viewModel: ScopeViewModel, modifier: Modifier, edges: WindowInsetsSides) {
    // Marges système seulement du côté où la fiche touche le bord de l'écran.
    Column(
        modifier.background(Tokens.surface).windowInsetsPadding(WindowInsets.safeDrawing.only(edges)).padding(16.dp).testTag("detail"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (detail) {
            Detail.TooDark -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(R.string.too_dark), Tokens.shadeNameSize, bold = true, modifier = Modifier.testTag("frozenTooDark"))
                Label(stringResource(R.string.too_dark_frozen_hint))
            }
            is Detail.Color -> {
                ColorHeader(detail, viewModel.baseName(detail.base, lang), detail.shade.name[lang])
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Le contenu glisse doucement à chaque nouvelle nuance touchée.
                    AnimatedContent(
                        targetState = detail,
                        contentKey = { it.shade.id },
                        transitionSpec = {
                            (fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 8 }) togetherWith fadeOut(tween(120))
                        },
                        label = "detailBody",
                    ) { d ->
                        DetailBody(d, lang, viewModel)
                    }
                    // Dans la zone qui défile: sur un téléphone, seul le bouton reste fixe en bas.
                    Label(stringResource(R.string.detail_tap_hint), Tokens.labelSize, color = Tokens.textMuted)
                }
            }
        }
        FlatButton(stringResource(R.string.resume), viewModel::resume, Modifier.fillMaxWidth().testTag("resume"))
    }
}

/**
 * Bandeau de la couleur mesurée, avec la nuance de référence à droite. Les couleurs changent en
 * fondu quand on touche un autre point: on sent passer d'une teinte à l'autre.
 */
@Composable
private fun ColorHeader(detail: Detail.Color, baseName: String, shadeName: String) {
    val measured by animateColorAsState(detail.measured.toColor(), tween(380), label = "measured")
    val reference by animateColorAsState(detail.shade.ref.toColor(), tween(380), label = "reference")
    Row(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp))) {
        Column(
            Modifier.weight(0.7f).fillMaxHeight().background(measured).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val ink = detail.measured.textColor()
            BasicText(
                baseName.uppercase(),
                Modifier.testTag("detailBase"),
                style = TextStyle(color = ink, fontSize = Tokens.labelSize, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            )
            Label(shadeName, Tokens.shadeNameSize, ink, bold = true, modifier = Modifier.testTag("detailShade"))
        }
        Column(
            Modifier.weight(0.3f).fillMaxHeight().background(reference).padding(12.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Label(stringResource(R.string.detail_reference), 13.sp, detail.shade.ref.textColor(), bold = true)
        }
    }
}

@Composable
private fun DetailBody(detail: Detail.Color, lang: Lang, viewModel: ScopeViewModel) {
    val shadeName = detail.shade.name[lang]
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Label(detail.shade.description[lang], modifier = Modifier.testTag("detailDescription"))

        Column(
            Modifier.fillMaxWidth().background(Tokens.surfaceRaised, RoundedCornerShape(Tokens.corner)).padding(14.dp),
        ) {
            // La phrase nomme déjà la nuance de référence: pas de titre qui la répète.
            Label(
                ComparisonText.render(detail.facts, shadeName, lang) { viewModel.baseName(it, lang) },
                modifier = Modifier.testTag("detailComparison"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label(stringResource(R.string.detail_nature), Tokens.labelSize, color = Tokens.textMuted, bold = true)
            FlowRow(
                Modifier.testTag("detailNature"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.shade.nature[lang].forEachIndexed { i, item ->
                    NatureTag(item, detail.shade.ref, i, key = detail.shade.id)
                }
            }
        }
    }
}

/** Un élément de la nature en étiquette, qui apparaît en cascade après les précédents. */
@Composable
private fun NatureTag(text: String, color: Srgb8, index: Int, key: String) {
    val appear = remember(key, text) { Animatable(0f) }
    LaunchedEffect(key, text) {
        delay(120L + index * 70L)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Row(
        Modifier
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val s = 0.85f + 0.15f * appear.value
                scaleX = s
                scaleY = s
            }
            .background(Tokens.surfaceRaised, CircleShape)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
            .testTag("natureTag"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Anneau clair: la pastille reste visible même quand la nuance est aussi sombre que l'étiquette.
        Box(Modifier.size(14.dp).background(Tokens.textMuted, CircleShape).padding(1.5.dp).background(color.toColor(), CircleShape))
        Box(Modifier.width(8.dp))
        Label(text, Tokens.labelSize)
    }
}
