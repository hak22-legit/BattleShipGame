package com.battleship.view;

/**
 * background-music control. split out of {@link gameaudio} so a client that only
 * starts/stops bgm can depend on just this slice (isp).
 */
public interface MusicAudio {

    void playMenuMusic();
    void playBattleMusic();
    void stopBgm();
}