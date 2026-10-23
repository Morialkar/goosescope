package com.morialkar.goosescope.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun `core module compiles and runs tests`() {
        assertEquals(1, CATALOG_FORMAT_VERSION)
    }
}
