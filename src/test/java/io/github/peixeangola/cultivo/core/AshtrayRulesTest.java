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

    /**
     * A lista que o AshtrayBlock#catchAsh percorre: 9 × 9 na horizontal e 5 de altura, 405 posições, cada uma uma vez,
     * com as bordas (4 na horizontal, 2 na vertical e os cantos) dentro e nada de fora.
     */
    @Test
    void searchOffsetsCoverExactlyTheRange() {
        assertEquals(405, AshtrayRules.SEARCH_OFFSETS.size());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int[] d : AshtrayRules.SEARCH_OFFSETS) {
            assertTrue(AshtrayRules.inRange(d[0], d[1], d[2]), java.util.Arrays.toString(d));
            assertTrue(seen.add(d[0] + "," + d[1] + "," + d[2]), "repetido: " + java.util.Arrays.toString(d));
        }
        for (String edge : java.util.List.of("4,0,0", "-4,0,0", "0,0,4", "0,0,-4", "0,2,0", "0,-2,0", "4,2,4", "-4,-2,-4", "4,-2,-4")) {
            assertTrue(seen.contains(edge), "borda fora da busca: " + edge);
        }
        for (String outside : java.util.List.of("5,0,0", "0,0,5", "0,3,0", "0,-3,0", "-5,0,0", "4,3,4")) {
            assertFalse(seen.contains(outside), "fora do alcance na busca: " + outside);
        }
    }

    /** Do mais perto para o mais longe: o cinzeiro mais próximo que não está cheio é o que pega a cinza. */
    @Test
    void searchGoesFromNearestToFarthest() {
        int[] first = AshtrayRules.SEARCH_OFFSETS.get(0);
        assertEquals(java.util.List.of(0, 0, 0), java.util.List.of(first[0], first[1], first[2]));
        int previous = 0;
        for (int[] d : AshtrayRules.SEARCH_OFFSETS) {
            int distance = AshtrayRules.distanceSquared(d);
            assertTrue(distance >= previous, "fora de ordem: " + java.util.Arrays.toString(d));
            previous = distance;
        }
        int[] last = AshtrayRules.SEARCH_OFFSETS.get(AshtrayRules.SEARCH_OFFSETS.size() - 1);
        assertEquals(4 * 4 + 2 * 2 + 4 * 4, AshtrayRules.distanceSquared(last), "o último é um canto");
    }

    @Test
    void comparatorIsThreePerAsh() {
        assertEquals(0, AshtrayRules.comparatorSignal(0));
        assertEquals(3, AshtrayRules.comparatorSignal(1));
        assertEquals(12, AshtrayRules.comparatorSignal(4));
    }
}
