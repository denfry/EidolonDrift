#!/usr/bin/env python3
"""Generate procedural PLACEHOLDER horror sounds for Eidolon Drift (M1).

License-clean: every sample is synthesized from scratch here (no downloaded audio),
so the output OGGs ship freely under the mod's MIT license. These are temporary —
final hand-crafted assets replace them in M8. Re-run to regenerate:

    python tools/gen_placeholder_sounds.py

Writes mono 44.1 kHz OGG/Vorbis into src/main/resources/assets/eidolon_drift/sounds/.
"""
import os

import numpy as np
import soundfile as sf

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "eidolon_drift", "sounds")

# Deterministic noise so regeneration is reproducible.
RNG = np.random.default_rng(20260607)


def _t(seconds):
    return np.linspace(0, seconds, int(SR * seconds), endpoint=False)


def _fade(sig, fade_in=0.01, fade_out=0.05):
    n = len(sig)
    fi = max(1, int(SR * fade_in))
    fo = max(1, int(SR * fade_out))
    env = np.ones(n)
    env[:fi] = np.linspace(0, 1, fi)
    env[-fo:] = np.linspace(1, 0, fo)
    return sig * env


def _norm(sig, peak=0.85):
    m = np.max(np.abs(sig)) or 1.0
    return sig / m * peak


def step_behind():
    """A soft, low footfall thump (~0.18 s)."""
    t = _t(0.18)
    body = np.sin(2 * np.pi * 70 * t) * np.exp(-t * 28)
    click = RNG.standard_normal(len(t)) * np.exp(-t * 90) * 0.25
    return _norm(_fade(body + click, 0.001, 0.04), 0.8)


def whisper():
    """Airy band-limited noise, amplitude-modulated like breath (~1.2 s)."""
    t = _t(1.2)
    noise = RNG.standard_normal(len(t))
    # crude band-pass: difference of two smoothed copies
    k = 40
    sm = np.convolve(noise, np.ones(k) / k, mode="same")
    band = noise - sm
    am = 0.5 + 0.5 * np.sin(2 * np.pi * 3.5 * t + RNG.uniform(0, 6))
    return _norm(_fade(band * am, 0.08, 0.2), 0.7)


def distant_voice():
    """Low formant-ish hum with slow vibrato, quiet and far (~1.5 s)."""
    t = _t(1.5)
    vib = np.sin(2 * np.pi * 5 * t) * 4
    sig = np.zeros(len(t))
    for f, a in [(180, 1.0), (360, 0.5), (540, 0.28)]:
        sig += a * np.sin(2 * np.pi * (f + vib) * t)
    env = np.exp(-((t - 0.7) ** 2) / 0.25)
    return _norm(_fade(sig * env, 0.12, 0.35), 0.55)


def creak():
    """A slow structural creak: resonant noise with a falling pitch wobble (~0.8 s)."""
    t = _t(0.8)
    sweep = 240 - 120 * (t / t[-1])
    phase = 2 * np.pi * np.cumsum(sweep) / SR
    tone = np.sin(phase)
    grit = RNG.standard_normal(len(t)) * 0.15
    wobble = 0.6 + 0.4 * np.sin(2 * np.pi * 11 * t)
    return _norm(_fade((tone * wobble + grit) * np.exp(-t * 1.5), 0.02, 0.2), 0.75)


def cave_resonance():
    """A low beating drone out of the rock (~2.0 s)."""
    t = _t(2.0)
    sig = (np.sin(2 * np.pi * 44 * t)
           + np.sin(2 * np.pi * 47 * t)        # beating
           + 0.4 * np.sin(2 * np.pi * 88 * t))
    swell = np.sin(np.pi * t / t[-1]) ** 1.5    # slow in/out
    return _norm(_fade(sig * swell, 0.2, 0.4), 0.7)


def observer_tone():
    """A thin, detuned watching tone with slow tremolo — directional, far (~1.8 s)."""
    t = _t(1.8)
    sig = (np.sin(2 * np.pi * 210 * t)
           + 0.7 * np.sin(2 * np.pi * 213 * t)   # close detune → slow beating
           + 0.3 * np.sin(2 * np.pi * 420 * t))
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * 2.5 * t)
    swell = np.sin(np.pi * t / t[-1]) ** 1.5      # fades in and out, no hard onset
    return _norm(_fade(sig * trem * swell, 0.25, 0.4), 0.5)


SOUNDS = {
    "ambient/step_behind.ogg": step_behind,
    "ambient/whisper.ogg": whisper,
    "ambient/distant_voice.ogg": distant_voice,
    "house/creak.ogg": creak,
    "cave/resonance.ogg": cave_resonance,
    "ambient/observer_tone.ogg": observer_tone,
}


def main():
    for rel, fn in SOUNDS.items():
        path = os.path.join(OUT, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = fn().astype(np.float32)
        sf.write(path, data, SR, format="OGG", subtype="VORBIS")
        print(f"  wrote {rel}  ({len(data) / SR:.2f}s, {os.path.getsize(path)} bytes)")
    print(f"Done — {len(SOUNDS)} placeholder sounds in {OUT}")


if __name__ == "__main__":
    main()
