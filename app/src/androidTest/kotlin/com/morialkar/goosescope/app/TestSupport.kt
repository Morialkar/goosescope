package com.morialkar.goosescope.app

import androidx.test.platform.app.InstrumentationRegistry
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.catalog.Catalog
import com.morialkar.goosescope.core.catalog.Lang

object TestSupport {
    val catalog: Catalog by lazy { Catalog.loadBundled() }

    /** Langue de l'app pendant le test (la tablette peut être en français). */
    val lang: Lang
        get() = if (InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0].language == "fr") Lang.FR else Lang.EN

    fun name(base: BaseColor) = catalog.base(base).name[lang]

    fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
