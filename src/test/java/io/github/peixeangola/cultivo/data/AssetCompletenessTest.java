package io.github.peixeangola.cultivo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.peixeangola.cultivo.core.ContentIds;
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
        Map<String, Integer> expectedY = Map.of("north", 0, "east", 90, "south", 180, "west", 270);
        for (String station : ContentIds.STATIONS) {
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + station + ".json")).getAsJsonArray("multipart");
            for (String facing : ContentIds.STATION_STATE_PROPERTIES.get("facing")) {
                for (String contents : ContentIds.STATION_STATE_PROPERTIES.get("contents")) {
                    Map<String, String> state = Map.of("facing", facing, "contents", contents);
                    int bases = 0;
                    for (JsonElement part : multipart) {
                        JsonObject p = part.getAsJsonObject();
                        if (!matches(p.getAsJsonObject("when"), state)) {
                            continue;
                        }
                        JsonObject apply = p.getAsJsonObject("apply");
                        int y = apply.has("y") ? apply.get("y").getAsInt() : 0;
                        assertEquals(expectedY.get(facing), y, station + " " + state + " " + apply);
                        if (apply.get("model").getAsString().equals("cultivo:block/" + station)) {
                            bases++;
                        }
                    }
                    assertEquals(1, bases, station + " " + state + ": a base aparece uma vez");
                }
            }
        }
    }

    @Test
    void multipartConditionsUseExistingProperties() {
        for (String station : ContentIds.STATIONS) {
            JsonArray multipart = Resources.json(Resources.assets("blockstates/" + station + ".json")).getAsJsonArray("multipart");
            Set<String> seenContents = new HashSet<>();
            for (JsonElement part : multipart) {
                JsonObject when = part.getAsJsonObject().getAsJsonObject("when");
                assertTrue(when != null, station + ": parte sem when");
                for (Map.Entry<String, JsonElement> term : when.entrySet()) {
                    List<String> allowed = ContentIds.STATION_STATE_PROPERTIES.get(term.getKey());
                    assertTrue(allowed != null, station + ": propriedade inexistente " + term.getKey());
                    for (String value : term.getValue().getAsString().split("\\|")) {
                        String plain = value.startsWith("!") ? value.substring(1) : value;
                        assertTrue(allowed.contains(plain), station + ": " + term.getKey() + "=" + plain);
                        if (term.getKey().equals("contents")) {
                            seenContents.add(plain);
                        }
                    }
                }
            }
            // Toda estação tem parte para trabalhando e para pronta (vazia é a base sozinha, exceto na prensa)
            assertTrue(seenContents.containsAll(List.of("working", "done")), station + " " + seenContents);
        }
        assertEquals(Set.of("north", "south", "west", "east"), Set.copyOf(ContentIds.STATION_STATE_PROPERTIES.get("facing")));
        assertEquals(List.of("empty", "working", "done"), ContentIds.STATION_STATE_PROPERTIES.get("contents"));
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

    @Test
    void stationItemsUseTheBlockModel() {
        for (String station : ContentIds.STATIONS) {
            JsonObject def = Resources.json(Resources.assets("items/" + station + ".json")).getAsJsonObject("model");
            assertEquals("cultivo:item/" + station, def.get("model").getAsString());
            JsonObject model = Resources.json(Resources.assets("models/item/" + station + ".json"));
            assertEquals("cultivo:block/" + station, model.get("parent").getAsString());
        }
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

    /** Baseado aceso enquanto o jogador fuma (minecraft:using_item), no ícone e na mão. */
    @Test
    void jointLightsUpWhileUsed() {
        JsonObject def = Resources.json(Resources.assets("items/joint.json")).getAsJsonObject("model");
        assertEquals("minecraft:condition", def.get("type").getAsString());
        assertEquals("minecraft:using_item", def.get("property").getAsString());
        assertEquals(List.of("cultivo:item/joint_lit", "cultivo:item/joint_in_hand_lit"), itemModels(def.getAsJsonObject("on_true")));
        assertEquals(List.of("cultivo:item/joint", "cultivo:item/joint_in_hand"), itemModels(def.getAsJsonObject("on_false")));
        JsonObject lit = Resources.json(Resources.assets("models/item/joint_in_hand_lit.json"));
        assertEquals("cultivo:item/smoke_ember", lit.getAsJsonObject("textures").get("tip").getAsString());
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
                    assertTrue(hand.startsWith("cultivo:item/" + item + "_in_hand"), item + " " + hand);
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

    /**
     * Sementes e flores são por variedade e nunca saem iguais; a lavoura é por família de arte, e duas famílias
     * nunca têm o mesmo estágio igual.
     */
    @Test
    void strainAndFamilyTexturesAreDistinct() throws IOException {
        for (String kind : List.of("item/%s_seeds", "item/%s_fresh_flower", "item/%s_dried_flower")) {
            Map<String, String> seen = new HashMap<>();
            for (Variety v : Variety.values()) {
                Path png = Resources.assets("textures/" + String.format(kind, v.id()) + ".png");
                String bytes = HexFormat.of().formatHex(Files.readAllBytes(png));
                String other = seen.putIfAbsent(bytes, v.id());
                assertTrue(other == null, String.format(kind, v.id()) + " igual à de " + other);
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
                "tooltip.cultivo.smoke.joint", "tooltip.cultivo.smoke.device", "tooltip.cultivo.lighter",
                "tooltip.cultivo.strain")) {
                assertTranslated(t, key, lang);
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

    static void assertPng(Path png, int size) {
        try (InputStream in = Files.newInputStream(png)) {
            byte[] header = in.readNBytes(24);
            assertEquals(0x89, header[0] & 0xff, png + " não é PNG");
            assertEquals("IHDR", new String(header, 12, 4));
            ByteBuffer buf = ByteBuffer.wrap(header);
            assertEquals(size, buf.getInt(16), png + " largura");
            assertEquals(size, buf.getInt(20), png + " altura");
        } catch (IOException e) {
            fail("textura faltando: " + png);
        }
    }
}
