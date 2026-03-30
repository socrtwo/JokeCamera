/**
 * SpeechManager - Text-to-Speech wrapper using Web Speech API.
 * Mirrors the Android TTS behavior with setup/pause/punchline flow.
 */
class SpeechManager {
    constructor() {
        this.synth = window.speechSynthesis;
        this.initialized = 'speechSynthesis' in window;
        this.onPunchlineComplete = null;
    }

    isAvailable() {
        return this.initialized;
    }

    /**
     * Tells a joke with setup, pause, then punchline.
     * @param {Object} joke - {setup, punchline}
     * @param {number} punchlineDelay - seconds between setup and punchline
     * @param {Function} onPunchlineDone - callback when punchline finishes
     */
    tellJoke(joke, punchlineDelay, onPunchlineDone) {
        this.stop();
        this.onPunchlineComplete = onPunchlineDone;

        const setupUtterance = new SpeechSynthesisUtterance(joke.setup);
        setupUtterance.lang = 'en-US';
        setupUtterance.rate = 0.95;

        setupUtterance.onend = () => {
            setTimeout(() => {
                const punchlineUtterance = new SpeechSynthesisUtterance(joke.punchline);
                punchlineUtterance.lang = 'en-US';
                punchlineUtterance.rate = 0.95;

                punchlineUtterance.onend = () => {
                    if (this.onPunchlineComplete) {
                        this.onPunchlineComplete();
                    }
                };

                punchlineUtterance.onerror = () => {
                    if (this.onPunchlineComplete) {
                        this.onPunchlineComplete();
                    }
                };

                this.synth.speak(punchlineUtterance);
            }, punchlineDelay * 1000);
        };

        this.synth.speak(setupUtterance);
    }

    stop() {
        this.synth.cancel();
        this.onPunchlineComplete = null;
    }
}
