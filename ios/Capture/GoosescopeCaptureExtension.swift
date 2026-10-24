// REQUIRES_REVIEW: affichage par-dessus l'écran verrouillé (SPEC-IOS F33).
// L'extension réutilise les écrans de l'app; elle n'enregistre rien (NG3) et n'a accès à aucune donnée de l'app.
import ExtensionKit
import LockedCameraCapture
import SwiftUI

@main
struct GoosescopeCaptureExtension: LockedCameraCaptureExtension {
    var body: some LockedCameraCaptureExtensionScene {
        LockedCameraCaptureUIScene { _ in
            CaptureRoot()
        }
    }
}

/// Même interface que l'app. Limite connue: l'extension n'a pas accès aux préférences de l'app,
/// donc elle s'ouvre en anglais (langue par défaut, F17) quel que soit le choix fait dans l'app.
struct CaptureRoot: View {
    @StateObject private var l10n = L10n()
    @StateObject private var model = ScopeModel(camera: CameraSource())

    var body: some View {
        RootView()
            .environmentObject(l10n)
            .environmentObject(model)
    }
}
