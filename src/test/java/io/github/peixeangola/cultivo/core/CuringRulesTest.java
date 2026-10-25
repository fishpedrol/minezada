package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pote: cura das flores secas e sinal do comparador (plano 1.5). */
class CuringRulesTest {
    @Test
    void cureTakes2400Ticks() {
        assertEquals(2400, CuringRules.CURE_TICKS);
    }

    @Test
    void curedQualityGoesUpByOneUpToFive() {
        assertEquals(3, CuringRules.curedQuality(2));
        assertEquals(4, CuringRules.curedQuality(3));
        assertEquals(5, CuringRules.curedQuality(4));
        assertEquals(5, CuringRules.curedQuality(5));
        assertEquals(2, CuringRules.curedQuality(1));
    }

    /** Sem qualidade conta como 2, como ao fumar: cura para 3. */
    @Test
    void missingQualityCountsAsTwo() {
        assertEquals(3, CuringRules.curedQuality(null));
    }

    /** O outdoor (2–3) vai a 3–4 e a estufinha (4–5) a 5: a estufinha continua melhor. */
    @Test
    void growTentStaysAheadAfterCuring() {
        int bestOutdoor = CuringRules.curedQuality(3);
        int worstTent = CuringRules.curedQuality(4);
        assertEquals(4, bestOutdoor);
        assertEquals(5, worstTent);
        assertTrue(worstTent > bestOutdoor);
    }

    @Test
    void onlyUncuredDriedFlowersCure() {
        assertTrue(CuringRules.canCure(true, false));
        assertFalse(CuringRules.canCure(true, true), "já curada não cura de novo");
        assertFalse(CuringRules.canCure(false, false), "resina, haxixe e baseado não curam");
        assertFalse(CuringRules.canCure(false, true));
    }

    @Test
    void cureCompletesOnTheTick2400() {
        int progress = 0;
        int completions = 0;
        for (int tick = 1; tick <= 2400; tick++) {
            CuringRules.Tick t = CuringRules.advance(progress, true);
            progress = t.progress();
            if (t.completed()) {
                completions++;
                assertEquals(2400, tick, "completou no tick " + tick);
            }
        }
        assertEquals(1, completions);
        assertEquals(0, progress);
        CuringRules.Tick last = CuringRules.advance(2399, true);
        assertTrue(last.completed());
        assertEquals(0, last.progress());
        CuringRules.Tick before = CuringRules.advance(2398, true);
        assertFalse(before.completed());
        assertEquals(2399, before.progress());
    }

    @Test
    void notCurableResetsTheCounter() {
        CuringRules.Tick t = CuringRules.advance(1500, false);
        assertEquals(0, t.progress());
        assertFalse(t.completed());
    }

    /** Comparador: ⌈15 × quantidade / pilha máxima⌉, vazio = 0. */
    @Test
    void comparatorSignal() {
        assertEquals(0, CuringRules.comparatorSignal(0, 64));
        assertEquals(1, CuringRules.comparatorSignal(1, 64));
        assertEquals(3, CuringRules.comparatorSignal(10, 64));
        assertEquals(8, CuringRules.comparatorSignal(32, 64));
        assertEquals(15, CuringRules.comparatorSignal(64, 64));
        assertEquals(1, CuringRules.comparatorSignal(1, 16), "baseado empilha 16");
        assertEquals(15, CuringRules.comparatorSignal(16, 16));
    }
}
