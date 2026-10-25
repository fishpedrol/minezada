package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.peixeangola.cultivo.core.Variety.Rarity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SeedSourcesTest {
    @Test
    void coversMoreThanHalfOfVillageTypes() {
        assertEquals(5, SeedSources.VILLAGE_TYPES.size());
        assertEquals(4, SeedSources.COVERED_VILLAGE_TYPES.size());
        assertTrue(SeedSources.coversMajorityOfVillageTypes());
        assertFalse(SeedSources.COVERED_VILLAGE_TYPES.contains("snowy"));
    }

    @Test
    void chestTables() {
        assertEquals(List.of(
            "minecraft:chests/village/village_plains_house",
            "minecraft:chests/village/village_savanna_house",
            "minecraft:chests/village/village_taiga_house",
            "minecraft:chests/village/village_desert_house"), SeedSources.chestTables());
    }

    // ------------------------------------------------------------- baús: pesos por raridade

    @Test
    void rarityWeightsAndCounts() {
        assertEquals(new SeedSources.RarityLoot(4, 1, 3), SeedSources.chestLoot(Rarity.COMMON));
        assertEquals(new SeedSources.RarityLoot(2, 1, 2), SeedSources.chestLoot(Rarity.UNCOMMON));
        assertEquals(new SeedSources.RarityLoot(1, 1, 1), SeedSources.chestLoot(Rarity.RARE));
        assertEquals(4, SeedSources.rarityWeight(Rarity.COMMON));
        assertEquals(2, SeedSources.rarityWeight(Rarity.UNCOMMON));
        assertEquals(1, SeedSources.rarityWeight(Rarity.RARE));
    }

    @Test
    void chestDrawsFromAllFortyStrains() {
        assertEquals(40, SeedSources.CHEST_ENTRIES.size());
        for (int i = 0; i < SeedSources.CHEST_ENTRIES.size(); i++) {
            SeedSources.ChestEntry entry = SeedSources.CHEST_ENTRIES.get(i);
            assertEquals(Variety.values()[i], entry.variety(), "mesma ordem da tabela");
            SeedSources.RarityLoot expected = SeedSources.chestLoot(entry.variety().rarity());
            assertEquals(expected.weight(), entry.weight(), entry.variety().id());
            assertEquals(expected.minCount(), entry.minCount(), entry.variety().id());
            assertEquals(expected.maxCount(), entry.maxCount(), entry.variety().id());
        }
        assertEquals(new SeedSources.ChestEntry(Variety.BRISA, 4, 1, 3), SeedSources.CHEST_ENTRIES.get(0));
        assertEquals(new SeedSources.ChestEntry(Variety.AURORA, 2, 1, 2), SeedSources.CHEST_ENTRIES.get(1));
        assertEquals(new SeedSources.ChestEntry(Variety.ECLIPSE, 1, 1, 1), SeedSources.CHEST_ENTRIES.get(2));
        assertEquals(new SeedSources.ChestEntry(Variety.MANGA_GELADA, 4, 1, 3), SeedSources.CHEST_ENTRIES.get(3));
        assertEquals(new SeedSources.ChestEntry(Variety.BANANONA, 1, 1, 1), SeedSources.CHEST_ENTRIES.get(39));
    }

    /**
     * 25 comuns × 4 + 4 incomuns × 2 + 11 raras × 1 = 119; vazio também 119; total 238. Metade dos baús rola
     * semente e uma variedade comum aparece em 4/238 dos baús.
     */
    @Test
    void chestProbabilities() {
        assertEquals(119, SeedSources.CHEST_ENTRIES.stream().mapToInt(SeedSources.ChestEntry::weight).sum());
        assertEquals(119, SeedSources.CHEST_EMPTY_WEIGHT);
        assertEquals(238, SeedSources.chestTotalWeight());
        assertEquals(0.5, SeedSources.chestSeedChance(), 1e-12);
        assertEquals(4.0 / 238, SeedSources.chestChance(Variety.MANGA_GELADA), 1e-12);
        assertEquals(4.0 / 238, SeedSources.chestChance(Variety.BRISA), 1e-12);
        assertEquals(2.0 / 238, SeedSources.chestChance(Variety.AURORA), 1e-12);
        assertEquals(2.0 / 238, SeedSources.chestChance(Variety.GORILAO), 1e-12);
        assertEquals(1.0 / 238, SeedSources.chestChance(Variety.ECLIPSE), 1e-12);
        assertEquals(1.0 / 238, SeedSources.chestChance(Variety.BANANONA), 1e-12);
        Map<Rarity, Double> byRarity = new EnumMap<>(Rarity.class);
        for (Variety v : Variety.values()) {
            byRarity.merge(v.rarity(), SeedSources.chestChance(v), Double::sum);
        }
        assertEquals(100.0 / 238, byRarity.get(Rarity.COMMON), 1e-12);
        assertEquals(8.0 / 238, byRarity.get(Rarity.UNCOMMON), 1e-12);
        assertEquals(11.0 / 238, byRarity.get(Rarity.RARE), 1e-12);
    }

    // ------------------------------------------------------------- nativas: lavouras e planta silvestre

    @Test
    void farmAndWildKeepTheNativeNumbers() {
        assertEquals(List.of(
            new SeedSources.FarmRule(Variety.BRISA, 0.05F),
            new SeedSources.FarmRule(Variety.AURORA, 0.025F),
            new SeedSources.FarmRule(Variety.ECLIPSE, 0.01F)), SeedSources.FARM_RULES);
        assertEquals(List.of(
            new SeedSources.WildDrop(Variety.BRISA, 60),
            new SeedSources.WildDrop(Variety.AURORA, 30),
            new SeedSources.WildDrop(Variety.ECLIPSE, 10)), SeedSources.WILD_PLANT_DROPS);
        assertTrue(SeedSources.FARM_RULES.stream().allMatch(r -> r.variety().isNative()));
        assertTrue(SeedSources.WILD_PLANT_DROPS.stream().allMatch(d -> d.variety().isNative()));
    }

    @Test
    void wildPlantCoversJungleAndPlains() {
        assertEquals("#minecraft:is_jungle", SeedSources.WILD_JUNGLE_BIOMES);
        assertTrue(SeedSources.WILD_PLAINS_BIOMES.contains("minecraft:plains"));
    }

    // ------------------------------------------------------------- mascate

    @Test
    void traderNumbers() {
        assertEquals(2, SeedSources.TRADER_OFFERS);
        assertEquals(2, SeedSources.TRADER_MIN_COUNT);
        assertEquals(3, SeedSources.TRADER_MAX_COUNT);
        assertEquals(4, SeedSources.TRADER_MAX_USES);
        assertEquals(2, SeedSources.traderPrice(Rarity.COMMON));
        assertEquals(4, SeedSources.traderPrice(Rarity.UNCOMMON));
        assertEquals(8, SeedSources.traderPrice(Rarity.RARE));
        // Catálogo: 24 comuns × 4 + 3 incomuns × 2 + 10 raras × 1
        assertEquals(112, SeedSources.totalWeight(Variety.catalog()));
    }

    /** Gerador roteirizado: devolve os valores dados e guarda os limites pedidos. */
    private static final class Scripted {
        final Deque<Integer> values;
        final List<Integer> bounds = new ArrayList<>();

        Scripted(Integer... values) {
            this.values = new ArrayDeque<>(List.of(values));
        }

        int next(int bound) {
            bounds.add(bound);
            int value = values.removeFirst();
            assertTrue(value >= 0 && value < bound, value + " fora de [0, " + bound + ")");
            return value;
        }
    }

    @Test
    void traderOffersFollowScriptedRolls() {
        // 0 → 1ª do catálogo (Manga Gelada, comum); 1 → 3 sementes. Sem ela sobram 108 de peso: 107 → a última
        // (Bananona, rara); 0 → 2 sementes.
        Scripted rolls = new Scripted(0, 1, 107, 0);
        assertEquals(List.of(
            new SeedSources.TraderOffer(Variety.MANGA_GELADA, 3, 2),
            new SeedSources.TraderOffer(Variety.BANANONA, 2, 8)), SeedSources.traderOffers(rolls::next));
        assertEquals(List.of(112, 2, 108, 2), rolls.bounds);

        // 96 é a 1ª incomum (Gorilão: pesos 0–95 são as 24 comuns); depois 100 cai em Do-Si-Dó, a 1ª rara
        Scripted uncommon = new Scripted(96, 0, 100, 1);
        assertEquals(List.of(
            new SeedSources.TraderOffer(Variety.GORILAO, 2, 4),
            new SeedSources.TraderOffer(Variety.DO_SI_DO, 3, 8)), SeedSources.traderOffers(uncommon::next));
        assertEquals(List.of(112, 2, 110, 2), uncommon.bounds);
    }

    @Test
    void weightedPickBoundaries() {
        List<Variety> catalog = Variety.catalog();
        assertEquals(Variety.MANGA_GELADA, SeedSources.weightedPick(catalog, 3));
        assertEquals(Variety.AMNESIA_NEBLINA, SeedSources.weightedPick(catalog, 4));
        assertEquals(Variety.CONFEITEZ, SeedSources.weightedPick(catalog, 95));
        assertEquals(Variety.LIGEIRINHA, SeedSources.weightedPick(catalog, 101));
        assertEquals(Variety.BANANONA, SeedSources.weightedPick(catalog, 111));
        assertThrows(IllegalArgumentException.class, () -> SeedSources.weightedPick(catalog, 112));
        assertThrows(IllegalArgumentException.class, () -> SeedSources.weightedPick(catalog, -1));
    }

    @Test
    void traderOffersAreTwoDistinctCatalogStrainsPricedByRarity() {
        Random random = new Random(263L);
        for (int i = 0; i < 10_000; i++) {
            List<SeedSources.TraderOffer> offers = SeedSources.traderOffers(random::nextInt);
            assertEquals(2, offers.size());
            assertNotEquals(offers.get(0).variety(), offers.get(1).variety(), "variedade repetida");
            for (SeedSources.TraderOffer offer : offers) {
                assertFalse(offer.variety().isNative(), "nativa no mascate: " + offer.variety());
                assertTrue(offer.count() == 2 || offer.count() == 3, "quantidade " + offer.count());
                int price = switch (offer.variety().rarity()) {
                    case COMMON -> 2;
                    case UNCOMMON -> 4;
                    case RARE -> 8;
                };
                assertEquals(price, offer.emeralds(), offer.variety().id());
            }
        }
    }

    /** A 1ª oferta segue os pesos de raridade: comum 96/112, incomum 6/112, rara 10/112. */
    @Test
    void firstTraderOfferFollowsRarityWeights() {
        Random random = new Random(42L);
        int n = 200_000;
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        int twoSeeds = 0;
        for (int i = 0; i < n; i++) {
            SeedSources.TraderOffer first = SeedSources.traderOffers(random::nextInt).get(0);
            counts.merge(first.variety().rarity(), 1, Integer::sum);
            if (first.count() == 2) {
                twoSeeds++;
            }
        }
        assertEquals(96.0 / 112, counts.get(Rarity.COMMON) / (double) n, 0.01);
        assertEquals(6.0 / 112, counts.get(Rarity.UNCOMMON) / (double) n, 0.005);
        assertEquals(10.0 / 112, counts.get(Rarity.RARE) / (double) n, 0.005);
        assertEquals(0.5, twoSeeds / (double) n, 0.01, "2 ou 3 sementes, meio a meio");
    }
}
