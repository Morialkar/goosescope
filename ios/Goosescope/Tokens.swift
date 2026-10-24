import SwiftUI
import GoosescopeCore

// Jetons visuels: mêmes valeurs que l'app Android (STANDARDS §4), seule source de couleurs côté iOS.
enum Tokens {
    static let surface = Color(hex: 0x3F4752)
    static let surfaceRaised = Color(hex: 0x545D69)
    static let textOnSurface = Color(hex: 0xF4F2EC)
    static let textMuted = Color(hex: 0xD8DBDF)
    static let reticleOuter = Color(hex: 0xF4F2EC)
    static let reticleInner = Color(hex: 0x1F2328)

    static let corner: CGFloat = 12
    static let baseNameSize: CGFloat = 40
    static let shadeNameSize: CGFloat = 30
    static let bodySize: CGFloat = 18
    static let labelSize: CGFloat = 15
    static let minTouch: CGFloat = 48
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    init(_ c: Srgb8) {
        self.init(red: Double(c.r) / 255, green: Double(c.g) / 255, blue: Double(c.b) / 255)
    }
}

extension Srgb8 {
    /// Texte noir ou blanc purs sur un aplat mesuré (STANDARDS §4), règle du cœur.
    var textColor: Color { Color(Contrast.shared.textColorOn(background: self)) }
}
