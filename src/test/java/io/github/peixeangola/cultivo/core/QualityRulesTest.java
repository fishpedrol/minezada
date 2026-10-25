package io.github.peixeangola.cultivo.core;

import static io.github.peixeangola.cultivo.core.QualityRules.Route.GLASS_ROOF;
import static io.github.peixeangola.cultivo.core.QualityRules.Route.OUTDOOR;
import static io.github.peixeangola.cultivo.core.QualityRules.Route.TENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QualityRulesTest {
    @Test
    void basesDifferByRoute() {
        assertEquals(2, QualityRules.baseQuality(OUTDOOR));
        assertEquals(3, QualityRules.baseQuality(GLASS_ROOF));
        assertEquals(4, QualityRules.baseQuality(TENT));
        assertEquals(2, QualityRules.OUTDOOR_BASE);
        assertEquals(3, QualityRules.GLASS_ROOF_BASE);
        assertEquals(4, QualityRules.TENT_BASE);
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
        assertEquals(3, QualityRules.harvestQuality(Variety.AMNESIA_NEBLINA, OUTDOOR, 0.49));
        assertEquals(2, QualityRules.harvestQuality(Variety.MANGA_GELADA, OUTDOOR, 0.35));
        assertEquals(4, QualityRules.harvestQuality(Variety.MANGA_GELADA, GLASS_ROOF, 0.34));
        assertEquals(3, QualityRules.harvestQuality(Variety.MANGA_GELADA, GLASS_ROOF, 0.35));
        assertEquals(5, QualityRules.harvestQuality(Variety.LUZES_DO_NORTE, TENT, 0.19));
        assertEquals(4, QualityRules.harvestQuality(Variety.LUZES_DO_NORTE, TENT, 0.20));
    }

    @ParameterizedTest
    @EnumSource(Variety.class)
    void rollBelowChanceAddsOne(Variety variety) {
        double below = variety.qualityBonusChance() - 1e-6;
        double at = variety.qualityBonusChance();
        assertEquals(3, QualityRules.harvestQuality(variety, OUTDOOR, below));
        assertEquals(2, QualityRules.harvestQuality(variety, OUTDOOR, at));
        assertEquals(4, QualityRules.harvestQuality(variety, GLASS_ROOF, below));
        assertEquals(3, QualityRules.harvestQuality(variety, GLASS_ROOF, at));
        assertEquals(5, QualityRules.harvestQuality(variety, TENT, 0.0));
        assertEquals(4, QualityRules.harvestQuality(variety, TENT, 0.999));
    }

    /** Ar livre 2–3, teto de vidro 3–4, estufinha 4–5: cada rota sobe a faixa em 1, e a estufinha fica no topo. */
    @ParameterizedTest
    @EnumSource(Variety.class)
    void routesStepUpByOne(Variety variety) {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            int outdoor = QualityRules.harvestQuality(variety, OUTDOOR, random.nextDouble());
            int glass = QualityRules.harvestQuality(variety, GLASS_ROOF, random.nextDouble());
            int tent = QualityRules.harvestQuality(variety, TENT, random.nextDouble());
            assertTrue(outdoor >= 2 && outdoor <= 3, "ar livre " + outdoor);
            assertTrue(glass >= 3 && glass <= 4, "teto de vidro " + glass);
            assertTrue(tent >= 4 && tent <= 5, "estufinha " + tent);
        }
        double roll = random.nextDouble();
        assertEquals(QualityRules.harvestQuality(variety, OUTDOOR, roll) + 1, QualityRules.harvestQuality(variety, GLASS_ROOF, roll));
        assertEquals(QualityRules.harvestQuality(variety, GLASS_ROOF, roll) + 1, QualityRules.harvestQuality(variety, TENT, roll));
    }

    @Test
    void empiricalBonusRateMatchesChance() {
        Random random = new Random(7);
        int n = 200_000;
        for (Variety variety : Variety.values()) {
            int bonus = 0;
            for (int i = 0; i < n; i++) {
                if (QualityRules.harvestQuality(variety, OUTDOOR, random.nextDouble()) == 3) {
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
