import SwiftUI
import GoosescopeCore

struct RootView: View {
    @EnvironmentObject private var model: ScopeModel
    @EnvironmentObject private var l10n: L10n
    @Environment(\.scenePhase) private var scenePhase
    // openURL plutôt que UIApplication.shared: ce dernier n'existe pas dans l'extension écran verrouillé.
    @Environment(\.openURL) private var openURL

    var body: some View {
        Group {
            switch model.state {
            case .needsPermission:
                Tokens.surface.ignoresSafeArea()
            case .permissionDenied:
                // REQUIRES_REVIEW: écran de permission caméra (F7).
                MessageView(
                    title: l10n.t("permission_title"), body_: l10n.t("permission_body"),
                    primary: (l10n.t("open_settings"), openSettings), tag: "permission")
            case let .live(reading, torchOn):
                LiveView(reading: reading, torchOn: torchOn)
            case let .frozen(frozen):
                FrozenView(frozen: frozen).id(frozen.id)
            case .failure(.catalog):
                MessageView(title: l10n.t("error_title"), body_: l10n.t("error_catalog"), tag: "failure")
            case .failure(.camera):
                MessageView(title: l10n.t("error_title"), body_: l10n.t("error_camera"),
                            primary: (l10n.t("retry"), model.retryCamera), tag: "failure")
            }
        }
        .preferredColorScheme(.dark)
        .onAppear { model.checkPermission() }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.onForeground() }
        }
    }

    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
    }
}

struct LiveView: View {
    @EnvironmentObject private var model: ScopeModel
    @EnvironmentObject private var l10n: L10n
    let reading: LiveReading
    let torchOn: Bool

    var body: some View {
        GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height
            ZStack {
                CameraPreview(session: model.camera.previewSession).ignoresSafeArea()
                Reticle()
                if landscape {
                    HStack { Spacer(); panel(landscape: true).frame(width: 340) }
                } else {
                    VStack { Spacer(); panel(landscape: false) }
                }
            }
        }
        .background(Tokens.surface)
    }

    private func panel(landscape: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if landscape { Spacer() }
            swatch
            HStack(spacing: 12) {
                FlatButton(label: l10n.t("freeze"), fill: true, action: model.requestFreeze).accessibilityIdentifier("freeze")
                if model.hasTorch {
                    FlatButton(label: l10n.t(torchOn ? "torch_off" : "torch_on"), action: model.toggleTorch)
                        .accessibilityIdentifier("torch")
                }
                // Le bouton affiche l'autre langue, dans cette langue.
                FlatButton(label: l10n.t("other_language"), action: l10n.toggle).accessibilityIdentifier("language")
            }
            if landscape { Spacer() }
        }
        .padding(16)
        .background(Tokens.surface.ignoresSafeArea())
    }

    @ViewBuilder private var swatch: some View {
        switch reading {
        case let .color(base, shade, color):
            let baseName = model.baseName(base, l10n.lang)
            let shadeName = tr(shade.name, l10n.lang)
            // SPEC révision 4: « Rouge — Écarlate », nuance omise si elle répète la base.
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(baseName).font(.system(size: Tokens.baseNameSize, weight: .bold)).accessibilityIdentifier("baseName")
                if shadeName.caseInsensitiveCompare(baseName) != .orderedSame {
                    Text("— \(shadeName)").font(.system(size: Tokens.baseNameSize)).accessibilityIdentifier("shadeName")
                }
            }
            .lineLimit(2).minimumScaleFactor(0.6)
            .foregroundStyle(color.textColor)
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 96, alignment: .leading)
            .background(Color(color), in: RoundedRectangle(cornerRadius: Tokens.corner))
            .animation(.easeOut(duration: 0.25), value: reading)
        case .tooDark:
            VStack(alignment: .leading, spacing: 4) {
                Text(l10n.t("too_dark")).font(.system(size: Tokens.baseNameSize, weight: .bold)).accessibilityIdentifier("tooDark")
                Text(l10n.t("too_dark_hint")).font(.system(size: Tokens.bodySize))
            }
            .foregroundStyle(Tokens.textOnSurface)
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 96, alignment: .leading)
            .background(Tokens.surfaceRaised, in: RoundedRectangle(cornerRadius: Tokens.corner))
        case .warming:
            Text(l10n.t("warming")).font(.system(size: Tokens.bodySize)).accessibilityIdentifier("warming")
                .foregroundStyle(Tokens.textOnSurface)
                .padding(16)
                .frame(maxWidth: .infinity, minHeight: 96, alignment: .leading)
                .background(Tokens.surfaceRaised, in: RoundedRectangle(cornerRadius: Tokens.corner))
        }
    }
}
