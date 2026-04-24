/**
 * SpeechManager - Text-to-Speech wrapper using Web Speech API.
 * Mirrors the Android TTS behavior with setup/pause/punchline flow.
 */
class SpeechManager {
    constructor() {
        this.synth = window.speechSynthesis;
        this.initialized = 'speechSynthesis' in window;
        this.onPunchlineComplete = null;
        this.rate = 0.95;
        this.pitch = 1.0;
        this._timer = null;
    }

    isAvailable() {
        return this.initialized;
    }

    setRate(rate) {
        const r = parseFloat(rate);
        if (!isNaN(r) && r >= 0.1 && r <= 5) this.rate = r;
    }

    setPitch(pitch) {
        const p = parseFloat(pitch);
        if (!isNaN(p) && p >= 0 && p <= 2) this.pitch = p;
    }

    /**
     * Tells a joke with setup, pause, then punchline.
     * @param {Object} joke - {setup, punchline}
     * @param {number} punchlineDelay - seconds between setup and punchline
     * @param {Function} onPunchlineDone - callback when punchline finishes
     * @param {Function} onSetupDone - optional callback when setup finishes (before delay)
     */
    tellJoke(joke, punchlineDelay, onPunchlineDone, onSetupDone) {
        this.stop();
        this.onPunchlineComplete = onPunchlineDone;

        const setupUtterance = new SpeechSynthesisUtterance(joke.setup);
        setupUtterance.lang = 'en-US';
        setupUtterance.rate = this.rate;
        setupUtterance.pitch = this.pitch;

        const speakPunchline = () => {
            if (typeof onSetupDone === 'function') {
                try { onSetupDone(); } catch (e) { /* ignore */ }
            }
            this._timer = setTimeout(() => {
                const punchlineUtterance = new SpeechSynthesisUtterance(joke.punchline);
                punchlineUtterance.lang = 'en-US';
                punchlineUtterance.rate = this.rate;
                punchlineUtterance.pitch = this.pitch;

                punchlineUtterance.onend = () => {
                    if (this.onPunchlineComplete) this.onPunchlineComplete();
                };
                punchlineUtterance.onerror = () => {
                    if (this.onPunchlineComplete) this.onPunchlineComplete();
                };
                this.synth.speak(punchlineUtterance);
            }, punchlineDelay * 1000);
        };

        setupUtterance.onend = speakPunchline;
        setupUtterance.onerror = speakPunchline;

        this.synth.speak(setupUtterance);
    }

    stop() {
        try { this.synth.cancel(); } catch (e) { /* ignore */ }
        if (this._timer) {
            clearTimeout(this._timer);
            this._timer = null;
        }
        this.onPunchlineComplete = null;
    }
}
