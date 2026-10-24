package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Cinzeiro (plano 1.6): cinza 0–4 das sessões por perto, farinha de osso quando cheio. */
class AshtrayRulesTest {
    @Test
    void numbersFromThePlan() {
        assertEquals(4, AshtrayRules.MAX_ASH);
        assertEquals(4, AshtrayRules.RADIUS_XZ);
        assertEquals(2, AshtrayRules.RADIUS_Y);
        assertEquals(1, AshtrayRules.BONE_MEAL_PER_EMPTYING);
    }

    @Test
    void ashGoesUpByOneAndStopsAtFour() {
        assertEquals(1, AshtrayRules.next(0));
        assertEquals(4, AshtrayRules.next(3));
        assertEquals(4, AshtrayRules.next(4), "cheio continua cheio");
    }

    @Test
    void fullOnlyAtFour() {
        assertFalse(AshtrayRules.isFull(0));
        assertFalse(AshtrayRules.isFull(3));
        assertTrue(AshtrayRules.isFull(4));
    }

    @Test
    void rangeIsFourAcrossAndTwoUpOrDown() {
        assertTrue(AshtrayRules.inRange(0, 0, 0));
        assertTrue(AshtrayRules.inRange(2, 0, 0), "o cinzeiro a 2 blocos do GameTest");
        assertTrue(AshtrayRules.inRange(4, 2, -4));
        assertTrue(AshtrayRules.inRange(-4, -2, 4));
        assertFalse(AshtrayRules.inRange(5, 0, 0));
        assertFalse(AshtrayRules.inRange(6, 0, 0), "o cinzeiro a 6 blocos do GameTest");
        assertFalse(AshtrayRules.inRange(0, 3, 0));
        assertFalse(AshtrayRules.inRange(0, -3, 0));
        assertFalse(AshtrayRules.inRange(0, 0, -5));
    }

    /** 9 × 9 na horizontal e 5 de altura: 405 posições por sessão. */
    @Test
    void searchVolumeIs405() {
        assertEquals(405, AshtrayRules.searchVolume());
    }

    @Test
    void comparatorIsThreePerAsh() {
        assertEquals(0, AshtrayRules.comparatorSignal(0));
        assertEquals(3, AshtrayRules.comparatorSignal(1));
        assertEquals(12, AshtrayRules.comparatorSignal(4));
    }
}
