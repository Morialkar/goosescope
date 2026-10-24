import AVFoundation
import CoreVideo
import GoosescopeCore

/// Caméra factice pour les tests d'interface (PLAN-IOS I6), activée par la variable d'environnement
/// GOOSESCOPE_FAKE_CAMERA. Scénarios: une couleur `#RRGGBB`, `dark`, `bands` (bandes rouge/vert/bleu),
/// `split` (haut bleu, bas rouge), `fail` (caméra indisponible), `denied` (permission refusée).
final class FakeCameraSource: CameraSourceProtocol {
    private let scenario: String
    private var timer: DispatchSourceTimer?
    private var clock: Int64 = 0
    var hasTorch: Bool { true }
    var authorization: AVAuthorizationStatus { scenario == "denied" ? .denied : .authorized }
    func requestAccess(_ completion: @escaping (Bool) -> Void) { completion(scenario != "denied") }
    var previewSession: AVCaptureSession? { nil }

    init(scenario: String) { self.scenario = scenario }

    func start(onFrame: @escaping (CameraFrame) -> Void, onError: @escaping () -> Void) {
        if scenario == "fail" {
            DispatchQueue.main.async { onError() }
            return
        }
        let queue = DispatchQueue(label: "goosescope.fakecamera")
        let t = DispatchSource.makeTimerSource(queue: queue)
        t.schedule(deadline: .now(), repeating: .milliseconds(33))
        t.setEventHandler { [weak self] in self?.emit(onFrame) }
        t.resume()
        timer = t
    }

    func stop() {
        timer?.cancel()
        timer = nil
    }

    func setTorch(_ on: Bool) {}

    private func emit(_ onFrame: (CameraFrame) -> Void) {
        let width = 180, height = 320
        var bytes = [UInt8](repeating: 0, count: width * height * 4)
        for y in 0..<height {
            let c = color(atRow: y, height: height)
            for x in 0..<width {
                let i = (y * width + x) * 4
                bytes[i] = c.2; bytes[i + 1] = c.1; bytes[i + 2] = c.0; bytes[i + 3] = 255
            }
        }
        clock += 33
        var buffer: CVPixelBuffer?
        CVPixelBufferCreate(nil, width, height, kCVPixelFormatType_32BGRA, nil, &buffer)
        guard let buffer else { return }
        CVPixelBufferLockBaseAddress(buffer, [])
        let dst = CVPixelBufferGetBaseAddress(buffer)!.assumingMemoryBound(to: UInt8.self)
        let bpr = CVPixelBufferGetBytesPerRow(buffer)
        for row in 0..<height {
            bytes.withUnsafeBufferPointer { src in (dst + row * bpr).update(from: src.baseAddress! + row * width * 4, count: width * 4) }
        }
        let grid = PixelBufferGrid(lockedBuffer: buffer)
        let image = BytesGrid(width: width, height: height, bgra: bytes)
        onFrame(CameraFrame(grid: grid, timestampMs: clock, snapshot: {
            guard let cg = Self.cgImage(bytes: bytes, width: width, height: height) else { return nil }
            return FrozenImage(cgImage: cg, grid: image)
        }))
        CVPixelBufferUnlockBaseAddress(buffer, [])
    }

    private func color(atRow y: Int, height: Int) -> (UInt8, UInt8, UInt8) {
        switch scenario {
        case "dark": return (4, 4, 4)
        // Bandes dans le tiers haut, au-dessus de la fiche dépliée (qui occupe jusqu'à 62 % de la hauteur).
        case "bands": return y < height / 5 ? (220, 30, 30) : y < height * 9 / 20 ? (40, 160, 60) : (30, 90, 220)
        case "split": return y >= height * 2 / 3 ? (220, 30, 30) : (30, 90, 220)
        default:
            let c = Srgb8.companion.fromHex(hex: scenario)
            return (UInt8(c.r), UInt8(c.g), UInt8(c.b))
        }
    }

    private static func cgImage(bytes: [UInt8], width: Int, height: Int) -> CGImage? {
        let data = Data(bytes) as CFData
        guard let provider = CGDataProvider(data: data) else { return nil }
        return CGImage(width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4,
                       space: CGColorSpaceCreateDeviceRGB(),
                       bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue),
                       provider: provider, decode: nil, shouldInterpolate: false, intent: .defaultIntent)
    }
}
