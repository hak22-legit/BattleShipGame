package com.battleship.view;

/**
 * one-shot sound effects. split out of {@link gameaudio} so a client that only
 * triggers sfx can depend on just this slice (isp), instead of on music and
 * volume control it never touches.
 */
public interface SfxAudio {

    void playClick();
    void playFire();
    void playHit();
    void playMiss();
    void playSunk();
    void playNuclear();
    void playPlaceShip();
    void playRemoveShip();
    void playTurnStart();
    /** plays the appropriate end-of-match sting (victory or defeat). */
    void playGameOver(boolean won);
}