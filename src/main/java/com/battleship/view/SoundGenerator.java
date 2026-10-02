package com.battleship.view;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * generates simple procedural sound effects as wav byte arrays at runtime.
 * no external audio files are needed -- every sound is synthesised from
 * basic waveforms (sine, sawtooth, noise) and amplitude envelopes.
 */
final class SoundGenerator {

    private static final int SAMPLE_RATE = 44100;

    private SoundGenerator() { }

    /** returns a map of filename -> wav byte[] for every sound the game needs. */
    static Map<String, byte[]> generateAll() {
        return Map.ofEntries(
            Map.entry("click.wav",       click()),
            Map.entry("fire.wav",        fire()),
            Map.entry("hit.wav",         hit()),
            Map.entry("miss.wav",        miss()),
            Map.entry("sunk.wav",        sunk()),
            Map.entry("nuclear.wav",     nuclear()),
            Map.entry("place-ship.wav",  placeShip()),
            Map.entry("remove-ship.wav", removeShip()),
            Map.entry("victory.wav",     victory()),
            Map.entry("defeat.wav",      defeat()),
            Map.entry("turn-start.wav",  turnStart()),
            Map.entry("menu-music.wav",  menuMusic()),
            Map.entry("battle-music.wav", battleMusic())
        );
    }

    // ── individual sound generators ─────────────────────────────────

    /** short ui blip -- 80ms sine at 880 hz with quick decay. */
    private static byte[] click() {
        return tone(0.08, 880, 0.5, 0.02);
    }

    /** weapon fire -- 250ms broadband noise burst with fast attack. */
    private static byte[] fire() {
        return noise(0.25, 0.7, 0.01, 0.20);
    }

    /** impact thud -- 300ms sine sweep 120→60 hz with rumble. */
    private static byte[] hit() {
        int samples = (int) (SAMPLE_RATE * 0.30);
        double[] data = new double[samples];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 120 - 60 * ((double) i / samples);
            double env = Math.exp(-4.0 * t / 0.30);
            data[i] = 0.6 * env * Math.sin(2 * Math.PI * freq * t);
            data[i] += 0.3 * env * noiseval(i) * Math.exp(-6.0 * t / 0.30);
        }
        return toWav(data, 0.6);
    }

    /** water splash -- 350ms filtered noise with high-pass character. */
    private static byte[] miss() {
        return noise(0.35, 0.45, 0.01, 0.10);
    }

    /** ship sinking explosion -- 800ms: deep boom + crackle decay. */
    private static byte[] sunk() {
        int samples = (int) (SAMPLE_RATE * 0.80);
        double[] data = new double[samples];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double boomEnv = Math.exp(-3.0 * t);
            double crackleEnv = Math.exp(-5.0 * t);
            data[i] = 0.55 * boomEnv * Math.sin(2 * Math.PI * 50 * t);
            data[i] += 0.25 * boomEnv * Math.sin(2 * Math.PI * 80 * t);
            data[i] += 0.20 * crackleEnv * noiseval(i);
        }
        return toWav(data, 0.75);
    }

    /** nuclear launch -- 1.5s: rising tone sweep + deep rumble + crackle. */
    private static byte[] nuclear() {
        int samples = (int) (SAMPLE_RATE * 1.5);
        double[] data = new double[samples];
        double phase = 0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            double freq = 80 + 400 * progress;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            double sweepEnv = Math.min(1.0, progress * 4) * Math.exp(-2.0 * Math.max(0, t - 1.0));
            double rumbleEnv = Math.exp(-1.5 * Math.max(0, t - 0.3));
            data[i] = 0.45 * sweepEnv * Math.sin(phase);
            data[i] += 0.35 * rumbleEnv * Math.sin(2 * Math.PI * 40 * t);
            data[i] += 0.15 * Math.exp(-4.0 * t) * noiseval(i);
        }
        return toWav(data, 0.85);
    }

    /** ship placed -- 120ms ascending two-tone chime. */
    private static byte[] placeShip() {
        int samples = (int) (SAMPLE_RATE * 0.12);
        double[] data = new double[samples];
        int split = samples / 2;
        for (int i = 0; i < samples; i++) {
            double freq = i < split ? 523.0 : 659.0;
            double env = Math.exp(-8.0 * ((double) i / SAMPLE_RATE));
            data[i] = 0.5 * env * Math.sin(2 * Math.PI * freq * ((double) i / SAMPLE_RATE));
        }
        return toWav(data, 0.5);
    }

    /** ship removed -- 100ms descending two-tone. */
    private static byte[] removeShip() {
        int samples = (int) (SAMPLE_RATE * 0.10);
        double[] data = new double[samples];
        int split = samples / 2;
        for (int i = 0; i < samples; i++) {
            double freq = i < split ? 659.0 : 523.0;
            double env = Math.exp(-10.0 * ((double) i / SAMPLE_RATE));
            data[i] = 0.45 * env * Math.sin(2 * Math.PI * freq * ((double) i / SAMPLE_RATE));
        }
        return toWav(data, 0.5);
    }

    /** victory fanfare -- 2.5s rising arpeggio with reverb tail. */
    private static byte[] victory() {
        int samples = (int) (SAMPLE_RATE * 2.5);
        double[] data = new double[samples];
        double[] notes = {523.25, 659.25, 783.99, 1046.50, 1318.51};
        double noteLen = 0.22;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double mix = 0;
            for (int n = 0; n < notes.length; n++) {
                double onset = n * noteLen;
                double localT = t - onset;
                if (localT < 0 || localT > 0.6) continue;
                double env = Math.exp(-3.0 * localT) * (localT < 0.01 ? localT / 0.01 : 1.0);
                mix += 0.25 * env * Math.sin(2 * Math.PI * notes[n] * localT);
            }
            double globalEnv = t < 0.05 ? t / 0.05 : Math.exp(-0.8 * Math.max(0, t - 0.3));
            data[i] = globalEnv * mix;
        }
        return toWav(data, 0.7);
    }

    /** defeat stinger -- 1.8s descending minor chord with low rumble. */
    private static byte[] defeat() {
        int samples = (int) (SAMPLE_RATE * 1.8);
        double[] data = new double[samples];
        double[] notes = {392.00, 349.23, 293.66, 220.00};
        double noteLen = 0.35;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double mix = 0;
            for (int n = 0; n < notes.length; n++) {
                double onset = n * noteLen;
                double localT = t - onset;
                if (localT < 0 || localT > 0.8) continue;
                double env = Math.exp(-2.5 * localT);
                mix += 0.22 * env * Math.sin(2 * Math.PI * notes[n] * localT);
            }
            double rumble = 0.15 * Math.exp(-1.5 * Math.max(0, t - 0.5)) * Math.sin(2 * Math.PI * 60 * t);
            data[i] = mix + rumble;
        }
        return toWav(data, 0.65);
    }

    /** turn start -- 150ms two-tone notification ping. */
    private static byte[] turnStart() {
        int samples = (int) (SAMPLE_RATE * 0.15);
        double[] data = new double[samples];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = i < samples / 2 ? 698.0 : 880.0;
            double env = Math.exp(-6.0 * t);
            data[i] = 0.4 * env * Math.sin(2 * Math.PI * freq * t);
        }
        return toWav(data, 0.5);
    }

    /** menu music -- 8s atmospheric loop: soft pad + gentle wave ambience. */
    private static byte[] menuMusic() {
        int samples = (int) (SAMPLE_RATE * 8.0);
        double[] data = new double[samples];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            // soft pad chord: c3 + e3 + g3 + c4 with slow vibrato
            double vibrato = 1.0 + 0.003 * Math.sin(2 * Math.PI * 0.25 * t);
            double pad = 0;
            pad += 0.12 * Math.sin(2 * Math.PI * 130.81 * vibrato * t);
            pad += 0.10 * Math.sin(2 * Math.PI * 164.81 * vibrato * t);
            pad += 0.08 * Math.sin(2 * Math.PI * 196.00 * vibrato * t);
            pad += 0.06 * Math.sin(2 * Math.PI * 261.63 * vibrato * t);
            // slow volume swell for loop seamlessness
            double swell = 0.7 + 0.3 * Math.sin(2 * Math.PI * progress);
            // gentle wave noise (filtered)
            double wave = 0.04 * Math.sin(2 * Math.PI * 0.3 * t) * noiseval(i);
            data[i] = pad * swell + wave;
        }
        // fade in/out for seamless loop
        fadeEdges(data, 0.5, 0.5);
        return toWav(data, 0.45);
    }

    /** battle music -- 8s tense loop: driving pulse + low drone + percussive tick. */
    private static byte[] battleMusic() {
        int samples = (int) (SAMPLE_RATE * 8.0);
        double[] data = new double[samples];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            // low drone: d2 + a2
            double drone = 0.18 * Math.sin(2 * Math.PI * 73.42 * t)
                         + 0.10 * Math.sin(2 * Math.PI * 110.00 * t);
            // driving pulse: tempo ~120 bpm (2 hz)
            double beatPhase = (t * 2.0) % 1.0;
            double pulse = Math.exp(-8.0 * beatPhase) * 0.15 * Math.sin(2 * Math.PI * 110 * t);
            // percussive tick on every beat
            double tickPhase = (t * 2.0) % 1.0;
            double tick = tickPhase < 0.03 ? 0.20 * noiseval(i) * (1.0 - tickPhase / 0.03) : 0;
            // tension pad: minor second interval
            double tension = 0.06 * Math.sin(2 * Math.PI * 146.83 * t)
                           * (0.5 + 0.5 * Math.sin(2 * Math.PI * 0.5 * t));
            double swell = 0.75 + 0.25 * Math.sin(2 * Math.PI * progress);
            data[i] = (drone + pulse + tick + tension) * swell;
        }
        fadeEdges(data, 0.5, 0.5);
        return toWav(data, 0.50);
    }

    // ── waveform helpers ────────────────────────────────────────────

    /** pure sine tone with linear attack and exponential decay. */
    private static byte[] tone(double duration, double freq, double amp, double attack) {
        int samples = (int) (SAMPLE_RATE * duration);
        double[] data = new double[samples];
        int attackSamples = (int) (SAMPLE_RATE * attack);
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env;
            if (i < attackSamples) {
                env = (double) i / attackSamples;
            } else {
                env = Math.exp(-6.0 * (t - attack));
            }
            data[i] = amp * env * Math.sin(2 * Math.PI * freq * t);
        }
        return toWav(data, amp);
    }

    /** band-limited noise burst with attack and release. */
    private static byte[] noise(double duration, double amp, double attack, double release) {
        int samples = (int) (SAMPLE_RATE * duration);
        double[] data = new double[samples];
        int attackSamples = (int) (SAMPLE_RATE * attack);
        int releaseStart = samples - (int) (SAMPLE_RATE * release);
        for (int i = 0; i < samples; i++) {
            double env;
            if (i < attackSamples) {
                env = (double) i / attackSamples;
            } else if (i > releaseStart) {
                env = (double) (samples - i) / (samples - releaseStart);
            } else {
                env = 1.0;
            }
            data[i] = amp * env * noiseval(i);
        }
        return toWav(data, amp);
    }

    /** simple pseudo-random noise from index (deterministic). */
    private static double noiseval(int i) {
        long x = i * 6364136223846793005L + 1442695040888963407L;
        x ^= x >> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >> 33;
        return ((x & 0x7FFFFFFF) / (double) 0x7FFFFFFF) * 2 - 1;
    }

    /** apply fade-in and fade-out to prevent click artifacts. */
    private static void fadeEdges(double[] data, double fadeInSec, double fadeOutSec) {
        int fadeIn = (int) (SAMPLE_RATE * fadeInSec);
        int fadeOut = (int) (SAMPLE_RATE * fadeOutSec);
        for (int i = 0; i < Math.min(fadeIn, data.length); i++) {
            data[i] *= (double) i / fadeIn;
        }
        for (int i = 0; i < Math.min(fadeOut, data.length); i++) {
            int idx = data.length - 1 - i;
            data[idx] *= (double) i / fadeOut;
        }
    }

    // ── wav encoding ────────────────────────────────────────────────

    /** convert double samples [-1,1] to a complete wav file byte array. */
    private static byte[] toWav(double[] samples, double peak) {
        if (peak <= 0) peak = 1.0;
        int numSamples = samples.length;
        int dataSize = numSamples * 2; // 16-bit = 2 bytes per sample
        int fileSize = 44 + dataSize;

        ByteBuffer buf = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN);

        // riff header
        buf.put("RIFF".getBytes());
        buf.putInt(fileSize - 8);
        buf.put("WAVE".getBytes());

        // fmt sub-chunk
        buf.put("fmt ".getBytes());
        buf.putInt(16);                   // sub-chunk size
        buf.putShort((short) 1);          // pcm format
        buf.putShort((short) 1);          // mono
        buf.putInt(SAMPLE_RATE);          // sample rate
        buf.putInt(SAMPLE_RATE * 2);      // byte rate (sr * channels * bits/8)
        buf.putShort((short) 2);          // block align (channels * bits/8)
        buf.putShort((short) 16);         // bits per sample

        // data sub-chunk
        buf.put("data".getBytes());
        buf.putInt(dataSize);

        for (double s : samples) {
            double clamped = Math.max(-1.0, Math.min(1.0, s / peak));
            buf.putShort((short) (clamped * Short.MAX_VALUE));
        }

        return buf.array();
    }
}
