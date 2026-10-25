package io.github.peixeangola.cultivo.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
     * @param components componentes gravados no resultado (só transmute), com o valor de cada um em JSON: por exemplo
     *     {@code "cultivo:strain" → "\"brisa\""} no baseado e {@code "cultivo:filtered" → "{}"} na piteira
     */
    public record Recipe(String id, Kind kind, String result, int count, List<String> pattern, Map<Character, Key> key,
                         List<Key> ingredients, Map<String, String> components) {
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

    /** Artigos de fumo (receitas de itens vanilla). */
    private static final List<Recipe> ARTICLES = List.of(
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
        shaped("ashtray", 1, List.of("T T", "TTT"), Map.of('T', TERRACOTTA)));

    /** Estações de processamento. */
    private static final List<Recipe> STATIONS = List.of(
        shaped("drying_rack", 1, List.of("SSS", "T T", "SSS"), Map.of('S', STICK, 'T', STRING)),
        shaped("sieve", 1, List.of("PTP", "P P"), Map.of('P', PLANKS, 'T', STRING)),
        shaped("press", 1, List.of("III", " S ", "PPP"), Map.of('I', IRON, 'S', STICK, 'P', PLANKS)));

    /**
     * Estufinha: lã roxa (o tecido), ametista (a lente roxa), tocha (a lâmpada) e vaso de flor (o vaso da planta). Sem
     * vidro, como o dono pediu.
     */
    private static final List<Recipe> CULTIVATION = List.of(
        shaped("grow_tent", 1, List.of("WAW", "WLW", "WPW"), Map.of(
            'W', Key.item("minecraft:purple_wool"), 'A', Key.item("minecraft:amethyst_shard"),
            'L', Key.item("minecraft:torch"), 'P', Key.item("minecraft:flower_pot"))));

    /**
     * Baseado com piteira: transmute do baseado (qualquer variedade e qualidade, que passam adiante porque o transmute
     * copia os componentes da entrada) com a piteira, gravando cultivo:filtered. Um baseado que já tem piteira não
     * casa: o TransmuteRecipe do 26.3 recusa um resultado igual à entrada.
     */
    public static final Recipe JOINT_FILTERED = new Recipe("joint_filtered", Kind.TRANSMUTE, "joint", 1, List.of(),
        Map.of(), List.of(Key.item("cultivo:joint"), Key.item("cultivo:filter_tip")), Map.of("cultivo:filtered", "{}"));

    /** Artigos, um baseado por variedade (todas as da tabela {@link Variety}), a piteira, as três estações e a estufinha. */
    public static final List<Recipe> ALL = Stream.of(
            ARTICLES.stream(),
            Arrays.stream(Variety.values()).map(ArticleRecipes::joint),
            Stream.of(JOINT_FILTERED),
            STATIONS.stream(),
            CULTIVATION.stream())
        .flatMap(recipes -> recipes)
        .toList();

    private ArticleRecipes() {}

    private static Recipe shaped(String result, int count, List<String> pattern, Map<Character, Key> key) {
        return new Recipe(result, Kind.SHAPED, result, count, pattern, key, List.of(), Map.of());
    }

    private static Recipe shapeless(String result, int count, List<Key> ingredients) {
        return new Recipe(result, Kind.SHAPELESS, result, count, List.of(), Map.of(), ingredients, Map.of());
    }

    /**
     * Baseado: folha de seda + flor seca de uma variedade. É um {@code crafting_transmute}, então a qualidade da
     * flor passa para o baseado, e a variedade fica gravada no componente cultivo:strain. Uma receita por variedade:
     * o transmute do 26.3 só copia o patch de componentes da entrada ({@code ItemStack#getComponentsPatch}), então
     * um componente padrão do item da flor não chegaria ao baseado e uma receita única com tag perderia a variedade.
     */
    private static Recipe joint(Variety variety) {
        Key flower = Key.item("cultivo:" + variety.driedFlowerId());
        return new Recipe("joint_" + variety.id(), Kind.TRANSMUTE, "joint", 1, List.of(), Map.of(),
            List.of(flower, Key.item("cultivo:rolling_paper")), Map.of("cultivo:strain", "\"" + variety.id() + "\""));
    }

    public static Recipe byId(String id) {
        return ALL.stream().filter(r -> r.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(id));
    }
}
