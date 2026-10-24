package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QualityRulesTest {
    @Test
    void basesDifferByRoute() {
        assertEquals(2, QualityRules.baseQuality(false));
        assertEquals(4, QualityRules.baseQuality(true));
    }

    @Test
    void bonusChancesPerVariety() {
        assertEquals(0.20, Variety.BRISA.qualityBonusChance(), 1e-9);
        assertEquals(0.35, Variety.AURORA.qualityBonusChance(), 1e-9);
        assertEquals(0.50, Variety.ECLIPSE.qualityBonusChance(), 1e-9);
    }

    /** No catálogo a potência define a chance do mesmo jeito: 1 → 0,20, 2 → 0,35, 3 → 0,50. */
    @Test
    void catalogBonusChancesFollowPotency() {
        assertEquals(1, Variety.LUZES_DO_NORTE.potency());
        assertEquals(0.20, Variety.LUZES_DO_NORTE.qualityBonusChance(), 1e-9);
        assertEquals(2, Variety.MANGA_GELADA.potency());
        assertEquals(0.35, Variety.MANGA_GELADA.qualityBonusChance(), 1e-9);
        assertEquals(3, Variety.AMNESIA_NEBLINA.potency());
        assertEquals(0.50, Variety.AMNESIA_NEBLINA.qualityBonusChance(), 1e-9);
        assertEquals(3, QualityRules.harvestQuality(Variety.AMNESIA_NEBLINA, false, 0.49));
        assertEquals(2, QualityRules.harvestQuality(Variety.MANGA_GELADA, false, 0.35));
        assertEquals(5, QualityRules.harvestQuality(Variety.LUZES_DO_NORTE, true, 0.19));
        assertEquals(4, QualityRules.harvestQuality(Variety.LUZES_DO_NORTE, true, 0.20));
    }

    @ParameterizedTest
    @EnumSource(Variety.class)
    void rollBelowChanceAddsOne(Variety variety) {
        double below = variety.qualityBonusChance() - 1e-6;
        double at = variety.qualityBonusChance();
        assertEquals(3, QualityRules.harvestQuality(variety, false, below));
        assertEquals(2, QualityRules.harvestQuality(variety, false, at));
        assertEquals(5, QualityRules.harvestQuality(variety, true, 0.0));
        assertEquals(4, QualityRules.harvestQuality(variety, true, 0.999));
    }

    @ParameterizedTest
    @EnumSource(Variety.class)
    void routesNeverOverlap(Variety variety) {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            int outdoor = QualityRules.harvestQuality(variety, false, random.nextDouble());
            int greenhouse = QualityRules.harvestQuality(variety, true, random.nextDouble());
            assertTrue(outdoor >= 2 && outdoor <= 3, "outdoor " + outdoor);
            assertTrue(greenhouse >= 4 && greenhouse <= 5, "greenhouse " + greenhouse);
            assertTrue(greenhouse > outdoor);
        }
    }

    @Test
    void empiricalBonusRateMatchesChance() {
        Random random = new Random(7);
        int n = 200_000;
        for (Variety variety : Variety.values()) {
            int bonus = 0;
            for (int i = 0; i < n; i++) {
                if (QualityRules.harvestQuality(variety, false, random.nextDouble()) == 3) {
                    bonus++;
                }
            }
            assertEquals(variety.qualityBonusChance(), bonus / (double) n, 0.01, variety.id());
        }
    }

    @Test
    void clampKeepsRange() {
        assertEquals(1, QualityRules.clamp(-3));
        assertEquals(5, QualityRules.clamp(9));
        assertEquals(3, QualityRules.clamp(3));
    }
}
