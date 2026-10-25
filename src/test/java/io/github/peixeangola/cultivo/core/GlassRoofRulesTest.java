package io.github.peixeangola.cultivo.core;

import static io.github.peixeangola.cultivo.core.GlassRoofRules.Cell.GLASS;
import static io.github.peixeangola.cultivo.core.GlassRoofRules.Cell.PASSABLE;
import static io.github.peixeangola.cultivo.core.GlassRoofRules.Cell.SOLID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GlassRoofRulesTest {
    /** Coluna acima da planta: os blocos dados, o resto vazio. */
    private static GlassRoofRules.Probe column(Map<Integer, GlassRoofRules.Cell> cells) {
        return dy -> cells.getOrDefault(dy, PASSABLE);
    }

    @Test
    void rangeIsSixteen() {
        assertEquals(16, GlassRoofRules.RANGE);
    }

    @Test
    void openSkyIsNotCovered() {
        assertFalse(GlassRoofRules.isCovered(column(Map.of())));
    }

    @Test
    void glassRightAboveOrHighUpCovers() {
        assertTrue(GlassRoofRules.isCovered(column(Map.of(1, GLASS))));
        assertTrue(GlassRoofRules.isCovered(column(Map.of(3, GLASS))));
        assertTrue(GlassRoofRules.isCovered(column(Map.of(16, GLASS))));
    }

    @Test
    void glassBeyondRangeDoesNotCover() {
        assertFalse(GlassRoofRules.isCovered(column(Map.of(17, GLASS))));
    }

    /** O primeiro bloco sólido decide: madeira antes do vidro tira o bônus; vidro antes da madeira mantém. */
    @Test
    void firstSolidBlockDecides() {
        assertFalse(GlassRoofRules.isCovered(column(Map.of(2, SOLID, 4, GLASS))));
        assertTrue(GlassRoofRules.isCovered(column(Map.of(2, GLASS, 4, SOLID))));
        assertFalse(GlassRoofRules.isCovered(column(Map.of(3, SOLID))));
    }

    /** Sem paredes: a regra só olha a coluna de cima, então o meio de uma lavoura grande conta. */
    @Test
    void onlyTheColumnAboveIsAsked() {
        assertTrue(GlassRoofRules.isCovered(dy -> {
            assertTrue(dy >= 1 && dy <= GlassRoofRules.RANGE, "dy " + dy);
            return dy == 5 ? GLASS : PASSABLE;
        }));
    }
}
