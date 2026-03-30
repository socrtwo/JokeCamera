/**
 * FaceDetector - Face and smile/laugh detection using face-api.js.
 * Mirrors the Android FaceAnalyzer behavior with configurable detection modes.
 */

const DetectionMode = {
    SMILE_ONLY: 0,
    LAUGH_ONLY: 1,
    SMILE_AND_LAUGH: 2,
    SMILE_OR_LAUGH: 3
};

class FaceDetector {
    constructor() {
        this.initialized = false;
        this.running = false;
        this.detectionMode = DetectionMode.SMILE_OR_LAUGH;
        this.hasTriggeredThisCycle = false;
        this.smileDetectedForAndMode = false;
        this.video = null;
        this.animFrameId = null;

        // Thresholds matching Android version
        this.SMILE_THRESHOLD = 0.4;
        this.LAUGH_THRESHOLD = 0.75;

        // Callbacks
        this.onSmileDetected = null;
        this.onLaughDetected = null;
        this.onFaceDetected = null;
        this.onNoFaceDetected = null;
    }

    async initialize() {
        if (typeof faceapi === 'undefined') {
            console.warn('face-api.js not loaded, face detection disabled');
            return false;
        }

        try {
            const MODEL_URL = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.12/model/';
            await Promise.all([
                faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
                faceapi.nets.faceExpressionNet.loadFromUri(MODEL_URL)
            ]);
            this.initialized = true;
            console.log('Face detection models loaded');
            return true;
        } catch (e) {
            console.error('Failed to load face detection models:', e);
            return false;
        }
    }

    setDetectionMode(mode) {
        this.detectionMode = mode;
        this.resetDetectionState();
    }

    resetDetectionState() {
        this.hasTriggeredThisCycle = false;
        this.smileDetectedForAndMode = false;
    }

    allowNewDetection() {
        this.hasTriggeredThisCycle = false;
    }

    start(videoElement) {
        if (!this.initialized) return;
        this.video = videoElement;
        this.running = true;
        this._detectLoop();
    }

    stop() {
        this.running = false;
        if (this.animFrameId) {
            cancelAnimationFrame(this.animFrameId);
            this.animFrameId = null;
        }
    }

    async _detectLoop() {
        if (!this.running || !this.video) return;

        try {
            const detections = await faceapi
                .detectAllFaces(this.video, new faceapi.TinyFaceDetectorOptions({
                    inputSize: 224,
                    scoreThreshold: 0.5
                }))
                .withFaceExpressions();

            this._processFaces(detections);
        } catch (e) {
            // Ignore transient detection errors
        }

        // Run at ~5fps for performance
        if (this.running) {
            this.animFrameId = setTimeout(() => {
                requestAnimationFrame(() => this._detectLoop());
            }, 200);
        }
    }

    _processFaces(detections) {
        if (!detections || detections.length === 0) {
            if (this.onNoFaceDetected) this.onNoFaceDetected();
            return;
        }

        if (this.onFaceDetected) this.onFaceDetected(detections.length);

        if (this.hasTriggeredThisCycle) return;

        for (const detection of detections) {
            const expressions = detection.expressions;
            // Use 'happy' expression as smile/laugh proxy
            const happyProb = expressions.happy || 0;

            const isSmiling = happyProb >= this.SMILE_THRESHOLD;
            const isLaughing = happyProb >= this.LAUGH_THRESHOLD;

            switch (this.detectionMode) {
                case DetectionMode.SMILE_ONLY:
                    if (isSmiling) {
                        this.hasTriggeredThisCycle = true;
                        if (this.onSmileDetected) this.onSmileDetected();
                        return;
                    }
                    break;

                case DetectionMode.LAUGH_ONLY:
                    if (isLaughing) {
                        this.hasTriggeredThisCycle = true;
                        if (this.onLaughDetected) this.onLaughDetected();
                        return;
                    }
                    break;

                case DetectionMode.SMILE_AND_LAUGH:
                    if (!this.smileDetectedForAndMode && isSmiling && !isLaughing) {
                        this.smileDetectedForAndMode = true;
                        if (this.onSmileDetected) this.onSmileDetected();
                    } else if (this.smileDetectedForAndMode && isLaughing) {
                        this.hasTriggeredThisCycle = true;
                        if (this.onLaughDetected) this.onLaughDetected();
                        return;
                    }
                    break;

                case DetectionMode.SMILE_OR_LAUGH:
                    if (isLaughing) {
                        this.hasTriggeredThisCycle = true;
                        if (this.onLaughDetected) this.onLaughDetected();
                        return;
                    } else if (isSmiling) {
                        this.hasTriggeredThisCycle = true;
                        if (this.onSmileDetected) this.onSmileDetected();
                        return;
                    }
                    break;
            }
        }
    }
}
