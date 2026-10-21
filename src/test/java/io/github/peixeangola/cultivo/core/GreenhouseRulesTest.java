package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.GreenhouseRules.Cell;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GreenhouseRulesTest {
    /** Mundo fictício: tudo é ar, exceto as células marcadas. */
    private static final class World {
        final Map<String, Cell> cells = new HashMap<>();

        World set(int x, int y, int z, Cell cell) {
            cells.put(x + "," + y + "," + z, cell);
            return this;
        }

        GreenhouseRules.Probe probe() {
            return (x, y, z) -> cells.getOrDefault(x + "," + y + "," + z, Cell.AIR);
        }
    }

    private static World box(int radius, int roofHeight) {
        World w = new World();
        w.set(0, roofHeight, 0, Cell.GLASS);
        w.set(radius, 0, 0, Cell.GLASS).set(-radius, 0, 0, Cell.GLASS);
        w.set(0, 0, radius, Cell.GLASS).set(0, 0, -radius, Cell.GLASS);
        return w;
    }

    @Test
    void openSkyIsOutdoor() {
        assertFalse(GreenhouseRules.isGreenhouse(new World().probe()));
    }

    @Test
    void glassRoofAndWallsIsGreenhouse() {
        assertTrue(GreenhouseRules.isGreenhouse(box(3, 3).probe()));
        assertTrue(GreenhouseRules.isGreenhouse(box(6, 6).probe()));
    }

    @Test
    void roofMustBeFirstNonAirBlock() {
        World w = box(3, 4).set(0, 2, 0, Cell.OTHER);
        assertFalse(GreenhouseRules.hasGlassRoof(w.probe()));
    }

    @Test
    void roofOutOfRangeFails() {
        assertFalse(GreenhouseRules.isGreenhouse(box(3, 7).probe()));
    }

    @Test
    void wallsOneBlockUpCount() {
        World w = new World().set(0, 3, 0, Cell.GLASS);
        w.set(2, 1, 0, Cell.GLASS).set(-2, 1, 0, Cell.GLASS).set(0, 1, 2, Cell.GLASS).set(0, 1, -2, Cell.GLASS);
        assertTrue(GreenhouseRules.isGreenhouse(w.probe()));
    }

    @Test
    void missingOneWallFails() {
        World w = new World().set(0, 3, 0, Cell.GLASS);
        w.set(2, 0, 0, Cell.GLASS).set(-2, 0, 0, Cell.GLASS).set(0, 0, 2, Cell.GLASS);
        assertFalse(GreenhouseRules.isGreenhouse(w.probe()));
    }

    @Test
    void wallTooFarOrTooHighFails() {
        World far = box(3, 3).set(3, 0, 0, Cell.AIR).set(7, 0, 0, Cell.GLASS);
        assertFalse(GreenhouseRules.hasGlassWalls(far.probe()));
        World high = box(3, 3).set(3, 0, 0, Cell.AIR).set(3, 2, 0, Cell.GLASS);
        assertFalse(GreenhouseRules.hasGlassWalls(high.probe()));
    }

    @Test
    void stoneRoofIsNotGreenhouse() {
        World w = box(3, 3).set(0, 3, 0, Cell.OTHER);
        assertFalse(GreenhouseRules.isGreenhouse(w.probe()));
    }
}
