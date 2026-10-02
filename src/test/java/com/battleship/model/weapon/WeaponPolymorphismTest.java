package com.battleship.model.weapon;

import com.battleship.view.SfxAudio;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponPolymorphismTest {

    @Test
    void standardShellDoesNotRequireAuthorization() {
        Weapon standard = WeaponCatalog.standard();
        assertFalse(standard.requiresAuthorization());
    }

    @Test
    void salvoBarrageDoesNotRequireAuthorization() {
        Weapon salvo = WeaponCatalog.salvo();
        assertFalse(salvo.requiresAuthorization());
    }

    @Test
    void nuclearWarheadRequiresAuthorization() {
        Weapon nuclear = WeaponCatalog.nuclear();
        assertTrue(nuclear.requiresAuthorization());
    }

    @Test
    void polymorphicSoundPlaybackDispatchesAppropriateSound() {
        Weapon standard = WeaponCatalog.standard();
        Weapon nuclear = WeaponCatalog.nuclear();

        AtomicBoolean standardFired = new AtomicBoolean(false);
        AtomicBoolean nuclearFired = new AtomicBoolean(false);

        SfxAudio recordingAudio = new SfxAudio() {
            @Override public void playClick() {}
            @Override public void playFire() { standardFired.set(true); }
            @Override public void playHit() {}
            @Override public void playMiss() {}
            @Override public void playSunk() {}
            @Override public void playNuclear() { nuclearFired.set(true); }
            @Override public void playPlaceShip() {}
            @Override public void playRemoveShip() {}
            @Override public void playTurnStart() {}
            @Override public void playGameOver(boolean won) {}
        };

        standard.playFiringSound(recordingAudio);
        assertTrue(standardFired.get());
        assertFalse(nuclearFired.get());

        nuclear.playFiringSound(recordingAudio);
        assertTrue(nuclearFired.get());
    }
}
