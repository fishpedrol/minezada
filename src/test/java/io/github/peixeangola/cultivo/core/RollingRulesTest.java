package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Regressão: 1 flor Aurora q3 na mão principal e 10 na secundária. A principal passa o clique (PASS) e a secundária
     * enrola, em vez de avisar "precisa de 2 flores iguais na mão" com uma mão capaz de enrolar.
     */
    @Test
    void mainHandWithoutFlowersLetsTheOffHandTry() {
        assertTrue(RollingRules.defersToOffHand(true, RollingRules.check(1, 5), true));
    }

    /** Sem flores secas na mão secundária, o aviso sai na principal (nenhuma das mãos enrola). */
    @Test
    void mainHandRefusesWhenTheOffHandHasNoDriedFlowers() {
        assertFalse(RollingRules.defersToOffHand(true, Refusal.NEEDS_FLOWERS, false));
    }

    /** Faltando folhas, a outra mão não ajuda (as folhas são as mesmas): o aviso das folhas sai já na principal. */
    @Test
    void missingPapersAreNeverDeferred() {
        assertFalse(RollingRules.defersToOffHand(true, Refusal.NEEDS_PAPERS, true));
    }

    /** A mão secundária é a última: ela enrola ou avisa, nunca passa adiante; a principal que enrola também não passa. */
    @Test
    void offHandAndRollingMainHandDecideThemselves() {
        assertFalse(RollingRules.defersToOffHand(false, Refusal.NEEDS_FLOWERS, true));
        assertFalse(RollingRules.defersToOffHand(true, Refusal.NONE, true));
    }

    @Test
    void refusalMessageKeys() {
        assertNull(Refusal.NONE.messageKey());
        assertEquals("message.cultivo.tray_need_flowers", Refusal.NEEDS_FLOWERS.messageKey());
        assertEquals("message.cultivo.tray_need_papers", Refusal.NEEDS_PAPERS.messageKey());
    }
}
