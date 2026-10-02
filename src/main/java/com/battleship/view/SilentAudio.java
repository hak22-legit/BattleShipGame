package com.battleship.view;

/**
 * null object implementation of {@link gameaudio}: every method is a no-op and
 * the settings permanently report "silent".
 *
 * <p>lets screens be built and unit-tested without any audio backend, and
 * removes null checks at every call site — the null object pattern suggested in
 * review. distinct from {@code soundmanager}, which is the real backend.</p>
 */
public final class SilentAudio implements GameAudio {

    /** shared stateless instance. */
    public static final SilentAudio INSTANCE = new SilentAudio();

    public SilentAudio() { }

    // ---------- sfxaudio: nothing to play ----------

    @Override public void playClick() { }
    @Override public void playFire() { }
    @Override public void playHit() { }
    @Override public void playMiss() { }
    @Override public void playSunk() { }
    @Override public void playNuclear() { }
    @Override public void playPlaceShip() { }
    @Override public void playRemoveShip() { }
    @Override public void playTurnStart() { }
    @Override public void playGameOver(boolean won) { }

    // ---------- musicaudio: nothing to play ----------

    @Override public void playMenuMusic() { }
    @Override public void playBattleMusic() { }
    @Override public void stopBgm() { }

    // ---------- audiosettings: permanently silent ----------

    @Override public void setMasterVolume(double v) { }
    @Override public double getMasterVolume() { return 0.0; }

    @Override public void setSfxVolume(double v) { }
    @Override public double getSfxVolume() { return 0.0; }

    @Override public void setMuted(boolean m) { }
    @Override public boolean isMuted() { return true; }
    @Override public void toggleMute() { }
}