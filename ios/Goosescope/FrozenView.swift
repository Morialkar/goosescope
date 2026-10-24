import SwiftUI
import GoosescopeCore

/// Image figée, réticule déplaçable au toucher et fiche détaillée (SPEC F6, F14–F16, F25).
struct FrozenView: View {
    @EnvironmentObject private var model: ScopeModel
    @EnvironmentObject private var l10n: L10n
    let frozen: FrozenState
    @State private var expanded = true
    @State private var entered = false

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            let landscape = size.width > size.height
            let target = viewPoint(in: size)
            ZStack(alignment: .topLeading) {
                Image(decorative: frozen.image, scale: 1)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: size.width, height: size.height)
                    .clipped()
                    .contentShape(Rectangle())
                    .onTapGesture(coordinateSpace: .local) { model.tap(at: $0, in: size) }
                    .accessibilityIdentifier("frozenImage")
                // Le réticule glisse jusqu'au nouveau point: on voit d'où vient la mesure.
                Reticle()
                    .position(target)
                    .animation(.spring(response: 0.45, dampingFraction: 0.7), value: target)
                sheet(landscape: landscape, target: target, size: size)
            }
        }
        .ignoresSafeArea()
        .background(Tokens.surface)
        .onAppear { withAnimation(.easeOut(duration: 0.3)) { entered = true } }
    }

    private func viewPoint(in size: CGSize) -> CGPoint {
        let p = ViewToImageMapper.shared.toView(point: frozen.target, viewW: Int32(size.width), viewH: Int32(size.height),
                                                imageW: frozen.grid.width, imageH: frozen.grid.height)
        return CGPoint(x: CGFloat(p.first?.floatValue ?? 0), y: CGFloat(p.second?.floatValue ?? 0))
    }

    /// La fiche ne couvre jamais le point visé (STANDARDS §4): elle se place du côté opposé.
    @ViewBuilder private func sheet(landscape: Bool, target: CGPoint, size: CGSize) -> some View {
        let atBottom = landscape || target.y < size.height * 0.55
        let content = DetailSheet(detail: frozen.detail, expanded: $expanded, atBottom: atBottom)
            .opacity(entered ? 1 : 0)
            .offset(y: entered ? 0 : 40)
        if landscape {
            let onRight = target.x < size.width * 0.55
            HStack {
                if onRight { Spacer() }
                content.frame(width: 400).frame(maxHeight: expanded ? .infinity : nil, alignment: .bottom)
                if !onRight { Spacer() }
            }
            .frame(maxHeight: .infinity, alignment: expanded ? .center : .bottom)
        } else {
            VStack {
                if atBottom { Spacer() }
                content.frame(maxHeight: expanded ? size.height * 0.62 : nil)
                if !atBottom { Spacer() }
            }
        }
    }
}

struct DetailSheet: View {
    @EnvironmentObject private var model: ScopeModel
    @EnvironmentObject private var l10n: L10n
    let detail: Detail
    @Binding var expanded: Bool
    /// Fiche en bas de l'écran: on la glisse vers le bas pour la réduire (vers le haut quand elle est en haut).
    let atBottom: Bool
    @GestureState private var drag: CGFloat = 0

    var body: some View {
        VStack(spacing: 10) {
            if !atBottom { content }
            grabber
            if atBottom { content }
        }
        .padding(.horizontal, 16)
        .padding(atBottom ? .bottom : .top, 16)
        .padding(atBottom ? .top : .bottom, 8)
        .background(Tokens.surface.ignoresSafeArea())
        .offset(y: dragOffset)
        .animation(.easeInOut(duration: 0.26), value: expanded)
        .animation(.interactiveSpring, value: drag)
    }

    @ViewBuilder private var content: some View {
        if expanded { full } else { compact }
    }

    /// Poignée à glisser, comme les feuilles d'iOS (demande de Naomi, 2026-10-24).
    private var grabber: some View {
        Capsule().fill(Tokens.textMuted).frame(width: 40, height: 5)
            .frame(maxWidth: .infinity, minHeight: 22)
            .contentShape(Rectangle())
            .gesture(swipe)
            .accessibilityLabel(l10n.t(expanded ? "detail_hide" : "detail_show"))
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { expanded.toggle() }
            .accessibilityIdentifier("detailGrabber")
    }

    /// Glisser vers le bord réduit la fiche, glisser vers le centre la rouvre. La fiche suit le doigt
    /// seulement dans le sens qui a un effet.
    private var swipe: some Gesture {
        DragGesture(minimumDistance: 10)
            .updating($drag) { value, state, _ in state = value.translation.height }
            .onEnded { value in
                let towardEdge = atBottom ? value.translation.height : -value.translation.height
                if towardEdge > 60 { expanded = false } else if towardEdge < -60 { expanded = true }
            }
    }

    private var dragOffset: CGFloat {
        let towardEdge = atBottom ? drag : -drag
        guard expanded, towardEdge > 0 else { return 0 }
        return drag
    }

    private var full: some View {
        VStack(alignment: .leading, spacing: 14) {
            switch detail {
            case .tooDark:
                Text(l10n.t("too_dark")).font(.system(size: Tokens.shadeNameSize, weight: .bold)).accessibilityIdentifier("frozenTooDark")
                Text(l10n.t("too_dark_frozen_hint")).font(.system(size: Tokens.bodySize))
            case let .color(base, shade, measured, facts):
                ColorHeader(base: model.baseName(base, l10n.lang), shade: tr(shade.name, l10n.lang), measured: measured, reference: shade.ref,
                            referenceLabel: l10n.t("detail_reference"))
                    .gesture(swipe)
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        DetailBody(shade: shade, facts: facts).id(shade.id)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                        Text(l10n.t("detail_tap_hint")).font(.system(size: Tokens.labelSize)).foregroundStyle(Tokens.textMuted)
                    }
                    .animation(.easeOut(duration: 0.3), value: shade.id)
                }
            }
            HStack(spacing: 12) {
                FlatButton(label: l10n.t("resume"), fill: true, action: model.resume).accessibilityIdentifier("resume")
                FlatButton(label: l10n.t("detail_hide")) { expanded = false }.accessibilityIdentifier("detailToggle")
                // Changer de langue sans quitter la fiche (SPEC F35).
                FlatButton(label: l10n.t("other_language"), action: l10n.toggle).accessibilityIdentifier("language")
            }
        }
        .foregroundStyle(Tokens.textOnSurface)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("detail")
    }

    /// Fiche réduite: de quoi savoir ce qui est visé pendant qu'on cherche (F25).
    private var compact: some View {
        HStack(spacing: 12) {
            switch detail {
            case .tooDark:
                Text(l10n.t("too_dark")).font(.system(size: Tokens.bodySize, weight: .bold))
                Spacer()
            case let .color(base, shade, measured, _):
                RoundedRectangle(cornerRadius: Tokens.corner).fill(Color(measured)).frame(width: 48, height: 48)
                    .animation(.easeOut(duration: 0.38), value: measured.toHex())
                VStack(alignment: .leading, spacing: 2) {
                    Text(model.baseName(base, l10n.lang).uppercased())
                        .font(.system(size: Tokens.labelSize, weight: .bold)).tracking(2)
                        .foregroundStyle(Tokens.textMuted).accessibilityIdentifier("compactBase")
                    Text(tr(shade.name, l10n.lang)).font(.system(size: 22, weight: .bold)).accessibilityIdentifier("compactShade")
                }
                Spacer()
            }
            FlatButton(label: l10n.t("detail_show")) { expanded = true }.accessibilityIdentifier("detailToggle")
        }
        .foregroundStyle(Tokens.textOnSurface)
        .contentShape(Rectangle())
        .gesture(swipe)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("detailCompact")
    }
}

/// Bandeau de la couleur mesurée et de la nuance de référence, en fondu d'un point à l'autre.
struct ColorHeader: View {
    let base: String
    let shade: String
    let measured: Srgb8
    let reference: Srgb8
    let referenceLabel: String

    var body: some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading) {
                Text(base.uppercased()).font(.system(size: Tokens.labelSize, weight: .bold)).tracking(2)
                    .accessibilityIdentifier("detailBase")
                Spacer()
                Text(shade).font(.system(size: Tokens.shadeNameSize, weight: .bold)).lineLimit(2).minimumScaleFactor(0.7)
                    .accessibilityIdentifier("detailShade")
            }
            .foregroundStyle(measured.textColor)
            .padding(16)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .background(Color(measured))
            VStack {
                Spacer()
                Text(referenceLabel).font(.system(size: 13, weight: .bold)).foregroundStyle(reference.textColor)
            }
            .padding(12)
            .frame(maxHeight: .infinity)
            .frame(width: 110)
            .background(Color(reference))
        }
        .frame(height: 112)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .animation(.easeOut(duration: 0.38), value: measured.toHex())
    }
}

struct DetailBody: View {
    @EnvironmentObject private var model: ScopeModel
    @EnvironmentObject private var l10n: L10n
    let shade: Shade
    let facts: ComparisonFacts

    var body: some View {
        let shadeName = tr(shade.name, l10n.lang)
        VStack(alignment: .leading, spacing: 16) {
            Text(tr(shade.description_, l10n.lang)).font(.system(size: Tokens.bodySize)).accessibilityIdentifier("detailDescription")
            // La phrase nomme déjà la nuance de référence: pas de titre qui la répète.
            Text(ComparisonText.shared.render(facts: facts, shadeName: shadeName, lang: l10n.lang) { model.baseName($0, l10n.lang) })
                .font(.system(size: Tokens.bodySize))
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Tokens.surfaceRaised, in: RoundedRectangle(cornerRadius: Tokens.corner))
                .accessibilityIdentifier("detailComparison")
            VStack(alignment: .leading, spacing: 10) {
                Text(l10n.t("detail_nature")).font(.system(size: Tokens.labelSize, weight: .bold)).foregroundStyle(Tokens.textMuted)
                FlowLayout {
                    ForEach(Array(trList(shade.nature, l10n.lang).enumerated()), id: \.offset) { i, item in
                        NatureTag(text: item, color: shade.ref, index: i)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("detailNature")
            }
        }
    }
}

/// Un élément de la nature en étiquette, qui apparaît en cascade.
struct NatureTag: View {
    let text: String
    let color: Srgb8
    let index: Int
    @State private var shown = false

    var body: some View {
        HStack(spacing: 8) {
            // Anneau clair: la pastille reste visible même quand la nuance est aussi sombre que l'étiquette.
            Circle().fill(Color(color)).frame(width: 11, height: 11)
                .padding(1.5).background(Circle().fill(Tokens.textMuted))
            Text(text).font(.system(size: Tokens.labelSize))
        }
        .padding(.leading, 10).padding(.trailing, 14).padding(.vertical, 8)
        .background(Tokens.surfaceRaised, in: Capsule())
        .scaleEffect(shown ? 1 : 0.85)
        .opacity(shown ? 1 : 0)
        .onAppear {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.6).delay(0.12 + Double(index) * 0.07)) { shown = true }
        }
        // Une étiquette = un élément lu d'un trait (pastille et texte).
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("natureTag")
    }
}
