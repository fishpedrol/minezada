package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VarietyAndGrowthTest {
    @Test
    void profiles() {
        assertEquals(Variety.Rarity.COMMON, Variety.BRISA.rarity());
        assertEquals(Variety.Cycle.AUTOFLOWERING, Variety.BRISA.cycle());
        assertEquals(Variety.Lineage.SATIVA, Variety.BRISA.lineage());
        assertEquals(Variety.Rarity.UNCOMMON, Variety.AURORA.rarity());
        assertEquals(Variety.Cycle.PHOTOPERIOD, Variety.AURORA.cycle());
        assertEquals(Variety.Lineage.HYBRID, Variety.AURORA.lineage());
        assertEquals(Variety.Rarity.RARE, Variety.ECLIPSE.rarity());
        assertEquals(Variety.Cycle.PHOTOPERIOD, Variety.ECLIPSE.cycle());
        assertEquals(Variety.Lineage.INDICA, Variety.ECLIPSE.lineage());
    }

    @Test
    void brisaFastestEclipseSlowest() {
        assertTrue(Variety.BRISA.growthMultiplier() > Variety.AURORA.growthMultiplier());
        assertTrue(Variety.AURORA.growthMultiplier() > Variety.ECLIPSE.growthMultiplier());
        assertEquals(1.0F, Variety.AURORA.growthMultiplier());
    }

    @Test
    void ids() {
        assertEquals("brisa_seeds", Variety.BRISA.seedsId());
        assertEquals("aurora_crop", Variety.AURORA.cropId());
        assertEquals("eclipse_fresh_flower", Variety.ECLIPSE.freshFlowerId());
        assertEquals("eclipse_dried_flower", Variety.ECLIPSE.driedFlowerId());
    }

    @Test
    void auroraOutdoorMatchesWheat() {
        // Trigo em farmland hidratada isolada: velocidade 1 + 3 = 4 → nextInt(7)
        assertEquals(7, GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, Variety.AURORA, false)));
    }

    @Test
    void greenhouseAndVarietyMultiply() {
        assertEquals(4.0F * 1.25F * 1.25F, GrowthRules.effectiveSpeed(4.0F, Variety.BRISA, true), 1e-6);
        assertEquals(4.0F * 0.8F, GrowthRules.effectiveSpeed(4.0F, Variety.ECLIPSE, false), 1e-6);
        int brisa = GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, Variety.BRISA, false));
        int eclipse = GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, Variety.ECLIPSE, false));
        int eclipseGreenhouse = GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, Variety.ECLIPSE, true));
        assertTrue(brisa < eclipse, "brisa cresce mais rápido");
        assertTrue(eclipseGreenhouse < eclipse, "estufa acelera");
        assertThrows(IllegalArgumentException.class, () -> GrowthRules.growthRollBound(0.0F));
    }

    @Test
    void lightThresholdLikeWheat() {
        assertFalse(GrowthRules.hasEnoughLight(8));
        assertTrue(GrowthRules.hasEnoughLight(9));
    }

    @Test
    void visualStages() {
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3};
        for (int age = 0; age <= 7; age++) {
            assertEquals(expected[age], GrowthRules.visualStage(age));
        }
        assertThrows(IllegalArgumentException.class, () -> GrowthRules.visualStage(8));
    }
}
