package com.morialkar.goosescope.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.morialkar.goosescope.R
import com.morialkar.goosescope.app.ui.ScopeApp
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.color.Srgb8
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveScreenTest {
    @get:Rule val rule = createComposeRule()
    private val source = FakeFrameSource()
    private lateinit var vm: ScopeViewModel

    @Before
    fun setUp() {
        vm = ScopeViewModel { TestSupport.catalog }
        rule.setContent { ScopeApp(vm, source, onRequestPermission = {}, onOpenSettings = {}) }
        vm.onPermissionResult(true)
        rule.waitUntil(SLOW_DEVICE_MS) { source.isPreviewing }
    }

    @Test
    fun shows_the_reticle_and_waits_for_a_first_image() {
        rule.onNodeWithTag("reticle").assertIsDisplayed()
        rule.onNodeWithTag("warming").assertIsDisplayed()
    }

    @Test
    fun names_a_blue_object() {
        source.emitColor(Srgb8.fromHex("#1E90FF"), times = 5)
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("baseName") }
        rule.onNodeWithTag("baseName").assertTextEquals(TestSupport.name(BaseColor.BLUE))
    }

    @Test
    fun shows_base_and_shade_on_one_line() {
        // AC17: « <base> — <nuance> » dans la langue de l'app.
        val scarlet = TestSupport.catalog.shades.first { it.id == "scarlet" }
        source.emitColor(scarlet.ref, times = 5)
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("shadeName") }
        rule.onNodeWithTag("baseName").assertTextEquals(TestSupport.name(BaseColor.RED))
        rule.onNodeWithTag("shadeName").assertTextEquals("— " + scarlet.name[TestSupport.lang])
    }

    @Test
    fun hides_the_shade_when_it_repeats_the_base_name() {
        val pink = TestSupport.catalog.shades.first { it.id == "pink" }
        source.emitColor(pink.ref, times = 5)
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("baseName") }
        rule.onNodeWithTag("baseName").assertTextEquals(TestSupport.name(BaseColor.PINK))
        rule.onNodeWithTag("shadeName").assertDoesNotExist()
    }

    @Test
    fun says_too_dark_when_the_camera_is_covered() {
        source.emitColor(Srgb8(5, 5, 5), times = 3)
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("tooDark") }
        rule.onNodeWithTag("tooDark").assertTextEquals(TestSupport.string(R.string.too_dark))
    }

    @Test
    fun black_object_in_a_lit_scene_is_black_not_too_dark() {
        repeat(3) { source.emit { x, y -> if (x in 140..180 && y in 70..110) Srgb8.BLACK else Srgb8(190, 180, 170) } }
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("baseName") }
        rule.onNodeWithTag("baseName").assertTextEquals(TestSupport.name(BaseColor.BLACK))
    }

    @Test
    fun torch_button_toggles_the_light() {
        rule.onNodeWithTag("torch").performClick()
        rule.waitForIdle()
        assertTrue(source.torchOn)
        rule.onNodeWithTag("torch").performClick()
        rule.waitForIdle()
        assertTrue(!source.torchOn)
    }

    @Test
    fun camera_failure_shows_an_error_and_retry_restarts() {
        source.fail()
        rule.waitUntil(SLOW_DEVICE_MS) { rule.onAllNodesWithTagExists("failure") }
        rule.onNodeWithTag("failure.primary").performClick()
        rule.waitUntil(SLOW_DEVICE_MS) { source.isPreviewing }
        rule.onNodeWithTag("reticle").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class StartupStateTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun denied_permission_explains_and_offers_to_ask_again() {
        val vm = ScopeViewModel { TestSupport.catalog }
        var asked = false
        rule.setContent { ScopeApp(vm, FakeFrameSource(), onRequestPermission = { asked = true }, onOpenSettings = {}) }
        vm.onPermissionResult(false)
        rule.onNodeWithTag("permission").assertIsDisplayed()
        rule.onNodeWithTag("permission.primary").performClick()
        assertTrue(asked)
    }

    @Test
    fun unreadable_catalog_shows_an_error_instead_of_crashing() {
        val vm = ScopeViewModel { Catalog.load("{}") }
        rule.setContent { ScopeApp(vm, FakeFrameSource(), onRequestPermission = {}, onOpenSettings = {}) }
        vm.onPermissionResult(true)
        rule.onNodeWithTag("failure").assertIsDisplayed()
    }
}
