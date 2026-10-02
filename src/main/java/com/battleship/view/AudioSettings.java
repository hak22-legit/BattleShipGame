package com.battleship.view;

/**
 * volume and mute control (options screen). split out of {@link gameaudio} so a
 * settings screen can depend on just this slice (isp).
 */
public interface AudioSettings {

    void setMasterVolume(double v);
    double getMasterVolume();
    void setSfxVolume(double v);
    double getSfxVolume();
    void setMuted(boolean m);
    boolean isMuted();
    void toggleMute();
}