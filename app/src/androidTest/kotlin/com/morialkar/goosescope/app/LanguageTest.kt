package com.morialkar.goosescope.app

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.morialkar.goosescope.app.ui.ScopeApp
import com.morialkar.goosescope.core.catalog.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageTest {
    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(LocaleManager::class.java)
    private lateinit var saved: LocaleList

    @Before fun save() { saved = manager.applicationLocales }
    @After fun restore() { manager.applicationLocales = saved }

    @Test
    fun first_launch_defaults_to_english_even_on_a_french_system() {
        manager.applicationLocales = LocaleList.getEmptyLocaleList()
        AppLanguage(context).ensureDefault()
        assertEquals("en", manager.applicationLocales.get(0).language)
    }

    @Test
    fun a_chosen_language_is_kept() {
        val language = AppLanguage(context)
        language.set(Lang.FR)
        language.ensureDefault()
        assertEquals(Lang.FR, AppLanguage(context).current())
    }

    @Test
    fun toggle_switches_between_english_and_french() {
        val language = AppLanguage(context)
        language.set(Lang.EN)
        language.toggle()
        assertEquals(Lang.FR, language.current())
        language.toggle()
        assertEquals(Lang.EN, language.current())
    }

    @Test
    fun the_language_button_asks_for_the_other_language() {
        val vm = ScopeViewModel { TestSupport.catalog }
        var toggled = false
        rule.setContent { ScopeApp(vm, FakeFrameSource(), {}, {}, onToggleLanguage = { toggled = true }) }
        vm.onPermissionResult(true)
        rule.onNodeWithTag("language").performClick()
        assertTrue(toggled)
    }
}
