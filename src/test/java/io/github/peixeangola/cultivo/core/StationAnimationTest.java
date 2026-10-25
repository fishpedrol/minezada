package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Ritmo visual e sonoro das estações: curso da placa da prensa e batidas dos sons de trabalho. */
class StationAnimationTest {
    private static final float EPS = 1.0E-5F;

    /** Tempo do mundo em que a estação desta posição está no tick {@code tick} do próprio ciclo. */
    private static long timeAt(int tick, long seed, int period) {
        return Math.floorMod(tick - StationAnimation.phaseOffset(seed, period), (long) period);
    }

    @Test
    void pressStrokeGoesDownHoldsAndComesBack() {
        long seed = 12345L;
        int period = StationAnimation.PRESS_PERIOD_TICKS;
        assertEquals(40, period);
        assertEquals(0.0F, StationAnimation.pressOffsetPixels(timeAt(0, seed, period), seed), EPS, "começa em cima");
        assertEquals(-1.25F, StationAnimation.pressOffsetPixels(timeAt(11, seed, period), seed), EPS, "metade da descida");
        assertEquals(-2.5F, StationAnimation.pressOffsetPixels(timeAt(22, seed, period), seed), EPS, "embaixo no tick 22");
        assertEquals(-2.5F, StationAnimation.pressOffsetPixels(timeAt(29, seed, period), seed), EPS, "apertando");
        assertEquals(-1.25F, StationAnimation.pressOffsetPixels(timeAt(35, seed, period), seed), EPS, "metade da subida");
        assertEquals(0.0F, StationAnimation.pressOffsetPixels(timeAt(40, seed, period), seed), EPS, "volta em cima");
        for (double t = 0; t < 200; t += 0.37) {
            float offset = StationAnimation.pressOffsetPixels(t, seed);
            assertTrue(offset <= 0.0F && offset >= -2.5F, "fora do curso em " + t + ": " + offset);
            assertEquals(offset, StationAnimation.pressOffsetPixels(t + period, seed), EPS, "não repete o ciclo em " + t);
        }
    }

    @Test
    void workSoundsBeatOncePerCycle() {
        long seed = -987654321L;
        for (StationKind kind : StationKind.values()) {
            int period = switch (kind) {
                case DRYING -> 80;
                case SIEVING -> 30;
                case PRESSING -> 40;
            };
            int beats = 0;
            for (long t = 1000; t < 1000 + period; t++) {
                if (StationAnimation.playsWorkSound(kind, t, seed)) {
                    beats++;
                }
            }
            assertEquals(1, beats, kind.name());
        }
    }

    /** O aperto toca quando a placa animada está embaixo (o mesmo relógio do cliente). */
    @Test
    void pressSoundPlaysAtTheBottomOfTheStroke() {
        for (long seed : new long[] {0L, 1L, 77L, 123456789L, -42L}) {
            int beats = 0;
            for (long t = 0; t < 400; t++) {
                if (StationAnimation.playsWorkSound(StationKind.PRESSING, t, seed)) {
                    beats++;
                    assertEquals(-2.5F, StationAnimation.pressOffsetPixels(t, seed), EPS, "som fora do fundo em " + t);
                }
            }
            assertEquals(10, beats);
        }
    }

    /** Estações vizinhas não batem juntas: a fase vem da posição. */
    @Test
    void neighbouringStationsAreOutOfPhase() {
        long a = packed(10, 64, 10);
        long b = packed(11, 64, 10);
        assertNotEquals(StationAnimation.phaseOffset(a, 40), StationAnimation.phaseOffset(b, 40));
        for (long seed = -50; seed < 50; seed++) {
            int phase = StationAnimation.phaseOffset(seed, 40);
            assertTrue(phase >= 0 && phase < 40);
        }
    }

    /** Mesmo empacotamento do BlockPos#asLong do Minecraft (26 bits x, 12 y, 26 z). */
    private static long packed(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }
}
