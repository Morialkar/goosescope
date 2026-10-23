package com.morialkar.goosescope.app.ui

import androidx.compose.runtime.Composable
import com.morialkar.goosescope.app.ScopeState
import com.morialkar.goosescope.app.ScopeViewModel
import com.morialkar.goosescope.core.catalog.Lang

// Mode figé: livré en T11.
@Composable
fun FrozenScreen(state: ScopeState.Frozen, lang: Lang, viewModel: ScopeViewModel) {
    MessageScreen("", "", "Resume" to viewModel::resume, tag = "frozen")
}
