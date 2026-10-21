package io.github.peixeangola.cultivo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    @Test
    void chestWeightsFollowRarity() {
        assertEquals(100, SeedSources.chestTotalWeight());
        assertEquals(0.30, SeedSources.chestChance(Variety.BRISA), 1e-9);
        assertEquals(0.14, SeedSources.chestChance(Variety.AURORA), 1e-9);
        assertEquals(0.06, SeedSources.chestChance(Variety.ECLIPSE), 1e-9);
        assertTrue(SeedSources.chestChance(Variety.BRISA) > SeedSources.chestChance(Variety.AURORA));
        assertTrue(SeedSources.chestChance(Variety.AURORA) > SeedSources.chestChance(Variety.ECLIPSE));
    }

    @Test
    void farmAndWildWeightsFollowRarity() {
        List<SeedSources.FarmRule> farm = SeedSources.FARM_RULES;
        assertEquals(3, farm.size());
        assertTrue(farm.get(0).probability() > farm.get(1).probability());
        assertTrue(farm.get(1).probability() > farm.get(2).probability());
        List<SeedSources.WildDrop> wild = SeedSources.WILD_PLANT_DROPS;
        assertEquals(100, wild.stream().mapToInt(SeedSources.WildDrop::weight).sum());
        assertTrue(wild.get(0).weight() > wild.get(1).weight() && wild.get(1).weight() > wild.get(2).weight());
    }

    @Test
    void wildPlantCoversJungleAndPlains() {
        assertEquals("#minecraft:is_jungle", SeedSources.WILD_JUNGLE_BIOMES);
        assertTrue(SeedSources.WILD_PLAINS_BIOMES.contains("minecraft:plains"));
    }
}
