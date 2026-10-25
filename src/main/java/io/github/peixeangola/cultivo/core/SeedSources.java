package io.github.peixeangola.cultivo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Fontes de sementes no mundo, como constantes conferidas por testes contra os JSONs de dados.
 * <ul>
 *   <li>Baús das casas de 4 dos 5 tipos de aldeia do 26.3 (snowy fica de fora): todas as variedades, nativas e do
 *       catálogo, com peso pela raridade (comum 4, incomum 2, rara 1) e um peso vazio igual à soma desses pesos,
 *       para metade dos baús ter semente.</li>
 *   <li>Lavoura das aldeias de planície ({@code minecraft:farm_plains}): só as nativas viram crop do mod.</li>
 *   <li>Planta silvestre ({@code cultivo:wild_plant}) em selvas (sem aldeia no 26.3) e em plains/meadow: só nativas.</li>
 *   <li>Mascate (wandering trader): {@link #TRADER_OFFERS} ofertas de variedades do catálogo por mascate, sorteadas
 *       pelos mesmos pesos de raridade, com preço em esmeraldas pela raridade.</li>
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

    /** Peso de uma variedade no sorteio (baús e mascate) e quantas sementes ela rende num baú. */
    public record RarityLoot(int weight, int minCount, int maxCount) {}

    /** Comum: peso 4, 1–3 sementes; incomum: peso 2, 1–2; rara: peso 1, 1 semente. */
    public static RarityLoot chestLoot(Variety.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> new RarityLoot(4, 1, 3);
            case UNCOMMON -> new RarityLoot(2, 1, 2);
            case RARE -> new RarityLoot(1, 1, 1);
        };
    }

    public static int rarityWeight(Variety.Rarity rarity) {
        return chestLoot(rarity).weight();
    }

    public record ChestEntry(Variety variety, int weight, int minCount, int maxCount) {}

    /** Uma entrada por variedade, na ordem da tabela {@link Variety}. */
    public static final List<ChestEntry> CHEST_ENTRIES = Arrays.stream(Variety.values())
        .map(v -> {
            RarityLoot loot = chestLoot(v.rarity());
            return new ChestEntry(v, loot.weight(), loot.minCount(), loot.maxCount());
        })
        .toList();

    /** Peso da entrada vazia: igual à soma dos pesos das variedades, então 50% dos baús rolam uma semente. */
    public static final int CHEST_EMPTY_WEIGHT = CHEST_ENTRIES.stream().mapToInt(ChestEntry::weight).sum();

    public static final String FARM_PROCESSOR_LIST = "minecraft:farm_plains";

    public record FarmRule(Variety variety, float probability) {}

    /** Aplicadas antes das regras vanilla, sobre {@code minecraft:wheat}. Só as nativas. */
    public static final List<FarmRule> FARM_RULES = List.of(
        new FarmRule(Variety.BRISA, 0.05F),
        new FarmRule(Variety.AURORA, 0.025F),
        new FarmRule(Variety.ECLIPSE, 0.01F));

    public record WildDrop(Variety variety, int weight) {}

    /** Sementes da planta silvestre. Só as nativas. */
    public static final List<WildDrop> WILD_PLANT_DROPS = List.of(
        new WildDrop(Variety.BRISA, 60),
        new WildDrop(Variety.AURORA, 30),
        new WildDrop(Variety.ECLIPSE, 10));

    /** Biomas do campo silvestre: selva (equivalente, sem aldeia) e planície. */
    public static final String WILD_JUNGLE_BIOMES = "#minecraft:is_jungle";
    public static final List<String> WILD_PLAINS_BIOMES = List.of("minecraft:plains", "minecraft:meadow");

    /** Ofertas de sementes do catálogo acrescentadas a cada mascate novo (variedades diferentes entre si). */
    public static final int TRADER_OFFERS = 2;
    public static final int TRADER_MIN_COUNT = 2;
    public static final int TRADER_MAX_COUNT = 3;
    /** Usos de cada oferta antes de esgotar (o padrão das trocas data-driven do 26.3). */
    public static final int TRADER_MAX_USES = 4;
    public static final int TRADER_XP = 1;
    /** Desconto por reputação, como as trocas de sementes vanilla do mascate. */
    public static final float TRADER_PRICE_MULTIPLIER = 0.05F;

    /** Uma oferta: {@code count} sementes da variedade por {@code emeralds} esmeraldas. */
    public record TraderOffer(Variety variety, int count, int emeralds) {}

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

    /** Probabilidade de a rolagem injetada dar alguma semente (qualquer variedade). */
    public static double chestSeedChance() {
        return (chestTotalWeight() - CHEST_EMPTY_WEIGHT) / (double) chestTotalWeight();
    }

    public static boolean coversMajorityOfVillageTypes() {
        return VILLAGE_TYPES.containsAll(COVERED_VILLAGE_TYPES)
            && COVERED_VILLAGE_TYPES.size() * 2 > VILLAGE_TYPES.size();
    }

    /** Preço em esmeraldas no mascate: comum 2, incomum 4, rara 8. */
    public static int traderPrice(Variety.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 2;
            case UNCOMMON -> 4;
            case RARE -> 8;
        };
    }

    /**
     * Sorteia as ofertas de um mascate entre as variedades do catálogo, sem repetir variedade: cada sorteio escolhe
     * pelos pesos de raridade entre as que sobraram e depois a quantidade, de {@link #TRADER_MIN_COUNT} a
     * {@link #TRADER_MAX_COUNT}.
     *
     * @param nextInt gerador do servidor: recebe um limite exclusivo e devolve um inteiro em [0, limite)
     */
    public static List<TraderOffer> traderOffers(IntUnaryOperator nextInt) {
        List<Variety> pool = new ArrayList<>(Variety.catalog());
        List<TraderOffer> offers = new ArrayList<>();
        while (offers.size() < TRADER_OFFERS && !pool.isEmpty()) {
            Variety pick = weightedPick(pool, nextInt.applyAsInt(totalWeight(pool)));
            pool.remove(pick);
            int count = TRADER_MIN_COUNT + nextInt.applyAsInt(TRADER_MAX_COUNT - TRADER_MIN_COUNT + 1);
            offers.add(new TraderOffer(pick, count, traderPrice(pick.rarity())));
        }
        return List.copyOf(offers);
    }

    /** Soma dos pesos de raridade das variedades. */
    public static int totalWeight(List<Variety> pool) {
        return pool.stream().mapToInt(v -> rarityWeight(v.rarity())).sum();
    }

    /** A variedade que ocupa a posição {@code roll} (de 0 à soma dos pesos − 1) nos pesos acumulados, na ordem dada. */
    public static Variety weightedPick(List<Variety> pool, int roll) {
        if (roll < 0) {
            throw new IllegalArgumentException("sorteio negativo: " + roll);
        }
        int accumulated = 0;
        for (Variety variety : pool) {
            accumulated += rarityWeight(variety.rarity());
            if (roll < accumulated) {
                return variety;
            }
        }
        throw new IllegalArgumentException("sorteio fora da soma dos pesos: " + roll);
    }
}
