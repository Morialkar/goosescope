import Foundation
import GoosescopeCore

/// Langue de l'app, indépendante de celle d'iOS (SPEC F17, IMPACT-IOS §2.5): anglais par défaut,
/// bascule immédiate, choix mémorisé. Les textes viennent du bundle `.lproj` de la langue choisie.
final class L10n: ObservableObject {
    static let storageKey = "goosescope.lang"

    @Published private(set) var lang: Lang
    private var bundle: Bundle
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let saved = defaults.string(forKey: Self.storageKey)
        lang = saved == "fr" ? .fr : .en
        bundle = Self.bundle(for: saved == "fr" ? "fr" : "en")
    }

    func t(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    func toggle() {
        set(lang == .fr ? .en : .fr)
    }

    func set(_ newLang: Lang) {
        let code = newLang == .fr ? "fr" : "en"
        defaults.set(code, forKey: Self.storageKey)
        bundle = Self.bundle(for: code)
        lang = newLang
    }

    private static func bundle(for code: String) -> Bundle {
        Bundle.main.path(forResource: code, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
    }
}

// Fonctions libres: Swift interdit les extensions d'une classe Kotlin générique (Localized<T>).

/// Texte d'un `Localized<String>` du catalogue dans la langue donnée.
func tr(_ value: Localized<NSString>, _ lang: Lang) -> String { (value.get(lang: lang) as String?) ?? "" }

/// Liste d'un `Localized<List<String>>` du catalogue dans la langue donnée.
func trList(_ value: Localized<NSArray>, _ lang: Lang) -> [String] { (value.get(lang: lang) as? [String]) ?? [] }
