package com.morialkar.goosescope.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// AC19: la tuile est visible du système, et seul le système peut s'y lier.
@RunWith(AndroidJUnit4::class)
class TileDeclarationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun the_quick_settings_tile_is_declared_and_protected() {
        val services = context.packageManager.queryIntentServices(
            Intent(TileService.ACTION_QS_TILE).setPackage(context.packageName),
            PackageManager.ResolveInfoFlags.of(0),
        )
        assertEquals(1, services.size)
        val info = services.single().serviceInfo
        assertEquals(GoosescopeTileService::class.java.name, info.name)
        assertEquals(Manifest.permission.BIND_QUICK_SETTINGS_TILE, info.permission)
    }
}
