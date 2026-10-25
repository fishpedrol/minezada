package io.github.peixeangola.cultivo.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
     * Artigos de fumo do MVP. Cada um tem exatamente um papel: cachimbo, chillum, bong e narguilé fumam
     * ({@link SmokeDevice}); o isqueiro tem durabilidade; os ingredientes entram em receitas
     * ({@link #INGREDIENT_ARTICLES}); o dichavador melhora a peneira ({@link #UPGRADE_ARTICLES}); e bandeja, pote e
     * cinzeiro viram blocos ({@link #PLACEABLE_ARTICLES}). Nenhum é só decorativo.
     */
    public static final List<String> ARTICLES = List.of(
        "bong", "pipe", "hookah", "chillum", "rolling_paper", "filter_tip",
        "grinder", "lighter", "rolling_tray", "jar", "ashtray");

    public static final String LIGHTER = "lighter";
    public static final String ROLLING_PAPER = "rolling_paper";
    public static final String FILTER_TIP = "filter_tip";
    public static final String GRINDER = "grinder";
    public static final String ROLLING_TRAY = "rolling_tray";
    public static final String JAR = "jar";
    public static final String ASHTRAY = "ashtray";
    public static final String BONG = "bong";

    /**
     * Artigos que são ingrediente de receita: a folha de seda entra no baseado (e na bandeja) e a piteira no baseado
     * com piteira. Mostram a dica tooltip.cultivo.&lt;id&gt;.
     */
    public static final List<String> INGREDIENT_ARTICLES = List.of(ROLLING_PAPER, FILTER_TIP);

    /** Artigos que se instalam numa estação: o dichavador na peneira (+1 resina por ciclo). */
    public static final List<String> UPGRADE_ARTICLES = List.of(GRINDER);

    /**
     * Artigos que viram bloco ao serem colocados (o item é um BlockItem com o mesmo ID e mantém o ícone plano, como o
     * vaso de flor vanilla): bandeja de enrolar, pote de cura e cinzeiro.
     */
    public static final List<String> PLACEABLE_ARTICLES = List.of(ROLLING_TRAY, JAR, ASHTRAY);

    /**
     * Aparelhos de fumo que também vão para o chão (pedido do dono de 25/10/2026): o bong. O item continua fumando com o
     * clique direito; agachado, o clique num bloco põe um bloco com o mesmo ID, que devolve o item ao quebrar.
     */
    public static final List<String> PLACED_DEVICES = List.of(BONG);

    /** Estufinha (pedido do dono de 25/10/2026): bloco de 2 de altura com um pé de planta dentro ({@link GrowTentRules}). */
    public static final String GROW_TENT = "grow_tent";

    /**
     * Blocos de cultivo craftáveis, fora dos artigos de fumo do MVP: a estufinha. O item mantém o ícone plano
     * (textures/item/&lt;id&gt;), como os artigos colocáveis.
     */
    public static final List<String> CULTIVATION_BLOCKS = List.of(GROW_TENT);

    /** Metades de um bloco de 2 de altura, na ordem do Minecraft (DoubleBlockHalf: UPPER, LOWER). */
    public static final List<String> DOUBLE_BLOCK_HALVES = List.of("upper", "lower");

    /** Efeitos próprios (textura em textures/mob_effect/&lt;id&gt;.png, 18×18). */
    public static final List<String> EFFECTS = List.of("high");

    /**
     * Eventos de som cultivo:&lt;id&gt; (registrados em ModSounds, definidos em assets/cultivo/sounds.json pelo gerador, a
     * partir da seção "sounds" do manifesto). Não há áudio próprio: cada evento reaproveita um evento vanilla e tem
     * legenda subtitles.cultivo.&lt;id&gt;.
     */
    public static final List<String> SOUNDS = List.of(
        "smoke.inhale",
        "smoke.exhale",
        "bong.bubble",
        "hookah.bubble",
        "lighter.click",
        "drying_rack.rustle",
        "sieve.sift",
        "press.squeeze",
        "station.finish",
        "tray.roll",
        "jar.open",
        "jar.close",
        "jar.cured",
        "grinder.grind",
        "ashtray.ash",
        "ashtray.empty",
        "grow_tent.plant",
        "grow_tent.harvest");

    public static final List<String> STATIONS =
        Arrays.stream(StationKind.values()).map(StationKind::blockId).toList();

    /** Valores da propriedade facing horizontal, na ordem do Minecraft. */
    public static final List<String> HORIZONTAL_FACINGS = List.of("north", "south", "west", "east");
    /** Valores de uma propriedade booleana no blockstate JSON. */
    public static final List<String> BOOLEAN_VALUES = List.of("true", "false");

    /**
     * Propriedades do blockstate das estações e seus valores, na ordem do Minecraft (espelho de
     * ProcessingStationBlock.FACING e CONTENTS). O teste dos modelos multipart confere que todo "when" usa só as
     * propriedades de {@link #stateProperties(String)}.
     */
    public static final Map<String, List<String>> STATION_STATE_PROPERTIES = Map.of(
        "facing", HORIZONTAL_FACINGS,
        "contents", Arrays.stream(StationVisualRules.Contents.values()).map(StationVisualRules.Contents::id).toList());

    /** Propriedade booleana da peneira com o dichavador instalado (SieveBlock.GRINDER). */
    public static final String GRINDER_PROPERTY = "grinder";

    /**
     * Estações do Serene Seasons (mod opcional de terceiros) em que as lavouras do Cultivo são férteis: as mesmas do
     * trigo no Serene Seasons 26.1.2.0.7 (verão e outono). O gerador escreve as tags sereneseasons:*_crops.
     */
    public static final List<String> SERENE_SEASONS_CROP_SEASONS = List.of("summer", "autumn");

    /** Nomes dos GameTests; cada um tem data/cultivo/test_instance/&lt;nome&gt;.json. */
    public static final List<String> GAME_TESTS = List.of(
        "outdoor_growth",
        "grow_tent_harvest",
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
        "villager_replants_mod_crop",
        "catalog_strain_grows_and_drops",
        "catalog_joint_carries_strain",
        "wandering_trader_catalog_seeds",
        "creative_tabs_split",
        "station_visual_state",
        "joint_filter_recipe",
        "smoke_filtered_joint",
        "sieve_grinder",
        "tray_rolls_joints",
        "jar_cures_flowers",
        "ashtray_collects_ash",
        "placeable_articles_drop_self",
        "sound_events_registered",
        "smoke_use_effects_run",
        "station_effects_run",
        "station_stalled_visual",
        "station_hold_click_keeps_input",
        "tray_rolls_from_off_hand",
        "interactions_swing_once",
        "full_inventory_drops_handouts",
        "creative_keeps_held_stacks",
        "jar_reload_resumes",
        "client_prediction_matches_server",
        "sieve_grinder_shape",
        "ashtray_search_edges",
        "block_states_match_ids",
        "tooltips_show_profile_and_marks",
        "serene_seasons_compat",
        "grow_tent_places_two_halves",
        "grow_tent_plants_and_grows",
        "grow_tent_bone_meal",
        "glass_gives_no_bonus",
        "grow_tent_breaks_either_half",
        "grow_tent_creative_break",
        "grow_tent_persists",
        "grow_tent_adventure_plant",
        "grow_tent_explosion",
        "bong_placed_on_ground");

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

    /**
     * Lavouras, estações, planta silvestre, artigos colocáveis, a estufinha e o bong no chão (estes três com item de
     * ícone plano).
     */
    public static List<String> allBlocks() {
        return Stream.of(cropBlocks(), blocksWithItems(), PLACEABLE_ARTICLES, CULTIVATION_BLOCKS, PLACED_DEVICES)
            .flatMap(List::stream).toList();
    }

    /** Idades da lavoura no blockstate (CropBlock.AGE, 0–7, como o trigo). */
    public static final List<String> CROP_AGES = IntStream.rangeClosed(0, 7).mapToObj(String::valueOf).toList();

    /**
     * Propriedades do blockstate de cada bloco do mod, com os valores como aparecem no JSON (espelho dos blocos Java):
     * lavouras age de 0 a 7; estações facing e contents, a peneira também grinder; bandeja e bong facing; pote filled; cinzeiro
     * ash de 0 a {@link AshtrayRules#MAX_ASH}; estufinha half e facing; a planta silvestre não tem nenhuma. Os testes dos modelos conferem que
     * todo "when" e toda variante usam só estas propriedades e valores, e o GameTest block_states_match_ids confere o
     * espelho contra a StateDefinition registrada de cada bloco.
     */
    public static Map<String, List<String>> stateProperties(String blockId) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (cropBlocks().contains(blockId)) {
            out.put("age", CROP_AGES);
        }
        if (STATIONS.contains(blockId)) {
            out.put("facing", HORIZONTAL_FACINGS);
            out.put("contents", STATION_STATE_PROPERTIES.get("contents"));
            if (blockId.equals(StationKind.SIEVING.blockId())) {
                out.put(GRINDER_PROPERTY, BOOLEAN_VALUES);
            }
        }
        switch (blockId) {
            case ROLLING_TRAY, BONG -> out.put("facing", HORIZONTAL_FACINGS);
            case JAR -> out.put("filled", BOOLEAN_VALUES);
            case ASHTRAY -> out.put("ash",
                IntStream.rangeClosed(0, AshtrayRules.MAX_ASH).mapToObj(String::valueOf).toList());
            case GROW_TENT -> {
                out.put("half", DOUBLE_BLOCK_HALVES);
                out.put("facing", HORIZONTAL_FACINGS);
            }
            default -> { }
        }
        return Collections.unmodifiableMap(out);
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
        ids.addAll(CULTIVATION_BLOCKS);
        return List.copyOf(ids);
    }

    public static List<String> allItems() {
        return Stream.concat(flatItems().stream(), blocksWithItems().stream()).toList();
    }

    /**
     * Itens da aba "Cultivo", em ordem: estações, estufinha, planta silvestre, processados (resina, haxixe, baseado) e
     * artigos. Sementes e flores ficam na aba "Cultivo: Genéticas".
     */
    public static List<String> mainTabItems() {
        List<String> ids = new ArrayList<>(STATIONS);
        ids.addAll(CULTIVATION_BLOCKS);
        ids.add(WILD_PLANT);
        ids.addAll(List.of(RESIN, HASH, JOINT));
        ids.addAll(ARTICLES);
        return List.copyOf(ids);
    }

    /**
     * Itens da aba "Cultivo: Genéticas": semente, flor fresca e flor seca de cada variedade (3 por variedade, então
     * cada linha de 9 espaços mostra 3 variedades), agrupadas por origem e depois raridade
     * ({@link Variety#byOriginThenRarity()}).
     */
    public static List<String> strainTabItems() {
        List<String> ids = new ArrayList<>();
        for (Variety v : Variety.byOriginThenRarity()) {
            ids.add(v.seedsId());
            ids.add(v.freshFlowerId());
            ids.add(v.driedFlowerId());
        }
        return List.copyOf(ids);
    }
}
