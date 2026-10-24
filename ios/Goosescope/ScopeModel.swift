import AVFoundation
import Combine
import GoosescopeCore
import UIKit

enum LiveReading: Equatable {
    case warming
    case tooDark
    case color(base: BaseColor, shade: Shade, swatch: Srgb8)
}

enum Detail {
    case tooDark
    case color(base: BaseColor, shade: Shade, measured: Srgb8, facts: ComparisonFacts)
}

struct FrozenState {
    let image: CGImage
    let grid: BytesGrid
    var target: Point
    var detail: Detail
    /// Change à chaque nouvelle image figée: la fiche se rouvre dépliée (F25).
    let id = UUID()
}

enum Failure { case catalog, camera }

enum ScopeState {
    case needsPermission
    case permissionDenied
    case live(LiveReading, torchOn: Bool)
    case frozen(FrozenState)
    case failure(Failure)
}

/// Logique couleur entièrement déléguée au cœur partagé (F30): ce fichier ne fait que l'orchestrer,
/// comme ScopeViewModel côté Android (ARCHITECTURE §2.1, §2.2).
@MainActor
final class ScopeModel: ObservableObject {
    @Published private(set) var state: ScopeState
    @Published private(set) var hasTorch = false

    let catalog: Catalog?
    let camera: CameraSourceProtocol
    private let processor: FrameProcessor?

    init(camera: CameraSourceProtocol) {
        self.camera = camera
        let loaded = try? Catalog.companion.loadBundled()
        catalog = loaded
        processor = loaded.map(FrameProcessor.init)
        state = loaded == nil ? .failure(.catalog) : .needsPermission
    }

    func baseName(_ base: BaseColor, _ lang: Lang) -> String {
        catalog.map { tr($0.base(color: base).name, lang) } ?? base.id
    }

    // REQUIRES_REVIEW: permission caméra (F7).
    func checkPermission() {
        guard case .needsPermission = state else { return }
        switch camera.authorization {
        case .authorized: startLive()
        case .notDetermined: requestPermission()
        default: state = .permissionDenied
        }
    }

    func requestPermission() {
        camera.requestAccess { granted in
            Task { @MainActor in
                if granted { self.startLive() } else { self.state = .permissionDenied }
            }
        }
    }

    func onForeground() {
        if case .permissionDenied = state, camera.authorization == .authorized {
            startLive()
        }
    }

    private func startLive() {
        guard let processor else { return }
        processor.reset()
        state = .live(.warming, torchOn: false)
        camera.start(onFrame: { [weak self] frame in
            guard let result = processor.process(frame) else { return }
            Task { @MainActor in self?.apply(result) }
        }, onError: { [weak self] in
            self?.state = .failure(.camera)
        })
        hasTorch = camera.hasTorch
    }

    private func apply(_ result: FrameProcessor.Result) {
        guard case let .live(_, torchOn) = state else { return }
        hasTorch = camera.hasTorch
        switch result {
        case let .reading(reading): state = .live(reading, torchOn: torchOn)
        case let .frozen(image): freeze(image)
        }
    }

    func retryCamera() {
        if case .failure(.camera) = state { startLive() }
    }

    func toggleTorch() {
        guard case let .live(reading, torchOn) = state else { return }
        camera.setTorch(!torchOn)
        state = .live(reading, torchOn: !torchOn)
    }

    /// Fige à la prochaine image d'analyse (F6).
    func requestFreeze() {
        if case .live = state { processor?.requestFreeze() }
    }

    private func freeze(_ image: FrozenImage) {
        guard let processor else { return }
        camera.setTorch(false)
        camera.stop()
        let center = Point(x: image.grid.width / 2, y: image.grid.height / 2)
        let tooDark = processor.isTooDark(image.grid)
        state = .frozen(FrozenState(image: image.cgImage, grid: image.grid, target: center,
                                    detail: processor.detail(image.grid, at: center, tooDark: tooDark)))
    }

    /// Toucher sur l'image figée, en points de la vue (F16). Ignoré hors de la vue.
    func tap(at location: CGPoint, in size: CGSize) {
        guard case var .frozen(frozen) = state, let processor,
              let point = ViewToImageMapper.shared.map(
                tapX: Float(location.x), tapY: Float(location.y),
                viewW: Int32(size.width), viewH: Int32(size.height),
                imageW: frozen.grid.width, imageH: frozen.grid.height)
        else { return }
        frozen.target = point
        frozen.detail = processor.detail(frozen.grid, at: point, tooDark: processor.isTooDark(frozen.grid))
        state = .frozen(frozen)
    }

    func resume() {
        guard case .frozen = state else { return }
        startLive()
    }
}

/// Travail sur le fil de la caméra: lissage et décisions du cœur. Jamais touché depuis le fil principal,
/// sauf `requestFreeze` (verrou) et les calculs sur image figée, qui n'utilisent pas le lissage.
final class FrameProcessor: @unchecked Sendable {
    enum Result { case reading(LiveReading), frozen(FrozenImage) }

    private let classifier: BaseClassifier
    private let matcher: ShadeMatcher
    private let smoother: Smoother
    private let shades: ShadeStabilizer
    private let gate = DarknessGate(threshold: Tuning.shared.TOO_DARK_MEAN_LUMINANCE)
    private let lock = NSLock()
    private var freezeRequested = false

    init(catalog: Catalog) {
        classifier = BaseClassifier(catalog: catalog)
        matcher = ShadeMatcher(catalog: catalog)
        smoother = Smoother(classifier: classifier, alpha: Tuning.shared.SMOOTHING_ALPHA,
                            margin: Tuning.shared.HYSTERESIS_MARGIN, dwellMs: Tuning.shared.HYSTERESIS_DWELL_MS)
        shades = ShadeStabilizer(matcher: matcher, margin: Tuning.shared.SHADE_HYSTERESIS_MARGIN)
    }

    func requestFreeze() {
        lock.withLock { freezeRequested = true }
    }

    func reset() {
        lock.withLock { freezeRequested = false }
        smoother.reset()
        shades.reset()
    }

    func process(_ frame: CameraFrame) -> Result? {
        let freeze = lock.withLock { () -> Bool in
            defer { freezeRequested = false }
            return freezeRequested
        }
        if freeze { return frame.snapshot().map(Result.frozen) }
        if isTooDark(frame.grid) {
            smoother.reset()
            shades.reset()
            return .reading(.tooDark)
        }
        let sample = Sampler.shared.sampleCenter(grid: frame.grid)
        let reading = smoother.push(c: ColorMath.shared.oklabFromSrgb(c: sample), timestampMs: frame.timestampMs)
        let shade = shades.update(c: reading.color, base: reading.base)
        return .reading(.color(base: reading.base, shade: shade, swatch: ColorMath.shared.srgbFromOklab(c: reading.color)))
    }

    func isTooDark(_ grid: PixelGrid) -> Bool {
        gate.isTooDark(meanLuminance: Sampler.shared.meanLuminance(grid: grid, step: Tuning.shared.LUMINANCE_STEP))
    }

    func detail(_ grid: PixelGrid, at point: Point, tooDark: Bool) -> Detail {
        if tooDark { return .tooDark }
        let measured = Sampler.shared.sample(grid: grid, center: point, sideFraction: Tuning.shared.SAMPLE_SIDE_FRACTION)
        let lab = ColorMath.shared.oklabFromSrgb(c: measured)
        let base = classifier.classify(c: lab).base
        let shade = matcher.match(c: lab, base: base)
        return .color(base: base, shade: shade, measured: measured,
                      facts: ComparisonEngine.shared.compare(measured: lab, reference: shade.refLab))
    }
}
