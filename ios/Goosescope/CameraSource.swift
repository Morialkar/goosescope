// REQUIRES_REVIEW: accès à la caméra et flux d'images (SPEC §8, SPEC-IOS NF13).
// Aucune image n'est écrite ni envoyée: chaque image d'analyse est lue puis relâchée.
import AVFoundation
import CoreImage
import UIKit

/// Une image d'analyse redressée. `grid` n'est valide que pendant l'appel à `onFrame`.
struct CameraFrame {
    let grid: PixelBufferGrid
    let timestampMs: Int64
    /// Copie redressée en mémoire pour le mode figé.
    let snapshot: () -> FrozenImage?
}

struct FrozenImage {
    let cgImage: CGImage
    let grid: BytesGrid
}

/// Source caméra commune à l'app et à l'extension écran verrouillé (CameraSourceProtocol permet la caméra factice des tests).
protocol CameraSourceProtocol: AnyObject {
    var hasTorch: Bool { get }
    /// État de l'autorisation caméra; la caméra factice le simule pour les tests.
    var authorization: AVAuthorizationStatus { get }
    func requestAccess(_ completion: @escaping (Bool) -> Void)
    func start(onFrame: @escaping (CameraFrame) -> Void, onError: @escaping () -> Void)
    func stop()
    func setTorch(_ on: Bool)
    var previewSession: AVCaptureSession? { get }
}

final class CameraSource: NSObject, CameraSourceProtocol, AVCaptureVideoDataOutputSampleBufferDelegate {
    private let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "goosescope.camera")
    private var device: AVCaptureDevice?
    private var onFrame: ((CameraFrame) -> Void)?
    private var rotation: AVCaptureDevice.RotationCoordinator?
    private var rotationObservation: NSKeyValueObservation?
    private let output = AVCaptureVideoDataOutput()
    private let ciContext = CIContext()
    private var configured = false

    var previewSession: AVCaptureSession? { session }
    var hasTorch: Bool { device?.hasTorch ?? false }
    var authorization: AVAuthorizationStatus { AVCaptureDevice.authorizationStatus(for: .video) }

    // REQUIRES_REVIEW: demande de la permission caméra (F7).
    func requestAccess(_ completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video, completionHandler: completion)
    }

    func start(onFrame: @escaping (CameraFrame) -> Void, onError: @escaping () -> Void) {
        self.onFrame = onFrame
        queue.async { [self] in
            if !configured {
                guard configure() else {
                    DispatchQueue.main.async { onError() }
                    return
                }
                configured = true
            }
            session.startRunning()
        }
    }

    func stop() {
        queue.async { [self] in
            session.stopRunning()
            onFrame = nil
        }
    }

    func setTorch(_ on: Bool) {
        guard let device, device.hasTorch else { return }
        try? device.lockForConfiguration()
        device.torchMode = on ? .on : .off
        device.unlockForConfiguration()
    }

    private func configure() -> Bool {
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: camera) else { return false }
        session.beginConfiguration()
        // Même résolution d'analyse qu'Android (ARCHITECTURE §2.1).
        session.sessionPreset = .hd1280x720
        guard session.canAddInput(input) else { return false }
        session.addInput(input)
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: queue)
        guard session.canAddOutput(output) else { return false }
        session.addOutput(output)
        session.commitConfiguration()
        device = camera
        // Les images arrivent déjà redressées selon l'orientation de l'interface: pas de rotation à gérer ailleurs.
        let coordinator = AVCaptureDevice.RotationCoordinator(device: camera, previewLayer: nil)
        rotation = coordinator
        applyRotation(coordinator.videoRotationAngleForHorizonLevelCapture)
        rotationObservation = coordinator.observe(\.videoRotationAngleForHorizonLevelCapture, options: [.new]) { [weak self] c, _ in
            self?.queue.async { self?.applyRotation(c.videoRotationAngleForHorizonLevelCapture) }
        }
        return true
    }

    private func applyRotation(_ angle: CGFloat) {
        if let connection = output.connection(with: .video), connection.isVideoRotationAngleSupported(angle) {
            connection.videoRotationAngle = angle
        }
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let onFrame, let buffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        CVPixelBufferLockBaseAddress(buffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(buffer, .readOnly) }
        let timestamp = Int64(CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer)) * 1000)
        let grid = PixelBufferGrid(lockedBuffer: buffer)
        onFrame(CameraFrame(grid: grid, timestampMs: timestamp, snapshot: { [ciContext] in
            Self.freeze(buffer, context: ciContext)
        }))
    }

    // REQUIRES_REVIEW: l'image figée vit en mémoire seulement (NG3).
    private static func freeze(_ buffer: CVPixelBuffer, context: CIContext) -> FrozenImage? {
        let width = CVPixelBufferGetWidth(buffer)
        let height = CVPixelBufferGetHeight(buffer)
        let bytesPerRow = CVPixelBufferGetBytesPerRow(buffer)
        guard let base = CVPixelBufferGetBaseAddress(buffer),
              let cg = context.createCGImage(CIImage(cvPixelBuffer: buffer), from: CGRect(x: 0, y: 0, width: width, height: height))
        else { return nil }
        var bytes = [UInt8](repeating: 0, count: width * height * 4)
        let src = base.assumingMemoryBound(to: UInt8.self)
        for row in 0..<height {
            bytes.withUnsafeMutableBufferPointer { dst in
                (dst.baseAddress! + row * width * 4).update(from: src + row * bytesPerRow, count: width * 4)
            }
        }
        return FrozenImage(cgImage: cg, grid: BytesGrid(width: width, height: height, bgra: bytes))
    }
}
