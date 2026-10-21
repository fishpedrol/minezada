package io.github.peixeangola.cultivo.core;

import java.util.List;

/**
 * Fontes de sementes no mundo, como constantes conferidas por testes contra os JSONs de dados.
 * <ul>
 *   <li>Baús das casas de 4 dos 5 tipos de aldeia do 26.3 (snowy fica de fora).</li>
 *   <li>Lavoura das aldeias de planície ({@code minecraft:farm_plains}): trigo vira crop do mod.</li>
 *   <li>Planta silvestre ({@code cultivo:wild_plant}) em selvas (sem aldeia no 26.3) e em plains/meadow.</li>
 * </ul>
 */
public final class SeedSources {
    /** Todos os tipos de aldeia existentes no Minecraft 26.3. */
    public static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");

    /** Tipos de aldeia cujos baús de casa recebem sementes. */
    public static final List<String> COVERED_VILLAGE_TYPES = List.of("plains", "savanna", "taiga", "desert");

    public static final String INJECT_TABLE = "cultivo:inject/village_seeds";
    public static final String INJECT_POOL_NAME = "cultivo_village_seeds";
    public static final int CHEST_ROLLS = 1;
    public static final int CHEST_EMPTY_WEIGHT = 50;

    public record ChestEntry(Variety variety, int weight, int minCount, int maxCount) {}

    public static final List<ChestEntry> CHEST_ENTRIES = List.of(
        new ChestEntry(Variety.BRISA, 30, 1, 3),
        new ChestEntry(Variety.AURORA, 14, 1, 2),
        new ChestEntry(Variety.ECLIPSE, 6, 1, 1));

    public static final String FARM_PROCESSOR_LIST = "minecraft:farm_plains";

    public record FarmRule(Variety variety, float probability) {}

    /** Aplicadas antes das regras vanilla, sobre {@code minecraft:wheat}. */
    public static final List<FarmRule> FARM_RULES = List.of(
        new FarmRule(Variety.BRISA, 0.05F),
        new FarmRule(Variety.AURORA, 0.025F),
        new FarmRule(Variety.ECLIPSE, 0.01F));

    public record WildDrop(Variety variety, int weight) {}

    public static final List<WildDrop> WILD_PLANT_DROPS = List.of(
        new WildDrop(Variety.BRISA, 60),
        new WildDrop(Variety.AURORA, 30),
        new WildDrop(Variety.ECLIPSE, 10));

    /** Biomas do campo silvestre: selva (equivalente, sem aldeia) e planície. */
    public static final String WILD_JUNGLE_BIOMES = "#minecraft:is_jungle";
    public static final List<String> WILD_PLAINS_BIOMES = List.of("minecraft:plains", "minecraft:meadow");

    private SeedSources() {}

    public static List<String> chestTables() {
        return COVERED_VILLAGE_TYPES.stream()
            .map(type -> "minecraft:chests/village/village_" + type + "_house")
            .toList();
    }

    public static int chestTotalWeight() {
        return CHEST_EMPTY_WEIGHT + CHEST_ENTRIES.stream().mapToInt(ChestEntry::weight).sum();
    }

    /** Probabilidade de a rolagem injetada em um baú dar a variedade. */
    public static double chestChance(Variety variety) {
        return CHEST_ENTRIES.stream()
            .filter(e -> e.variety() == variety)
            .mapToInt(ChestEntry::weight)
            .sum() / (double) chestTotalWeight();
    }

    public static boolean coversMajorityOfVillageTypes() {
        return VILLAGE_TYPES.containsAll(COVERED_VILLAGE_TYPES)
            && COVERED_VILLAGE_TYPES.size() * 2 > VILLAGE_TYPES.size();
    }
}
