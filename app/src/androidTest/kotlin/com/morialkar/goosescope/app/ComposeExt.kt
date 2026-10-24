package com.morialkar.goosescope.app

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag

fun ComposeContentTestRule.onAllNodesWithTagExists(tag: String): Boolean =
    onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

/** Attente maximale des tests: l'émulateur API 37 met plusieurs secondes là où la tablette répond en 100 ms. */
const val SLOW_DEVICE_MS = 5_000L
