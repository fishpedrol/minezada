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

    /**
     * Artigos de fumo do MVP. Cachimbo, chillum, bong e narguilé fumam ({@link SmokeDevice}); o isqueiro tem
     * durabilidade; os ingredientes têm dica própria; os demais são decorativos.
     */
    public static final List<String> ARTICLES = List.of(
        "bong", "pipe", "hookah", "chillum", "rolling_paper", "filter_tip",
        "grinder", "lighter", "rolling_tray", "jar", "ashtray");

    public static final String LIGHTER = "lighter";

    /**
     * Artigos que são ingrediente de receita (a folha de seda entra no baseado). Mostram a dica
     * tooltip.cultivo.&lt;id&gt; em vez de "Item decorativo".
     */
    public static final List<String> INGREDIENT_ARTICLES = List.of("rolling_paper");

    /** Artigos sem uso próprio, só decoração/coleção (dica "Item decorativo"). */
    public static final List<String> DECORATIVE_ARTICLES = List.of(
        "filter_tip", "grinder", "rolling_tray", "jar", "ashtray");

    /** Efeitos próprios (textura em textures/mob_effect/&lt;id&gt;.png, 18×18). */
    public static final List<String> EFFECTS = List.of("high");

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
        "article_recipes",
        "joint_recipe_copies_quality",
        "smoke_joint_effects",
        "smoke_requires_lighter_and_fuel",
        "smoke_pipe_uses_fuel",
        "smoke_bong_hash_stronger",
        "smoke_stacking_and_cap",
        "smoke_resmoke_after_heavy",
        "smoke_at_cap_refused",
        "smoke_hash_multiplier",
        "smoke_offhand_fuel_first",
        "smoke_lighter_breaks",
        "smoke_creative_consumes_nothing",
        "smoke_joint_without_strain",
        "smoke_refusal_messages",
        "joint_strain_persists",
        "hash_keeps_strain",
        "smoke_devices_registered",
        "smoke_keeps_potion_effects",
        "smoke_infinite_high_refused",
        "station_block_interaction",
        "station_real_ticker",
        "station_reload_resumes",
        "smoke_refused_at_finish_explains",
        "villager_replants_mod_crop");

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
