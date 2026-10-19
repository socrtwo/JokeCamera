/**
 * Effects - Visual & audio effects for Joke Camera.
 * Provides confetti, toasts, floating emojis, sound effects, and photo gallery.
 */
(function (global) {
    'use strict';

    // ---------- Floating emoji reactions ----------
    const ReactionLayer = {
        spawn(emoji, count = 1) {
            const layer = document.getElementById('reaction-layer');
            if (!layer) return;
            for (let i = 0; i < count; i++) {
                const span = document.createElement('span');
                span.className = 'floating-emoji';
                span.textContent = emoji;
                const x = 20 + Math.random() * 60;
                const y = 50 + Math.random() * 30;
                span.style.left = x + '%';
                span.style.top = y + '%';
                span.style.animationDelay = (i * 80) + 'ms';
                layer.appendChild(span);
                setTimeout(() => span.remove(), 2200 + i * 100);
            }
        }
    };

    // ---------- Toast notifications ----------
    const Toast = {
        show(message, type = 'info') {
            const container = document.getElementById('toast-container');
            if (!container) return;
            const toast = document.createElement('div');
            toast.className = 'toast toast-' + type;
            toast.textContent = message;
            toast.addEventListener('click', () => toast.remove());
            container.appendChild(toast);
            setTimeout(() => toast.remove(), 3500);
        }
    };

    // ---------- Confetti ----------
    const Confetti = {
        canvas: null,
        ctx: null,
        particles: [],
        rafId: null,
        lastTime: 0,

        _init() {
            this.canvas = document.getElementById('confetti-canvas');
            if (!this.canvas) return false;
            this.ctx = this.canvas.getContext('2d');
            this._resize();
            window.addEventListener('resize', () => this._resize());
            return true;
        },

        _resize() {
            if (!this.canvas) return;
            const parent = this.canvas.parentElement;
            const rect = parent.getBoundingClientRect();
            this.canvas.width = rect.width;
            this.canvas.height = rect.height;
        },

        burst(count = 80) {
            if (!this.canvas && !this._init()) return;
            this._resize();
            const colors = ['#ff2e93', '#7c3aed', '#22d3ee', '#ffd166', '#34d399', '#fff'];
            const cx = this.canvas.width / 2;
            const cy = this.canvas.height / 3;

            for (let i = 0; i < count; i++) {
                const angle = (Math.PI * 2 * i / count) + (Math.random() - 0.5) * 0.4;
                const speed = 4 + Math.random() * 6;
                this.particles.push({
                    x: cx,
                    y: cy,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed - 4,
                    color: colors[Math.floor(Math.random() * colors.length)],
                    size: 4 + Math.random() * 6,
                    rot: Math.random() * Math.PI * 2,
                    vrot: (Math.random() - 0.5) * 0.3,
                    life: 1.0,
                    shape: Math.random() < 0.5 ? 'rect' : 'circle'
                });
            }
            if (!this.rafId) {
                this.lastTime = performance.now();
                this._tick();
            }
        },

        _tick() {
            const now = performance.now();
            const dt = Math.min(0.04, (now - this.lastTime) / 1000);
            this.lastTime = now;

            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

            const gravity = 220;
            const drag = 0.985;

            for (let i = this.particles.length - 1; i >= 0; i--) {
                const p = this.particles[i];
                p.vy += gravity * dt;
                p.vx *= drag;
                p.vy *= drag;
                p.x += p.vx * 60 * dt;
                p.y += p.vy * 60 * dt;
                p.rot += p.vrot;
                p.life -= dt * 0.35;

                if (p.life <= 0 || p.y > this.canvas.height + 40) {
                    this.particles.splice(i, 1);
                    continue;
                }

                this.ctx.save();
                this.ctx.globalAlpha = Math.max(0, p.life);
                this.ctx.translate(p.x, p.y);
                this.ctx.rotate(p.rot);
                this.ctx.fillStyle = p.color;
                if (p.shape === 'rect') {
                    this.ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
                } else {
                    this.ctx.beginPath();
                    this.ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
                    this.ctx.fill();
                }
                this.ctx.restore();
            }

            if (this.particles.length > 0) {
                this.rafId = requestAnimationFrame(() => this._tick());
            } else {
                this.rafId = null;
                this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            }
        }
    };

    // ---------- Sound effects (Web Audio, generated) ----------
    const Sound = {
        ctx: null,
        enabled: true,

        _getCtx() {
            if (!this.ctx) {
                try {
                    const Ctx = window.AudioContext || window.webkitAudioContext;
                    if (Ctx) this.ctx = new Ctx();
                } catch (e) { /* ignore */ }
            }
            if (this.ctx && this.ctx.state === 'suspended') {
                this.ctx.resume().catch(() => {});
            }
            return this.ctx;
        },

        setEnabled(on) { this.enabled = !!on; },

        _tone(freq, duration = 0.15, type = 'sine', gain = 0.12, attack = 0.005, release = 0.08) {
            if (!this.enabled) return;
            const ctx = this._getCtx();
            if (!ctx) return;
            const osc = ctx.createOscillator();
            const g = ctx.createGain();
            osc.type = type;
            osc.frequency.value = freq;
            g.gain.value = 0;
            const t = ctx.currentTime;
            g.gain.linearRampToValueAtTime(gain, t + attack);
            g.gain.linearRampToValueAtTime(0, t + duration + release);
            osc.connect(g).connect(ctx.destination);
            osc.start(t);
            osc.stop(t + duration + release + 0.02);
        },

        shutter() {
            if (!this.enabled) return;
            const ctx = this._getCtx();
            if (!ctx) return;
            // Short click burst
            const buf = ctx.createBuffer(1, ctx.sampleRate * 0.12, ctx.sampleRate);
            const data = buf.getChannelData(0);
            for (let i = 0; i < data.length; i++) {
                const t = i / ctx.sampleRate;
                data[i] = (Math.random() * 2 - 1) * Math.exp(-t * 35) * 0.6;
            }
            const src = ctx.createBufferSource();
            src.buffer = buf;
            const g = ctx.createGain();
            g.gain.value = 0.35;
            src.connect(g).connect(ctx.destination);
            src.start();
        },

        ding() {
            this._tone(880, 0.1, 'sine', 0.1);
            setTimeout(() => this._tone(1320, 0.12, 'sine', 0.1), 60);
        },

        success() {
            this._tone(523, 0.08, 'triangle', 0.1);
            setTimeout(() => this._tone(659, 0.08, 'triangle', 0.1), 70);
            setTimeout(() => this._tone(784, 0.12, 'triangle', 0.11), 140);
        },

        laugh() {
            // Quick rising arpeggio
            const notes = [440, 554, 659, 880];
            notes.forEach((n, i) => setTimeout(() => this._tone(n, 0.08, 'triangle', 0.09), i * 60));
        },

        start() {
            this._tone(440, 0.06, 'square', 0.06);
            setTimeout(() => this._tone(660, 0.08, 'square', 0.07), 60);
        },

        stop() {
            this._tone(660, 0.06, 'square', 0.06);
            setTimeout(() => this._tone(440, 0.08, 'square', 0.06), 60);
        }
    };

    // ---------- Photo gallery ----------
    const Gallery = {
        MAX_THUMBS: 6,
        photos: [],

        add(blob, dataUrl) {
            this.photos.push({ blob, dataUrl, t: Date.now() });
            this._render();
            this._updateStats();
        },

        clear() {
            this.photos.forEach(p => { if (p.dataUrl && p.dataUrl.startsWith('blob:')) URL.revokeObjectURL(p.dataUrl); });
            this.photos = [];
            this._render();
            this._updateStats();
        },

        _render() {
            const strip = document.getElementById('gallery-strip');
            if (!strip) return;
            strip.innerHTML = '';
            const recent = this.photos.slice(-this.MAX_THUMBS).reverse();
            for (const photo of recent) {
                const thumb = document.createElement('div');
                thumb.className = 'gallery-thumb';
                const img = document.createElement('img');
                img.src = photo.dataUrl;
                img.alt = 'Captured photo';
                thumb.appendChild(img);
                thumb.addEventListener('click', () => this._openModal(photo));
                strip.appendChild(thumb);
            }
        },

        _updateStats() {
            const el = document.getElementById('photo-stats');
            if (el) el.textContent = `${this.photos.length} photo${this.photos.length === 1 ? '' : 's'} captured`;
        },

        _openModal(photo) {
            const modal = document.getElementById('photo-modal');
            const img = document.getElementById('photo-modal-img');
            if (!modal || !img) return;
            img.src = photo.dataUrl;
            modal.classList.remove('hidden');
            const closeBtn = document.getElementById('btn-photo-close');
            const dlBtn = document.getElementById('btn-photo-download');
            const backdrop = modal.querySelector('.photo-modal-backdrop');

            const close = () => modal.classList.add('hidden');
            closeBtn.onclick = close;
            backdrop.onclick = close;
            dlBtn.onclick = () => {
                const a = document.createElement('a');
                a.href = photo.dataUrl;
                a.download = `JokeCamera_${new Date(photo.t).toISOString().replace(/[:.]/g, '-')}.jpg`;
                a.click();
            };
        },

        count() { return this.photos.length; }
    };

    global.Effects = { ReactionLayer, Toast, Confetti, Sound, Gallery };
})(window);
