package com.battleship.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** validates the {@link silentaudio} null object and the {@link gameaudio} role split. */
class SilentAudioTest {

    @Test
    void reportsPermanentlySilentSettings() {
        SilentAudio audio = new SilentAudio();

        assertEquals(0.0, audio.getMasterVolume());
        assertEquals(0.0, audio.getSfxVolume());
        assertTrue(audio.isMuted());
    }

    @Test
    void settingsMutatorsHaveNoEffect() {
        SilentAudio audio = new SilentAudio();

        audio.setMasterVolume(1.0);
        audio.setSfxVolume(0.8);
        audio.setMuted(false);
        audio.toggleMute();

        assertEquals(0.0, audio.getMasterVolume());
        assertEquals(0.0, audio.getSfxVolume());
        assertTrue(audio.isMuted());
    }

    @Test
    void everyContractMethodIsSafeToCall() {
        SilentAudio audio = new SilentAudio();

        assertDoesNotThrow(() -> {
            audio.playClick();
            audio.playFire();
            audio.playHit();
            audio.playMiss();
            audio.playSunk();
            audio.playNuclear();
            audio.playPlaceShip();
            audio.playRemoveShip();
            audio.playTurnStart();
            audio.playGameOver(true);
            audio.playGameOver(false);
            audio.playMenuMusic();
            audio.playBattleMusic();
            audio.stopBgm();
        });
    }

    /** gameaudio is the composition of the three narrow roles (isp fix). */
    @Test
    void satisfiesEveryNarrowAudioRole() {
        GameAudio audio = SilentAudio.INSTANCE;

        assertInstanceOf(SfxAudio.class, audio);
        assertInstanceOf(MusicAudio.class, audio);
        assertInstanceOf(AudioSettings.class, audio);
    }
}