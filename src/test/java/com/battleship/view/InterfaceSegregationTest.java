package com.battleship.view;

import com.battleship.model.AmmoReadout;
import com.battleship.model.FleetDeployment;
import com.battleship.model.FleetReadout;
import com.battleship.model.HumanPlayer;
import com.battleship.model.Player;
import com.battleship.model.PrimaryGrid;
import com.battleship.model.ShotTarget;
import com.battleship.model.Theater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * validates the interface segregation principle (isp) and component role boundaries
 * introduced during academic refactoring.
 */
class InterfaceSegregationTest {

    @Test
    void viewNavigatorSatisfiesSegregatedRoleInterfaces() {
        assertTrue(ScreenNavigator.class.isAssignableFrom(ViewNavigator.class),
                "ViewNavigator must satisfy ScreenNavigator");
        assertTrue(AudioProvider.class.isAssignableFrom(ViewNavigator.class),
                "ViewNavigator must satisfy AudioProvider");
        assertTrue(WindowProvider.class.isAssignableFrom(ViewNavigator.class),
                "ViewNavigator must satisfy WindowProvider");
    }

    @Test
    void playerComponentAccessorsSatisfyNarrowRoleContracts() {
        Player player = new HumanPlayer("Test Admiral", Theater.SKIRMISH);

        PrimaryGrid primary = player.primaryGrid();
        assertNotNull(primary);
        assertInstanceOf(FleetReadout.class, primary);
        assertInstanceOf(FleetDeployment.class, primary);
        assertInstanceOf(ShotTarget.class, primary);

        AmmoReadout ammo = player.ammoReadout();
        assertNotNull(ammo);
        assertTrue(ammo.hasAmmo(com.battleship.model.weapon.WeaponCatalog.standard()));
    }

    @Test
    void soundManagerInstantiableWithoutSingleton() {
        SoundManager soundManager = new SoundManager();
        assertNotNull(soundManager);
        assertInstanceOf(GameAudio.class, soundManager);
    }
}
