import AppIntents

/// Action « ouvrir Goosescope » (SPEC-IOS F32, F33). Type « capture caméra »: iPhone déverrouillé,
/// iOS ouvre l'app; verrouillé, il ouvre l'extension écran verrouillé, sans déverrouiller.
struct OpenScopeIntent: CameraCaptureIntent {
    static let title: LocalizedStringResource = "Goosescope"
    static let description = IntentDescription("Open the camera to name the color you point at.")

    @MainActor
    func perform() async throws -> some IntentResult { .result() }
}
