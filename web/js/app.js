/**
 * JokeCamera Web App - Main Application Controller
 * Coordinates camera, joke telling, face detection, and photo capture.
 */
(function () {
    'use strict';

    // Managers
    const jokeManager = new JokeManager();
    const speechManager = new SpeechManager();
    const faceDetector = new FaceDetector();
    const FX = (window.Effects || {});

    // DOM elements
    const splash = document.getElementById('splash');
    const splashStatus = document.getElementById('splash-status');
    const video = document.getElementById('video');
    const captureCanvas = document.getElementById('capture-canvas');
    const jokeCard = document.getElementById('joke-card');
    const jokeCategory = document.getElementById('joke-category');
    const jokeSetup = document.getElementById('joke-setup');
    const jokePunchline = document.getElementById('joke-punchline');
    const statusText = document.getElementById('status-text');
    const faceStatus = document.getElementById('face-status');
    const smileMeter = document.getElementById('smile-meter');
    const smileMeterFill = document.getElementById('smile-meter-fill');
    const statsCount = document.getElementById('stats-count');
    const btnStartStop = document.getElementById('btn-start-stop');
    const btnStartLabel = document.getElementById('btn-start-label');
    const btnStartEmoji = document.getElementById('btn-start-emoji');
    const btnTellJoke = document.getElementById('btn-tell-joke');
    const btnCapture = document.getElementById('btn-capture');
    const btnSwitchCamera = document.getElementById('btn-switch-camera');
    const btnSettings = document.getElementById('btn-settings');
    const btnCloseSettings = document.getElementById('btn-close-settings');
    const settingsPanel = document.getElementById('settings-panel');
    const flashOverlay = document.getElementById('flash-overlay');

    // State
    let isAutoMode = false;
    let isWaitingForReaction = false;
    let currentJoke = null;
    let photoTaken = false;
    let currentStream = null;
    let facingMode = 'user';
    let pendingTimers = [];
    let jokesDataVersion = 0;

    // Settings (defaults)
    let settings = {
        manualMode: false,
        detectionEnabled: true,
        timerMode: false,
        timerDelay: 3.0,
        detectionMode: DetectionMode.SMILE_OR_LAUGH,
        punchlineDelay: 0.81,
        nextJokeWait: 2.5,
        voiceRate: 1.0,
        voicePitch: 1.0,
        confettiEnabled: true,
        soundEnabled: true,
        autoSave: true
    };

    const CATEGORY_LABELS = {
        'general': '🙂 General',
        'programming': '💻 Programming',
        'knock-knock': '🚪 Knock-Knock',
        'dad': '👨 Dad'
    };

    // ---- Initialization ----

    async function init() {
        setSplashStatus('Loading settings…');
        loadSettings();
        setupEventListeners();

        setSplashStatus('Starting camera…');
        await startCamera();

        setSplashStatus('Loading face detection…');
        const faceLoaded = await faceDetector.initialize();

        if (faceLoaded) {
            setupFaceDetection();
            faceDetector.start(video);
            smileMeter.classList.add('visible');
        } else {
            setStatus('Face detection unavailable - timer or manual mode only', false);
            FX.Toast && FX.Toast.show('⚠️ Face detection offline', 'error');
        }

        // Start reloading full jokes list in background
        watchJokesData();

        updateStatus();
        hideSplash();
    }

    function setSplashStatus(text) {
        if (splashStatus) splashStatus.textContent = text;
    }

    function hideSplash() {
        setTimeout(() => splash && splash.classList.add('hidden'), 300);
    }

    // ---- Camera ----

    async function startCamera() {
        try {
            if (currentStream) {
                currentStream.getTracks().forEach(t => t.stop());
            }
            const constraints = {
                video: {
                    facingMode: facingMode,
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                },
                audio: false
            };
            currentStream = await navigator.mediaDevices.getUserMedia(constraints);
            video.srcObject = currentStream;
            await video.play();
            video.style.transform = facingMode === 'user' ? 'scaleX(-1)' : 'scaleX(1)';
        } catch (e) {
            console.error('Camera error:', e);
            setStatus('Camera access denied or unavailable', false);
            FX.Toast && FX.Toast.show('📷 Camera unavailable', 'error');
        }
    }

    function switchCamera() {
        facingMode = facingMode === 'user' ? 'environment' : 'user';
        startCamera();
        FX.Toast && FX.Toast.show(`Switched to ${facingMode === 'user' ? 'front' : 'back'} camera`, 'info');
    }

    // ---- Face Detection Setup ----

    function setupFaceDetection() {
        faceDetector.onSmileDetected = () => {
            faceStatus.textContent = '😊 Smile detected!';
            if (isWaitingForReaction && !photoTaken) {
                setStatus('😊 Smile captured!', true);
                FX.ReactionLayer && FX.ReactionLayer.spawn('😊', 3);
                setTimeout(() => takePhoto('smile'), 100);
            }
        };

        faceDetector.onLaughDetected = () => {
            faceStatus.textContent = '😂 Laugh detected!';
            if (isWaitingForReaction && !photoTaken) {
                setStatus('😂 LOL captured!', true);
                FX.ReactionLayer && FX.ReactionLayer.spawn('😂', 5);
                if (settings.confettiEnabled && FX.Confetti) FX.Confetti.burst(100);
                if (settings.soundEnabled && FX.Sound) FX.Sound.laugh();
                setTimeout(() => takePhoto('laugh'), 100);
            }
        };

        faceDetector.onFaceDetected = (count) => {
            if (!isWaitingForReaction) {
                faceStatus.textContent = `👤 ${count} face${count > 1 ? 's' : ''} detected`;
            }
        };

        faceDetector.onNoFaceDetected = () => {
            faceStatus.textContent = '👁 Looking for a face…';
        };

        faceDetector.onHappinessUpdate = (happiness) => {
            if (smileMeterFill) {
                smileMeterFill.style.width = Math.min(100, happiness * 100) + '%';
            }
        };
    }

    // ---- Joke Telling ----

    function tellJoke() {
        if (!speechManager.isAvailable()) {
            setStatus('Speech not available in this browser', false);
            return;
        }

        currentJoke = jokeManager.getNextJoke();
        photoTaken = false;
        isWaitingForReaction = false;
        faceDetector.resetDetectionState();
        clearPendingTimers();

        // Update UI
        jokeCard.classList.remove('visible');
        jokePunchline.classList.remove('visible');

        // Brief delay for exit animation, then display setup
        setTimeout(() => {
            jokeCategory.textContent = CATEGORY_LABELS[currentJoke.type] || currentJoke.type;
            jokeSetup.textContent = currentJoke.setup;
            jokePunchline.textContent = '';
            jokeCard.classList.add('visible');
            setStatus('🎤 Telling joke…', true);
        }, 120);

        speechManager.setRate(0.95 * settings.voiceRate);
        speechManager.setPitch(settings.voicePitch);

        speechManager.tellJoke(
            currentJoke,
            settings.punchlineDelay,
            () => onJokeComplete(),
            () => {
                // Setup finished - now reveal punchline text along with the spoken punchline
                if (currentJoke) {
                    jokePunchline.textContent = currentJoke.punchline;
                    jokePunchline.classList.add('visible');
                }
            }
        );
        updateStatsPill();
    }

    function onJokeComplete() {
        if (!currentJoke) return;

        // Ensure punchline is shown (in case setup callback didn't fire)
        if (!jokePunchline.classList.contains('visible')) {
            jokePunchline.textContent = currentJoke.punchline;
            jokePunchline.classList.add('visible');
        }

        if (settings.timerMode) {
            setStatus(`📸 Taking photo in ${settings.timerDelay.toFixed(1)}s…`, true);
            const tid = setTimeout(() => {
                if (!photoTaken) takePhoto('timer');
            }, settings.timerDelay * 1000);
            pendingTimers.push(tid);
        } else if (settings.detectionEnabled && faceDetector.initialized) {
            isWaitingForReaction = true;
            faceDetector.allowNewDetection();
            setStatus('👀 Waiting for smile or laugh…', true);

            const tid = setTimeout(() => {
                if (!photoTaken && isAutoMode) {
                    setStatus('🎭 No reaction - telling another joke…', true);
                    isWaitingForReaction = false;
                    const tid2 = setTimeout(() => tellJoke(), 500);
                    pendingTimers.push(tid2);
                }
            }, settings.nextJokeWait * 1000);
            pendingTimers.push(tid);
        } else {
            setStatus('✅ Joke told - capture manually', false);
            isWaitingForReaction = false;

            if (isAutoMode) {
                const tid = setTimeout(() => {
                    if (!photoTaken) tellJoke();
                }, settings.nextJokeWait * 1000);
                pendingTimers.push(tid);
            }
        }
    }

    // ---- Photo Capture ----

    function takePhoto(reason) {
        if (photoTaken) return;
        photoTaken = true;
        isWaitingForReaction = false;
        clearPendingTimers();

        // Flash effect
        flashOverlay.classList.remove('hidden');
        flashOverlay.classList.add('flash');
        setTimeout(() => {
            flashOverlay.classList.remove('flash');
            flashOverlay.classList.add('hidden');
        }, 320);

        // Capture-button feedback
        btnCapture.classList.add('flashing');
        setTimeout(() => btnCapture.classList.remove('flashing'), 420);

        // Shutter sound
        if (settings.soundEnabled && FX.Sound) FX.Sound.shutter();

        if (!video.videoWidth || !video.videoHeight) {
            setStatus('⚠️ Camera not ready', false);
            photoTaken = false;
            return;
        }

        captureCanvas.width = video.videoWidth;
        captureCanvas.height = video.videoHeight;
        const ctx = captureCanvas.getContext('2d');

        ctx.save();
        if (facingMode === 'user') {
            ctx.translate(captureCanvas.width, 0);
            ctx.scale(-1, 1);
        }
        ctx.drawImage(video, 0, 0);
        ctx.restore();

        captureCanvas.toBlob((blob) => {
            if (!blob) {
                setStatus('⚠️ Capture failed', false);
                photoTaken = false;
                return;
            }
            const url = URL.createObjectURL(blob);
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const filename = `JokeCamera_${timestamp}.jpg`;

            // Add to gallery
            if (FX.Gallery) FX.Gallery.add(blob, url);

            // Auto-save (download)
            if (settings.autoSave) {
                const a = document.createElement('a');
                a.href = url;
                a.download = filename;
                a.click();
            }

            setStatus('📸 Photo captured!', true);
            FX.Toast && FX.Toast.show('📸 Got the shot!', 'success');

            if (isAutoMode) {
                const tid = setTimeout(() => {
                    photoTaken = false;
                    tellJoke();
                }, 2000);
                pendingTimers.push(tid);
            } else {
                const tid = setTimeout(() => {
                    updateStatus();
                    photoTaken = false;
                }, 2200);
                pendingTimers.push(tid);
            }
        }, 'image/jpeg', 0.92);
    }

    // ---- Auto Mode ----

    function toggleAutoMode() {
        isAutoMode = !isAutoMode;

        if (isAutoMode) {
            btnStartLabel.textContent = 'Stop';
            btnStartEmoji.textContent = '⏹';
            btnStartStop.classList.add('active');
            btnStartStop.title = 'Stop auto mode';
            btnTellJoke.disabled = true;
            btnCapture.disabled = true;
            setStatus('🚀 Auto mode active…', true);
            if (settings.soundEnabled && FX.Sound) FX.Sound.start();
            const tid = setTimeout(() => tellJoke(), 800);
            pendingTimers.push(tid);
        } else {
            btnStartLabel.textContent = 'Start';
            btnStartEmoji.textContent = '▶';
            btnStartStop.classList.remove('active');
            btnStartStop.title = 'Start auto mode';
            btnTellJoke.disabled = false;
            btnCapture.disabled = false;
            isWaitingForReaction = false;
            clearPendingTimers();
            speechManager.stop();
            jokeCard.classList.remove('visible');
            if (settings.soundEnabled && FX.Sound) FX.Sound.stop();
            updateStatus();
        }
    }

    // ---- Settings ----

    function loadSettings() {
        const saved = localStorage.getItem('jokeCameraSettings');
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                Object.assign(settings, parsed);
            } catch (e) { /* use defaults */ }
        }
        applySettingsToUI();
        faceDetector.setDetectionMode(settings.detectionMode);
        if (FX.Sound) FX.Sound.setEnabled(settings.soundEnabled);
    }

    function saveSettings() {
        try {
            localStorage.setItem('jokeCameraSettings', JSON.stringify(settings));
        } catch (e) { /* ignore quota */ }
    }

    function applySettingsToUI() {
        const setChecked = (id, val) => { const el = document.getElementById(id); if (el) el.checked = !!val; };
        const setVal = (id, val) => { const el = document.getElementById(id); if (el) el.value = val; };
        const setText = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };

        setChecked('opt-manual-mode', settings.manualMode);
        setChecked('opt-detection', settings.detectionEnabled);
        setChecked('opt-timer-mode', settings.timerMode);
        setChecked('opt-confetti', settings.confettiEnabled);
        setChecked('opt-sound-fx', settings.soundEnabled);
        setChecked('opt-auto-save', settings.autoSave);

        setVal('slider-timer-delay', settings.timerDelay);
        setText('timer-delay-val', settings.timerDelay.toFixed(1));
        setVal('slider-punchline-delay', settings.punchlineDelay);
        setText('punchline-delay-val', settings.punchlineDelay.toFixed(2));
        setVal('slider-next-joke-wait', settings.nextJokeWait);
        setText('next-joke-wait-val', settings.nextJokeWait.toFixed(1));
        setVal('slider-voice-rate', settings.voiceRate);
        setText('voice-rate-val', settings.voiceRate.toFixed(2));
        setVal('slider-voice-pitch', settings.voicePitch);
        setText('voice-pitch-val', settings.voicePitch.toFixed(2));

        document.querySelectorAll('input[name="detection-mode"]').forEach(r => {
            r.checked = parseInt(r.value) === settings.detectionMode;
        });

        btnTellJoke.style.display = settings.manualMode ? '' : 'none';

        const cats = jokeManager.enabledCategories;
        setChecked('cat-general', cats.includes('general'));
        setChecked('cat-programming', cats.includes('programming'));
        setChecked('cat-knock-knock', cats.includes('knock-knock'));
        setChecked('cat-dad', cats.includes('dad'));

        updateJokeStats();
    }

    function readSettingsFromUI() {
        const getChecked = (id) => { const el = document.getElementById(id); return el ? el.checked : false; };
        const getFloat = (id) => { const el = document.getElementById(id); return el ? parseFloat(el.value) : 0; };

        settings.manualMode = getChecked('opt-manual-mode');
        settings.detectionEnabled = getChecked('opt-detection');
        settings.timerMode = getChecked('opt-timer-mode');
        settings.confettiEnabled = getChecked('opt-confetti');
        settings.soundEnabled = getChecked('opt-sound-fx');
        settings.autoSave = getChecked('opt-auto-save');
        settings.timerDelay = getFloat('slider-timer-delay');
        settings.punchlineDelay = getFloat('slider-punchline-delay');
        settings.nextJokeWait = getFloat('slider-next-joke-wait');
        settings.voiceRate = getFloat('slider-voice-rate');
        settings.voicePitch = getFloat('slider-voice-pitch');

        const modeRadio = document.querySelector('input[name="detection-mode"]:checked');
        settings.detectionMode = modeRadio ? parseInt(modeRadio.value) : DetectionMode.SMILE_OR_LAUGH;

        const cats = [];
        if (getChecked('cat-general')) cats.push('general');
        if (getChecked('cat-programming')) cats.push('programming');
        if (getChecked('cat-knock-knock')) cats.push('knock-knock');
        if (getChecked('cat-dad')) cats.push('dad');
        if (cats.length === 0) {
            cats.push('general');
            document.getElementById('cat-general').checked = true;
        }
        jokeManager.setEnabledCategories(cats);

        btnTellJoke.style.display = settings.manualMode ? '' : 'none';
        faceDetector.setDetectionMode(settings.detectionMode);
        if (FX.Sound) FX.Sound.setEnabled(settings.soundEnabled);

        saveSettings();
        updateJokeStats();
        updateStatsPill();
    }

    function updateJokeStats() {
        const remaining = jokeManager.getRemainingCount();
        const total = jokeManager.getTotalCount();
        const told = total - remaining;
        const el = document.getElementById('joke-stats');
        if (el) el.textContent = `Told ${told} of ${total} · ${remaining} remaining`;
    }

    function updateStatsPill() {
        if (!statsCount) return;
        const remaining = jokeManager.getRemainingCount();
        statsCount.textContent = remaining;
    }

    // Watch for async joke data load
    function watchJokesData() {
        const checkAndRefresh = () => {
            const total = jokeManager.getTotalCount();
            if (total !== jokesDataVersion) {
                jokesDataVersion = total;
                updateJokeStats();
                updateStatsPill();
            }
        };
        setInterval(checkAndRefresh, 1000);
    }

    // ---- Event Listeners ----

    function setupEventListeners() {
        btnStartStop.addEventListener('click', toggleAutoMode);
        btnTellJoke.addEventListener('click', () => {
            if (speechManager.isAvailable()) tellJoke();
        });
        btnCapture.addEventListener('click', () => takePhoto('manual'));
        btnSwitchCamera.addEventListener('click', switchCamera);

        btnSettings.addEventListener('click', () => {
            applySettingsToUI();
            settingsPanel.classList.remove('hidden');
        });
        btnCloseSettings.addEventListener('click', () => {
            readSettingsFromUI();
            settingsPanel.classList.add('hidden');
            updateStatus();
        });

        // Close settings on backdrop click
        const backdrop = settingsPanel.querySelector('.settings-backdrop');
        if (backdrop) backdrop.addEventListener('click', () => {
            readSettingsFromUI();
            settingsPanel.classList.add('hidden');
            updateStatus();
        });

        // Settings change handlers
        ['opt-manual-mode', 'opt-detection', 'opt-timer-mode', 'opt-confetti', 'opt-sound-fx', 'opt-auto-save'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('change', readSettingsFromUI);
        });
        document.querySelectorAll('input[name="detection-mode"]').forEach(r => {
            r.addEventListener('change', readSettingsFromUI);
        });

        // Sliders with live label update
        function wireSlider(sliderId, labelId, formatter, onChange) {
            const slider = document.getElementById(sliderId);
            const label = document.getElementById(labelId);
            if (!slider) return;
            slider.addEventListener('input', (e) => {
                const val = parseFloat(e.target.value);
                if (label) label.textContent = formatter(val);
                if (onChange) onChange(val);
                readSettingsFromUI();
            });
        }
        wireSlider('slider-timer-delay', 'timer-delay-val', v => v.toFixed(1));
        wireSlider('slider-punchline-delay', 'punchline-delay-val', v => v.toFixed(2));
        wireSlider('slider-next-joke-wait', 'next-joke-wait-val', v => v.toFixed(1));
        wireSlider('slider-voice-rate', 'voice-rate-val', v => v.toFixed(2));
        wireSlider('slider-voice-pitch', 'voice-pitch-val', v => v.toFixed(2));

        // Category checkboxes
        ['cat-general', 'cat-programming', 'cat-knock-knock', 'cat-dad'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.addEventListener('change', readSettingsFromUI);
        });

        // Joke management
        const btnReset = document.getElementById('btn-reset-jokes');
        if (btnReset) btnReset.addEventListener('click', () => {
            jokeManager.resetToldJokes();
            updateJokeStats();
            updateStatsPill();
            updateStatus();
            FX.Toast && FX.Toast.show('🔄 Joke history reset', 'success');
        });

        const btnExport = document.getElementById('btn-export-jokes');
        if (btnExport) btnExport.addEventListener('click', () => {
            const json = jokeManager.exportJokesAsJson();
            const blob = new Blob([json], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'jokes_export.json';
            a.click();
            URL.revokeObjectURL(url);
            FX.Toast && FX.Toast.show('⬇️ Jokes exported', 'success');
        });

        const fileImport = document.getElementById('file-import-jokes');
        if (fileImport) fileImport.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = (ev) => {
                try {
                    const count = jokeManager.importJokes(ev.target.result, false);
                    FX.Toast && FX.Toast.show(`✅ Imported ${count} jokes!`, 'success');
                    updateJokeStats();
                    updateStatsPill();
                } catch (err) {
                    FX.Toast && FX.Toast.show('❌ Import failed: ' + err.message, 'error');
                }
            };
            reader.readAsText(file);
            e.target.value = '';
        });

        const btnClearPhotos = document.getElementById('btn-clear-photos');
        if (btnClearPhotos) btnClearPhotos.addEventListener('click', () => {
            if (FX.Gallery) FX.Gallery.clear();
            FX.Toast && FX.Toast.show('🗑️ Gallery cleared', 'success');
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            if (!settingsPanel.classList.contains('hidden')) {
                if (e.key === 'Escape') {
                    readSettingsFromUI();
                    settingsPanel.classList.add('hidden');
                }
                return;
            }
            switch (e.key) {
                case ' ':
                case 'Spacebar':
                    e.preventDefault();
                    takePhoto('keyboard');
                    break;
                case 'j':
                case 'J':
                    if (speechManager.isAvailable()) tellJoke();
                    break;
                case 's':
                case 'S':
                    toggleAutoMode();
                    break;
                case 'c':
                case 'C':
                    switchCamera();
                    break;
                case ',':
                    applySettingsToUI();
                    settingsPanel.classList.remove('hidden');
                    break;
            }
        });

        // Resume AudioContext on first user gesture (browser autoplay policy)
        const resumeAudio = () => {
            if (FX.Sound) FX.Sound._getCtx && FX.Sound._getCtx();
        };
        document.addEventListener('click', resumeAudio, { once: true });
        document.addEventListener('touchstart', resumeAudio, { once: true });
    }

    // ---- Utility ----

    function setStatus(text, active) {
        statusText.textContent = text;
        if (active) statusText.classList.add('active');
        else statusText.classList.remove('active');
    }

    function updateStatus() {
        const remaining = jokeManager.getRemainingCount();
        setStatus(`Ready · ${remaining} jokes remaining · Press ▶ to start`, false);
        updateStatsPill();
    }

    function clearPendingTimers() {
        pendingTimers.forEach(t => clearTimeout(t));
        pendingTimers = [];
    }

    // ---- Start ----
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
