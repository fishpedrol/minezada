package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Regras puras da animação de fumar (só visual): passos da 3ª pessoa, subida suave da 1ª e pose dos braços. */
class SmokeAnimationTest {
    private static final float EPS = 1.0E-5F;

    @Test
    void thirdPersonRisesAndLowersOneStepPerTick() {
        // Baseado: 32 ticks de uso, 5 passos de subida e 5 de descida
        float[] expected = {0.0F, 0.2F, 0.4F, 0.6F, 0.8F, 1.0F, 1.0F};
        for (int elapsed = 0; elapsed < expected.length; elapsed++) {
            assertEquals(expected[elapsed], SmokeAnimation.thirdPersonRaise(elapsed, 32), EPS, "tick " + elapsed);
        }
        assertEquals(1.0F, SmokeAnimation.thirdPersonRaise(27, 32), EPS, "restam 5");
        assertEquals(0.8F, SmokeAnimation.thirdPersonRaise(28, 32), EPS, "restam 4");
        assertEquals(0.6F, SmokeAnimation.thirdPersonRaise(29, 32), EPS);
        assertEquals(0.4F, SmokeAnimation.thirdPersonRaise(30, 32), EPS);
        assertEquals(0.2F, SmokeAnimation.thirdPersonRaise(31, 32), EPS);
        assertEquals(0.0F, SmokeAnimation.thirdPersonRaise(32, 32), EPS, "acabou");
        // Narguilé: 48 ticks
        assertEquals(1.0F, SmokeAnimation.thirdPersonRaise(43, 48), EPS);
        assertEquals(0.8F, SmokeAnimation.thirdPersonRaise(44, 48), EPS);
        assertEquals(5, SmokeAnimation.KEYFRAMES);
    }

    @Test
    void firstPersonRisesSmoothlyHoldsAndLowers() {
        assertEquals(0.0F, SmokeAnimation.firstPersonRaise(0.0F, 32), EPS);
        assertEquals(0.5F, SmokeAnimation.firstPersonRaise(3.0F, 32), EPS, "metade da subida de 6 ticks");
        assertEquals(1.0F, SmokeAnimation.firstPersonRaise(6.0F, 32), EPS);
        assertEquals(1.0F, SmokeAnimation.firstPersonRaise(16.0F, 32), EPS);
        assertEquals(1.0F, SmokeAnimation.firstPersonRaise(27.0F, 32), EPS, "faltam 5: começa a descer");
        assertEquals(0.5F, SmokeAnimation.firstPersonRaise(29.5F, 32), EPS);
        assertEquals(0.0F, SmokeAnimation.firstPersonRaise(32.0F, 32), EPS);
        float previous = -1.0F;
        for (float t = 0.0F; t <= 6.0F; t += 0.25F) {
            float r = SmokeAnimation.firstPersonRaise(t, 40);
            assertTrue(r >= previous, "a subida não volta: " + t);
            previous = r;
        }
        // Aceleração suave: começa devagar
        assertTrue(SmokeAnimation.firstPersonRaise(0.5F, 32) < 0.5F / 6.0F);
    }

    @Test
    void breathingHasAPeriodOf16Ticks() {
        assertEquals(0.0F, SmokeAnimation.breath(0.0F), EPS);
        assertEquals(1.0F, SmokeAnimation.breath(4.0F), EPS);
        assertEquals(0.0F, SmokeAnimation.breath(8.0F), 1.0E-4F);
        assertEquals(-1.0F, SmokeAnimation.breath(12.0F), EPS);
        assertEquals(0.0F, SmokeAnimation.inhaleProgress(6.0F, 32), EPS);
        assertEquals(1.0F, SmokeAnimation.inhaleProgress(27.0F, 32), EPS);
    }

    /** Em r = 0 a matriz é a identidade: o aparelho começa exatamente onde estava na mão, sem salto. */
    @Test
    void firstPersonMatrixStartsAtIdentity() {
        float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        for (SmokeDevice device : SmokeDevice.values()) {
            for (boolean left : new boolean[] {false, true}) {
                assertArrayEquals(identity, SmokeAnimation.firstPersonMatrix(device, 0.0F, device.useTicks(), left), 1.0E-5F,
                    device + (left ? " esquerda" : " direita"));
            }
        }
    }

    /**
     * Na boca, a matriz leva o centro do modelo (que a exibição de segurar põe na translação de segurar) até a
     * translação da boca: baseado no meio da tragada (tick 16, respiração 0) → (-8, 3, 2,5)/16 blocos.
     */
    @Test
    void firstPersonMatrixReachesTheMouth() {
        float[] m = SmokeAnimation.firstPersonMatrix(SmokeDevice.JOINT, 16.0F, 32, false);
        // centro na mão: translação de segurar do baseado (0, 5, 0)/16
        float[] center = apply(m, 0.0F, 5.0F / 16.0F, 0.0F);
        assertEquals(-8.0F / 16.0F, center[0], 1.0E-5F);
        assertEquals(3.0F / 16.0F, center[1], 1.0E-5F);
        assertEquals(2.5F / 16.0F, center[2], 1.0E-5F);
        // Mão esquerda: o x espelha
        float[] left = apply(SmokeAnimation.firstPersonMatrix(SmokeDevice.JOINT, 16.0F, 32, true), 0.0F, 5.0F / 16.0F, 0.0F);
        assertEquals(8.0F / 16.0F, left[0], 1.0E-5F);
        assertEquals(3.0F / 16.0F, left[1], 1.0E-5F);
        // Bong (40 ticks) no meio: (-9, 1,5, -1)/16 a partir de (-1, 5, -2)/16
        float[] bong = apply(SmokeAnimation.firstPersonMatrix(SmokeDevice.BONG, 16.0F, 40, false), -1.0F / 16.0F, 5.0F / 16.0F,
            -2.0F / 16.0F);
        assertEquals(-9.0F / 16.0F, bong[0], 1.0E-5F);
        assertEquals(1.5F / 16.0F, bong[1], 1.0E-5F);
        assertEquals(-1.0F / 16.0F, bong[2], 1.0E-5F);
    }

    @Test
    void armsRiseFromHoldingToTheMouth() {
        SmokeAnimation.Arm raised = SmokeAnimation.pose(SmokeDevice.JOINT).raised();
        assertEquals(-1.25F, raised.x(), EPS);
        assertEquals(-0.7F, raised.y(), EPS);
        float base = SmokeAnimation.ITEM_ARM_X;
        assertArrayEquals(new float[] {base, 0.0F}, SmokeAnimation.armAngles(raised, 0.0F, base, 0.3F, 0.2F, 1.0F, false), EPS);
        // Na boca, cabeça reta e sem respiração: a pose do manifesto
        assertArrayEquals(new float[] {-1.25F, -0.7F}, SmokeAnimation.armAngles(raised, 1.0F, base, 0.0F, 0.0F, 0.0F, false), EPS);
        // Braço esquerdo: o giro espelha
        assertArrayEquals(new float[] {-1.25F, 0.7F}, SmokeAnimation.armAngles(raised, 1.0F, base, 0.0F, 0.0F, 0.0F, true), EPS);
        // Acompanha metade do olhar (até 0,8 rad) e todo o giro da cabeça; a respiração soma até 0,04 rad
        assertArrayEquals(new float[] {-1.25F + 0.2F + 0.04F, -0.7F + 0.3F},
            SmokeAnimation.armAngles(raised, 1.0F, base, 0.4F, 0.3F, 1.0F, false), EPS);
        assertArrayEquals(new float[] {-1.25F - 0.4F, -0.7F},
            SmokeAnimation.armAngles(raised, 1.0F, base, -1.5F, 0.0F, 0.0F, false), EPS);
        // Metade da subida
        assertArrayEquals(new float[] {(base - 1.25F) / 2.0F, -0.35F},
            SmokeAnimation.armAngles(raised, 0.5F, base, 0.0F, 0.0F, 0.0F, false), EPS);
    }

    @Test
    void bongAndHookahUseBothHands() {
        assertTrue(SmokeAnimation.twoHanded(SmokeDevice.BONG));
        assertTrue(SmokeAnimation.twoHanded(SmokeDevice.HOOKAH));
        for (SmokeDevice device : new SmokeDevice[] {SmokeDevice.JOINT, SmokeDevice.PIPE, SmokeDevice.CHILLUM}) {
            assertFalse(SmokeAnimation.twoHanded(device), device.name());
            assertNull(SmokeAnimation.pose(device).support(), device.name());
        }
        assertEquals(-0.75F, SmokeAnimation.pose(SmokeDevice.BONG).support().x(), EPS);
        assertEquals(0.35F, SmokeAnimation.pose(SmokeDevice.BONG).support().y(), EPS);
    }

    /** Aplica a matriz coluna-maior a um ponto. */
    private static float[] apply(float[] m, float x, float y, float z) {
        return new float[] {
            m[0] * x + m[4] * y + m[8] * z + m[12],
            m[1] * x + m[5] * y + m[9] * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14],
        };
    }
}
