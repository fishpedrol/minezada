package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ArticleRecipes;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Os JSONs de dados batem com as constantes puras (receitas, fontes de sementes, estações, testes). */
class DataConsistencyTest {
    private static Set<String> values(Path tag) {
        Set<String> out = new HashSet<>();
        Resources.json(tag).getAsJsonArray("values").forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private static JsonArray modifiers(JsonObject entry) {
        JsonElement m = entry.get("modifier");
        if (m == null) {
            return new JsonArray();
        }
        if (m.isJsonArray()) {
            return m.getAsJsonArray();
        }
        JsonArray single = new JsonArray();
        single.add(m);
        return single;
    }

    // ------------------------------------------------------------- receitas

    @Test
    void everyArticleStationAndJointHasCraftingRecipe() {
        Set<String> expected = new HashSet<>(ContentIds.ARTICLES);
        expected.addAll(ContentIds.STATIONS);
        expected.add(ContentIds.JOINT);
        Set<String> actual = new HashSet<>();
        ArticleRecipes.ALL.forEach(r -> actual.add(r.result()));
        assertEquals(expected, actual);
    }

    @Test
    void craftingJsonMatchesArticleRecipes() {
        for (ArticleRecipes.Recipe r : ArticleRecipes.ALL) {
            JsonObject json = Resources.json(Resources.data("cultivo", "recipe/" + r.result() + ".json"));
            JsonObject result = json.getAsJsonObject("result");
            assertEquals("cultivo:" + r.result(), result.get("id").getAsString());
            assertEquals(r.count(), result.get("count").getAsInt(), r.result());
            if (r.shaped()) {
                assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
                List<String> pattern = new ArrayList<>();
                json.getAsJsonArray("pattern").forEach(e -> pattern.add(e.getAsString()));
                assertEquals(r.pattern(), pattern, r.result());
                JsonObject key = json.getAsJsonObject("key");
                assertEquals(r.key().size(), key.size(), r.result());
                for (Map.Entry<Character, ArticleRecipes.Key> k : r.key().entrySet()) {
                    assertEquals(k.getValue().ingredient(), key.get(String.valueOf(k.getKey())).getAsString(), r.result());
                    assertTrue(pattern.stream().anyMatch(row -> row.indexOf(k.getKey()) >= 0), "símbolo sem uso");
                }
            } else {
                assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());
                List<String> ingredients = new ArrayList<>();
                json.getAsJsonArray("ingredients").forEach(e -> ingredients.add(e.getAsString()));
                assertEquals(r.ingredients().stream().map(ArticleRecipes.Key::ingredient).toList(), ingredients);
            }
        }
    }

    @Test
    void jointUsesRollingPaperAndDriedFlowerTag() {
        ArticleRecipes.Recipe joint = ArticleRecipes.forResult("joint");
        assertEquals(List.of("cultivo:rolling_paper", "#cultivo:dried_flowers"),
            joint.ingredients().stream().map(ArticleRecipes.Key::ingredient).toList());
        Set<String> dried = values(Resources.data("cultivo", "tags/item/dried_flowers.json"));
        for (Variety v : Variety.values()) {
            assertTrue(dried.contains("cultivo:" + v.driedFlowerId()));
        }
    }

    @Test
    void sampleItemsSatisfyTagIngredients() {
        for (ArticleRecipes.Recipe r : ArticleRecipes.ALL) {
            List<ArticleRecipes.Key> keys = new ArrayList<>(r.key().values());
            keys.addAll(r.ingredients());
            for (ArticleRecipes.Key k : keys) {
                if (!k.ingredient().startsWith("#")) {
                    assertEquals(k.ingredient(), k.sampleItem());
                }
            }
        }
    }

    @Test
    void processingRecipesMatchStations() {
        for (Variety v : Variety.values()) {
            JsonObject drying = Resources.json(Resources.data("cultivo", "recipe/drying/" + v.driedFlowerId() + ".json"));
            assertEquals("cultivo:" + StationKind.DRYING.recipeTypeId(), drying.get("type").getAsString());
            assertEquals("cultivo:" + v.freshFlowerId(), drying.get("ingredient").getAsString());
            assertEquals("cultivo:" + v.driedFlowerId(), drying.getAsJsonObject("result").get("id").getAsString());
            assertEquals(StationKind.DRYING.defaultTicks(), drying.get("processing_time").getAsInt());
        }
        JsonObject sieve = Resources.json(Resources.data("cultivo", "recipe/sieving/resin.json"));
        assertEquals("cultivo:sieving", sieve.get("type").getAsString());
        assertEquals("#cultivo:dried_flowers", sieve.get("ingredient").getAsString());
        assertEquals("cultivo:resin", sieve.getAsJsonObject("result").get("id").getAsString());
        assertEquals(StationKind.SIEVING.defaultTicks(), sieve.get("processing_time").getAsInt());

        JsonObject press = Resources.json(Resources.data("cultivo", "recipe/pressing/hash.json"));
        assertEquals("cultivo:pressing", press.get("type").getAsString());
        assertEquals("cultivo:resin", press.get("ingredient").getAsString());
        assertEquals(2, press.get("input_count").getAsInt());
        assertEquals("cultivo:hash", press.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, press.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(StationKind.PRESSING.defaultTicks(), press.get("processing_time").getAsInt());
    }

    // ------------------------------------------------------------- fontes de sementes

    @Test
    void chestInjectionTableMatchesSeedSources() {
        JsonObject table = Resources.json(Resources.data("cultivo", "loot_table/inject/village_seeds.json"));
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(1, pools.size());
        JsonObject pool = pools.get(0).getAsJsonObject();
        assertEquals(SeedSources.CHEST_ROLLS, pool.get("rolls").getAsInt());
        JsonArray entries = pool.getAsJsonArray("entries");
        assertEquals(1 + SeedSources.CHEST_ENTRIES.size(), entries.size());
        JsonObject empty = entries.get(0).getAsJsonObject();
        assertEquals("minecraft:empty", empty.get("type").getAsString());
        assertEquals(SeedSources.CHEST_EMPTY_WEIGHT, empty.get("weight").getAsInt());
        for (int i = 0; i < SeedSources.CHEST_ENTRIES.size(); i++) {
            SeedSources.ChestEntry expected = SeedSources.CHEST_ENTRIES.get(i);
            JsonObject entry = entries.get(i + 1).getAsJsonObject();
            assertEquals("cultivo:" + expected.variety().seedsId(), entry.get("name").getAsString());
            assertEquals(expected.weight(), entry.get("weight").getAsInt());
            JsonObject setCount = modifiers(entry).get(0).getAsJsonObject();
            assertEquals("minecraft:set_count", setCount.get("type").getAsString());
            JsonObject count = setCount.getAsJsonObject("count");
            assertEquals(expected.minCount(), count.get("min").getAsInt());
            assertEquals(expected.maxCount(), count.get("max").getAsInt());
        }
    }

    @Test
    void farmPlainsAddsModRulesBeforeVanillaRules() {
        JsonObject list = Resources.json(Resources.GENERATED.resolve("data/minecraft/worldgen/processor_list/farm_plains.json"));
        JsonArray rules = list.getAsJsonArray("processors").get(0).getAsJsonObject().getAsJsonArray("rules");
        assertTrue(rules.size() > SeedSources.FARM_RULES.size(), "regras vanilla precisam continuar presentes");
        for (int i = 0; i < rules.size(); i++) {
            JsonObject rule = rules.get(i).getAsJsonObject();
            JsonObject input = rule.getAsJsonObject("input_predicate");
            assertEquals("minecraft:random_block_match", input.get("predicate_type").getAsString());
            assertEquals("minecraft:wheat", input.get("block").getAsString());
            String output = rule.get("output_state").getAsString();
            if (i < SeedSources.FARM_RULES.size()) {
                SeedSources.FarmRule expected = SeedSources.FARM_RULES.get(i);
                assertEquals("cultivo:" + expected.variety().cropId(), output);
                assertEquals(expected.probability(), input.get("probability").getAsFloat(), 1e-6);
            } else {
                assertTrue(output.startsWith("minecraft:"), "regra vanilla mantida: " + output);
            }
        }
    }

    @Test
    void wildPlantDropsMatchWeights() {
        JsonObject table = Resources.json(Resources.data("cultivo", "loot_table/blocks/wild_plant.json"));
        JsonArray entries = table.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(SeedSources.WILD_PLANT_DROPS.size(), entries.size());
        for (int i = 0; i < entries.size(); i++) {
            SeedSources.WildDrop expected = SeedSources.WILD_PLANT_DROPS.get(i);
            JsonObject entry = entries.get(i).getAsJsonObject();
            assertEquals("cultivo:" + expected.variety().seedsId(), entry.get("name").getAsString());
            assertEquals(expected.weight(), entry.get("weight").getAsInt());
        }
    }

    @Test
    void wildPlantBiomeModifiersCoverJungleAndPlains() {
        JsonObject jungle = Resources.json(Resources.data("cultivo", "forge/biome_modifier/wild_plant_jungle.json"));
        assertEquals("forge:add_features", jungle.get("type").getAsString());
        assertEquals(SeedSources.WILD_JUNGLE_BIOMES, jungle.get("biomes").getAsString());
        JsonObject plains = Resources.json(Resources.data("cultivo", "forge/biome_modifier/wild_plant_plains.json"));
        List<String> biomes = new ArrayList<>();
        plains.getAsJsonArray("biomes").forEach(e -> biomes.add(e.getAsString()));
        assertEquals(SeedSources.WILD_PLAINS_BIOMES, biomes);
        for (JsonObject modifier : List.of(jungle, plains)) {
            String placed = modifier.get("features").getAsString().substring("cultivo:".length());
            JsonObject placedFeature = Resources.json(Resources.data("cultivo", "worldgen/placed_feature/" + placed + ".json"));
            String feature = placedFeature.get("feature").getAsString().substring("cultivo:".length());
            assertTrue(Files.exists(Resources.data("cultivo", "worldgen/feature/" + feature + ".json")));
        }
    }

    // ------------------------------------------------------------- loot de colheita

    @Test
    void matureCropDropsFlowerWithHarvestQuality() {
        for (Variety v : Variety.values()) {
            JsonObject table = Resources.json(Resources.data("cultivo", "loot_table/blocks/" + v.cropId() + ".json"));
            JsonArray pools = table.getAsJsonArray("pools");
            JsonArray children = pools.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonArray("children");
            JsonObject flower = children.get(0).getAsJsonObject();
            assertEquals("cultivo:" + v.freshFlowerId(), flower.get("name").getAsString());
            JsonObject condition = flower.getAsJsonObject("condition");
            assertEquals("minecraft:match_block", condition.get("type").getAsString());
            assertEquals("cultivo:" + v.cropId(), condition.get("blocks").getAsString());
            assertEquals("7", condition.getAsJsonObject("state").get("age").getAsString());
            assertEquals("cultivo:set_harvest_quality", modifiers(flower).get(0).getAsJsonObject().get("type").getAsString());
            JsonObject seedFallback = children.get(1).getAsJsonObject();
            assertEquals("cultivo:" + v.seedsId(), seedFallback.get("name").getAsString());
            assertFalse(seedFallback.has("condition"), "imaturo dá 1 semente");
            JsonObject bonusPool = pools.get(1).getAsJsonObject();
            assertEquals("7", bonusPool.getAsJsonObject("condition").getAsJsonObject("state").get("age").getAsString());
            JsonObject bonusSeeds = bonusPool.getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("cultivo:" + v.seedsId(), bonusSeeds.get("name").getAsString());
            assertEquals("minecraft:apply_bonus", modifiers(bonusSeeds).get(0).getAsJsonObject().get("type").getAsString());
        }
    }

    @Test
    void everyBlockHasLootTable() {
        for (String block : ContentIds.allBlocks()) {
            JsonObject table = Resources.json(Resources.data("cultivo", "loot_table/blocks/" + block + ".json"));
            assertEquals("minecraft:block", table.get("type").getAsString());
        }
    }

    @Test
    void lootTablesUse263FieldNames() throws IOException {
        List<String> legacy = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Resources.data("cultivo", "loot_table"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                collectLegacyKeys(Resources.json(file), file.toString(), legacy);
            }
        }
        assertTrue(legacy.isEmpty(), "chaves pré-26.3: " + legacy);
    }

    private static void collectLegacyKeys(JsonElement element, String where, List<String> out) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
                // "condition" pode ser objeto ou ID registrado (string), como no short_grass.json vanilla
                if (Set.of("functions", "conditions", "function", "block_state_property").contains(e.getKey())
                    || (e.getValue().isJsonPrimitive() && e.getValue().getAsString().equals("minecraft:block_state_property"))) {
                    out.add(where + ":" + e.getKey());
                }
                collectLegacyKeys(e.getValue(), where, out);
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectLegacyKeys(child, where, out));
        }
    }

    // ------------------------------------------------------------- tags e testes

    @Test
    void cropTags() {
        Set<String> crops = new HashSet<>();
        for (Variety v : Variety.values()) {
            crops.add("cultivo:" + v.cropId());
        }
        for (String tag : List.of("crops", "maintains_farmland", "bee_growables")) {
            assertEquals(crops, values(Resources.GENERATED.resolve("data/minecraft/tags/block/" + tag + ".json")), tag);
        }
    }

    @Test
    void greenhouseGlassTagHasGlassAndPanes() {
        Set<String> glass = values(Resources.data("cultivo", "tags/block/greenhouse_glass.json"));
        assertTrue(glass.contains("minecraft:glass"));
        assertTrue(glass.contains("minecraft:glass_pane"));
        assertTrue(glass.contains("minecraft:lime_stained_glass"));
        assertTrue(glass.contains("minecraft:lime_stained_glass_pane"));
        assertEquals(35, glass.size(), "vidro, vidro fumê, painel, 16 tingidos e 16 painéis tingidos");
    }

    @Test
    void everyGameTestHasInstance() throws IOException {
        Set<String> files = new HashSet<>();
        try (Stream<Path> list = Files.list(Resources.data("cultivo", "test_instance"))) {
            list.forEach(p -> files.add(p.getFileName().toString().replace(".json", "")));
        }
        assertEquals(new HashSet<>(ContentIds.GAME_TESTS), files);
        for (String test : ContentIds.GAME_TESTS) {
            JsonObject json = Resources.json(Resources.data("cultivo", "test_instance/" + test + ".json"));
            assertEquals("minecraft:function", json.get("type").getAsString());
            assertEquals("cultivo:" + test, json.get("function").getAsString());
            assertEquals("forge:empty9x9x9", json.get("structure").getAsString());
        }
    }
}
