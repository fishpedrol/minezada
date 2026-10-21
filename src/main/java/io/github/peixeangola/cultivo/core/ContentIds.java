package io.github.peixeangola.cultivo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lista única de IDs de conteúdo (namespace cultivo). Os registros Java iteram estas listas e os testes de
 * completude conferem assets e traduções para cada ID.
 */
public final class ContentIds {
    public static final String MOD_ID = "cultivo";

    public static final String WILD_PLANT = "wild_plant";
    public static final String RESIN = "resin";
    public static final String HASH = "hash";
    public static final String JOINT = "joint";

    /** Artigos de fumo do MVP (itens decorativos, sem efeito de consumo). */
    public static final List<String> ARTICLES = List.of(
        "bong", "pipe", "hookah", "chillum", "rolling_paper", "filter_tip",
        "grinder", "lighter", "rolling_tray", "jar", "ashtray");

    public static final List<String> STATIONS =
        Arrays.stream(StationKind.values()).map(StationKind::blockId).toList();

    /** Nomes dos GameTests; cada um tem data/cultivo/test_instance/&lt;nome&gt;.json. */
    public static final List<String> GAME_TESTS = List.of(
        "outdoor_growth",
        "greenhouse_quality",
        "harvest_drops",
        "drying_station",
        "sieve_station",
        "press_station",
        "station_output_full",
        "station_persistence",
        "village_chest_seeds",
        "article_recipes");

    private ContentIds() {}

    public static List<String> cropBlocks() {
        return Arrays.stream(Variety.values()).map(Variety::cropId).toList();
    }

    /** Blocos com item próprio (modelo de bloco + item que herda dele). */
    public static List<String> blocksWithItems() {
        List<String> ids = new ArrayList<>(STATIONS);
        ids.add(WILD_PLANT);
        return List.copyOf(ids);
    }

    public static List<String> allBlocks() {
        return Stream.concat(cropBlocks().stream(), blocksWithItems().stream()).toList();
    }

    /** Itens que carregam o componente cultivo:quality. */
    public static List<String> qualityItems() {
        List<String> ids = new ArrayList<>();
        for (Variety v : Variety.values()) {
            ids.add(v.freshFlowerId());
            ids.add(v.driedFlowerId());
        }
        ids.add(RESIN);
        ids.add(HASH);
        return List.copyOf(ids);
    }

    /** Itens com textura própria em textures/item (não inclui itens de bloco). */
    public static List<String> flatItems() {
        List<String> ids = new ArrayList<>();
        for (Variety v : Variety.values()) {
            ids.add(v.seedsId());
        }
        ids.addAll(qualityItems());
        ids.add(JOINT);
        ids.addAll(ARTICLES);
        return List.copyOf(ids);
    }

    public static List<String> allItems() {
        return Stream.concat(flatItems().stream(), blocksWithItems().stream()).toList();
    }
}
