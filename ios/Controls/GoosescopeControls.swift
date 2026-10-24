import AppIntents
import SwiftUI
import WidgetKit

@main
struct GoosescopeControlsBundle: WidgetBundle {
    var body: some Widget { OpenScopeControl() }
}

/// Contrôle du Centre de contrôle et de l'écran verrouillé, assignable au bouton Action (SPEC-IOS F32).
struct OpenScopeControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.morialkar.goosescope.open") {
            ControlWidgetButton(action: OpenScopeIntent()) {
                Label("Goosescope", systemImage: "eyedropper.halffull")
            }
        }
        .displayName("Goosescope")
        .description("Open the camera to name a color.")
    }
}
