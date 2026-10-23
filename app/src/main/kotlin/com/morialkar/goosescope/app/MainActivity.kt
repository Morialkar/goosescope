package com.morialkar.goosescope.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.morialkar.goosescope.app.camera.CameraXFrameSource
import com.morialkar.goosescope.app.ui.ScopeApp
import com.morialkar.goosescope.core.catalog.Catalog

class MainActivity : ComponentActivity() {

    private val viewModel: ScopeViewModel by viewModels {
        viewModelFactory { initializer { ScopeViewModel(Catalog::loadBundled) } }
    }

    // REQUIRES_REVIEW: demande de la permission caméra (SPEC F7).
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> viewModel.onPermissionResult(granted) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val language = AppLanguage(this)
        language.ensureDefault()
        val source = CameraXFrameSource(applicationContext)
        setContent {
            ScopeApp(
                viewModel = viewModel,
                source = source,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = ::openAppSettings,
                onToggleLanguage = language::toggle,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Au retour des paramètres Android, la permission a pu changer.
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        when {
            granted -> viewModel.onPermissionResult(true)
            viewModel.state.value == ScopeState.NeedsPermission -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}
