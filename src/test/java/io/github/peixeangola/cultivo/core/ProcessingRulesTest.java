package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProcessingRulesTest {
    @Test
    void stationDurations() {
        assertEquals(600, StationKind.DRYING.defaultTicks());
        assertEquals(200, StationKind.SIEVING.defaultTicks());
        assertEquals(300, StationKind.PRESSING.defaultTicks());
    }

    @Test
    void sieveBonusFromQualityFour() {
        assertEquals(1, ProcessingRules.outputCount(StationKind.SIEVING, 1, 3));
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, 4));
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, 5));
        assertEquals(1, ProcessingRules.outputCount(StationKind.SIEVING, 1, null));
    }

    @Test
    void otherStationsHaveNoBonus() {
        assertEquals(1, ProcessingRules.outputCount(StationKind.DRYING, 1, 5));
        assertEquals(1, ProcessingRules.outputCount(StationKind.PRESSING, 1, 5));
    }

    /** Dichavador na peneira: +1 resina por ciclo, somado ao bônus de qualidade (plano 1.3). */
    @Test
    void grinderAddsOneResinOnTheSieveOnly() {
        assertEquals(1, ProcessingRules.GRINDER_BONUS);
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, 2, true));
        assertEquals(3, ProcessingRules.outputCount(StationKind.SIEVING, 1, 4, true));
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, null, true));
        assertEquals(1, ProcessingRules.outputCount(StationKind.PRESSING, 1, 5, true));
        assertEquals(1, ProcessingRules.outputCount(StationKind.DRYING, 1, 3, true));
    }

    @Test
    void withoutGrinderTheOldNumbersHold() {
        assertEquals(1, ProcessingRules.outputCount(StationKind.SIEVING, 1, 3, false));
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, 5, false));
        assertEquals(2, ProcessingRules.outputCount(StationKind.SIEVING, 1, 4, false));
        assertEquals(1, ProcessingRules.outputCount(StationKind.SIEVING, 1, null, false));
    }

    @Test
    void outputCompatibility() {
        assertTrue(ProcessingRules.outputAccepts(true, false, 0, 2, 64));
        assertTrue(ProcessingRules.outputAccepts(false, true, 62, 2, 64));
        assertFalse(ProcessingRules.outputAccepts(false, true, 63, 2, 64), "saída cheia");
        assertFalse(ProcessingRules.outputAccepts(false, false, 1, 1, 64), "outra qualidade/item");
    }

    @Test
    void progressCompletesExactlyOnTime() {
        int progress = 0;
        int completions = 0;
        for (int tick = 0; tick < 600; tick++) {
            ProcessingRules.Tick t = ProcessingRules.advance(progress, 600, true, true);
            progress = t.progress();
            if (t.completed()) {
                completions++;
            }
        }
        assertEquals(1, completions);
        assertEquals(0, progress);
    }

    @Test
    void blockedOutputFreezesProgress() {
        ProcessingRules.Tick t = ProcessingRules.advance(150, 200, true, false);
        assertEquals(150, t.progress());
        assertFalse(t.completed());
        for (int i = 0; i < 1000; i++) {
            t = ProcessingRules.advance(t.progress(), 200, true, false);
        }
        assertEquals(150, t.progress());
        assertFalse(t.completed());
    }

    @Test
    void missingRecipeResets() {
        ProcessingRules.Tick t = ProcessingRules.advance(150, 200, false, true);
        assertEquals(0, t.progress());
        assertFalse(t.completed());
    }
}
