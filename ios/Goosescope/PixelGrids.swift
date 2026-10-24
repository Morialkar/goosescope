import CoreVideo
import GoosescopeCore

/// Pixels d'une image caméra BGRA, lus sans copie pendant l'analyse (IMPACT-IOS §2.6).
/// Valide seulement tant que le tampon est verrouillé: ne pas garder l'objet après l'analyse.
final class PixelBufferGrid: NSObject, PixelGrid {
    let width: Int32
    let height: Int32
    private let base: UnsafePointer<UInt8>
    private let bytesPerRow: Int

    init(lockedBuffer buffer: CVPixelBuffer) {
        width = Int32(CVPixelBufferGetWidth(buffer))
        height = Int32(CVPixelBufferGetHeight(buffer))
        bytesPerRow = CVPixelBufferGetBytesPerRow(buffer)
        base = UnsafePointer(CVPixelBufferGetBaseAddress(buffer)!.assumingMemoryBound(to: UInt8.self))
    }

    func rgbAt(x: Int32, y: Int32) -> Srgb8 {
        let i = Int(y) * bytesPerRow + Int(x) * 4
        return Srgb8(r: Int32(base[i + 2]), g: Int32(base[i + 1]), b: Int32(base[i]))
    }
}

/// Copie d'une image figée: chaque toucher est instantané (NF8), et l'image vit en mémoire seulement (NG3).
final class BytesGrid: NSObject, PixelGrid {
    let width: Int32
    let height: Int32
    private let bytes: [UInt8]

    init(width: Int, height: Int, bgra: [UInt8]) {
        self.width = Int32(width)
        self.height = Int32(height)
        self.bytes = bgra
    }

    func rgbAt(x: Int32, y: Int32) -> Srgb8 {
        let i = (Int(y) * Int(width) + Int(x)) * 4
        return Srgb8(r: Int32(bytes[i + 2]), g: Int32(bytes[i + 1]), b: Int32(bytes[i]))
    }
}
