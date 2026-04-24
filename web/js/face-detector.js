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
        this.loopTimer = null;

        // Thresholds matching Android version
        this.SMILE_THRESHOLD = 0.4;
        this.LAUGH_THRESHOLD = 0.75;

        // Live happiness for meter (smoothed)
        this.currentHappiness = 0;

        // Callbacks
        this.onSmileDetected = null;
        this.onLaughDetected = null;
        this.onFaceDetected = null;
        this.onNoFaceDetected = null;
        this.onHappinessUpdate = null; // (value 0-1, faceCount)
    }

    async initialize() {
        if (typeof faceapi === 'undefined') {
            console.warn('face-api.js not loaded, face detection disabled');
            return false;
        }

        const MODEL_URLS = [
            'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.12/model/',
            'https://cdn.jsdelivr.net/gh/justadudewhohacks/face-api.js@master/weights/'
        ];

        for (const url of MODEL_URLS) {
            try {
                await Promise.all([
                    faceapi.nets.tinyFaceDetector.loadFromUri(url),
                    faceapi.nets.faceExpressionNet.loadFromUri(url)
                ]);
                this.initialized = true;
                console.log('Face detection models loaded from', url);
                return true;
            } catch (e) {
                console.warn('Failed to load models from', url, e);
            }
        }
        return false;
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
        if (this.loopTimer) {
            clearTimeout(this.loopTimer);
            this.loopTimer = null;
        }
    }

    async _detectLoop() {
        if (!this.running || !this.video) return;

        // Only run detection when video is ready to avoid errors
        if (this.video.readyState >= 2 && this.video.videoWidth > 0) {
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
        }

        // Run at ~5fps for performance
        if (this.running) {
            this.loopTimer = setTimeout(() => {
                if (this.running) this._detectLoop();
            }, 200);
        }
    }

    _processFaces(detections) {
        if (!detections || detections.length === 0) {
            this.currentHappiness = Math.max(0, this.currentHappiness - 0.1);
            if (this.onHappinessUpdate) this.onHappinessUpdate(this.currentHappiness, 0);
            if (this.onNoFaceDetected) this.onNoFaceDetected();
            return;
        }

        // Find the happiest face for meter
        let maxHappy = 0;
        for (const d of detections) {
            const h = (d.expressions && d.expressions.happy) || 0;
            if (h > maxHappy) maxHappy = h;
        }
        // Smooth (exponential moving average)
        this.currentHappiness = this.currentHappiness * 0.5 + maxHappy * 0.5;
        if (this.onHappinessUpdate) this.onHappinessUpdate(this.currentHappiness, detections.length);

        if (this.onFaceDetected) this.onFaceDetected(detections.length);

        if (this.hasTriggeredThisCycle) return;

        for (const detection of detections) {
            const expressions = detection.expressions;
            const happyProb = (expressions && expressions.happy) || 0;

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
