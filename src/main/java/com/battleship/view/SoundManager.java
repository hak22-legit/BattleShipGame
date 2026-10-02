package com.battleship.view;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * singleton manager for all game audio using only {@code javax.sound.sampled}
 * (ships with every jdk, no extra modules needed).
 *
 * <p>every public method is completely crash-safe: if anything goes wrong
 * during init or playback, the game keeps running silently.</p>
 */
public final class SoundManager implements GameAudio {

    private static volatile SoundManager instance;

    /**
     * @deprecated prefer dependency-injecting {@link gameaudio} via {@link viewnavigator} or constructors.
     */
    @Deprecated
    public static SoundManager getInstance() {
        if (instance == null) {
            synchronized (SoundManager.class) {
                if (instance == null) {
                    try {
                        instance = new SoundManager();
                    } catch (Exception e) {
                        // if sound system fails entirely, return a silent no-op instance
                        instance = new SoundManager(true);
                    }
                }
            }
        }
        return instance;
    }

    // ── state ───────────────────────────────────────────────────────
    private Clip bgmClip;
    private String currentBgmName;
    private boolean muted = false;
    private double masterVolume = 0.70;
    private double sfxVolume = 0.65;
    private final Map<String, byte[]> sfxData = new HashMap<>();
    private boolean initFailed = false;

    private static final File AUDIO_DIR;

    static {
        File dir;
        try {
            dir = new File(System.getProperty("user.home", "."), ".battleship/audio");
        } catch (Exception e) {
            dir = null;
        }
        AUDIO_DIR = dir;
    }

    private static final String[] ALL_EFFECTS = {
        "click", "fire", "hit", "miss", "sunk", "nuclear",
        "place-ship", "remove-ship", "victory", "defeat", "turn-start"
    };

    /** no-op constructor for the silent fallback instance. */
    private SoundManager(boolean silent) {
        this.initFailed = true;
    }

    public SoundManager() {
        try {
            ensureGeneratedFiles();
            for (String name : ALL_EFFECTS) {
                try {
                    byte[] data = loadWavBytes(name);
                    if (data != null) sfxData.put(name, data);
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            initFailed = true;
        }
    }

    private void ensureGeneratedFiles() {
        if (AUDIO_DIR == null) return;
        // if classpath audio exists, skip generation
        try {
            if (getClass().getResource("/audio/click.wav") != null) return;
        } catch (Exception ignored) { }
        try {
            if (!AUDIO_DIR.exists()) AUDIO_DIR.mkdirs();
            if (new File(AUDIO_DIR, "click.wav").exists()) return;
            Map<String, byte[]> sounds = SoundGenerator.generateAll();
            for (Map.Entry<String, byte[]> e : sounds.entrySet()) {
                java.nio.file.Files.write(new File(AUDIO_DIR, e.getKey()).toPath(), e.getValue());
            }
        } catch (Exception ignored) { }
    }

    // ── public api: background music ────────────────────────────────

    public void playMenuMusic() { playBgm("menu-music"); }
    public void playBattleMusic() { playBgm("battle-music"); }

    public void stopBgm() {
        currentBgmName = null;
        try {
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
            }
        } catch (Exception ignored) { }
        bgmClip = null;
    }

    // ── public api: sound effects ───────────────────────────────────

    public void playClick() { play("click"); }
    public void playFire() { play("fire"); }
    public void playHit() { play("hit"); }
    public void playMiss() { play("miss"); }
    public void playSunk() { play("sunk"); }
    public void playNuclear() { play("nuclear"); }
    public void playPlaceShip() { play("place-ship"); }
    public void playRemoveShip() { play("remove-ship"); }
    public void playVictory() { play("victory"); }
    public void playDefeat() { play("defeat"); }
    public void playTurnStart() { play("turn-start"); }

    public void playGameOver(boolean playerWon) {
        if (playerWon) playVictory(); else playDefeat();
    }

    // ── volume / mute ───────────────────────────────────────────────

    public void setMasterVolume(double v) { masterVolume = clamp(v); applyBgmVolume(); }
    public double getMasterVolume() { return masterVolume; }
    public void setSfxVolume(double v) { sfxVolume = clamp(v); }
    public double getSfxVolume() { return sfxVolume; }
    public void setMuted(boolean m) {
        muted = m;
        if (muted) {
            try {
                if (bgmClip != null) {
                    bgmClip.stop();
                    bgmClip.close();
                }
            } catch (Exception ignored) { }
            bgmClip = null;
        } else {
            if (currentBgmName != null) {
                playBgm(currentBgmName);
            }
        }
    }
    public boolean isMuted() { return muted; }
    public void toggleMute() { setMuted(!muted); }

    // ── internal: sfx ──────────────────────────────────────────────

    private void play(String name) {
        if (muted || initFailed) return;
        byte[] data = sfxData.get(name);
        if (data == null) return;
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)));
            FloatControl vol = clip.isControlSupported(FloatControl.Type.MASTER_GAIN)
                    ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN) : null;
            if (vol != null) {
                double v = effectiveSfxVolume();
                float dB = v > 0 ? (float) (20 * Math.log10(v)) : vol.getMinimum();
                dB = Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), dB));
                vol.setValue(dB);
            }
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    try { clip.close(); } catch (Exception ignored) { }
                }
            });
            clip.start();
        } catch (Exception ignored) { }
    }

    // ── internal: bgm ──────────────────────────────────────────────

    private void playBgm(String name) {
        currentBgmName = name;
        if (muted || initFailed) return;
        try {
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
            }
        } catch (Exception ignored) { }
        bgmClip = null;
        byte[] data = loadWavBytes(name);
        if (data == null) return;
        try {
            bgmClip = AudioSystem.getClip();
            bgmClip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)));
            applyBgmVolume();
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            bgmClip = null;
        }
    }

    private void applyBgmVolume() {
        if (bgmClip == null || muted) return;
        try {
            if (bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl vol = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                double v = effectiveBgmVolume();
                float dB = v > 0 ? (float) (20 * Math.log10(v)) : vol.getMinimum();
                dB = Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), dB));
                vol.setValue(dB);
            }
        } catch (Exception ignored) { }
    }

    // ── internal: file resolution ──────────────────────────────────

    private byte[] loadWavBytes(String name) {
        // 1. classpath
        try {
            java.net.URL url = getClass().getResource("/audio/" + name + ".wav");
            if (url != null) {
                try (InputStream is = url.openStream()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception ignored) { }
        // 2. generated directory
        if (AUDIO_DIR != null) {
            File f = new File(AUDIO_DIR, name + ".wav");
            if (f.exists()) {
                try {
                    return java.nio.file.Files.readAllBytes(f.toPath());
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    private double effectiveBgmVolume() { return muted ? 0 : masterVolume * 0.45; }
    private double effectiveSfxVolume() { return muted ? 0 : masterVolume * sfxVolume; }
}
