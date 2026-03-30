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

    // DOM elements
    const video = document.getElementById('video');
    const captureCanvas = document.getElementById('capture-canvas');
    const jokeSetup = document.getElementById('joke-setup');
    const jokePunchline = document.getElementById('joke-punchline');
    const statusText = document.getElementById('status-text');
    const faceStatus = document.getElementById('face-status');
    const btnStartStop = document.getElementById('btn-start-stop');
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
    let facingMode = 'user'; // 'user' = front, 'environment' = back
    let pendingTimers = [];

    // Settings (defaults)
    let settings = {
        manualMode: false,
        detectionEnabled: true,
        timerMode: false,
        timerDelay: 3.0,
        detectionMode: DetectionMode.SMILE_OR_LAUGH,
        punchlineDelay: 0.81,
        nextJokeWait: 2.5
    };

    // ---- Initialization ----

    async function init() {
        statusText.textContent = 'Initializing...';
        loadSettings();
        setupEventListeners();
        await startCamera();

        statusText.textContent = 'Loading face detection...';
        const faceLoaded = await faceDetector.initialize();

        if (faceLoaded) {
            setupFaceDetection();
            faceDetector.start(video);
        } else {
            statusText.textContent = 'Face detection unavailable - using timer/manual mode';
        }

        updateStatus();
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

            // Set mirror for front camera
            video.style.transform = facingMode === 'user' ? 'scaleX(-1)' : 'scaleX(1)';
        } catch (e) {
            console.error('Camera error:', e);
            statusText.textContent = 'Camera access denied or unavailable';
        }
    }

    function switchCamera() {
        facingMode = facingMode === 'user' ? 'environment' : 'user';
        startCamera();
    }

    // ---- Face Detection Setup ----

    function setupFaceDetection() {
        faceDetector.onSmileDetected = () => {
            faceStatus.textContent = '\u{1F60A} Smile detected!';
            if (isWaitingForReaction && !photoTaken) {
                statusText.textContent = 'Smile detected - capturing!';
                setTimeout(() => takePhoto(), 100);
            }
        };

        faceDetector.onLaughDetected = () => {
            faceStatus.textContent = '\u{1F602} Laugh detected!';
            if (isWaitingForReaction && !photoTaken) {
                statusText.textContent = 'Laugh detected - capturing!';
                setTimeout(() => takePhoto(), 100);
            }
        };

        faceDetector.onFaceDetected = (count) => {
            if (!isWaitingForReaction) {
                faceStatus.textContent = `\u{1F464} ${count} face${count > 1 ? 's' : ''} detected`;
            }
        };

        faceDetector.onNoFaceDetected = () => {
            faceStatus.textContent = 'No face detected';
        };
    }

    // ---- Joke Telling ----

    function tellJoke() {
        if (!speechManager.isAvailable()) {
            statusText.textContent = 'Speech not available in this browser';
            return;
        }

        currentJoke = jokeManager.getNextJoke();
        photoTaken = false;
        isWaitingForReaction = false;
        faceDetector.resetDetectionState();
        clearPendingTimers();

        // Display setup
        jokeSetup.textContent = currentJoke.setup;
        jokePunchline.textContent = '';
        statusText.textContent = 'Telling joke...';

        // Speak joke
        speechManager.tellJoke(currentJoke, settings.punchlineDelay, () => {
            onJokeComplete();
        });
    }

    function onJokeComplete() {
        if (!currentJoke) return;

        jokePunchline.textContent = currentJoke.punchline;

        if (settings.timerMode) {
            statusText.textContent = `Taking photo in ${settings.timerDelay}s...`;
            const tid = setTimeout(() => {
                if (!photoTaken) takePhoto();
            }, settings.timerDelay * 1000);
            pendingTimers.push(tid);
        } else if (settings.detectionEnabled && faceDetector.initialized) {
            isWaitingForReaction = true;
            faceDetector.allowNewDetection();
            statusText.textContent = 'Waiting for smile/laugh...';

            const tid = setTimeout(() => {
                if (!photoTaken && isAutoMode) {
                    statusText.textContent = 'No reaction - telling another joke...';
                    isWaitingForReaction = false;
                    const tid2 = setTimeout(() => tellJoke(), 500);
                    pendingTimers.push(tid2);
                }
            }, settings.nextJokeWait * 1000);
            pendingTimers.push(tid);
        } else {
            statusText.textContent = 'Joke told - capture manually';
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

    function takePhoto() {
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
        }, 300);

        // Capture frame from video
        captureCanvas.width = video.videoWidth;
        captureCanvas.height = video.videoHeight;
        const ctx = captureCanvas.getContext('2d');

        // Mirror if front camera
        if (facingMode === 'user') {
            ctx.translate(captureCanvas.width, 0);
            ctx.scale(-1, 1);
        }
        ctx.drawImage(video, 0, 0);

        // Download the photo
        captureCanvas.toBlob((blob) => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            a.download = `JokeCamera_${timestamp}.jpg`;
            a.click();
            URL.revokeObjectURL(url);

            statusText.textContent = 'Photo captured!';

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
                }, 2000);
                pendingTimers.push(tid);
            }
        }, 'image/jpeg', 0.92);
    }

    // ---- Auto Mode ----

    function toggleAutoMode() {
        isAutoMode = !isAutoMode;

        if (isAutoMode) {
            btnStartStop.textContent = 'Stop';
            btnStartStop.classList.add('active');
            btnTellJoke.disabled = true;
            btnCapture.disabled = true;
            statusText.textContent = 'Auto mode - Starting...';
            const tid = setTimeout(() => tellJoke(), 1000);
            pendingTimers.push(tid);
        } else {
            btnStartStop.textContent = 'Start Auto';
            btnStartStop.classList.remove('active');
            btnTellJoke.disabled = false;
            btnCapture.disabled = false;
            isWaitingForReaction = false;
            clearPendingTimers();
            speechManager.stop();
            updateStatus();
        }
    }

    // ---- Settings ----

    function loadSettings() {
        const saved = localStorage.getItem('jokeCameraSettings');
        if (saved) {
            try {
                Object.assign(settings, JSON.parse(saved));
            } catch (e) { /* use defaults */ }
        }
        applySettingsToUI();
    }

    function saveSettings() {
        localStorage.setItem('jokeCameraSettings', JSON.stringify(settings));
    }

    function applySettingsToUI() {
        document.getElementById('opt-manual-mode').checked = settings.manualMode;
        document.getElementById('opt-detection').checked = settings.detectionEnabled;
        document.getElementById('opt-timer-mode').checked = settings.timerMode;
        document.getElementById('slider-timer-delay').value = settings.timerDelay;
        document.getElementById('timer-delay-val').textContent = settings.timerDelay.toFixed(1);
        document.getElementById('slider-punchline-delay').value = settings.punchlineDelay;
        document.getElementById('punchline-delay-val').textContent = settings.punchlineDelay.toFixed(2);
        document.getElementById('slider-next-joke-wait').value = settings.nextJokeWait;
        document.getElementById('next-joke-wait-val').textContent = settings.nextJokeWait.toFixed(1);

        const modeRadios = document.querySelectorAll('input[name="detection-mode"]');
        modeRadios.forEach(r => {
            r.checked = parseInt(r.value) === settings.detectionMode;
        });

        btnTellJoke.style.display = settings.manualMode ? '' : 'none';

        // Category checkboxes
        const cats = jokeManager.enabledCategories;
        document.getElementById('cat-general').checked = cats.includes('general');
        document.getElementById('cat-programming').checked = cats.includes('programming');
        document.getElementById('cat-knock-knock').checked = cats.includes('knock-knock');
        document.getElementById('cat-dad').checked = cats.includes('dad');

        updateJokeStats();
    }

    function readSettingsFromUI() {
        settings.manualMode = document.getElementById('opt-manual-mode').checked;
        settings.detectionEnabled = document.getElementById('opt-detection').checked;
        settings.timerMode = document.getElementById('opt-timer-mode').checked;
        settings.timerDelay = parseFloat(document.getElementById('slider-timer-delay').value);
        settings.punchlineDelay = parseFloat(document.getElementById('slider-punchline-delay').value);
        settings.nextJokeWait = parseFloat(document.getElementById('slider-next-joke-wait').value);

        const modeRadio = document.querySelector('input[name="detection-mode"]:checked');
        settings.detectionMode = modeRadio ? parseInt(modeRadio.value) : DetectionMode.SMILE_OR_LAUGH;

        // Categories
        const cats = [];
        if (document.getElementById('cat-general').checked) cats.push('general');
        if (document.getElementById('cat-programming').checked) cats.push('programming');
        if (document.getElementById('cat-knock-knock').checked) cats.push('knock-knock');
        if (document.getElementById('cat-dad').checked) cats.push('dad');
        if (cats.length === 0) {
            cats.push('general');
            document.getElementById('cat-general').checked = true;
        }
        jokeManager.setEnabledCategories(cats);

        btnTellJoke.style.display = settings.manualMode ? '' : 'none';
        faceDetector.setDetectionMode(settings.detectionMode);

        saveSettings();
        updateJokeStats();
    }

    function updateJokeStats() {
        const remaining = jokeManager.getRemainingCount();
        const total = jokeManager.getTotalCount();
        const told = total - remaining;
        document.getElementById('joke-stats').textContent =
            `Jokes told: ${told} / ${total} (${remaining} remaining)`;
    }

    // ---- Event Listeners ----

    function setupEventListeners() {
        btnStartStop.addEventListener('click', toggleAutoMode);
        btnTellJoke.addEventListener('click', () => {
            if (speechManager.isAvailable()) tellJoke();
        });
        btnCapture.addEventListener('click', takePhoto);
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

        // Settings change handlers
        ['opt-manual-mode', 'opt-detection', 'opt-timer-mode'].forEach(id => {
            document.getElementById(id).addEventListener('change', readSettingsFromUI);
        });
        document.querySelectorAll('input[name="detection-mode"]').forEach(r => {
            r.addEventListener('change', readSettingsFromUI);
        });

        // Sliders
        document.getElementById('slider-timer-delay').addEventListener('input', (e) => {
            document.getElementById('timer-delay-val').textContent = parseFloat(e.target.value).toFixed(1);
            readSettingsFromUI();
        });
        document.getElementById('slider-punchline-delay').addEventListener('input', (e) => {
            document.getElementById('punchline-delay-val').textContent = parseFloat(e.target.value).toFixed(2);
            readSettingsFromUI();
        });
        document.getElementById('slider-next-joke-wait').addEventListener('input', (e) => {
            document.getElementById('next-joke-wait-val').textContent = parseFloat(e.target.value).toFixed(1);
            readSettingsFromUI();
        });

        // Category checkboxes
        ['cat-general', 'cat-programming', 'cat-knock-knock', 'cat-dad'].forEach(id => {
            document.getElementById(id).addEventListener('change', readSettingsFromUI);
        });

        // Joke management
        document.getElementById('btn-reset-jokes').addEventListener('click', () => {
            jokeManager.resetToldJokes();
            updateJokeStats();
            updateStatus();
        });

        document.getElementById('btn-export-jokes').addEventListener('click', () => {
            const json = jokeManager.exportJokesAsJson();
            const blob = new Blob([json], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'jokes_export.json';
            a.click();
            URL.revokeObjectURL(url);
        });

        document.getElementById('file-import-jokes').addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = (ev) => {
                try {
                    const count = jokeManager.importJokes(ev.target.result, false);
                    alert(`Imported ${count} jokes!`);
                    updateJokeStats();
                } catch (err) {
                    alert('Error importing jokes: ' + err.message);
                }
            };
            reader.readAsText(file);
            e.target.value = '';
        });
    }

    // ---- Utility ----

    function updateStatus() {
        const remaining = jokeManager.getRemainingCount();
        statusText.textContent = `Ready - ${remaining} jokes remaining`;
    }

    function clearPendingTimers() {
        pendingTimers.forEach(t => clearTimeout(t));
        pendingTimers = [];
    }

    // ---- Start ----
    document.addEventListener('DOMContentLoaded', init);
})();
