import AVFoundation

class SpeechManager: ObservableObject {
    private let synthesizer = AVSpeechSynthesizer()
    @Published var isAvailable = true

    private var onPunchlineComplete: (() -> Void)?
    private var pendingPunchline: String?
    private var punchlineDelay: Double = 0.81
    private let delegate = SpeechDelegate()

    init() {
        synthesizer.delegate = delegate
    }

    func tellJoke(joke: Joke, punchlineDelay: Double, onComplete: @escaping () -> Void) {
        stop()
        self.onPunchlineComplete = onComplete
        self.pendingPunchline = joke.punchline
        self.punchlineDelay = punchlineDelay

        delegate.onSetupComplete = { [weak self] in
            guard let self = self, let punchline = self.pendingPunchline else { return }

            DispatchQueue.main.asyncAfter(deadline: .now() + self.punchlineDelay) {
                let utterance = AVSpeechUtterance(string: punchline)
                utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
                utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95

                self.delegate.onPunchlineComplete = { [weak self] in
                    self?.onPunchlineComplete?()
                }

                self.synthesizer.speak(utterance)
            }
        }

        let setupUtterance = AVSpeechUtterance(string: joke.setup)
        setupUtterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        setupUtterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95

        delegate.isSetupUtterance = true
        synthesizer.speak(setupUtterance)
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        onPunchlineComplete = nil
        pendingPunchline = nil
        delegate.onSetupComplete = nil
        delegate.onPunchlineComplete = nil
    }
}

private class SpeechDelegate: NSObject, AVSpeechSynthesizerDelegate {
    var isSetupUtterance = true
    var onSetupComplete: (() -> Void)?
    var onPunchlineComplete: (() -> Void)?

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        if isSetupUtterance {
            isSetupUtterance = false
            onSetupComplete?()
        } else {
            onPunchlineComplete?()
        }
    }
}
