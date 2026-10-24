import AVFoundation
import SwiftUI

/// Aperçu caméra plein écran, rogné au centre comme sur Android (FILL_CENTER).
struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession?

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.videoGravity = .resizeAspectFill
        view.previewLayer.session = session
        view.startRotationTracking()
        return view
    }

    func updateUIView(_ view: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
        private var coordinator: AVCaptureDevice.RotationCoordinator?
        private var observation: NSKeyValueObservation?

        func startRotationTracking() {
            let device = (previewLayer.session?.inputs.first as? AVCaptureDeviceInput)?.device
                ?? AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
            guard let device else { return }
            let c = AVCaptureDevice.RotationCoordinator(device: device, previewLayer: previewLayer)
            coordinator = c
            previewLayer.connection?.videoRotationAngle = c.videoRotationAngleForHorizonLevelPreview
            observation = c.observe(\.videoRotationAngleForHorizonLevelPreview, options: [.new]) { [weak self] c, _ in
                self?.previewLayer.connection?.videoRotationAngle = c.videoRotationAngleForHorizonLevelPreview
            }
        }
    }
}
