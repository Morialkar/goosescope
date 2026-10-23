package com.morialkar.goosescope.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.FailureReason
import com.morialkar.goosescope.app.ScopeState
import com.morialkar.goosescope.app.ScopeViewModel
import com.morialkar.goosescope.app.camera.FrameSource
import com.morialkar.goosescope.app.ui.theme.Tokens
import com.morialkar.goosescope.core.catalog.Lang

/** Langue des textes du catalogue: suit la langue de l'app (SPEC F17). */
@Composable
fun currentLang(): Lang =
    if (LocalConfiguration.current.locales[0].language == "fr") Lang.FR else Lang.EN

@Composable
fun ScopeApp(
    viewModel: ScopeViewModel,
    source: FrameSource,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLanguage: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hasTorch by source.hasTorch.collectAsStateWithLifecycle()
    val lang = currentLang()

    Box(Modifier.fillMaxSize().background(Tokens.surface)) {
        when (val s = state) {
            ScopeState.NeedsPermission -> Box(Modifier.fillMaxSize())
            ScopeState.PermissionDenied -> PermissionScreen(onRequestPermission, onOpenSettings)
            is ScopeState.Live -> {
                LaunchedEffect(s.torchOn) { source.setTorch(s.torchOn) }
                LiveScreen(
                    reading = s.reading,
                    torchOn = s.torchOn,
                    hasTorch = hasTorch,
                    source = source,
                    baseName = { viewModel.baseName(it, lang) },
                    onFrame = viewModel::onFrame,
                    onCameraError = { viewModel.onCameraError() },
                    onFreeze = viewModel::requestFreeze,
                    onToggleTorch = viewModel::toggleTorch,
                    onToggleLanguage = onToggleLanguage,
                )
            }
            is ScopeState.Frozen -> FrozenScreen(s, lang, viewModel)
            is ScopeState.Failure -> when (s.reason) {
                FailureReason.CATALOG -> MessageScreen(
                    stringResource(R.string.error_title), stringResource(R.string.error_catalog), null, tag = "failure",
                )
                FailureReason.CAMERA -> MessageScreen(
                    stringResource(R.string.error_title), stringResource(R.string.error_camera),
                    stringResource(R.string.retry) to viewModel::retryCamera, tag = "failure",
                )
            }
        }
    }
}
