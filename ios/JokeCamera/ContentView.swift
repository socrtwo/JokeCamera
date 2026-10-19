import SwiftUI
import AVFoundation

struct ContentView: View {
    @StateObject private var cameraManager = CameraManager()
    @StateObject private var jokeManager = JokeManager()
    @StateObject private var speechManager = SpeechManager()
    @StateObject private var faceDetector = FaceDetector()
    @State private var showSettings = false
    @State private var isAutoMode = false
    @State private var isWaitingForReaction = false
    @State private var currentJoke: Joke?
    @State private var photoTaken = false
    @State private var showFlash = false
    @State private var statusText = "Ready"
    @State private var faceStatusText = ""
    @State private var showPunchline = false
    @State private var pendingWorkItems: [DispatchWorkItem] = []

    // Settings
    @AppStorage("manualMode") private var manualMode = false
    @AppStorage("detectionEnabled") private var detectionEnabled = true
    @AppStorage("timerMode") private var timerMode = false
    @AppStorage("timerDelay") private var timerDelay = 3.0
    @AppStorage("detectionMode") private var detectionMode = 3
    @AppStorage("punchlineDelay") private var punchlineDelay = 0.81
    @AppStorage("nextJokeWait") private var nextJokeWait = 2.5

    var body: some View {
        ZStack {
            // Camera Preview
            CameraPreviewView(session: cameraManager.session)
                .ignoresSafeArea()

            // Flash overlay
            if showFlash {
                Color.white
                    .ignoresSafeArea()
                    .transition(.opacity)
            }

            VStack {
                // Joke display
                VStack(spacing: 8) {
                    if let joke = currentJoke {
                        Text(joke.setup)
                            .font(.title3)
                            .fontWeight(.semibold)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(.black.opacity(0.5))
                            .foregroundColor(.white)
                            .cornerRadius(12)

                        if showPunchline {
                            Text(joke.punchline)
                                .font(.title3)
                                .fontWeight(.medium)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(.black.opacity(0.5))
                                .foregroundColor(.yellow)
                                .cornerRadius(12)
                        }
                    }
                }
                .padding(.top, 60)
                .padding(.horizontal)

                Spacer()

                // Status
                VStack(spacing: 4) {
                    Text(statusText)
                        .font(.callout)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                        .background(.black.opacity(0.6))
                        .foregroundColor(.white)
                        .cornerRadius(20)

                    if !faceStatusText.isEmpty {
                        Text(faceStatusText)
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                .padding(.bottom, 8)

                // Controls
                HStack(spacing: 20) {
                    // Start/Stop button
                    Button(action: toggleAutoMode) {
                        Text(isAutoMode ? "Stop" : "Start Auto")
                            .fontWeight(.semibold)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(isAutoMode ? Color.red : Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }

                    if manualMode {
                        Button(action: tellJoke) {
                            Text("Tell a Joke")
                                .fontWeight(.semibold)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 12)
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                        }
                    }

                    // Capture button
                    Button(action: takePhoto) {
                        Circle()
                            .fill(.white)
                            .frame(width: 50, height: 50)
                            .overlay(
                                Circle()
                                    .stroke(.white, lineWidth: 3)
                                    .frame(width: 60, height: 60)
                            )
                    }

                    // Switch camera
                    Button(action: { cameraManager.switchCamera() }) {
                        Image(systemName: "camera.rotate")
                            .font(.title2)
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(.white.opacity(0.15))
                            .clipShape(Circle())
                    }

                    // Settings
                    Button(action: { showSettings = true }) {
                        Image(systemName: "gear")
                            .font(.title2)
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(.white.opacity(0.15))
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 30)
            }
        }
        .onAppear {
            cameraManager.configure()
            if detectionEnabled {
                faceDetector.attachToCamera(cameraManager)
            }
            setupFaceDetectionCallbacks()
            updateStatus()
        }
        .sheet(isPresented: $showSettings) {
            SettingsView(jokeManager: jokeManager)
        }
    }

    // MARK: - Face Detection Callbacks

    private func setupFaceDetectionCallbacks() {
        faceDetector.onSmileDetected = {
            DispatchQueue.main.async {
                faceStatusText = "😊 Smile detected!"
                if isWaitingForReaction && !photoTaken {
                    statusText = "Smile detected - capturing!"
                    scheduleDelayed(0.1) { takePhoto() }
                }
            }
        }

        faceDetector.onLaughDetected = {
            DispatchQueue.main.async {
                faceStatusText = "😂 Laugh detected!"
                if isWaitingForReaction && !photoTaken {
                    statusText = "Laugh detected - capturing!"
                    scheduleDelayed(0.1) { takePhoto() }
                }
            }
        }

        faceDetector.onFaceDetected = { count in
            DispatchQueue.main.async {
                if !isWaitingForReaction {
                    faceStatusText = "👤 \(count) face\(count > 1 ? "s" : "") detected"
                }
            }
        }

        faceDetector.onNoFaceDetected = {
            DispatchQueue.main.async {
                faceStatusText = "No face detected"
            }
        }
    }

    // MARK: - Joke Telling

    private func tellJoke() {
        guard speechManager.isAvailable else {
            statusText = "Speech not available"
            return
        }

        let joke = jokeManager.getNextJoke()
        currentJoke = joke
        showPunchline = false
        photoTaken = false
        isWaitingForReaction = false
        faceDetector.resetDetectionState()
        cancelPendingWork()

        statusText = "Telling joke..."

        speechManager.tellJoke(joke: joke, punchlineDelay: punchlineDelay) {
            DispatchQueue.main.async {
                onJokeComplete()
            }
        }
    }

    private func onJokeComplete() {
        showPunchline = true

        if timerMode {
            statusText = "Taking photo in \(String(format: "%.1f", timerDelay))s..."
            scheduleDelayed(timerDelay) {
                if !photoTaken { takePhoto() }
            }
        } else if detectionEnabled {
            isWaitingForReaction = true
            faceDetector.allowNewDetection()
            statusText = "Waiting for smile/laugh..."

            scheduleDelayed(nextJokeWait) {
                if !photoTaken && isAutoMode {
                    statusText = "No reaction - telling another joke..."
                    isWaitingForReaction = false
                    scheduleDelayed(0.5) { tellJoke() }
                }
            }
        } else {
            statusText = "Joke told - capture manually"
            if isAutoMode {
                scheduleDelayed(nextJokeWait) {
                    if !photoTaken { tellJoke() }
                }
            }
        }
    }

    // MARK: - Photo Capture

    private func takePhoto() {
        guard !photoTaken else { return }
        photoTaken = true
        isWaitingForReaction = false
        cancelPendingWork()

        // Flash effect
        withAnimation(.easeOut(duration: 0.3)) {
            showFlash = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            withAnimation { showFlash = false }
        }

        cameraManager.capturePhoto { image in
            if let image = image {
                UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
                DispatchQueue.main.async {
                    statusText = "Photo captured!"
                }
            }

            DispatchQueue.main.async {
                if isAutoMode {
                    scheduleDelayed(2.0) {
                        photoTaken = false
                        tellJoke()
                    }
                } else {
                    scheduleDelayed(2.0) {
                        photoTaken = false
                        updateStatus()
                    }
                }
            }
        }
    }

    // MARK: - Auto Mode

    private func toggleAutoMode() {
        isAutoMode.toggle()

        if isAutoMode {
            statusText = "Auto mode - Starting..."
            scheduleDelayed(1.0) { tellJoke() }
        } else {
            isWaitingForReaction = false
            cancelPendingWork()
            speechManager.stop()
            updateStatus()
        }
    }

    // MARK: - Utility

    private func updateStatus() {
        let remaining = jokeManager.getRemainingCount()
        statusText = "Ready - \(remaining) jokes remaining"
    }

    private func scheduleDelayed(_ seconds: Double, action: @escaping () -> Void) {
        let item = DispatchWorkItem { action() }
        pendingWorkItems.append(item)
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: item)
    }

    private func cancelPendingWork() {
        pendingWorkItems.forEach { $0.cancel() }
        pendingWorkItems.removeAll()
    }
}

// MARK: - Camera Preview UIViewRepresentable

struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = view.bounds
        view.layer.addSublayer(previewLayer)

        // Store reference for layout updates
        view.tag = 100
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        if let layer = uiView.layer.sublayers?.first as? AVCaptureVideoPreviewLayer {
            layer.frame = uiView.bounds
        }
    }
}
