import Vision
import AVFoundation
import UIKit

enum DetectionMode: Int {
    case smileOnly = 0
    case laughOnly = 1
    case smileAndLaugh = 2
    case smileOrLaugh = 3
}

class FaceDetector: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private var detectionMode: DetectionMode = .smileOrLaugh
    private var hasTriggeredThisCycle = false
    private var smileDetectedForAndMode = false

    // Thresholds matching Android version
    private let smileThreshold: Float = 0.4
    private let laughThreshold: Float = 0.75

    // Callbacks
    var onSmileDetected: (() -> Void)?
    var onLaughDetected: (() -> Void)?
    var onFaceDetected: ((Int) -> Void)?
    var onNoFaceDetected: (() -> Void)?

    private let sequenceHandler = VNSequenceRequestHandler()
    private var frameCount = 0

    func attachToCamera(_ cameraManager: CameraManager) {
        cameraManager.videoDataDelegate = self
    }

    func setDetectionMode(_ mode: DetectionMode) {
        detectionMode = mode
        resetDetectionState()
    }

    func resetDetectionState() {
        hasTriggeredThisCycle = false
        smileDetectedForAndMode = false
    }

    func allowNewDetection() {
        hasTriggeredThisCycle = false
    }

    // MARK: - AVCaptureVideoDataOutputSampleBufferDelegate

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        // Process every 6th frame (~5fps) for performance
        frameCount += 1
        guard frameCount % 6 == 0 else { return }

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let request = VNDetectFaceLandmarksRequest { [weak self] request, error in
            guard let self = self, error == nil else { return }
            self.processFaceObservations(request.results as? [VNFaceObservation] ?? [])
        }

        // Also request face capture quality for smile approximation
        let qualityRequest = VNDetectFaceCaptureQualityRequest()

        try? sequenceHandler.perform([request, qualityRequest], on: pixelBuffer, orientation: .right)
    }

    private func processFaceObservations(_ observations: [VNFaceObservation]) {
        if observations.isEmpty {
            onNoFaceDetected?()
            return
        }

        onFaceDetected?(observations.count)

        guard !hasTriggeredThisCycle else { return }

        for face in observations {
            // Vision framework doesn't directly provide smile probability,
            // but we can approximate using face capture quality and landmarks.
            // For a more robust solution, use the face landmarks to detect
            // mouth curvature as a smile proxy.
            let smileProbability = estimateSmileProbability(face: face)

            let isSmiling = smileProbability >= smileThreshold
            let isLaughing = smileProbability >= laughThreshold

            switch detectionMode {
            case .smileOnly:
                if isSmiling {
                    hasTriggeredThisCycle = true
                    onSmileDetected?()
                    return
                }

            case .laughOnly:
                if isLaughing {
                    hasTriggeredThisCycle = true
                    onLaughDetected?()
                    return
                }

            case .smileAndLaugh:
                if !smileDetectedForAndMode && isSmiling && !isLaughing {
                    smileDetectedForAndMode = true
                    onSmileDetected?()
                } else if smileDetectedForAndMode && isLaughing {
                    hasTriggeredThisCycle = true
                    onLaughDetected?()
                    return
                }

            case .smileOrLaugh:
                if isLaughing {
                    hasTriggeredThisCycle = true
                    onLaughDetected?()
                    return
                } else if isSmiling {
                    hasTriggeredThisCycle = true
                    onSmileDetected?()
                    return
                }
            }
        }
    }

    /// Estimates smile probability from face landmarks.
    /// Uses the mouth region landmarks to approximate mouth curvature.
    private func estimateSmileProbability(face: VNFaceObservation) -> Float {
        guard let landmarks = face.landmarks,
              let outerLips = landmarks.outerLips else {
            // Fallback: use face capture quality as a rough proxy
            return (face.faceCaptureQuality ?? 0.3) > 0.5 ? 0.5 : 0.2
        }

        let points = outerLips.normalizedPoints

        guard points.count >= 6 else { return 0.2 }

        // Calculate mouth width vs height ratio
        // A smile tends to be wider relative to height
        let xs = points.map { $0.x }
        let ys = points.map { $0.y }

        let width = (xs.max() ?? 0) - (xs.min() ?? 0)
        let height = (ys.max() ?? 0) - (ys.min() ?? 0)

        guard width > 0 else { return 0.2 }

        let ratio = width / height

        // Also check if mouth corners are higher than center bottom
        // (upward curvature = smile)
        let leftCorner = points[0]
        let rightCorner = points[points.count / 2]
        let bottomCenter = points.min(by: { $0.y < $1.y }) ?? points[0]
        let cornerAvgY = (leftCorner.y + rightCorner.y) / 2
        let curvature = cornerAvgY - bottomCenter.y

        // Combine ratio and curvature into a smile probability
        var probability: Float = 0.0

        // Wide mouth relative to height suggests smile
        if ratio > 2.5 {
            probability += 0.3
        } else if ratio > 2.0 {
            probability += 0.15
        }

        // Upward curvature suggests smile
        if curvature > 0.02 {
            probability += 0.5
        } else if curvature > 0.01 {
            probability += 0.25
        }

        return min(probability, 1.0)
    }
}
