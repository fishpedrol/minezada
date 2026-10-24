package io.github.peixeangola.cultivo.core;

import java.util.List;
import java.util.Map;

/**
 * Receitas de crafting dos artigos e estações, como dados puros. Os JSONs em data/cultivo/recipe são
 * conferidos contra esta tabela (JUnit) e o GameTest usa {@link Key#sampleItem()} para montar a grade.
 */
public final class ArticleRecipes {
    /** Ingrediente do JSON ("minecraft:x" ou "#tag") e um item concreto que o satisfaz. */
    public record Key(String ingredient, String sampleItem) {
        static Key item(String id) {
            return new Key(id, id);
        }
    }

    public enum Kind { SHAPED, SHAPELESS, TRANSMUTE }

    /**
     * @param id nome do arquivo em data/cultivo/recipe (sem .json)
     * @param pattern linhas da receita shaped; vazio nos outros tipos
     * @param key mapa símbolo → ingrediente (shaped)
     * @param ingredients ingredientes (shapeless) ou [entrada, material] (transmute)
     * @param strain variedade gravada no resultado (só transmute do baseado), ou null
     */
    public record Recipe(String id, Kind kind, String result, int count, List<String> pattern, Map<Character, Key> key,
                         List<Key> ingredients, String strain) {
        public boolean shaped() {
            return kind == Kind.SHAPED;
        }
    }

    private static final Key GLASS = Key.item("minecraft:glass");
    private static final Key IRON = Key.item("minecraft:iron_ingot");
    private static final Key NUGGET = Key.item("minecraft:iron_nugget");
    private static final Key STRING = Key.item("minecraft:string");
    private static final Key PAPER = Key.item("minecraft:paper");
    private static final Key TERRACOTTA = Key.item("minecraft:terracotta");
    private static final Key FLINT = Key.item("minecraft:flint");
    private static final Key STICK = Key.item("minecraft:stick");
    private static final Key PLANKS = new Key("#minecraft:planks", "minecraft:oak_planks");
    private static final Key SLABS = new Key("#minecraft:wooden_slabs", "minecraft:oak_slab");

    public static final List<Recipe> ALL = List.of(
        shaped("bong", 1, List.of(" G ", " G ", "GWG"), Map.of('G', GLASS, 'W', Key.item("minecraft:water_bucket"))),
        shaped("pipe", 1, List.of("B  ", " BB"), Map.of('B', Key.item("minecraft:brick"))),
        shaped("hookah", 1, List.of(" I ", "SGS", "IGI"), Map.of('I', IRON, 'S', STRING, 'G', GLASS)),
        shaped("chillum", 1, List.of("T", "T"), Map.of('T', TERRACOTTA)),
        shapeless("rolling_paper", 4, List.of(PAPER, Key.item("minecraft:sugar_cane"))),
        shapeless("filter_tip", 4, List.of(PAPER, STRING)),
        shaped("grinder", 1, List.of("I", "F", "I"), Map.of('I', IRON, 'F', FLINT)),
        shaped("lighter", 1, List.of("F", "N", "N"), Map.of('F', FLINT, 'N', NUGGET)),
        shaped("rolling_tray", 1, List.of("N N", "NNN"), Map.of('N', NUGGET)),
        shaped("jar", 1, List.of("S", "G", "G"), Map.of('S', SLABS, 'G', GLASS)),
        shaped("ashtray", 1, List.of("T T", "TTT"), Map.of('T', TERRACOTTA)),
        joint(Variety.BRISA),
        joint(Variety.AURORA),
        joint(Variety.ECLIPSE),
        shaped("drying_rack", 1, List.of("SSS", "T T", "SSS"), Map.of('S', STICK, 'T', STRING)),
        shaped("sieve", 1, List.of("PTP", "P P"), Map.of('P', PLANKS, 'T', STRING)),
        shaped("press", 1, List.of("III", " S ", "PPP"), Map.of('I', IRON, 'S', STICK, 'P', PLANKS)));

    private ArticleRecipes() {}

    private static Recipe shaped(String result, int count, List<String> pattern, Map<Character, Key> key) {
        return new Recipe(result, Kind.SHAPED, result, count, pattern, key, List.of(), null);
    }

    private static Recipe shapeless(String result, int count, List<Key> ingredients) {
        return new Recipe(result, Kind.SHAPELESS, result, count, List.of(), Map.of(), ingredients, null);
    }

    /**
     * Baseado: folha de seda + flor seca de uma variedade. É um {@code crafting_transmute}, então a qualidade da
     * flor passa para o baseado, e a variedade fica gravada no componente cultivo:strain.
     */
    private static Recipe joint(Variety variety) {
        Key flower = Key.item("cultivo:" + variety.driedFlowerId());
        return new Recipe("joint_" + variety.id(), Kind.TRANSMUTE, "joint", 1, List.of(), Map.of(),
            List.of(flower, Key.item("cultivo:rolling_paper")), variety.id());
    }

    public static Recipe byId(String id) {
        return ALL.stream().filter(r -> r.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(id));
    }
}
