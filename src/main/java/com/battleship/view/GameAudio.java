package com.battleship.view;

/**
 * abstraction for game audio (fixes c2). views depend on this interface and
 * receive it via {@link viewnavigator#getaudio()} instead of calling the
 * static {@code soundmanager} singleton — so a silent stub can be injected in
 * tests (see {@link silentaudio}) and the sound implementation stays swappable.
 *
 * <p>fixes the isp concern noted in review: instead of one 17-method blob, the
 * contract is the composition of three narrow roles — {@link sfxaudio},
 * {@link musicaudio} and {@link audiosettings} — which can also be injected
 * individually by clients that need only one of them.</p>
 */
public interface GameAudio extends SfxAudio, MusicAudio, AudioSettings {
}
