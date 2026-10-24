package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.peixeangola.cultivo.core.RollingRules.Refusal;
import org.junit.jupiter.api.Test;

/** Bandeja de enrolar (plano 1.4): 2 flores iguais + 3 folhas de seda → 3 baseados. */
class RollingRulesTest {
    @Test
    void numbersFromThePlan() {
        assertEquals(2, RollingRules.FLOWERS_PER_ROLL);
        assertEquals(3, RollingRules.PAPERS_PER_ROLL);
        assertEquals(3, RollingRules.JOINTS_PER_ROLL);
    }

    /** 1,5 baseado por flor na bandeja, contra 1 por flor na bancada. */
    @Test
    void trayYieldsOneAndAHalfJointsPerFlower() {
        assertEquals(1.5D, (double) RollingRules.JOINTS_PER_ROLL / RollingRules.FLOWERS_PER_ROLL);
    }

    @Test
    void enoughFlowersAndPapersRoll() {
        assertEquals(Refusal.NONE, RollingRules.check(2, 3));
        assertEquals(Refusal.NONE, RollingRules.check(64, 64));
    }

    @Test
    void oneFlowerInTheStackIsRefused() {
        assertEquals(Refusal.NEEDS_FLOWERS, RollingRules.check(1, 3));
        assertEquals(Refusal.NEEDS_FLOWERS, RollingRules.check(0, 64));
    }

    @Test
    void twoPapersAreRefused() {
        assertEquals(Refusal.NEEDS_PAPERS, RollingRules.check(2, 2));
        assertEquals(Refusal.NEEDS_PAPERS, RollingRules.check(10, 0));
    }

    /** As flores são conferidas antes das folhas: faltando as duas coisas, o aviso é das flores. */
    @Test
    void flowersAreCheckedFirst() {
        assertEquals(Refusal.NEEDS_FLOWERS, RollingRules.check(1, 0));
    }

    @Test
    void refusalMessageKeys() {
        assertNull(Refusal.NONE.messageKey());
        assertEquals("message.cultivo.tray_need_flowers", Refusal.NEEDS_FLOWERS.messageKey());
        assertEquals("message.cultivo.tray_need_papers", Refusal.NEEDS_PAPERS.messageKey());
    }
}
