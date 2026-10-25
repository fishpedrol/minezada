package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.TooltipHints;
import io.github.peixeangola.cultivo.core.Variety;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Todo ID registrado tem blockstate, modelo, definição de item, textura 16x16 e tradução nas duas línguas; os modelos
 * 3D (estações e aparelhos na mão) só citam texturas que existem, ficam dentro dos limites do 26.3 e o multipart só
 * usa propriedades que o bloco tem.
 */
class AssetCompletenessTest {
    private static final List<String> LANGS = List.of("en_us", "pt_br");
    /** Pais vanilla aceitos e a variável de textura que cada um exige do filho. */
    private static final Map<String, List<String>> VANILLA_PARENTS = Map.of(
        "minecraft:block/crop", List.of("crop"),
        "minecraft:block/cross", List.of("cross"),
        "minecraft:item/generated", List.of("layer0"),
        "minecraft:block/block", List.of());
    /** Aparelhos de fumo e isqueiro: ícone plano no inventário e modelo 3D na mão. */
    private static final List<String> HAND_MODEL_ITEMS = List.of("bong", "hookah", "pipe", "chillum", "joint", "lighter");
    private static final List<String> FLAT_CONTEXTS = List.of("gui", "ground", "fixed", "on_shelf");
    /** Rotação y do modelo por facing (frente do modelo para o norte, como a fornalha vanilla). */
    private static final Map<String, Integer> FACING_Y = Map.of("north", 0, "east", 90, "south", 180, "west", 270);

    // ------------------------------------------------------------------ blockstates e modelos

    @Test
    void everyBlockHasBlockstateModelsAndTextures() {
        for (String block : ContentIds.allBlocks()) {
            List<String> models = blockstateModels(block);
            assertFalse(models.isEmpty(), block);
            models.forEach(AssetCompletenessTest::assertModelComplete);
        }
    }

    @Test
    void cropsMapEightAgesToEightStages() {
        for (Variety v : Variety.values()) {
            JsonObject variants = Resources.json(Resources.assets("blockstates/" + v.cropId() + ".json"))
                .getAsJsonObject("variants");
            assertEquals(8, variants.size(), v.id());
            Set<String> models = new HashSet<>();
            for (int age = 0; age <= 7; age++) {
                String model = variants.getAsJsonObject("age=" + age).get("model").getAsString();
                // A planta no campo é da família de arte: um estágio por idade, como o trigo
                assertEquals("cultivo:block/" + v.artFamily() + "_crop_stage" + age, model, v.id());
                models.add(model);
            }
            assertEquals(8, models.size(), v.id());
        }
    }

    @Test
    void stationBlockstatesRotateTheBaseForEachFacing() {
        // Convenção vanilla (fornalha): frente do modelo para o norte; leste y=90, sul y=180, oeste y=270
        for (String station : ContentIds.STATIONS) {
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + station + ".json")).getAsJsonArray("multipart");
            // Todos os estados do bloco (na peneira também grinder=true|false): toda parte que aparece gira com a base
            for (Map<String, String> state : allStates(ContentIds.stateProperties(station))) {
                int bases = 0;
                for (JsonElement part : multipart) {
                    JsonObject p = part.getAsJsonObject();
                    if (!matches(p.getAsJsonObject("when"), state)) {
                        continue;
                    }
                    JsonObject apply = p.getAsJsonObject("apply");
                    int y = apply.has("y") ? apply.get("y").getAsInt() : 0;
                    assertEquals(FACING_Y.get(state.get("facing")), y, station + " " + state + " " + apply);
                    if (apply.get("model").getAsString().equals("cultivo:block/" + station)) {
                        bases++;
                    }
                }
                assertEquals(1, bases, station + " " + state + ": a base aparece uma vez");
            }
        }
    }

    /**
     * Todo "when" dos blockstates multipart (estações, pote e cinzeiro) usa só propriedades e valores que o bloco tem
     * (ContentIds.stateProperties, espelho dos blocos Java).
     */
    @Test
    void multipartConditionsUseExistingProperties() {
        List<String> multipartBlocks = new ArrayList<>(ContentIds.STATIONS);
        multipartBlocks.addAll(List.of(ContentIds.JAR, ContentIds.ASHTRAY));
        for (String block : multipartBlocks) {
            Map<String, List<String>> properties = ContentIds.stateProperties(block);
            assertFalse(properties.isEmpty(), block + " sem propriedades no espelho");
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + block + ".json")).getAsJsonArray("multipart");
            Set<String> seenContents = new HashSet<>();
            for (JsonElement part : multipart) {
                JsonObject when = part.getAsJsonObject().getAsJsonObject("when");
                if (when == null) {
                    // Só a base dos artigos sem facing aparece sempre
                    assertFalse(ContentIds.STATIONS.contains(block), block + ": parte de estação sem when");
                    continue;
                }
                for (Map.Entry<String, JsonElement> term : when.entrySet()) {
                    List<String> allowed = properties.get(term.getKey());
                    assertTrue(allowed != null, block + ": propriedade inexistente " + term.getKey());
                    for (String value : term.getValue().getAsString().split("\\|")) {
                        String plain = value.startsWith("!") ? value.substring(1) : value;
                        assertTrue(allowed.contains(plain), block + ": " + term.getKey() + "=" + plain);
                        if (term.getKey().equals("contents")) {
                            seenContents.add(plain);
                        }
                    }
                }
            }
            if (ContentIds.STATIONS.contains(block)) {
                // Toda estação tem parte para carregada, trabalhando e pronta (vazia é a base sozinha, exceto na prensa)
                assertTrue(seenContents.containsAll(List.of("loaded", "working", "done")), block + " " + seenContents);
            }
        }
        assertEquals(Set.of("north", "south", "west", "east"), Set.copyOf(ContentIds.STATION_STATE_PROPERTIES.get("facing")));
        assertEquals(List.of("empty", "loaded", "working", "done"), ContentIds.STATION_STATE_PROPERTIES.get("contents"));
        assertEquals(List.of("true", "false"), ContentIds.stateProperties("sieve").get("grinder"));
        assertFalse(ContentIds.stateProperties("drying_rack").containsKey("grinder"), "só a peneira tem dichavador");
        assertEquals(List.of("0", "1", "2", "3", "4"), ContentIds.stateProperties("ashtray").get("ash"));
        assertEquals(List.of("true", "false"), ContentIds.stateProperties("jar").get("filled"));
        assertEquals(Set.of("facing"), ContentIds.stateProperties("rolling_tray").keySet());
    }

    /** O dichavador aparece na peneira só com grinder=true, uma vez, em todos os facings e conteúdos. */
    @Test
    void sieveShowsTheGrinderOnlyWhenInstalled() {
        JsonArray multipart = Resources.json(Resources.assets("blockstates/sieve.json")).getAsJsonArray("multipart");
        for (Map<String, String> state : allStates(ContentIds.stateProperties("sieve"))) {
            long grinders = appliedModels(multipart, state).stream().filter(m -> m.equals("cultivo:block/sieve_grinder")).count();
            assertEquals(state.get("grinder").equals("true") ? 1 : 0, grinders, state.toString());
        }
        JsonObject grinder = Resources.json(Resources.assets("models/block/sieve_grinder.json"));
        JsonArray from = grinder.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonArray("from");
        assertEquals(13.0F, from.get(1).getAsFloat(), "o dichavador fica em cima da moldura (y=13)");
    }

    /**
     * Estação parada (a receita não anda: 1 resina na prensa, saída bloqueada) aparece carregada e sem movimento: nenhuma
     * textura animada e, na prensa, a placa parada em cima (platen_up) em vez da resina apertada. Trabalhando, o
     * suporte e a peneira usam as texturas animadas.
     */
    @Test
    void loadedStationsShowTheInputWithoutMotion() {
        for (String station : ContentIds.STATIONS) {
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + station + ".json")).getAsJsonArray("multipart");
            Map<String, String> loaded = new HashMap<>(Map.of("facing", "north", "contents", "loaded"));
            Map<String, String> working = new HashMap<>(Map.of("facing", "north", "contents", "working"));
            if (ContentIds.stateProperties(station).containsKey("grinder")) {
                loaded.put("grinder", "false");
                working.put("grinder", "false");
            }
            List<String> loadedModels = appliedModels(multipart, loaded);
            assertEquals(List.of("cultivo:block/" + station, "cultivo:block/" + station + "_loaded"),
                loadedModels.stream().filter(m -> !m.endsWith("_platen_up")).toList(), station);
            for (String model : loadedModels) {
                assertFalse(usesAnimatedTexture(model), station + " carregada com textura animada em " + model);
            }
            if (!station.equals("press")) {
                assertTrue(appliedModels(multipart, working).stream().anyMatch(AssetCompletenessTest::usesAnimatedTexture),
                    station + " trabalhando sem animação");
            }
        }
        JsonArray press = Resources.json(Resources.assets("blockstates/press.json")).getAsJsonArray("multipart");
        List<String> pressLoaded = appliedModels(press, Map.of("facing", "north", "contents", "loaded"));
        assertTrue(pressLoaded.contains("cultivo:block/press_platen_up"), "prensa carregada sem a placa parada: " + pressLoaded);
        assertFalse(pressLoaded.contains("cultivo:block/press_squeeze"), "prensa carregada apertando: " + pressLoaded);
        assertFalse(pressLoaded.contains("cultivo:block/press_hash"), "prensa carregada com haxixe: " + pressLoaded);
    }

    /** Alguma textura do modelo (pela cadeia de pais do mod) tem .mcmeta de animação. */
    private static boolean usesAnimatedTexture(String modelId) {
        Map<String, String> textures = new HashMap<>();
        collectChain(modelId, textures, new ArrayList<>());
        return textures.values().stream().filter(t -> !t.startsWith("#"))
            .anyMatch(t -> Files.exists(Path.of(Resources.cultivoResource(t, "textures", ".png") + ".mcmeta")));
    }

    /** A bandeja é uma base só, girada por variante de facing (sem multipart). */
    @Test
    void rollingTrayVariantsRotateForEachFacing() {
        JsonObject variants = Resources.json(Resources.assets("blockstates/rolling_tray.json")).getAsJsonObject("variants");
        assertEquals(Set.of("facing=north", "facing=east", "facing=south", "facing=west"), variants.keySet());
        for (String facing : ContentIds.stateProperties("rolling_tray").get("facing")) {
            JsonObject v = variants.getAsJsonObject("facing=" + facing);
            assertEquals("cultivo:block/rolling_tray", v.get("model").getAsString());
            assertEquals(FACING_Y.get(facing), v.has("y") ? v.get("y").getAsInt() : 0, facing);
        }
    }

    /**
     * Estufinha: uma variante para cada um dos 8 estados (half × facing), com o modelo da metade e girada como as
     * estações; a metade de cima tem o painel de LEDs virado para baixo com light_emission 15 (do 26.3) e o mylar das
     * duas metades brilha. O item continua com o ícone plano.
     */
    @Test
    void growTentBlockstateCoversEveryState() {
        JsonObject variants = Resources.json(Resources.assets("blockstates/grow_tent.json")).getAsJsonObject("variants");
        Map<String, List<String>> properties = ContentIds.stateProperties(ContentIds.GROW_TENT);
        assertEquals(List.of("upper", "lower"), properties.get("half"));
        assertEquals(8, variants.size());
        for (Map<String, String> state : allStates(properties)) {
            JsonObject v = variants.getAsJsonObject("facing=" + state.get("facing") + ",half=" + state.get("half"));
            assertTrue(v != null, "sem variante para " + state);
            assertEquals("cultivo:block/grow_tent_" + state.get("half"), v.get("model").getAsString(), state.toString());
            assertEquals(FACING_Y.get(state.get("facing")), v.has("y") ? v.get("y").getAsInt() : 0, state.toString());
        }
        assertTrue(ledPanels("grow_tent_upper") >= 1, "a metade de cima sem o painel de LEDs aceso");
        assertEquals(0, ledPanels("grow_tent_lower"), "a metade de baixo não tem lâmpada");
        assertTrue(glowingMylar("grow_tent_lower") >= 3 && glowingMylar("grow_tent_upper") >= 4, "mylar sem brilho");
        JsonObject item = Resources.json(Resources.assets("models/item/grow_tent.json"));
        assertEquals("minecraft:item/generated", item.get("parent").getAsString());
        assertEquals("cultivo:item/grow_tent", item.getAsJsonObject("textures").get("layer0").getAsString());
    }

    /** Elementos com a face de baixo na textura da lâmpada e brilho 15 (o painel de LEDs). */
    private static long ledPanels(String model) {
        return emissive(model).stream().filter(el -> el.get("light_emission").getAsInt() == 15
            && el.getAsJsonObject("faces").has("down")
            && el.getAsJsonObject("faces").getAsJsonObject("down").get("texture").getAsString().equals("#lamp")).count();
    }

    /** Planos de mylar que brilham (as paredes e o teto por dentro). */
    private static long glowingMylar(String model) {
        return emissive(model).stream().filter(el -> el.getAsJsonObject("faces").entrySet().stream()
            .allMatch(f -> f.getValue().getAsJsonObject().get("texture").getAsString().equals("#mylar"))).count();
    }

    /** Elementos com light_emission, todos entre 1 e 15 (o CuboidModelElement do 26.3 recusa fora de 0–15); nenhum com "shade". */
    private static List<JsonObject> emissive(String model) {
        JsonArray elements = Resources.json(Resources.assets("models/block/" + model + ".json")).getAsJsonArray("elements");
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : elements) {
            JsonObject el = e.getAsJsonObject();
            // O 26.3 não tem a chave "shade" (só shade_direction_override): o deserializador a ignoraria em silêncio
            assertFalse(el.has("shade"), model + " com a chave shade, que o 26.3 não lê");
            if (el.has("light_emission")) {
                int glow = el.get("light_emission").getAsInt();
                assertTrue(glow >= 1 && glow <= 15, model + " light_emission " + glow);
                out.add(el);
            }
        }
        return out;
    }

    /** Pote: o conteúdo atrás do vidro só com filled=true. Cinzeiro: uma camada de cinza por nível, nada no 0. */
    @Test
    void jarAndAshtrayPartsFollowTheirState() {
        JsonArray jar = Resources.json(Resources.assets("blockstates/jar.json")).getAsJsonArray("multipart");
        assertEquals(List.of("cultivo:block/jar"), appliedModels(jar, Map.of("filled", "false")));
        assertEquals(List.of("cultivo:block/jar", "cultivo:block/jar_filled"), appliedModels(jar, Map.of("filled", "true")));
        JsonArray ashtray = Resources.json(Resources.assets("blockstates/ashtray.json")).getAsJsonArray("multipart");
        assertEquals(List.of("cultivo:block/ashtray"), appliedModels(ashtray, Map.of("ash", "0")));
        for (int ash = 1; ash <= 4; ash++) {
            assertEquals(List.of("cultivo:block/ashtray", "cultivo:block/ashtray_ash" + ash),
                appliedModels(ashtray, Map.of("ash", String.valueOf(ash))), "ash=" + ash);
        }
        // A cinza sobe 0,25 por nível a partir do fundo (0,5)
        for (int ash = 1; ash <= 4; ash++) {
            JsonObject layer = Resources.json(Resources.assets("models/block/ashtray_ash" + ash + ".json"))
                .getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals(0.5F + 0.25F * ash, layer.getAsJsonArray("to").get(1).getAsFloat(), "ash=" + ash);
        }
    }

    /**
     * Bandeja, pote e cinzeiro: o item continua com o ícone plano (item/generated com a própria textura, como o vaso
     * de flor), e o bloco tem modelo 3D próprio com textura de bloco.
     */
    @Test
    void placeableArticlesKeepFlatIconsAndHaveBlockModels() {
        for (String article : ContentIds.PLACEABLE_ARTICLES) {
            JsonObject def = Resources.json(Resources.assets("items/" + article + ".json")).getAsJsonObject("model");
            assertEquals("cultivo:item/" + article, def.get("model").getAsString(), article);
            JsonObject item = Resources.json(Resources.assets("models/item/" + article + ".json"));
            assertEquals("minecraft:item/generated", item.get("parent").getAsString(), article);
            assertEquals("cultivo:item/" + article, item.getAsJsonObject("textures").get("layer0").getAsString(), article);
            JsonObject block = Resources.json(Resources.assets("models/block/" + article + ".json"));
            assertEquals("minecraft:block/block", block.get("parent").getAsString(), article);
            assertFalse(block.getAsJsonArray("elements").isEmpty(), article);
            for (Map.Entry<String, JsonElement> t : block.getAsJsonObject("textures").entrySet()) {
                assertTrue(t.getValue().getAsString().startsWith("cultivo:block/"), article + " usa textura de item no bloco");
            }
        }
    }

    @Test
    void modelElementsStayWithinLimits() throws IOException {
        try (Stream<Path> files = Files.walk(Resources.assets("models"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonArray elements = Resources.json(file).getAsJsonArray("elements");
                if (elements == null) {
                    continue;
                }
                for (JsonElement e : elements) {
                    JsonObject el = e.getAsJsonObject();
                    for (String key : List.of("from", "to")) {
                        for (JsonElement c : el.getAsJsonArray(key)) {
                            float v = c.getAsFloat();
                            // CuboidModelElement do 26.3 recusa coordenadas fora de [-16, 32]
                            assertTrue(v >= -16.0F && v <= 32.0F, file + " " + key + " " + v);
                        }
                    }
                    JsonObject faces = el.getAsJsonObject("faces");
                    assertFalse(faces.isEmpty(), file + ": elemento sem faces");
                    for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                        assertTrue(Set.of("down", "up", "north", "south", "west", "east").contains(face.getKey()), file + " face " + face.getKey());
                        JsonObject f = face.getValue().getAsJsonObject();
                        assertTrue(f.get("texture").getAsString().startsWith("#"), file + ": textura de face sem #");
                        if (f.has("uv")) {
                            for (JsonElement c : f.getAsJsonArray("uv")) {
                                float v = c.getAsFloat();
                                assertTrue(v >= 0.0F && v <= 16.0F, file + " uv " + v);
                            }
                        }
                        if (f.has("rotation")) {
                            assertTrue(Set.of(0, 90, 180, 270).contains(f.get("rotation").getAsInt()), file + " rotação de face");
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ itens

    @Test
    void everyItemHasItemDefinitionModelAndTexture() {
        for (String item : ContentIds.allItems()) {
            List<String> models = itemModels(Resources.json(Resources.assets("items/" + item + ".json")).getAsJsonObject("model"));
            assertFalse(models.isEmpty(), item);
            models.forEach(AssetCompletenessTest::assertModelComplete);
        }
    }

    @Test
    void flatItemsHaveOwnTextures() {
        for (String item : ContentIds.flatItems()) {
            assertPng16(Resources.assets("textures/item/" + item + ".png"));
        }
    }

    /**
     * O ícone da estação é a estação vazia como ela fica colocada: o modelo do bloco e, na prensa, também a placa com o
     * parafuso (platen_up), que o bloco vazio desenha pelo multipart (minecraft:composite, como a cama vanilla).
     */
    @Test
    void stationItemsShowTheEmptyPlacedStation() {
        for (String station : ContentIds.STATIONS) {
            JsonObject def = Resources.json(Resources.assets("items/" + station + ".json")).getAsJsonObject("model");
            JsonObject model = Resources.json(Resources.assets("models/item/" + station + ".json"));
            assertEquals("cultivo:block/" + station, model.get("parent").getAsString());
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + station + ".json")).getAsJsonArray("multipart");
            Map<String, String> empty = new HashMap<>(Map.of("facing", "north", "contents", "empty"));
            if (ContentIds.stateProperties(station).containsKey("grinder")) {
                empty.put("grinder", "false");
            }
            List<String> placed = new ArrayList<>(appliedModels(multipart, empty));
            placed.set(0, "cultivo:item/" + station);
            assertEquals(placed, itemModels(def), station + ": o ícone e a estação vazia colocada");
        }
        JsonObject press = Resources.json(Resources.assets("items/press.json")).getAsJsonObject("model");
        assertEquals("minecraft:composite", press.get("type").getAsString());
        assertEquals(List.of("cultivo:item/press", "cultivo:block/press_platen_up"), itemModels(press));
        assertEquals("minecraft:block/block",
            Resources.json(Resources.assets("models/block/press_platen_up.json")).get("parent").getAsString(),
            "a placa usa as mesmas transformações de exibição da base");
    }

    /** O haxixe troca o carimbo pela família da variedade: cada variedade aparece uma vez, no modelo da sua família. */
    @Test
    void hashStampSelectsTheFamilyOfEveryStrain() {
        JsonObject def = Resources.json(Resources.assets("items/hash.json")).getAsJsonObject("model");
        assertEquals("minecraft:select", def.get("type").getAsString());
        assertEquals("minecraft:component", def.get("property").getAsString());
        assertEquals("cultivo:strain", def.get("component").getAsString());
        assertEquals("cultivo:item/hash", def.getAsJsonObject("fallback").get("model").getAsString());
        Map<String, String> modelByStrain = new HashMap<>();
        for (JsonElement c : def.getAsJsonArray("cases")) {
            JsonObject cs = c.getAsJsonObject();
            String model = cs.getAsJsonObject("model").get("model").getAsString();
            for (JsonElement when : cs.getAsJsonArray("when")) {
                assertTrue(modelByStrain.put(when.getAsString(), model) == null, "variedade repetida: " + when);
            }
        }
        assertEquals(Variety.values().length, modelByStrain.size());
        for (Variety v : Variety.values()) {
            assertEquals("cultivo:item/hash_" + v.artFamily(), modelByStrain.get(v.id()), v.id());
        }
        assertEquals("cultivo:item/hash_brisa", modelByStrain.get("brisa"));
        assertEquals("cultivo:item/hash_dourado", modelByStrain.get("manga_gelada"));
    }

    /**
     * Baseado: primeiro a piteira (minecraft:has_component cultivo:filtered), depois aceso enquanto o jogador fuma
     * (minecraft:using_item), como as condições aninhadas do items/bow.json vanilla. Em uso: ícone aceso no inventário,
     * modelo aceso na 1ª pessoa e, na 3ª, os quadros k0..k4 da animação e o modelo aceso na boca.
     */
    @Test
    void jointShowsTheFilterAndLightsUpWhileUsed() {
        JsonObject def = Resources.json(Resources.assets("items/joint.json")).getAsJsonObject("model");
        assertEquals("minecraft:condition", def.get("type").getAsString());
        assertEquals("minecraft:has_component", def.get("property").getAsString());
        assertEquals("cultivo:filtered", def.get("component").getAsString());
        JsonObject filtered = def.getAsJsonObject("on_true");
        JsonObject plain = def.getAsJsonObject("on_false");
        for (JsonObject branch : List.of(filtered, plain)) {
            assertEquals("minecraft:condition", branch.get("type").getAsString());
            assertEquals("minecraft:using_item", branch.get("property").getAsString());
        }
        for (Map.Entry<String, JsonObject> branch : Map.of("joint_filtered", filtered, "joint", plain).entrySet()) {
            String base = "cultivo:item/" + branch.getKey();
            JsonObject inUse = branch.getValue().getAsJsonObject("on_true");
            assertEquals("minecraft:select", inUse.get("type").getAsString());
            assertEquals("minecraft:display_context", inUse.get("property").getAsString());
            JsonArray cases = inUse.getAsJsonArray("cases");
            assertEquals(List.of(base + "_lit"), itemModels(cases.get(0).getAsJsonObject().getAsJsonObject("model")));
            assertEquals(List.of(base + "_in_hand_lit"), itemModels(cases.get(1).getAsJsonObject().getAsJsonObject("model")));
            Set<String> thirdPerson = new HashSet<>(itemModels(inUse.getAsJsonObject("fallback")));
            Set<String> expected = new HashSet<>(List.of(base + "_in_hand_lit"));
            for (int k = 0; k < io.github.peixeangola.cultivo.core.SmokeAnimation.KEYFRAMES; k++) {
                expected.add(base + "_in_hand_lit_k" + k);
            }
            assertEquals(expected, thirdPerson, branch.getKey());
            assertEquals(List.of(base, base + "_in_hand"), itemModels(branch.getValue().getAsJsonObject("on_false")));
        }

        JsonObject lit = Resources.json(Resources.assets("models/item/joint_in_hand_lit.json"));
        assertEquals("cultivo:item/smoke_ember", lit.getAsJsonObject("textures").get("tip").getAsString());
        // Na mão a boca é papel; com piteira, papelão (o resto da geometria e da exibição é herdado)
        assertEquals("cultivo:item/model_paper",
            Resources.json(Resources.assets("models/item/joint_in_hand.json")).getAsJsonObject("textures").get("crutch").getAsString());
        JsonObject filteredHand = Resources.json(Resources.assets("models/item/joint_filtered_in_hand.json"));
        assertEquals("cultivo:item/joint_in_hand", filteredHand.get("parent").getAsString());
        assertEquals("cultivo:item/model_cardboard", filteredHand.getAsJsonObject("textures").get("crutch").getAsString());
        JsonObject filteredLit = Resources.json(Resources.assets("models/item/joint_filtered_in_hand_lit.json"));
        assertEquals("cultivo:item/joint_in_hand_lit", filteredLit.get("parent").getAsString());
        assertEquals("cultivo:item/model_cardboard", filteredLit.getAsJsonObject("textures").get("crutch").getAsString());
    }

    /**
     * Aparelhos de fumo e isqueiro: ícone plano em inventário/chão/moldura e modelo 3D com transformações próprias de
     * 1ª e 3ª pessoa (o plano gerado aparecia como um palito na 3ª pessoa e como um borrão na 1ª).
     */
    @Test
    void smokingItemsHaveHandModelsWithTransforms() {
        for (String item : HAND_MODEL_ITEMS) {
            JsonObject def = Resources.json(Resources.assets("items/" + item + ".json")).getAsJsonObject("model");
            List<JsonObject> selects = new ArrayList<>();
            collectSelects(def, "minecraft:display_context", selects);
            assertFalse(selects.isEmpty(), item + ": sem select em display_context");
            for (JsonObject select : selects) {
                JsonObject flatCase = select.getAsJsonArray("cases").get(0).getAsJsonObject();
                List<String> when = new ArrayList<>();
                flatCase.getAsJsonArray("when").forEach(w -> when.add(w.getAsString()));
                assertEquals(FLAT_CONTEXTS, when, item);
                for (String flat : itemModels(flatCase.getAsJsonObject("model"))) {
                    assertEquals("minecraft:item/generated", Resources.json(Resources.cultivoResource(flat, "models", ".json"))
                        .get("parent").getAsString(), item + " " + flat);
                }
                for (String hand : itemModels(select.getAsJsonObject("fallback"))) {
                    // joint_in_hand, joint_in_hand_lit, joint_filtered_in_hand, joint_filtered_in_hand_lit...
                    assertTrue(hand.startsWith("cultivo:item/" + item + "_") && hand.contains("_in_hand"), item + " " + hand);
                    JsonObject display = resolvedDisplay(hand);
                    for (String context : List.of("thirdperson_righthand", "thirdperson_lefthand",
                        "firstperson_righthand", "firstperson_lefthand")) {
                        JsonObject t = display.getAsJsonObject(context);
                        assertTrue(t != null && t.has("rotation") && t.has("translation") && t.has("scale"), hand + " " + context);
                        for (JsonElement s : t.getAsJsonArray("scale")) {
                            assertTrue(s.getAsFloat() > 0.3F && s.getAsFloat() < 0.8F, hand + " " + context + " escala " + s);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ texturas

    @Test
    void noOrphanTextures() throws IOException {
        Set<String> referenced = new HashSet<>();
        try (Stream<Path> models = Files.walk(Resources.assets("models"))) {
            for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject textures = Resources.json(model).getAsJsonObject("textures");
                if (textures != null) {
                    textures.entrySet().forEach(e -> referenced.add(e.getValue().getAsString()));
                }
            }
        }
        try (Stream<Path> pngs = Files.walk(Resources.assets("textures"))) {
            for (Path png : pngs.filter(p -> p.toString().endsWith(".png")).toList()) {
                String rel = Resources.assets("textures").relativize(png).toString().replace('\\', '/');
                String id = "cultivo:" + rel.substring(0, rel.length() - ".png".length());
                if (rel.startsWith("mob_effect/")) {
                    // Ícones de efeito são achados pelo ID do efeito, não por modelo
                    assertTrue(ContentIds.EFFECTS.contains(rel.substring("mob_effect/".length(), rel.length() - 4)),
                        "ícone de efeito sem efeito registrado: " + id);
                    continue;
                }
                assertTrue(referenced.contains(id), "textura sem uso: " + id);
            }
        }
    }

    /** Diferença de canal (0–255) a partir da qual dois pixels opacos contam como visivelmente diferentes. */
    static final int VISIBLE_CHANNEL_DIFFERENCE = 24;
    /** Pixels visivelmente diferentes que dois ícones de variedades diferentes precisam ter, no mínimo. */
    static final int MIN_VISIBLE_PIXELS = 16;

    /**
     * Pixels em que dois ícones 16x16 diferem a olho: opaco num e transparente no outro, ou opaco nos dois com algum canal
     * a {@link #VISIBLE_CHANNEL_DIFFERENCE} ou mais de distância.
     */
    static int visiblyDifferentPixels(Png a, Png b) {
        int n = 0;
        for (int i = 0; i < a.argb().length; i++) {
            int pa = a.argb()[i];
            int pb = b.argb()[i];
            boolean oa = (pa >>> 24) != 0;
            boolean ob = (pb >>> 24) != 0;
            if (oa != ob) {
                n++;
            } else if (oa) {
                int channel = 0;
                for (int shift = 0; shift <= 16; shift += 8) {
                    channel = Math.max(channel, Math.abs(((pa >> shift) & 0xFF) - ((pb >> shift) & 0xFF)));
                }
                if (channel >= VISIBLE_CHANNEL_DIFFERENCE) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Sementes, flores frescas e flores secas de duas variedades quaisquer se distinguem a olho no inventário: pelo menos
     * {@link #MIN_VISIBLE_PIXELS} pixels visivelmente diferentes. Comparar bytes deixava passar ícones que diferiam em 1/255
     * de um canal (Ultravioleta, Dó-Si-Dó e Soco Roxo eram três sementes marrons iguais). A lavoura é por família de arte,
     * e duas famílias nunca têm o mesmo estágio igual.
     */
    @Test
    void strainAndFamilyTexturesAreDistinct() throws IOException {
        for (String kind : List.of("item/%s_seeds", "item/%s_fresh_flower", "item/%s_dried_flower")) {
            List<Variety> strains = List.of(Variety.values());
            List<Png> icons = strains.stream()
                .map(v -> Png.read(Resources.assets("textures/" + String.format(kind, v.id()) + ".png"))).toList();
            for (int i = 0; i < icons.size(); i++) {
                for (int j = i + 1; j < icons.size(); j++) {
                    int visible = visiblyDifferentPixels(icons.get(i), icons.get(j));
                    assertTrue(visible >= MIN_VISIBLE_PIXELS, String.format(kind, strains.get(i).id()) + " x "
                        + strains.get(j).id() + ": só " + visible + " pixels visivelmente diferentes");
                }
            }
        }
        for (int stage = 0; stage < 8; stage++) {
            Map<String, String> seen = new HashMap<>();
            for (String family : families()) {
                Path png = Resources.assets("textures/block/" + family + "_crop_stage" + stage + ".png");
                String bytes = HexFormat.of().formatHex(Files.readAllBytes(png));
                String other = seen.putIfAbsent(bytes, family);
                assertTrue(other == null, family + " estágio " + stage + " igual ao de " + other);
            }
        }
    }

    /** O critério a olho: um tom 1/255 diferente não conta; 16 pixels a mais na silhueta contam 16. */
    @Test
    void visibleDifferenceIgnoresTinyTints() {
        int[] brown = new int[256];
        int[] tinted = new int[256];
        int[] bigger = new int[256];
        for (int i = 0; i < 256; i++) {
            boolean seed = i < 40;
            brown[i] = seed ? 0xFF8A6A48 : 0;
            tinted[i] = seed ? 0xFF8B6A48 : 0;
            bigger[i] = i < 56 ? 0xFF8A6A48 : 0;
        }
        assertEquals(0, visiblyDifferentPixels(new Png(16, 16, brown), new Png(16, 16, tinted)));
        assertEquals(16, visiblyDifferentPixels(new Png(16, 16, brown), new Png(16, 16, bigger)));
        int[] purple = brown.clone();
        purple[0] = 0xFF8A6A48 + VISIBLE_CHANNEL_DIFFERENCE;
        assertEquals(1, visiblyDifferentPixels(new Png(16, 16, brown), new Png(16, 16, purple)));
    }

    // ------------------------------------------------------------------ traduções

    @Test
    void everyIdIsTranslatedInBothLanguages() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (String item : ContentIds.flatItems()) {
                assertTranslated(t, "item.cultivo." + item, lang);
            }
            for (String block : ContentIds.allBlocks()) {
                assertTranslated(t, "block.cultivo." + block, lang);
            }
            assertTranslated(t, "itemGroup.cultivo", lang);
            assertTranslated(t, "itemGroup.cultivo.strains", lang);
        }
    }

    @Test
    void languagesHaveSameKeys() {
        Set<String> en = Resources.json(Resources.assets("lang/en_us.json")).keySet();
        Set<String> pt = Resources.json(Resources.assets("lang/pt_br.json")).keySet();
        assertEquals(en, pt);
    }

    @Test
    void translationKeysUsedInJavaExist() throws IOException {
        Pattern literal = Pattern.compile("\"((?:tooltip|itemGroup|message|effect)\\.cultivo[a-z0-9_.]*)\"");
        Set<String> en = Resources.json(Resources.assets("lang/en_us.json")).keySet();
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Resources.JAVA)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = literal.matcher(Files.readString(file));
                while (m.find()) {
                    String key = m.group(1);
                    if (key.endsWith(".")) {
                        // prefixo concatenado em Java: precisa haver ao menos uma chave com ele
                        if (en.stream().noneMatch(k -> k.startsWith(key))) {
                            missing.add(key + "*");
                        }
                    } else if (TooltipHints.LINES.containsKey(key)) {
                        // Dica em linhas curtas: o Java cita a chave base e o lang tem <chave>.1 … <chave>.n
                        if (!en.containsAll(TooltipHints.lineKeys(key))) {
                            missing.add(key + ".1…");
                        }
                    } else if (!en.contains(key)) {
                        missing.add(key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "chaves sem tradução: " + missing);
    }

    @Test
    void qualityAndProfileTooltipKeysExist() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (int q = 1; q <= 5; q++) {
                assertTranslated(t, "tooltip.cultivo.quality." + q, lang);
            }
            for (Variety v : Variety.values()) {
                assertTranslated(t, "tooltip.cultivo.rarity." + v.rarity().name().toLowerCase(), lang);
                assertTranslated(t, "tooltip.cultivo.cycle." + v.cycle().name().toLowerCase(), lang);
                assertTranslated(t, "tooltip.cultivo.lineage." + v.lineage().name().toLowerCase(), lang);
                assertTranslated(t, "tooltip.cultivo.potency." + v.potency(), lang);
            }
            // Raridade · ciclo · genética · potência
            assertEquals("%s · %s · %s · %s", t.get("tooltip.cultivo.strain_profile").getAsString(), lang);
        }
        JsonObject pt = Resources.json(Resources.assets("lang/pt_br.json"));
        assertEquals("Comum", pt.get("tooltip.cultivo.rarity.common").getAsString());
        assertEquals("Automática", pt.get("tooltip.cultivo.cycle.autoflowering").getAsString());
        assertEquals("Sativa", pt.get("tooltip.cultivo.lineage.sativa").getAsString());
        assertEquals("Potência ★☆☆", pt.get("tooltip.cultivo.potency.1").getAsString());
        assertEquals("Potência ★★☆", pt.get("tooltip.cultivo.potency.2").getAsString());
        assertEquals("Potência ★★★", pt.get("tooltip.cultivo.potency.3").getAsString());
        assertEquals("Cultivo: Genéticas", pt.get("itemGroup.cultivo.strains").getAsString());
    }

    @Test
    void everyEffectHasIconAndTranslation() {
        for (String effect : ContentIds.EFFECTS) {
            assertPng(Resources.assets("textures/mob_effect/" + effect + ".png"), 18);
            for (String lang : LANGS) {
                assertTranslated(Resources.json(Resources.assets("lang/" + lang + ".json")), "effect.cultivo." + effect, lang);
            }
        }
    }

    @Test
    void smokingMessagesAndVarietyNamesTranslated() {
        for (String lang : LANGS) {
            JsonObject t = Resources.json(Resources.assets("lang/" + lang + ".json"));
            for (io.github.peixeangola.cultivo.core.SmokeRules.Refusal refusal
                : io.github.peixeangola.cultivo.core.SmokeRules.Refusal.values()) {
                if (refusal.messageKey() != null) {
                    assertTranslated(t, refusal.messageKey(), lang);
                }
            }
            for (String key : List.of("message.cultivo.lighter_broke",
                "tooltip.cultivo.strain", "tooltip.cultivo.filtered", "tooltip.cultivo.cured")) {
                assertTranslated(t, key, lang);
            }
            for (String hint : List.of("tooltip.cultivo.smoke.joint", "tooltip.cultivo.smoke.device", "tooltip.cultivo.lighter")) {
                TooltipHints.lineKeys(hint).forEach(line -> assertTranslated(t, line, lang));
            }
            for (io.github.peixeangola.cultivo.core.RollingRules.Refusal refusal
                : io.github.peixeangola.cultivo.core.RollingRules.Refusal.values()) {
                if (refusal.messageKey() != null) {
                    assertTranslated(t, refusal.messageKey(), lang);
                }
            }
            for (Variety v : Variety.values()) {
                assertTranslated(t, "tooltip.cultivo.variety." + v.id(), lang);
            }
        }
    }

    // ------------------------------------------------------------------ apoio

    /** Famílias de arte da tabela de variedades, na ordem de primeira aparição. */
    static List<String> families() {
        Set<String> families = new LinkedHashSet<>();
        for (Variety v : Variety.values()) {
            families.add(v.artFamily());
        }
        return List.copyOf(families);
    }

    /** Todos os estados (combinações de valores) de um mapa de propriedades. */
    static List<Map<String, String>> allStates(Map<String, List<String>> properties) {
        List<Map<String, String>> states = new ArrayList<>();
        states.add(Map.of());
        for (Map.Entry<String, List<String>> property : properties.entrySet()) {
            List<Map<String, String>> next = new ArrayList<>();
            for (Map<String, String> partial : states) {
                for (String value : property.getValue()) {
                    Map<String, String> state = new HashMap<>(partial);
                    state.put(property.getKey(), value);
                    next.add(state);
                }
            }
            states = next;
        }
        return states;
    }

    /** Modelos aplicados por um multipart num estado, na ordem do JSON. */
    private static List<String> appliedModels(JsonArray multipart, Map<String, String> state) {
        List<String> models = new ArrayList<>();
        for (JsonElement part : multipart) {
            JsonObject p = part.getAsJsonObject();
            if (matches(p.getAsJsonObject("when"), state)) {
                models.add(p.getAsJsonObject("apply").get("model").getAsString());
            }
        }
        return models;
    }

    private static boolean matches(JsonObject when, Map<String, String> state) {
        if (when == null) {
            return true;
        }
        for (Map.Entry<String, JsonElement> term : when.entrySet()) {
            List<String> values = List.of(term.getValue().getAsString().split("\\|"));
            if (!values.contains(state.get(term.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** Modelos citados por um blockstate (variants ou multipart). */
    private static List<String> blockstateModels(String block) {
        JsonObject state = Resources.json(Resources.assets("blockstates/" + block + ".json"));
        List<String> models = new ArrayList<>();
        if (state.has("variants")) {
            for (Map.Entry<String, JsonElement> variant : state.getAsJsonObject("variants").entrySet()) {
                models.add(variant.getValue().getAsJsonObject().get("model").getAsString());
            }
        }
        if (state.has("multipart")) {
            for (JsonElement part : state.getAsJsonArray("multipart")) {
                models.add(part.getAsJsonObject().getAsJsonObject("apply").get("model").getAsString());
            }
        }
        return models;
    }

    /** Modelos folha de uma definição de item do 26.3 (model, condition, select), na ordem do JSON. */
    private static List<String> itemModels(JsonObject node) {
        List<String> out = new ArrayList<>();
        switch (node.get("type").getAsString()) {
            case "minecraft:model" -> out.add(node.get("model").getAsString());
            case "minecraft:condition" -> {
                out.addAll(itemModels(node.getAsJsonObject("on_true")));
                out.addAll(itemModels(node.getAsJsonObject("on_false")));
            }
            case "minecraft:select" -> {
                for (JsonElement c : node.getAsJsonArray("cases")) {
                    out.addAll(itemModels(c.getAsJsonObject().getAsJsonObject("model")));
                }
                if (node.has("fallback")) {
                    out.addAll(itemModels(node.getAsJsonObject("fallback")));
                }
            }
            case "minecraft:range_dispatch" -> {
                for (JsonElement e : node.getAsJsonArray("entries")) {
                    out.addAll(itemModels(e.getAsJsonObject().getAsJsonObject("model")));
                }
                if (node.has("fallback")) {
                    out.addAll(itemModels(node.getAsJsonObject("fallback")));
                }
            }
            case "minecraft:composite" -> {
                for (JsonElement e : node.getAsJsonArray("models")) {
                    out.addAll(itemModels(e.getAsJsonObject()));
                }
            }
            default -> fail("tipo de modelo de item sem suporte no teste: " + node.get("type"));
        }
        return out;
    }

    private static void collectSelects(JsonObject node, String property, List<JsonObject> out) {
        String type = node.get("type").getAsString();
        if (type.equals("minecraft:select") && property.equals(node.get("property").getAsString())) {
            out.add(node);
            return;
        }
        if (type.equals("minecraft:condition")) {
            collectSelects(node.getAsJsonObject("on_true"), property, out);
            collectSelects(node.getAsJsonObject("on_false"), property, out);
        } else if (type.equals("minecraft:range_dispatch")) {
            for (JsonElement e : node.getAsJsonArray("entries")) {
                collectSelects(e.getAsJsonObject().getAsJsonObject("model"), property, out);
            }
            if (node.has("fallback")) {
                collectSelects(node.getAsJsonObject("fallback"), property, out);
            }
        } else if (type.equals("minecraft:select")) {
            for (JsonElement c : node.getAsJsonArray("cases")) {
                collectSelects(c.getAsJsonObject().getAsJsonObject("model"), property, out);
            }
            if (node.has("fallback")) {
                collectSelects(node.getAsJsonObject("fallback"), property, out);
            }
        }
    }

    /** Transformações de exibição herdadas pela cadeia de pais do mod (o filho sobrescreve contexto a contexto). */
    private static JsonObject resolvedDisplay(String modelId) {
        JsonObject model = Resources.json(Resources.cultivoResource(modelId, "models", ".json"));
        JsonObject display = new JsonObject();
        if (model.has("parent") && model.get("parent").getAsString().startsWith("cultivo:")) {
            resolvedDisplay(model.get("parent").getAsString()).entrySet().forEach(e -> display.add(e.getKey(), e.getValue()));
        }
        if (model.has("display")) {
            model.getAsJsonObject("display").entrySet().forEach(e -> display.add(e.getKey(), e.getValue()));
        }
        return display;
    }

    private static void assertTranslated(JsonObject lang, String key, String langName) {
        JsonElement value = lang.get(key);
        assertTrue(value != null && !value.getAsString().isBlank(), langName + " sem " + key);
    }

    /**
     * O modelo existe; a cadeia de pais termina num pai vanilla conhecido (cujas variáveis o mod define) ou num modelo
     * com elementos; toda textura citada existe em 16x16; e todo "#ref" das faces resolve para uma textura.
     */
    private static void assertModelComplete(String modelId) {
        if (!modelId.startsWith("cultivo:")) {
            fail("modelo fora do mod: " + modelId);
        }
        Map<String, String> textures = new HashMap<>();
        List<JsonArray> elementLists = new ArrayList<>();
        String vanillaParent = collectChain(modelId, textures, elementLists);
        if (vanillaParent != null) {
            assertTrue(VANILLA_PARENTS.containsKey(vanillaParent), modelId + ": pai vanilla sem verificação " + vanillaParent);
            for (String var : VANILLA_PARENTS.get(vanillaParent)) {
                assertTrue(textures.containsKey(var), modelId + ": falta a textura '" + var + "' de " + vanillaParent);
            }
        } else {
            assertFalse(elementLists.isEmpty(), modelId + ": sem pai e sem elementos");
        }
        for (String value : textures.values()) {
            if (!value.startsWith("#")) {
                assertPng16(Resources.cultivoResource(value, "textures", ".png"));
            }
        }
        // Só os elementos efetivos (os do modelo mais próximo que define elements) são desenhados
        if (!elementLists.isEmpty()) {
            for (JsonElement e : elementLists.get(0)) {
                for (Map.Entry<String, JsonElement> face : e.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    String ref = face.getValue().getAsJsonObject().get("texture").getAsString();
                    assertPng16(Resources.cultivoResource(resolve(modelId, textures, ref), "textures", ".png"));
                }
            }
        }
        if (textures.containsKey("particle")) {
            assertPng16(Resources.cultivoResource(resolve(modelId, textures, textures.get("particle")), "textures", ".png"));
        }
    }

    /** Junta texturas (filho por cima) e listas de elementos da cadeia; devolve o pai vanilla do fim, se houver. */
    private static String collectChain(String modelId, Map<String, String> textures, List<JsonArray> elementLists) {
        JsonObject model = Resources.json(Resources.cultivoResource(modelId, "models", ".json"));
        if (model.has("textures")) {
            for (Map.Entry<String, JsonElement> t : model.getAsJsonObject("textures").entrySet()) {
                textures.putIfAbsent(t.getKey(), t.getValue().getAsString());
            }
        }
        if (model.has("elements")) {
            elementLists.add(model.getAsJsonArray("elements"));
        }
        if (!model.has("parent")) {
            return null;
        }
        String parent = model.get("parent").getAsString();
        return parent.startsWith("cultivo:") ? collectChain(parent, textures, elementLists) : parent;
    }

    private static String resolve(String modelId, Map<String, String> textures, String ref) {
        String value = ref;
        for (int i = 0; i < 10 && value.startsWith("#"); i++) {
            String next = textures.get(value.substring(1));
            assertTrue(next != null, modelId + ": referência sem textura " + value);
            value = next;
        }
        assertFalse(value.startsWith("#"), modelId + ": referência circular " + ref);
        return value;
    }

    static void assertPng16(Path png) {
        assertPng(png, 16);
    }

    /**
     * PNG quadrado de {@code size} px. Uma textura animada (com &lt;nome&gt;.png.mcmeta ao lado) é uma pilha vertical de
     * quadros quadrados: a altura é um múltiplo da largura (AnimatedTexturesTest confere o .mcmeta).
     */
    static void assertPng(Path png, int size) {
        try (InputStream in = Files.newInputStream(png)) {
            byte[] header = in.readNBytes(24);
            assertEquals(0x89, header[0] & 0xff, png + " não é PNG");
            assertEquals("IHDR", new String(header, 12, 4));
            ByteBuffer buf = ByteBuffer.wrap(header);
            assertEquals(size, buf.getInt(16), png + " largura");
            int height = buf.getInt(20);
            if (Files.exists(Path.of(png + ".mcmeta"))) {
                assertTrue(height >= 2 * size && height % size == 0, png + " animada com altura " + height);
            } else {
                assertEquals(size, height, png + " altura");
            }
        } catch (IOException e) {
            fail("textura faltando: " + png);
        }
    }
}
