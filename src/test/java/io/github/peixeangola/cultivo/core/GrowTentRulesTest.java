package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.GrowTentRules.Use;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Regras da estufinha (pedido do dono de 25/10/2026), com os valores da especificação escritos como literais. */
class GrowTentRulesTest {
    @Test
    void numbersOfTheSpec() {
        assertEquals(4.0F, GrowTentRules.SOIL_SPEED);
        assertEquals(1.25F, GrowTentRules.MULTIPLIER);
        assertEquals(12, GrowTentRules.LAMP_LIGHT);
        assertEquals(2, GrowTentRules.BONE_MEAL_MIN);
        assertEquals(5, GrowTentRules.BONE_MEAL_MAX);
    }

    /** Rápida 4 × 1,25 × 1,25 = 6,25 → 5; média 5 → 6; lenta 4 × 0,8 × 1,25 = 4 → 7. */
    @Test
    void rollBoundsByGrowthClass() {
        assertEquals(6.25F, GrowTentRules.speed(Variety.BRISA), 1e-6);
        assertEquals(5.0F, GrowTentRules.speed(Variety.AURORA), 1e-6);
        assertEquals(4.0F, GrowTentRules.speed(Variety.MORANGO_AZEDO), 1e-5);
        assertEquals(5, GrowTentRules.rollBound(Variety.BRISA));
        assertEquals(5, GrowTentRules.rollBound(Variety.KUSH_ROXINHA));
        assertEquals(6, GrowTentRules.rollBound(Variety.AURORA));
        assertEquals(6, GrowTentRules.rollBound(Variety.MANGA_GELADA));
        assertEquals(7, GrowTentRules.rollBound(Variety.ECLIPSE));
        assertEquals(7, GrowTentRules.rollBound(Variety.MORANGO_AZEDO));
    }

    /**
     * Em toda genética a estufinha sorteia com limite 1 menor (mais rápida) que uma farmland hidratada sozinha ao ar livre
     * (6/7/8 contra 5/6/7).
     */
    @Test
    void tentBeatsALoneHydratedPlot() {
        for (Variety v : Variety.values()) {
            int outdoor = GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, v));
            assertEquals(outdoor - 1, GrowTentRules.rollBound(v), v.id());
        }
    }

    @Test
    void growsUntilRipe() {
        for (int age = 0; age <= 6; age++) {
            assertTrue(GrowTentRules.canGrow(true, age), "idade " + age);
            assertFalse(GrowTentRules.isRipe(true, age), "idade " + age);
        }
        assertFalse(GrowTentRules.canGrow(true, 7));
        assertTrue(GrowTentRules.isRipe(true, 7));
        assertFalse(GrowTentRules.canGrow(false, 0), "vazia não cresce");
        assertFalse(GrowTentRules.isRipe(false, 7), "vazia não está madura");
    }

    /**
     * Planta só com semente na desocupada e podendo construir; colhe só madura (com qualquer coisa na mão); a inerte (id
     * desconhecido) não planta nem colhe; o resto não faz nada.
     */
    @Test
    void rightClick() {
        assertEquals(Use.PLANT, GrowTentRules.use(false, false, 0, true, true));
        assertEquals(Use.NONE, GrowTentRules.use(false, false, 0, true, false), "modo aventura sem can_place_on");
        assertEquals(Use.NONE, GrowTentRules.use(false, false, 0, false, true));
        assertEquals(Use.NONE, GrowTentRules.use(true, true, 3, true, true), "não replanta por cima");
        assertEquals(Use.NONE, GrowTentRules.use(true, true, 6, false, true));
        assertEquals(Use.HARVEST, GrowTentRules.use(true, true, 7, false, true));
        assertEquals(Use.HARVEST, GrowTentRules.use(true, true, 7, true, true));
        assertEquals(Use.HARVEST, GrowTentRules.use(true, true, 7, false, false), "colher vale no modo aventura");
        assertEquals(Use.NONE, GrowTentRules.use(true, false, 7, false, true), "inerte não colhe");
        assertEquals(Use.NONE, GrowTentRules.use(true, false, 0, true, true), "inerte não replanta");
    }

    @Test
    void boneMealCapsAtSeven() {
        assertEquals(2, GrowTentRules.afterBoneMeal(0, 2));
        assertEquals(5, GrowTentRules.afterBoneMeal(0, 5));
        assertEquals(7, GrowTentRules.afterBoneMeal(6, 5));
        assertEquals(7, GrowTentRules.afterBoneMeal(5, 2));
    }

    /** A colheita da estufinha é 4–5 e a de fora 2–3: a estufinha usa a rota tent da QualityRules. */
    @Test
    void tentQualityIsFourToFive() {
        Random random = new Random(25);
        for (Variety v : Variety.values()) {
            for (int i = 0; i < 1000; i++) {
                int q = QualityRules.harvestQuality(v, true, random.nextDouble());
                assertTrue(q == 4 || q == 5, v.id() + " " + q);
            }
        }
        assertEquals(4, QualityRules.harvestQuality(Variety.ECLIPSE, true, 0.5));
        assertEquals(5, QualityRules.harvestQuality(Variety.ECLIPSE, true, 0.49));
        assertEquals(2, QualityRules.harvestQuality(Variety.ECLIPSE, false, 0.5));
        assertEquals(3, QualityRules.harvestQuality(Variety.ECLIPSE, false, 0.49));
    }
}
