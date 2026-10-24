import SwiftUI
import GoosescopeCore

struct FlatButton: View {
    let label: String
    var background: Color = Tokens.surfaceRaised
    var fill = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: Tokens.labelSize, weight: .bold))
                .foregroundStyle(Tokens.textOnSurface)
                .padding(.horizontal, 18)
                .frame(maxWidth: fill ? .infinity : nil, minHeight: Tokens.minTouch)
                .background(background, in: RoundedRectangle(cornerRadius: Tokens.corner))
        }
        .buttonStyle(.plain)
    }
}

/// Anneau clair doublé d'un anneau sombre: lisible sur n'importe quelle image (STANDARDS §4).
struct Reticle: View {
    var body: some View {
        ZStack {
            Circle().stroke(Tokens.reticleInner, lineWidth: 3).padding(1.5)
            Circle().stroke(Tokens.reticleOuter, lineWidth: 3).padding(4.5)
            Circle().stroke(Tokens.reticleInner, lineWidth: 2).padding(8)
        }
        .frame(width: 56, height: 56)
        .accessibilityIdentifier("reticle")
    }
}

struct MessageView: View {
    let title: String
    let body_: String
    var primary: (String, () -> Void)?
    var secondary: (String, () -> Void)?
    let tag: String

    var body: some View {
        ZStack {
            Tokens.surface.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 16) {
                Text(title).font(.system(size: Tokens.shadeNameSize, weight: .bold))
                Text(body_).font(.system(size: Tokens.bodySize))
                if let primary { FlatButton(label: primary.0, action: primary.1).accessibilityIdentifier("\(tag).primary") }
                if let secondary {
                    FlatButton(label: secondary.0, background: Tokens.surface, action: secondary.1)
                        .accessibilityIdentifier("\(tag).secondary")
                }
            }
            .foregroundStyle(Tokens.textOnSurface)
            .frame(maxWidth: 520, alignment: .leading)
            .padding(24)
        }
        // .contain: le conteneur porte son identifiant sans écraser ceux de ses boutons.
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(tag)
    }
}

/// Étiquettes qui passent à la ligne (éléments de la nature).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(subviews, width: proposal.width ?? .infinity)
        return CGSize(width: proposal.width ?? rows.map(\.width).max() ?? 0,
                      height: rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(rows.count - 1, 0)))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row { var indices: [Int] = []; var width: CGFloat = 0; var height: CGFloat = 0 }

    private func arrange(_ subviews: Subviews, width: CGFloat) -> [Row] {
        var rows = [Row()]
        for (i, view) in subviews.enumerated() {
            let size = view.sizeThatFits(.unspecified)
            if !rows[rows.count - 1].indices.isEmpty && rows[rows.count - 1].width + spacing + size.width > width {
                rows.append(Row())
            }
            var row = rows.removeLast()
            row.width += (row.indices.isEmpty ? 0 : spacing) + size.width
            row.height = max(row.height, size.height)
            row.indices.append(i)
            rows.append(row)
        }
        return rows
    }
}
