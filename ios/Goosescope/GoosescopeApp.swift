import SwiftUI

@main
struct GoosescopeApp: App {
    @StateObject private var l10n = L10n()
    @StateObject private var model = ScopeModel(camera: CameraFactory.make())

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(l10n)
                .environmentObject(model)
        }
    }
}

/// Caméra réelle, ou caméra factice quand les tests d'interface la demandent (PLAN-IOS I6).
enum CameraFactory {
    static func make() -> CameraSourceProtocol {
        if let scenario = ProcessInfo.processInfo.environment["GOOSESCOPE_FAKE_CAMERA"] {
            return FakeCameraSource(scenario: scenario)
        }
        return CameraSource()
    }
}
