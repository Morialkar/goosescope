package com.morialkar.goosescope.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.morialkar.goosescope.app.ui.ScopeApp
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.color.Srgb8
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrozenScreenTest {
    @get:Rule val rule = createComposeRule()
    private val source = FakeFrameSource()
    private lateinit var vm: ScopeViewModel

    @Before
    fun setUp() {
        vm = ScopeViewModel { TestSupport.catalog }
        rule.setContent { ScopeApp(vm, source, onRequestPermission = {}, onOpenSettings = {}) }
        vm.onPermissionResult(true)
        rule.waitUntil { source.isPreviewing }
    }

    private fun freezeOn(pixel: (Int, Int) -> Srgb8) {
        source.emit(pixel = pixel)
        rule.onNodeWithTag("freeze").performClick()
        rule.waitForIdle()
        source.emit(pixel = pixel)
        rule.waitUntil { rule.onAllNodesWithTagExists("detail") }
    }

    private fun shadeName(id: String) = TestSupport.catalog.shades.first { it.id == id }.name[TestSupport.lang]

    @Test
    fun freezing_on_peach_opens_the_detail_sheet() {
        freezeOn { _, _ -> Srgb8.fromHex("#FFC4A0") }
        rule.onNodeWithTag("detailBase").assertTextEquals(TestSupport.name(BaseColor.ORANGE))
        rule.onNodeWithTag("detailShade").assertTextEquals(shadeName("peach"))
        rule.onNodeWithTag("detailComparison").assertIsDisplayed()
        rule.onNodeWithTag("detailNature").assertIsDisplayed()
    }

    @Test
    fun frozen_image_stops_the_camera_and_resume_restarts_it() {
        freezeOn { _, _ -> Srgb8.fromHex("#1E90FF") }
        assertFalse(source.isPreviewing)
        // Une nouvelle image de la caméra ne change rien: l'aperçu est arrêté.
        assertFalse(source.emit { _, _ -> Srgb8.fromHex("#E02020") })
        rule.onNodeWithTag("detailBase").assertTextEquals(TestSupport.name(BaseColor.BLUE))
        rule.onNodeWithTag("resume").performClick()
        rule.waitUntil { source.isPreviewing }
        rule.onNodeWithTag("freeze").assertIsDisplayed()
    }

    @Test
    fun tapping_three_spots_updates_the_sheet_each_time() {
        // Bandes horizontales: le rognage d'une vue portrait garde toute la hauteur de l'image.
        val red = Srgb8(220, 30, 30); val green = Srgb8(40, 160, 60); val blue = Srgb8(30, 90, 220)
        freezeOn { _, y ->
            when {
                y < 36 -> red
                y < 60 -> green
                y < 90 -> blue
                else -> Srgb8(128, 128, 128)
            }
        }
        listOf(0.10f to BaseColor.RED, 0.27f to BaseColor.GREEN, 0.42f to BaseColor.BLUE).forEach { (fy, expected) ->
            rule.onNodeWithTag("frozenImage").performTouchInput { click(Offset(width / 2f, height * fy)) }
            rule.waitForIdle()
            rule.onNodeWithTag("detailBase").assertTextEquals(TestSupport.name(expected))
        }
    }

    @Test
    fun a_tap_updates_the_sheet_within_300_ms() {
        freezeOn { x, _ -> if (x < 160) Srgb8(220, 30, 30) else Srgb8(30, 90, 220) }
        val start = System.nanoTime()
        rule.runOnUiThread { vm.onTap(10f, 10f, 1000, 2000) }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("fiche mise à jour en $ms ms", ms < 300)
    }

    @Test
    fun dark_frozen_image_says_too_dark() {
        freezeOn { _, _ -> Srgb8(4, 4, 4) }
        rule.onNodeWithTag("frozenTooDark").assertIsDisplayed()
    }

    @Test
    fun tap_outside_the_view_is_ignored() {
        freezeOn { _, _ -> Srgb8.fromHex("#1E90FF") }
        val before = vm.state.value
        rule.runOnUiThread { vm.onTap(-5f, 10f, 1000, 2000) }
        assertTrue(vm.state.value === before)
    }
}
